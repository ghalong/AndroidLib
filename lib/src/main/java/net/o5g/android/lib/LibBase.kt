package net.o5g.android.lib

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import net.o5g.android.lib.utils.CommonUtils
import net.o5g.android.lib.utils.RtcXLogUtil
import net.o5g.android.lib.utils.UIUtil
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


/**
 * Author:  王海宇<br>
 * Date:    2021/04/22 15:06<br>
 * Desc:    <br>
 * Edit History:<br>
 */
object LibBase {
    lateinit var application: Application
    lateinit var handler: Handler
    private var hasInit = false

    fun init(application: Application) {
        if (hasInit)
            return
        if (!isMainProcess(application))
            return
        handler = Handler(Looper.myLooper()!!)
        LibBase.application = application
        CommonUtils.init(application)
        RtcXLogUtil.init(application)
        UIUtil
        hasInit = true
    }

    /**
     * 检查当前进程是否为主进程（主进程名字不能有变更）
     */
    fun isMainProcess(context: Application): Boolean {
        return context.packageName == getProcessName(context)
    }

    fun getProcessName(context: Application): String? {
        try {
            return if (Build.VERSION.SDK_INT >= 28) Application.getProcessName() else try {
                @SuppressLint("PrivateApi") val activityThread =
                    Class.forName("android.app.ActivityThread")

                // Before API 18, the method was incorrectly named "currentPackageName", but it still returned the process name
                // See https://github.com/aosp-mirror/platform_frameworks_base/commit/b57a50bd16ce25db441da5c1b63d48721bb90687
                val methodName =
                    if (Build.VERSION.SDK_INT >= 18) "currentProcessName" else "currentPackageName"
                val getProcessName: Method = activityThread.getDeclaredMethod(methodName)
                getProcessName.invoke(null) as String
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            }
        } catch (e: Exception) {
            RtcXLogUtil.logE(e)
            try {
                val pid = android.os.Process.myPid()
                val manager =
                    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                for (process in manager.runningAppProcesses) {
                    if (process.pid == pid) {
                        return process.processName
                    }
                }
            } catch (e: Exception) {
                RtcXLogUtil.logE(e)
            }
            return null
        }
        // Using the same technique as Application.getProcessName() for older devices
        // Using reflection since ActivityThread is an internal API
    }
}