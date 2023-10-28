package moe.fuqiuluo.remote.action.handlers

import moe.protocol.servlet.utils.FileUtils
import moe.fuqiuluo.remote.action.ActionSession
import moe.fuqiuluo.remote.action.IActionHandler
import moe.fuqiuluo.utils.MMKVFetcher

internal object CleanCache: IActionHandler() {
    override suspend fun internalHandle(session: ActionSession): String {
        return invoke(session.echo)
    }

    operator fun invoke(echo: String = ""): String {
        FileUtils.clearCache()
        MMKVFetcher.mmkvWithId("hash2id")
            .clear()
        MMKVFetcher.mmkvWithId("seq2id")
            .clear()
        MMKVFetcher.mmkvWithId("audio2silk")
            .clear()
        return ok("成功", echo)
    }

    override fun path(): String = "clean_cache"
}