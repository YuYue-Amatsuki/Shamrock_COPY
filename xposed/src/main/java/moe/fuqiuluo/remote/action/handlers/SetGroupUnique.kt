package moe.fuqiuluo.remote.action.handlers

import moe.protocol.servlet.GroupSvc
import moe.fuqiuluo.remote.action.ActionSession
import moe.fuqiuluo.remote.action.IActionHandler

internal object SetGroupUnique: IActionHandler() {
    override suspend fun internalHandle(session: ActionSession): String {
        val groupId = session.getString("group_id")
        val userId = session.getString("user_id")
        val unique = session.getString("special_title")
        return invoke(groupId, userId, unique, session.echo)
    }

    suspend operator fun invoke(groupId: String, userId: String, unique: String, echo: String = ""): String {
        if (!GroupSvc.isAdmin(groupId)) {
            return error("you are not admin", echo)
        }
        GroupSvc.setGroupUniqueTitle(groupId, userId, unique)
        return ok("成功", echo)
    }

    override val requiredParams: Array<String> = arrayOf("group_id", "user_id", "special_title")


    override fun path(): String = "set_group_special_title"
}