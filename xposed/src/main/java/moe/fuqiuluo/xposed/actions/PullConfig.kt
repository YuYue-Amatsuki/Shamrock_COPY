package moe.fuqiuluo.xposed.actions

import android.content.Context
import com.tencent.mobileqq.helper.ShamrockConfig
import moe.fuqiuluo.xposed.helper.internal.DataRequester

import moe.fuqiuluo.http.HTTPServer
import com.tencent.qqnt.utils.PlatformUtils
import moe.fuqiuluo.xposed.helper.internal.DynamicReceiver
import moe.fuqiuluo.xposed.helper.internal.IPCRequest
import moe.fuqiuluo.xposed.loader.ActionLoader
import moe.fuqiuluo.xposed.loader.NativeLoader
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class PullConfig: IAction {
    companion object {
        @JvmStatic
        var isConfigOk = false
    }

    private external fun testNativeLibrary(): String

    override fun invoke(ctx: Context) {
        if (!PlatformUtils.isMainProcess()) return

        DynamicReceiver.register("fetchPort", IPCRequest {
            DataRequester.request("success", mapOf("port" to HTTPServer.PORT))
        })

        DataRequester.request("init", onFailure = {
            if (!ShamrockConfig.isInit(ctx)) {
                ctx.toast("请启动Shamrock主进程以初始化服务，进程将退出。")
                thread {
                    Thread.sleep(3000)
                    exitProcess(1)
                }
            } else {
                ctx.toast("Shamrock进程未启动，不会推送配置文件。。")
                initHttp(ctx)
            }
        }, bodyBuilder = null) {
            isConfigOk = true
            ShamrockConfig.updateConfig(ctx, it)
            initHttp(ctx)
        }
    }

    private fun initHttp(ctx: Context) {
        NativeLoader.load("shamrock")
        ctx.toast(testNativeLibrary())
        ActionLoader.runService(ctx)
    }
}