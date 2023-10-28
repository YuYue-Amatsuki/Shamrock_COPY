package moe.fuqiuluo.http.api

import com.tencent.mobileqq.sign.QQSecuritySign.SignResult
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import moe.fuqiuluo.xposed.helper.internal.DynamicReceiver
import moe.fuqiuluo.xposed.helper.internal.IPCRequest
import moe.fuqiuluo.xposed.tools.broadcast
import moe.fuqiuluo.xposed.tools.fetchGetOrThrow
import moe.fuqiuluo.xposed.tools.fetchPostOrThrow
import moe.fuqiuluo.xposed.tools.hex2ByteArray
import moe.fuqiuluo.xposed.tools.toHexString
import mqq.app.MobileQQ
import kotlin.coroutines.resume

fun Routing.sign() {
    get("/sign") {
        val uin = fetchGetOrThrow("uin")
        val cmd = fetchGetOrThrow("cmd")
        val seq = fetchGetOrThrow("seq").toInt()
        val buffer = fetchGetOrThrow("buffer").hex2ByteArray()

        requestSign(cmd, uin, seq, buffer)
    }

    post("/sign") {
        val uin = fetchPostOrThrow("uin")
        val cmd = fetchPostOrThrow("cmd")
        val seq = fetchPostOrThrow("seq").toInt()
        val buffer = fetchPostOrThrow("buffer").hex2ByteArray()

        requestSign(cmd, uin, seq, buffer)
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

private val reqLock = Mutex() // 懒得做高并发支持，写个锁，能用就行

private suspend fun PipelineContext<Unit, ApplicationCall>.requestSign(
    cmd: String,
    uin: String,
    seq: Int,
    buffer: ByteArray,
) {
    val sign = reqLock.withLock {
        withTimeoutOrNull(5000) {
            suspendCancellableCoroutine { con ->
                DynamicReceiver.register("sign_callback", IPCRequest {
                    con.resume(SignResult().apply {
                        this.sign = it.getByteArrayExtra("sign") ?: error("无法获取SIGN")
                        this.token = it.getByteArrayExtra("token")
                        this.extra = it.getByteArrayExtra("extra")
                    })
                })
                MobileQQ.getContext().broadcast("msf") {
                    putExtra("__cmd", "sign")
                    putExtra("wupCmd", cmd)
                    putExtra("uin", uin)
                    putExtra("seq", seq)
                    putExtra("buffer", buffer)
                }
                con.invokeOnCancellation {
                    DynamicReceiver.unregister("sign")
                    con.resume(SignResult())
                }
            }
        }
    } ?: SignResult()

    call.respond(
        OldApiResult(0, "success",
            Sign(
                sign.token.toHexString(),
                sign.extra.toHexString(),
                sign.sign.toHexString(),
                "",
                listOf()
            )
        )
    )
}