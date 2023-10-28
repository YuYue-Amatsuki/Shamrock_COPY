package moe.protocol.service

import com.tencent.qphone.base.remote.FromServiceMsg
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.fuqiuluo.xposed.helper.Level
import moe.fuqiuluo.xposed.helper.internal.DynamicReceiver
import moe.fuqiuluo.xposed.helper.internal.IPCRequest
import moe.fuqiuluo.xposed.helper.LogCenter
import moe.fuqiuluo.xposed.tools.broadcast
import mqq.app.MobileQQ

internal object PacketReceiver {
    private val allowCommandList: MutableSet<String> by lazy { mutableSetOf(
        "trpc.msg.olpush.OlPushService.MsgPush",

    ) } // 非动态注册，永久常驻的包
    private val HandlerByIpcSet = hashSetOf<String>()

    init {
        DynamicReceiver.register("register_handler_cmd", IPCRequest {
            val cmd = it.getStringExtra("handler_cmd")!!
            LogCenter.log("RegisterHandler(cmd = $cmd)", Level.DEBUG)
            HandlerByIpcSet.add(cmd)
        })
        DynamicReceiver.register("unregister_handler_cmd", IPCRequest {
            val cmd = it.getStringExtra("handler_cmd")!!
            LogCenter.log("UnRegisterHandler(cmd = $cmd)", Level.DEBUG)
            HandlerByIpcSet.remove(cmd)
        })
        MobileQQ.getContext().broadcast("xqbot") {
            putExtra("__cmd", "msf_waiter")
            LogCenter.log("MSF Packet Receiver running!")
        }
    }

    private fun msgFilter(cmd: String): Boolean {
        return cmd !in allowCommandList && cmd !in HandlerByIpcSet
    }

    private fun onReceive(from: FromServiceMsg) {
        if (HandlerByIpcSet.contains(from.serviceCmd)
            || allowCommandList.contains(from.serviceCmd)
        ) {
            LogCenter.log("ReceivePacket(cmd = ${from.serviceCmd})", Level.DEBUG)
            MobileQQ.getContext().broadcast("xqbot") {
                putExtra("__cmd", from.serviceCmd)
                putExtra("buffer", from.wupBuffer)
            }
        }
    }

    fun internalOnReceive(from: FromServiceMsg?) {
        if (from == null || msgFilter(from.serviceCmd)) return
        GlobalScope.launch {
            onReceive(from)
        }
    }
}