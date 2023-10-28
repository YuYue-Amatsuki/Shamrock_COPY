package moe.fuqiuluo.shamrock.remote.action.handlers

import moe.fuqiuluo.shamrock.helper.MessageHelper
import moe.fuqiuluo.shamrock.remote.action.ActionSession
import moe.fuqiuluo.shamrock.remote.action.IActionHandler
import moe.fuqiuluo.shamrock.remote.service.data.MessageDetail
import moe.fuqiuluo.shamrock.remote.service.data.MessageSender
import moe.fuqiuluo.qqinterface.servlet.MsgSvc
import moe.fuqiuluo.qqinterface.servlet.msg.MsgConvert

internal object GetMsg: IActionHandler() {
    override suspend fun internalHandle(session: ActionSession): String {
        val hashCode = session.getIntOrNull("message_id")
            ?: session.getInt("msg_id")
        return invoke(hashCode, session.echo)
    }

    suspend operator fun invoke(msgHash: Int, echo: String = ""): String {
        val msg = MsgSvc.getMsg(msgHash).onFailure {
            return logic("Obtain msg failed, please check your msg_id.", echo)
        }.getOrThrow()
        val seq = msg.clientSeq.toInt()
        return ok(MessageDetail(
            time = msg.msgTime.toInt(),
            msgType = MessageHelper.obtainDetailTypeByMsgType(msg.chatType),
            msgId = msgHash,
            realId = seq,
            sender = MessageSender(
                msg.senderUin, msg.sendNickName, "unknown", 0, msg.senderUid
            ),
            message = MsgConvert.convertMsgRecordToMsgSegment(msg),
            groupId = msg.peerUin
        ), echo)
    }

    override val requiredParams: Array<String> = arrayOf("message_id")

    override val alias: Array<String> = arrayOf("get_message")

    override fun path(): String = "get_msg"
}