package moe.fuqiuluo.remote.api

import com.tencent.mobileqq.app.QQAppInterface
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.coroutines.delay
import moe.fuqiuluo.remote.HTTPServer
import moe.fuqiuluo.remote.entries.CommonResult
import moe.fuqiuluo.remote.entries.CurrentAccount
import moe.fuqiuluo.remote.entries.Status
import moe.fuqiuluo.remote.entries.StdAccount
import moe.fuqiuluo.xposed.helper.LogCenter
import moe.fuqiuluo.xposed.tools.getOrPost
import moe.fuqiuluo.xposed.tools.respond
import mqq.app.MobileQQ
import kotlin.system.exitProcess

fun Routing.obtainFrameworkInfo() {
    getOrPost("/get_start_time") {
        respond(
            isOk = true,
            code = Status.Ok,
            HTTPServer.startTime
        )
    }

    get("/shut") {
        HTTPServer.stop()
        LogCenter.log("正在关闭Shamrock。", toast = true)
        delay(3000)
        exitProcess(0)
    }

    getOrPost("/get_account_info") {
        val accounts = MobileQQ.getMobileQQ().allAccounts
        val runtime = MobileQQ.getMobileQQ().waitAppRuntime()
        val curUin = runtime.currentAccountUin
        val account = accounts.firstOrNull { it.uin == curUin }
        if (account == null || !account.isLogined) {
            respond(false, Status.BadParam, msg = "当前不处于已登录状态")
        } else {
            this.call.respond(CommonResult("ok", 0, CurrentAccount(
                curUin.toLong(), runtime.isLogin, if (runtime is QQAppInterface) runtime.currentNickname else "unknown"
            )))
        }
    }

    getOrPost("/get_history_account_info") {
        val accounts = MobileQQ.getMobileQQ().allAccounts
        val accountList = accounts.map {
            CurrentAccount(it.uin.toLong(), it.isLogined)
        }
        respond(true, Status.Ok, accountList)
    }

    getOrPost("/get_login_info") {
        val accounts = MobileQQ.getMobileQQ().allAccounts
        val runtime = MobileQQ.getMobileQQ().waitAppRuntime()
        val curUin = runtime.currentAccountUin
        val account = accounts.firstOrNull { it.uin == curUin }
        if (account == null || !account.isLogined) {
            respond(false, Status.BadParam, msg = "当前不处于已登录状态")
        } else {
            respond(true, Status.Ok, StdAccount(
                curUin.toLong(),if (runtime is QQAppInterface) runtime.currentNickname else "unknown"
            ))
        }
    }
}
