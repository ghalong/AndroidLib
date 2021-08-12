package net.o5g.android.lib.utils

import net.o5g.android.lib.RtcBase.application
import java.util.*


/**
 * Version: V1.4.2<br>
 * Author:  王海宇<br>
 * Date:    2019/11/15 14:13<br>
 * Desc:    <br>
 * Edit History:<br>
 */
object RtcSystemUtil {
    fun getDeviceId(): String {
        var id = RtcSPUtils.getValue("KEY_DEVICE_ID", "")
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString()
            RtcSPUtils.setValue("KEY_DEVICE_ID", id)
        }
        return id
    }

    /**
     * 获取当前手机系统版本号
     *
     * @return  系统版本号
     */
    fun getSystemVersion(): String {
        return android.os.Build.VERSION.RELEASE ?: ""
    }

    /**
     * 获取手机型号
     *
     * @return  手机型号
     */
    fun getSystemModel(): String {
        return android.os.Build.MODEL ?: ""
    }

    /**
     * 获取手机厂商
     *
     * @return  手机厂商
     */
    fun getDeviceBrand(): String {
        return android.os.Build.BRAND ?: ""

    }

    /**
     * 获取App的名称
     *
     *
     * @return 名称
     */

    fun getAppName(): String {
        return try {
            "${application.packageManager.getApplicationLabel(application.applicationInfo)}"
        } catch (e: Exception) {
            RtcXLogUtil.logE(e)
            ""
        }
    }

    /**
     * 获取版本名称
     *
     *
     * @return 版本名称
     */
    fun getAppVersionName(): String {
        //获取包信息
        return try {
            //返回版本号
            application.packageManager.getPackageInfo(application.packageName, 0).versionName
                ?: ""
        } catch (e: Exception) {
            RtcXLogUtil.logE(e)
            ""
        }
    }

    /**
     * 获取版本号
     *
     *
     * @return 版本号
     */
    fun getAppVersionCode(): Int {
        //获取包信息
        return try {
            //返回版本号
            application.packageManager.getPackageInfo(application.packageName, 0).versionCode
        } catch (e: Exception) {
            RtcXLogUtil.logE(e)
            0
        }
    }
}