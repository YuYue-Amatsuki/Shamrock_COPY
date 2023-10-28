package moe.fuqiuluo.remote.action.handlers

import moe.fuqiuluo.remote.action.ActionSession
import moe.fuqiuluo.remote.action.IActionHandler

internal object GetRecord: IActionHandler() {
    override suspend fun internalHandle(session: ActionSession): String {
        val file = session.getString("file")
            .replace(regex = "[{}\\-]".toRegex(), replacement = "")
            .split(".")[0].lowercase()
        val format = session.getString("out_format")
        return invoke(file, format)
    }

    operator fun invoke(file: String, format: String): String {

        return logic("unable to fetch record file.")
    }

    override val requiredParams: Array<String> = arrayOf("file", "out_format")

    override fun path(): String = "get_record"
}