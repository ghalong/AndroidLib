package net.o5g.android.lib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.PermissionChecker
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import net.o5g.android.lib.utils.RtcXLogUtil


/**
 * Author:  王海宇<br>
 * Date:    2021/01/31 10:24<br>
 * Desc:    <br>
 * Edit History:<br>
 */
object PermissionUtils {
    private val permissionsMap = HashMap<Int, Array<out PermissionRequestInfo>>()
    private val callbacks = HashMap<Int, (Boolean, MultiplePermissionsReport?) -> Unit>()
    private var requestCount = 0

    fun checkPermissions(
        vararg permissions: PermissionRequestInfo,
        callback: (Boolean, MultiplePermissionsReport?) -> Unit
    ) {
        var allGranted = true
        permissions.forEach {
            if ((PermissionChecker.checkSelfPermission(
                    RtcBase.application,
                    it.permission
                )) != PermissionChecker.PERMISSION_GRANTED
            ) {
                allGranted = false
                return@forEach
            }
        }
        if (allGranted) {
            callback(true, null)
            return
        }
        permissionsMap[requestCount] = permissions
        callbacks[requestCount] = callback
        PermissionRequestActivity.start(requestCount++)
    }

    internal fun checkPermissions(activity: Activity, requestCount: Int, callback: () -> Unit) {
        if (permissionsMap.containsKey(requestCount)) {
            checkPermissions(
                activity,
                permissionsMap[requestCount]!!
            ) { granted, report ->
                callbacks[requestCount]!!(granted, report)
                callback()
            }
        }
    }

    private fun checkPermissions(
        activity: Activity,
        permissions: Array<out PermissionRequestInfo>,
        callback: (Boolean, MultiplePermissionsReport?) -> Unit
    ) {
        val permissionValues = mutableMapOf<String, PermissionRequestInfo>().apply {
            permissions.forEach {
                put(it.permission, it)
            }
        }
        Dexter.withActivity(activity)
            .withPermissions(
                permissionValues.keys
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    callback(report?.areAllPermissionsGranted() ?: false, report)
                    report?.let {
                        it.grantedPermissionResponses.forEach {
                            RtcXLogUtil.logE(" grantedPermissionResponses ${it.permissionName}")
                        }
                        it.deniedPermissionResponses.forEach {
                            RtcXLogUtil.logE(" deniedPermissionResponses ${it.permissionName}")
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    permissions?.forEach {
                        RtcXLogUtil.logE(" onPermissionRationaleShouldBeShown ${it.name}")
                        if (permissionValues[it.name]?.requestIfNotGranted != false)
                            token?.continuePermissionRequest()
                    }

                    token?.continuePermissionRequest()
                }
            }).check()
    }

    fun openAppSettingsPage(context: Context) {
        try {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 将用户引导到系统设置页面
            val action = intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
        } catch (e: Exception) { //抛出异常就直接打开设置页面
            RtcXLogUtil.logE(e)
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

data class PermissionRequestInfo(
    val permission: String,
    val requestIfNotGranted: Boolean
)