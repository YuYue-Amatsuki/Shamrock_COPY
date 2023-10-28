@file:OptIn(DelicateCoroutinesApi::class)
package moe.fuqiuluo.xposed.actions

import android.content.Context
import com.tencent.qqnt.kernel.api.IKernelService
import com.tencent.qqnt.kernel.api.impl.KernelServiceImpl
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.fuqiuluo.xposed.helper.Level
import moe.fuqiuluo.xposed.helper.LogCenter
import moe.fuqiuluo.xposed.helper.NTServiceFetcher
import moe.fuqiuluo.xposed.loader.NativeLoader
import moe.fuqiuluo.xposed.tools.hookMethod

internal class FetchService: IAction {
    override fun invoke(ctx: Context) {
        //IQQNTWrapperSession.CppProxy::class.java.hookMethod("startNT").after {
        //    NTServiceFetcher.onNTStart()
        //}
        /*
            AppRuntime::class.java.hookMethod("getRuntimeService").after {
            val service = it.result as? IRuntimeService
            if (service != null && service is IKernelService) {
                GlobalScope.launch {
                    NTServiceFetcher.onFetch(service)
                }
            }
        }
         */
        NativeLoader.load("shamrock")

        KernelServiceImpl::class.java.hookMethod("initService").after {
            val service = it.thisObject as IKernelService
            LogCenter.log("NTKernel try to init service: $service", Level.DEBUG)
            GlobalScope.launch {
                NTServiceFetcher.onFetch(service)
            }
        }
    }
}