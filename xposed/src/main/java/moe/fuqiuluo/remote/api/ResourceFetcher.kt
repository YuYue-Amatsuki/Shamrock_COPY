package moe.fuqiuluo.remote.api

import moe.protocol.servlet.utils.FileUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.document
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import moe.fuqiuluo.remote.action.handlers.GetImage
import moe.fuqiuluo.remote.action.handlers.GetRecord
import moe.fuqiuluo.xposed.tools.fetchGetOrThrow
import moe.fuqiuluo.xposed.tools.fetchOrThrow
import moe.fuqiuluo.xposed.tools.getOrPost

private fun formatFileName(file: String): String = file
    .replace(regex = "[{}\\-]".toRegex(), replacement = "")
    .replace(" ", "")
    .split(".")[0].lowercase()

fun Routing.fetchRes() {
    getOrPost("/get_record") {
        val file = formatFileName( fetchGetOrThrow("file") )
        val format = fetchOrThrow("out_format")
        call.respondText(GetRecord(file, format))
    }

    getOrPost("/get_image") {
        val file = formatFileName( fetchGetOrThrow("file") )
        call.respondText(GetImage(file))
    }

    route("/res/[a-fA-F0-9]{32}".toRegex()) {
        get {
            val md5 = call.request.document()
            val file = FileUtils.getFile(md5)
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respondFile(file)
            }
        }
    }
}