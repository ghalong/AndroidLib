package net.o5g.android.lib

import android.Manifest
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import net.o5g.android.lib.utils.CommonUtils
import net.o5g.android.lib.utils.RtcXLogUtil
import net.o5g.android.lib.utils.UIUtil
import kotlin.math.abs

object ScreenshotDetector {
    private var observing = false
    private var contentObserver: ContentObserver? = null
    private val screenShotListeners = mutableListOf<(String) -> Unit>()
    private val EXTERNAL_CONTENT_URI_MATCHER =
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()
    private val PROJECTION = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DATE_ADDED
    )
    private const val SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC"
    private const val DEFAULT_DETECT_WINDOW_SECONDS: Long = 10
    private var permissionChecked = false

    private val appForegroundListener: (Boolean) -> Unit = { foreground ->
        contentObserver?.let {
            if (foreground) {
                observer()
            } else
                cancelObserver()
        }

    }

    fun registerScreenListener(call: (String) -> Unit) {
        synchronized(screenShotListeners) {
            screenShotListeners.add(call)
            checkDetectStatus()
        }
    }

    fun unregisterScreenListener(call: (String) -> Unit) {
        synchronized(screenShotListeners) {
            screenShotListeners.remove(call)
            checkDetectStatus()
        }
    }

    private fun checkDetectStatus() {
        if (screenShotListeners.isEmpty()) {
            if (contentObserver != null) {
                cancelObserver()
                contentObserver = null
                CommonUtils.unregisterAppForeground(appForegroundListener)
            }
        } else {
            if (contentObserver == null) {
                createObserver()
                if (CommonUtils.isAppForeground())
                    observer()
                CommonUtils.registerAppForeground(appForegroundListener)
            }
        }
    }

    private fun checkPermission(failCallback: (() -> Unit)? = null) {
        if(permissionChecked)
            return
        PermissionUtils.checkPermissions(
            PermissionRequestInfo(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                true
            )
        ) { allGranted, _ ->
            permissionChecked = true
            if (!allGranted) {
                if (failCallback == null)
                    UIUtil.showDialog(CommonUtils.currentActivity!!, "请打开存储权限", {}) {
                        PermissionUtils.openAppSettingsPage(RtcBase.application)
                    }
            }
        }
    }

    private fun matchPath(path: String): Boolean {
        return path.contains("screenshot", true) || path.contains("截屏") ||
                path.contains("截图")
    }

    private fun matchTime(currentTime: Long, dateAdded: Long): Boolean {
        return abs(currentTime - dateAdded) <= DEFAULT_DETECT_WINDOW_SECONDS
    }

    private fun createObserver() {
        contentObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                RtcXLogUtil.logV(
                    "onChange: " + selfChange + ", " + uri.toString()
                )
                if (uri == null)
                    return
                if (uri.toString()
                        .startsWith(EXTERNAL_CONTENT_URI_MATCHER)
                ) {
                    var cursor: Cursor? = null
                    try {
                        cursor = RtcBase.application.contentResolver.query(
                            uri,
                            PROJECTION,
                            null,
                            null,
                            SORT_ORDER
                        )
                        if (cursor != null && cursor.moveToFirst()) {
                            val path = cursor.getString(
                                cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            )
                            val dateAdded = cursor.getLong(
                                cursor.getColumnIndex(
                                    MediaStore.Images.Media.DATE_ADDED
                                )
                            )
                            val currentTime = System.currentTimeMillis() / 1000
                            RtcXLogUtil.logV(
                                "path: " + path + ", dateAdded: " + dateAdded +
                                    ", currentTime: " + currentTime
                            )
                            if (matchPath(
                                    path
                                ) && matchTime(
                                    currentTime,
                                    dateAdded
                                )
                            ) {
                                synchronized(screenShotListeners) {
                                    screenShotListeners.forEach {
                                        it(path)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        RtcXLogUtil.logE(e)
                        RtcXLogUtil.logE(
                            "open cursor fail"
                        )
                    } finally {
                        cursor?.close()
                    }
                }
                super.onChange(selfChange, uri)
            }
        }
        checkPermission { }
    }

    private fun observer() {
        if (observing)
            return
        contentObserver?.let {
            RtcBase.application.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, it
            )
            observing = true
        }

    }

    private fun cancelObserver() {
        if (!observing)
            return
        contentObserver?.let {
            RtcBase.application.contentResolver.unregisterContentObserver(it)
            observing = false
        }
    }
}