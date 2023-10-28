package moe.fuqiuluo.shamrock.remote.api

import moe.fuqiuluo.shamrock.helper.MessageHelper
import com.tencent.qqnt.kernel.nativeinterface.MsgConstant
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import moe.fuqiuluo.shamrock.remote.action.handlers.*
import moe.fuqiuluo.shamrock.tools.fetchGetOrNull
import moe.fuqiuluo.shamrock.tools.fetchGetOrThrow
import moe.fuqiuluo.shamrock.tools.fetchOrThrow
import moe.fuqiuluo.shamrock.tools.fetchPostJsonArray
import moe.fuqiuluo.shamrock.tools.fetchPostJsonObject
import moe.fuqiuluo.shamrock.tools.fetchPostJsonString
import moe.fuqiuluo.shamrock.tools.fetchPostOrNull
import moe.fuqiuluo.shamrock.tools.fetchPostOrThrow
import moe.fuqiuluo.shamrock.tools.getOrPost
import moe.fuqiuluo.shamrock.tools.isJsonData
import moe.fuqiuluo.shamrock.tools.isString

fun Routing.messageAction() {
    getOrPost("/delete_msg") {
        val msgHash = fetchOrThrow("message_id").toInt()
        call.respondText(DeleteMessage(msgHash))
    }

    getOrPost("/get_msg") {
        val msgHash = fetchOrThrow("message_id").toInt()
        call.respondText(GetMsg(msgHash))
    }

    route("/(send_msg|send_message)".toRegex()) {
        get {
            val msgType = fetchGetOrThrow("message_type")
            val message = fetchGetOrThrow("message")
            val autoEscape = fetchGetOrNull("auto_escape")?.toBooleanStrict() ?: false
            val peerIdKey = if(msgType == "group") "group_id" else "user_id"
            val chatType = MessageHelper.obtainMessageTypeByDetailType(msgType)
            call.respondText(SendMessage(chatType, fetchGetOrThrow(peerIdKey), message, autoEscape))
        }
        post {
            val msgType = fetchPostOrThrow("message_type")
            val peerIdKey = if(msgType == "group") "group_id" else "user_id"
            val chatType = MessageHelper.obtainMessageTypeByDetailType(msgType)
            call.respondText(if (isJsonData() && !isString("message")) {
                SendMessage(chatType, fetchPostOrThrow(peerIdKey), fetchPostJsonArray("message"))
            } else {
                val autoEscape = fetchPostOrNull("auto_escape")?.toBooleanStrict() ?: false
                SendMessage(chatType, fetchPostOrThrow(peerIdKey), fetchPostOrThrow("message"), autoEscape)
            })
        }
    }

    route("/send_group_msg") {
        get {
            val groupId = fetchGetOrThrow("group_id")
            val message = fetchGetOrThrow("message")
            val autoEscape = fetchGetOrNull("auto_escape")?.toBooleanStrict() ?: false
            call.respondText(SendMessage(MsgConstant.KCHATTYPEGROUP, groupId, message, autoEscape))
        }
        post {
            val groupId = fetchPostOrThrow("group_id")

            val autoEscape = fetchPostOrNull("auto_escape")?.toBooleanStrict() ?: false

            val result = if (isJsonData()) {
                if (isString("message")) {
                    SendMessage(MsgConstant.KCHATTYPEGROUP, groupId, fetchPostJsonString("message"), autoEscape)
                } else {
                    SendMessage(MsgConstant.KCHATTYPEGROUP, groupId, fetchPostJsonArray("message"))
                }
            } else {
                SendMessage(MsgConstant.KCHATTYPEGROUP, groupId, fetchPostOrThrow("message"), autoEscape)
            }

            call.respondText(result)
        }
    }

    route("/send_private_msg") {
        get {
            val userId = fetchGetOrThrow("user_id")
            val message = fetchGetOrThrow("message")
            val autoEscape = fetchGetOrNull("auto_escape")?.toBooleanStrict() ?: false
            call.respondText(SendMessage(MsgConstant.KCHATTYPEC2C, userId, message, autoEscape))
        }
        post {
            val userId = fetchPostOrThrow("user_id")
            val autoEscape = fetchPostOrNull("auto_escape")?.toBooleanStrict() ?: false

            val result = if (isJsonData()) {
                if (isString("message")) {
                    SendMessage(MsgConstant.KCHATTYPEC2C, userId, fetchPostJsonString("message"), autoEscape)
                } else {
                    SendMessage(MsgConstant.KCHATTYPEC2C, userId, fetchPostJsonArray("message"))
                }
            } else {
                SendMessage(MsgConstant.KCHATTYPEC2C, userId, fetchPostOrThrow("message"), autoEscape)
            }

            call.respondText(result)
        }
    }
}