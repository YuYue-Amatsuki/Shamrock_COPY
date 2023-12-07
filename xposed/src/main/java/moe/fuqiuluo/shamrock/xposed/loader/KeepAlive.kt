@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")
package moe.fuqiuluo.shamrock.xposed.loader

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import moe.fuqiuluo.shamrock.tools.hookMethod
import java.lang.reflect.Method
import kotlin.concurrent.timer

internal object KeepAlive {
    private val KeepPackage = arrayOf(
        "com.tencent.mobileqq", "moe.fuqiuluo.shamrock"
    )
    private val KeepRecords = arrayListOf<Any>()
    private lateinit var KeepThread: Thread

    private lateinit var METHOD_IS_KILLED: Method
    private var allowPersistent: Boolean = false

    operator fun invoke(loader: ClassLoader) {
        val pref = XSharedPreferences("moe.fuqiuluo.shamrock", "shared_config")
        hookAMS(pref, loader)
        hookDoze(pref, loader)
    }

    private fun hookDoze(pref: XSharedPreferences, loader: ClassLoader) {
        if (pref.file.canRead() && pref.getBoolean("hook_doze", false)) {
            val result = runCatching {
                val DeviceIdleController = XposedHelpers.findClass("com.android.server.DeviceIdleController", loader)
                    ?: return@runCatching -1
                val becomeActiveLocked = XposedHelpers.findMethodBestMatch(DeviceIdleController, "becomeActiveLocked", String::class.java, Integer.TYPE)
                    ?: return@runCatching -2
                if (!becomeActiveLocked.isAccessible) {
                    becomeActiveLocked.isAccessible = true
                }
                DeviceIdleController.hookMethod("onStart").after {
                    XposedBridge.log("[Shamrock] DeviceIdleController onStart")
                    // timer(initialDelay = 120_000L, period = 240_000L) {
                    //     XposedBridge.log("[Shamrock] try to wakeup screen")
                    //     becomeActiveLocked.invoke(it.thisObject, "screen", Process.myUid())
                    // } //点亮屏幕
                }
                DeviceIdleController.hookMethod("becomeInactiveIfAppropriateLocked").before {
                    XposedBridge.log("[Shamrock] DeviceIdleController becomeInactiveIfAppropriateLocked")
                    it.result = Unit
                }
                DeviceIdleController.hookMethod("stepIdleStateLocked").before {
                    XposedBridge.log("[Shamrock] DeviceIdleController stepIdleStateLocked")
                    it.result = Unit
                }
                return@runCatching 0
            }.getOrElse { -5 }
            if(result < 0) {
                XposedBridge.log("[Shamrock] Unable to hookDoze: $result")
            }
        }
    }

    private fun hookAMS(pref: XSharedPreferences, loader: ClassLoader) {
        kotlin.runCatching {
            val ActivityManagerService = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", loader)
            ActivityManagerService.hookMethod("newProcessRecordLocked").after {
                increaseAdj(it.result)
            }
        }.onFailure {
            XposedBridge.log("[Shamrock] Plan A failed: ${it.message}")
        }

        if (pref.file.canRead()) {
            allowPersistent = pref.getBoolean("persistent", false)
            XposedBridge.log("[Shamrock] allowPersistent = $allowPersistent")
        } else {
            XposedBridge.log("[Shamrock] unable to load XSharedPreferences")
        }

        kotlin.runCatching {
            val ProcessList = XposedHelpers.findClass("com.android.server.am.ProcessList", loader)
            ProcessList.hookMethod("newProcessRecordLocked").after {
                increaseAdj(it.result)
            }
        }.onFailure {
            XposedBridge.log("[Shamrock] Plan B failed: ${it.message}")
        }
    }

    private fun checkThread() {
        if (!::KeepThread.isInitialized || !KeepThread.isAlive) {
            KeepThread = Thread {
                val deletedList = mutableSetOf<Any>()
                while (true) {
                    Thread.sleep(100)
                    KeepRecords.forEach {
                        val isKilled = if (::METHOD_IS_KILLED.isInitialized) {
                            kotlin.runCatching {
                                 METHOD_IS_KILLED.invoke(it) as Boolean
                            }.getOrElse { false }
                        } else false
                        if (isKilled) {
                            deletedList.add(it)
                            XposedBridge.log("Process Closed: $it")
                        } else {
                            keepByAdj(it)
                        }
                    }
                    if (deletedList.isNotEmpty()) {
                        KeepRecords.removeAll(deletedList)
                        deletedList.clear()
                    }
                }
            }.also {
                it.isDaemon = true
                it.start()
            }
        }
    }

    private fun increaseAdj(record: Any) {
        if (record.toString().contains("system", ignoreCase = true)) {
            return
        }
        val applicationInfo = record.javaClass.getDeclaredField("info").also {
            if (!it.isAccessible) it.isAccessible = true
        }.get(record) as ApplicationInfo
        if(applicationInfo.processName in KeepPackage) {
            XposedBridge.log("[Shamrock] Process is keeping: $record")
            KeepRecords.add(record)
            keepByAdj(record)
            // Error
            if (allowPersistent) {
                XposedBridge.log("[Shamrock] Open NoDied Mode!!!")
                keepByPersistent(record)
            }
            checkThread()
        }
    }

    private fun keepByAdj(record: Any) {
        val clazz = record.javaClass
        if (!::METHOD_IS_KILLED.isInitialized) {
            kotlin.runCatching {
                METHOD_IS_KILLED = clazz.getDeclaredMethod("isKilled").also {
                    if (!it.isAccessible) it.isAccessible = true
                }
            }
        }
        kotlin.runCatching {
            val newState = clazz.getDeclaredField("mState").also {
                if (!it.isAccessible) it.isAccessible = true
            }.get(record)
            val MethodSetMaxAdj = newState.javaClass.getDeclaredMethod("setMaxAdj", Int::class.java).also {
                if (!it.isAccessible) it.isAccessible = true
            }.invoke(newState, 1)
        }.onFailure {
            clazz.getDeclaredField("maxAdj").also {
                if (!it.isAccessible) it.isAccessible = true
            }.set(record, 1)
        }
    }

    private fun keepByPersistent(record: Any) {
        val clazz = record.javaClass
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT < 29) {
                clazz.getDeclaredField("persistent").also {
                    if (!it.isAccessible) it.isAccessible = true
                }.set(record, true)
            } else {
                clazz.getDeclaredMethod("setPersistent", Boolean::class.java).also {
                    if (!it.isAccessible) it.isAccessible = true
                }.invoke(record, true)
            }
        }
    }
}