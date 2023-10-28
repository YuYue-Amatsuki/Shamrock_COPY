package moe.fuqiuluo.http.action.handlers

import moe.fuqiuluo.http.action.ActionSession
import moe.fuqiuluo.http.action.IActionHandler
import com.tencent.qqnt.helper.MessageHelper
import com.tencent.qqnt.protocol.MsgSvc
import moe.fuqiuluo.http.entries.EmptyObject

internal object DeleteMessage: IActionHandler() {
    override suspend fun handle(session: ActionSession): String {
        val hashCode = (session.getStringOrNull("message_id")
            ?: return noParam("message_id")).toInt()
        val msgId = MessageHelper.getMsgIdByHashCode(hashCode)

        MsgSvc.deleteMsg(msgId)

        return ok(EmptyObject)
    }

    override fun path(): String = "delete_message"
}