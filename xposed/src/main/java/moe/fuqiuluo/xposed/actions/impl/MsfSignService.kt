package moe.fuqiuluo.xposed.actions.impl

import android.content.Context
import com.tencent.mobileqq.fe.FEKit
import moe.fuqiuluo.xposed.actions.IAction
import moe.fuqiuluo.xposed.helper.PlatformHelper
import moe.fuqiuluo.xposed.helper.internal.DynamicReceiver
import moe.fuqiuluo.xposed.helper.internal.IPCRequest
import moe.fuqiuluo.xposed.tools.broadcast
import mqq.app.MobileQQ

internal class MsfSignService: IAction {
    override fun invoke(ctx: Context) {
        if (!PlatformHelper.isMsfProcess()) return

        DynamicReceiver.register("sign", IPCRequest {
            val cmd = it.getStringExtra("wupCmd")
            val seq = it.getIntExtra("seq", -1)
            val buffer = it.getByteArrayExtra("buffer")
            val uin = it.getStringExtra("uin")
            val sign = FEKit.getInstance().getSign(cmd, buffer, seq, uin)
            ctx.broadcast("xqbot") {
                putExtra("__cmd", "sign_callback")
                putExtra("sign", sign.sign)
                putExtra("token", sign.token)
                putExtra("extra", sign.extra)
            }
        })
    }
}