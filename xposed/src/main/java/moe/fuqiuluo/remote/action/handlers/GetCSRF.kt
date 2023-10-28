package moe.fuqiuluo.remote.action.handlers

import com.tencent.mobileqq.data.CSRF
import com.tencent.qqnt.protocol.TicketSvc
import moe.fuqiuluo.remote.action.ActionSession
import moe.fuqiuluo.remote.action.IActionHandler

internal object GetCSRF: IActionHandler() {
    override suspend fun internalHandle(session: ActionSession): String {
        val domain = session.getStringOrNull("domain")
            ?: return invoke()
        return invoke(domain)
    }

    operator fun invoke(domain: String): String {
        val uin = TicketSvc.getUin()
        val pskey = TicketSvc.getPSKey(uin, domain)
            ?: return invoke()
        return ok(CSRF(TicketSvc.getCSRF(pskey)))
    }

    operator fun invoke(): String {
        val uin = TicketSvc.getUin()
        val pskey = TicketSvc.getPSKey(uin)
        return ok(CSRF(TicketSvc.getCSRF(pskey)))
    }

    override fun path(): String = "get_csrf_token"
}