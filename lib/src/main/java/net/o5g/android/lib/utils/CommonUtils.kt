package net.o5g.android.lib.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * Author:  王海宇<br>
 * Date:    2020/04/27 17:27<br>
 * Desc:    <br>
 * Edit History:<br>
 */
inline fun ui(crossinline block: () -> Unit) {
    // Checking first for activity and view saves us from some synchronyzed and thread local checks
    // If we already are running on the Main Thread (UI Thread), just go ahead and execute the block
    if (Looper.getMainLooper() == Looper.myLooper()) {
        block()
    } else {
        // Launch a Job on the UI context and check again if the activity and view are still valid
        GlobalScope.launch(Dispatchers.Main) {
            block()
        }
    }
}

/**
 * [newThread]是否强制另开新线程处理当前任务，默认情况下当前线程是子线程会直接在当前子线程执行任务
 */
inline fun background(newThread: Boolean = false, crossinline block: () -> Unit) {
    // Checking first for activity and view saves us from some synchronyzed and thread local checks
    // If we already are running on the Main Thread (UI Thread), just go ahead and execute the block
    if (Looper.getMainLooper() == Looper.myLooper() || newThread) {
        GlobalScope.launch {
            block()
        }
    } else {
        // Launch a Job on the UI context and check again if the activity and view are still valid
        block()
    }
}

fun Uri.getMimeType(context: Context): String {
    return if (scheme == ContentResolver.SCHEME_CONTENT) {
        context.contentResolver?.getType(this) ?: ""
    } else {
        var fileExtension: String? = null
        val i = toString().lastIndexOf('.')
        if (i > 0) {
            fileExtension = toString().substring(i + 1)
        }

        if (fileExtension != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase())
                ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }
}

@SuppressLint("StaticFieldLeak")
object CommonUtils {
    private var appForegroundDetect: Runnable? = null
    private var appForegroundLast = false
    private var appForeground = 0
    private val foregroundListeners = mutableSetOf<(Boolean) -> Unit>()
    private lateinit var handler: Handler
    private var hasInit = false
    var currentActivity: Activity? = null
        private set

    /**
     * 必须在主进程的Application.onCreate方法中调用
     */
    fun init(context: Application) {
        if (hasInit)
            return
        handler = Handler()
        context.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityStarted(activity: Activity) {
                appForeground++
                checkAppInForeground()
            }

            override fun onActivityDestroyed(activity: Activity) {
            }


            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityStopped(activity: Activity) {
                appForeground--
                checkAppInForeground()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }
        })
        hasInit = true
    }

    internal fun registerAppForeground(listener: (Boolean) -> Unit) {
        synchronized(foregroundListeners) {
            foregroundListeners.add(listener)
        }
    }

    internal fun unregisterAppForeground(listener: (Boolean) -> Unit) {
        synchronized(foregroundListeners) {
            foregroundListeners.remove(listener)
        }
    }

    /**
     * 根据activity状态检查App是否位于前台。由于页面切换时会在短时间内引起activity状态多重变化，所以通过延后一
     * 段时间来处理的方式来过滤掉无意义的状态切换
     */
    private fun checkAppInForeground() {
        appForegroundDetect?.let {
            handler.removeCallbacks(it)
        }
        appForegroundDetect = Runnable {
            appForegroundDetect = null
            if (appForegroundLast != isAppForeground()) {//限定只有前后台状态发生变化时再做出响应
                val foreground = isAppForeground()
                foregroundListeners.forEach {
                    it(foreground)
                }
            }

            appForegroundLast = isAppForeground()
        }
        handler.postDelayed(appForegroundDetect!!, 300)
    }


    fun isAppForeground(): Boolean {
        return appForeground > 0
    }
}