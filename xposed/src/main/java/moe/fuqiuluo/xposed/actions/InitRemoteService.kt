@file:OptIn(DelicateCoroutinesApi::class)
package moe.fuqiuluo.xposed.actions

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.fuqiuluo.remote.HTTPServer
import moe.fuqiuluo.remote.InternalWebSocketClient
import moe.fuqiuluo.remote.InternalWebSocketServer
import moe.fuqiuluo.remote.ShamrockWebSocketClient
import moe.fuqiuluo.remote.ShamrockWebSocketServer
import moe.fuqiuluo.xposed.helper.Level
import moe.fuqiuluo.xposed.helper.LogCenter
import moe.protocol.service.config.ShamrockConfig
import moe.protocol.servlet.utils.PlatformUtils
import mqq.app.MobileQQ
import kotlin.concurrent.timer

internal class InitRemoteService: IAction {
    override fun invoke(ctx: Context) {
        if (!PlatformUtils.isMainProcess()) return

        GlobalScope.launch {
            try {
                HTTPServer.start(ShamrockConfig.getPort())
            } catch (e: Throwable) {
                LogCenter.log(e.stackTraceToString(), Level.ERROR)
            }
        }

        if (ShamrockConfig.openWebSocket()) {
            GlobalScope.launch {
                try {
                    if (InternalWebSocketServer != null) {
                        InternalWebSocketServer?.stop()
                    }
                    InternalWebSocketServer = ShamrockWebSocketServer(ShamrockConfig.getWebSocketPort())
                    InternalWebSocketServer?.start()
                    InternalWebSocketServer?.initHeartbeat()
                } catch (e: Throwable) {
                    LogCenter.log(e.stackTraceToString(), Level.ERROR)
                }
            }
        }

        if (ShamrockConfig.openWebSocketClient()) {
            timer(initialDelay = 0L, period = 5000L) {
                if (InternalWebSocketClient == null) {
                    startWebSocketClient()
                }
            }
        }
    }

    private fun startWebSocketClient() {
        GlobalScope.launch {
            try {
                if (InternalWebSocketClient != null) {
                    InternalWebSocketClient?.close()
                }
                val runtime = MobileQQ.getMobileQQ().waitAppRuntime()
                val curUin = runtime.currentAccountUin
                val wsHeaders =  mapOf("X-Self-ID" to curUin)
                InternalWebSocketClient = ShamrockWebSocketClient(ShamrockConfig.getWebSocketClientAddress(), wsHeaders)
                InternalWebSocketClient?.connect()
            } catch (e: Throwable) {
                LogCenter.log(e.stackTraceToString(), Level.ERROR)
            }
        }
    }
}