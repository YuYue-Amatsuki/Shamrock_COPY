package moe.fuqiuluo.shamrock.remote.action.handlers

import moe.fuqiuluo.qqinterface.servlet.TicketSvc
import moe.fuqiuluo.shamrock.remote.action.ActionSession
import moe.fuqiuluo.shamrock.remote.action.IActionHandler
import moe.fuqiuluo.shamrock.remote.service.data.Credentials

internal object GetCSRF: IActionHandler() {
    override suspend fun internalHandle(session: ActionSession): String {
        val domain = session.getStringOrNull("domain")
            ?: return invoke(session.echo)
        return invoke(domain, session.echo)
    }

    suspend operator fun invoke(domain: String, echo: String = ""): String {
        val uin = TicketSvc.getUin()
        val pskey = TicketSvc.getPSKey(uin, domain)
            ?: return invoke()
        return ok(Credentials(bkn = TicketSvc.getCSRF(pskey)), echo)
    }

    operator fun invoke(echo: String = ""): String {
        val uin = TicketSvc.getUin()
        val pskey = TicketSvc.getPSKey(uin)
        return ok(Credentials(bkn = TicketSvc.getCSRF(pskey)), echo)
    }

    override fun path(): String = "get_csrf_token"
}