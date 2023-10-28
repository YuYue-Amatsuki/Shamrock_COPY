package moe.fuqiuluo.remote.api

import com.tencent.mobileqq.qsec.qsecdandelionsdk.Dandelion
import com.tencent.mobileqq.qsec.qsecurity.QSec
import com.tencent.secprotocol.ByteData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import moe.fuqiuluo.remote.action.handlers.ModifyTroopName
import moe.fuqiuluo.remote.entries.EmptyObject
import moe.fuqiuluo.remote.entries.Status
import moe.fuqiuluo.xposed.ipc.qsign.IQSigner
import moe.fuqiuluo.xposed.ipc.ShamrockIpc
import moe.fuqiuluo.xposed.ipc.bytedata.IByteData
import moe.fuqiuluo.xposed.tools.EMPTY_BYTE_ARRAY
import moe.fuqiuluo.xposed.tools.fetchGetOrThrow
import moe.fuqiuluo.xposed.tools.fetchOrNull
import moe.fuqiuluo.xposed.tools.fetchOrThrow
import moe.fuqiuluo.xposed.tools.fetchPostOrThrow
import moe.fuqiuluo.xposed.tools.getOrPost
import moe.fuqiuluo.xposed.tools.hex2ByteArray
import moe.fuqiuluo.xposed.tools.respond
import moe.fuqiuluo.xposed.tools.toHexString
import mqq.app.MobileQQ
import java.nio.ByteBuffer

private var signer: IQSigner? = null
private var byteData: IByteData? = null

fun Routing.qsign() {
    getOrPost("/get_xw_debug_id") {
        if (signer == null || signer?.asBinder()?.isBinderAlive == false) {
            if (!initSigner()) {
                respond(false, Status.InternalHandlerError)
                return@getOrPost
            }
        }

        val uin = fetchOrThrow("uin")
        val data = fetchGetOrThrow("data")

        lateinit var start: String
        lateinit var end: String

        data.split("_").let {
            start = it[0]
            end = it[1]
        }
        val xwDebugId = signer!!.xwDebugId(uin, start, end)

        call.respond(OldApiResult(0, "success", xwDebugId.toHexString()))
    }

    route("/sign") {
        get {
            val uin = fetchGetOrThrow("uin")
            val cmd = fetchGetOrThrow("cmd")
            val seq = fetchGetOrThrow("seq").toInt()
            val buffer = fetchGetOrThrow("buffer").hex2ByteArray()

            requestSign(cmd, uin, seq, buffer)
        }
        post {
            val uin = fetchPostOrThrow("uin")
            val cmd = fetchPostOrThrow("cmd")
            val seq = fetchPostOrThrow("seq").toInt()
            val buffer = fetchPostOrThrow("buffer").hex2ByteArray()

            requestSign(cmd, uin, seq, buffer)
        }
    }

    get("/custom_energy") {
        val data = fetchGetOrThrow("data")
        val salt = fetchGetOrThrow("salt").hex2ByteArray()

        val sign = Dandelion.getInstance().fly(data, salt)
        call.respond(OldApiResult(0, "success", sign.toHexString()))
    }

    route("/energy") {
        get {
            val data = fetchGetOrThrow("data")
            if(!(data.startsWith("810_") || data.startsWith("812_"))) {
                call.respond(OldApiResult(-2, "data参数不合法", null))
                return@get
            }

            val uin = fetchOrThrow("uin")
            val salt = fetchSalt(data, uin)
            if (salt.isEmpty()) {
                call.respond(OldApiResult(-2, "无法自动决断mode，请主动提供", null))
                return@get
            }

            val sign = Dandelion.getInstance().fly(data, salt)
            call.respond(OldApiResult(0, "success", sign.toHexString()))
        }
        post {
            val data = fetchPostOrThrow("data")
            if(!(data.startsWith("810_") || data.startsWith("812_"))) {
                call.respond(OldApiResult(-2, "data参数不合法", null))
                return@post
            }
            val uin = fetchOrThrow("uin")
            val salt = fetchSalt(data, uin)
            if (salt.isEmpty()) {
                call.respond(OldApiResult(-2, "无法自动决断mode，请主动提供", null))
                return@post
            }

            val sign = Dandelion.getInstance().fly(data, salt)
            call.respond(OldApiResult(0, "success", sign.toHexString()))
        }
    }

    get("/get_byte") {
        if (byteData == null || byteData?.asBinder()?.isBinderAlive == false) {
            val binder = ShamrockIpc.get(ShamrockIpc.IPC_BYTEDATA)
            if (binder == null) {
                call.respond(OldApiResult(-2, "获取失败", null))
                return@get
            } else {
                byteData = IByteData.Stub.asInterface(binder)
                binder.linkToDeath({
                    byteData = null
                }, 0)
            }
        }

        val data = fetchGetOrThrow("data")
        if(!(data.startsWith("810_") || data.startsWith("812_"))) {
            call.respond(OldApiResult(-2, "data参数不合法", null))
            return@get
        }

        val uin = fetchOrThrow("uin")
        val salt = fetchSalt(data, uin)
        if (salt.isEmpty()) {
            call.respond(OldApiResult(-2, "无法自动决断mode，请主动提供", null))
            return@get
        }

        val sign = byteData!!.sign(uin, data, salt).sign

        if (sign == null) {
            call.respond(OldApiResult(-2, "获取失败", null))
        } else {
            call.respond(OldApiResult(0, "success", sign.toHexString()))
        }
    }
}

private suspend inline fun PipelineContext<Unit, ApplicationCall>.fetchSalt(
    data: String,
    uin: String
): ByteArray {
    var mode = fetchOrNull("mode")
    if (mode == null) {
        mode = when(data) {
            "810_d", "810_a", "810_f", "810_9" -> "v2"
            "810_2", "810_25", "810_7", "810_24" -> "v1"
            "812_b", "812_a" -> "v3"
            "812_5" -> "v4"
            else -> null
        }
    }
    if (mode == null) {
        return EMPTY_BYTE_ARRAY
    }

    val version = fetchOrThrow("version")
    if (!version.startsWith("6.0.0")) {
        throw RuntimeException("version参数应该是6.0.0开头")
    }

    return when (mode) {
        "v1" -> {
            val guid = fetchOrThrow("guid").hex2ByteArray()
            val salt = ByteBuffer.allocate(8 + 2 + guid.size + 2 + 10 + 4)
            val sub = data.substring(4).toInt(16)
            salt.putLong(uin.toLong())
            salt.putShort(guid.size.toShort())
            salt.put(guid)
            salt.putShort(version.length.toShort())
            salt.put(version.toByteArray())
            salt.putInt(sub)
            salt.array()
        }
        "v2" -> {
            val guid = fetchOrThrow("guid").hex2ByteArray()
            val sub = data.substring(4).toInt(16)
            val salt = ByteBuffer.allocate(4 + 2 + guid.size + 2 + 10 + 4 + 4)
            salt.putInt(0)
            salt.putShort(guid.size.toShort())
            salt.put(guid)
            salt.putShort(version.length.toShort())
            salt.put(version.toByteArray())
            salt.putInt(sub)
            salt.putInt(0)
            salt.array()
        }
        "v3" -> { // 812_a
            val phone = fetchOrThrow("phone").toByteArray() // 86-xxx
            val salt = ByteBuffer.allocate(phone.size + 2 + 2 + version.length + 2)
            salt.put(phone)
            //println(String(phone))
            salt.putShort(0)
            salt.putShort(version.length.toShort())
            salt.put(version.toByteArray())
            salt.putShort(0)
            salt.array()
        }
        "v4" -> { // 812_5
            error("Not support [v4] mode.")
        }
        else -> EMPTY_BYTE_ARRAY
    }
}

@Serializable
private data class Sign(
    val token: String,
    val extra: String,
    val sign: String,
    val o3did: String,
    val requestCallback: List<Int>
)

private suspend fun initSigner(): Boolean {
    val binder = ShamrockIpc.get(ShamrockIpc.IPC_QSIGN)
    if (binder == null) {
        //respond(false, Status.InternalHandlerError)
        return false
    } else {
        signer = IQSigner.Stub.asInterface(binder)
        binder.linkToDeath({
            signer = null
        }, 0)
        return true
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.requestSign(
    cmd: String,
    uin: String,
    seq: Int,
    buffer: ByteArray,
) {
    if (signer == null || signer?.asBinder()?.isBinderAlive == false) {
        if (!initSigner()) {
            respond(false, Status.InternalHandlerError)
            return
        }
    }

    val sign = withTimeoutOrNull(5000) {
        signer!!.sign(cmd, seq, uin, buffer)
    } ?: run {
        respond(false, Status.IAmTired)
        return
    }

    call.respond(OldApiResult(0, "success", Sign(
        sign.token.toHexString(),
        sign.extra.toHexString(),
        sign.sign.toHexString(), "", listOf()
    )))
}

