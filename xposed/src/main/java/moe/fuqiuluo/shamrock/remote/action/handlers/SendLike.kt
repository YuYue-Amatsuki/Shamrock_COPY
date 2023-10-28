package moe.fuqiuluo.shamrock.remote.action.handlers

import moe.fuqiuluo.qqinterface.servlet.VisitorSvc
import moe.fuqiuluo.shamrock.remote.action.ActionSession
import moe.fuqiuluo.shamrock.remote.action.IActionHandler
import moe.fuqiuluo.shamrock.tools.errMsg

internal object SendLike: IActionHandler() {
    override suspend fun internalHandle(session: ActionSession): String {
        val times = session.getInt("times")
        val uin = session.getLong("user_id")
        val result = VisitorSvc.vote(uin, times)
        return if(result.isSuccess) {
            ok("成功", session.echo)
        } else {
            logic(result.errMsg(), session.echo)
        }
    }

    override val requiredParams: Array<String> = arrayOf("times", "user_id")

    override fun path(): String = "send_like"
}