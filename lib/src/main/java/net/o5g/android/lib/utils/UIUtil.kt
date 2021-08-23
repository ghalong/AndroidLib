package net.o5g.android.lib.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import net.o5g.android.lib.LibBase


/**
 * Version: V1.4.2<br>
 * Author:  王海宇<br>
 * Date:    2019/07/12 16:58<br>
 * Desc:    <br>
 * Edit History:<br>
 */

object UIUtil {
    private val handler = Handler()

    /**
     * 根据手机分辨率从DP转成PX
     * @param context
     * @param dpValue
     * @return
     */
    fun dip2px(dpValue: Float): Int {
        val scale = LibBase.application.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     * @param spValue
     * @return
     */
    fun sp2px(spValue: Float): Int {
        val fontScale = LibBase.application.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率PX(像素)转成DP
     * @param context
     * @param pxValue
     * @return
     */
    fun px2dip(pxValue: Float): Int {
        val scale = LibBase.application.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     * @param pxValue
     * @return
     */

    fun px2sp(pxValue: Float): Int {
        val fontScale = LibBase.application.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    fun showToast(text: String) {
        showToastWithGravity(text, Gravity.CENTER)
    }

    /**
     * [gravity] #android.view.Gravity
     */
    fun showToastWithGravity(text: String, gravity: Int? = null) {
        val call = {
            val toast = Toast.makeText(LibBase.application, text, Toast.LENGTH_SHORT)
            if (gravity != null)
                toast.setGravity(gravity, 0, 0)
            toast.show()
        }
        if (isMainThread())
            call()
        else
            handler.post {
                call()
            }
    }

    fun isMainThread(): Boolean {
        return Looper.getMainLooper() == Looper.myLooper()
    }

    fun showDialog(
        context: Context,
        title: String,
        cancelCallback: (() -> Unit)? = null,
        okCallback: () -> Unit
    ) {
        val call = {
            AlertDialog.Builder(context).apply {
                setTitle(title)
                setPositiveButton("确定") { _, _ ->
                    okCallback()
                }

                setNegativeButton("取消") { _, _ ->
                    cancelCallback?.invoke()
                }
                show()
            }
        }
        if (isMainThread())
            call()
        else
            handler.post {
                call()
            }
    }

    //根据时间长短计算语音条宽度:220dp
    @Synchronized
    fun getVoiceLineWight(seconds: Int): Int {
        //1-2s是最短的。2-10s每秒增加一个单位。10-60s每10s增加一个单位。
        return if (seconds <= 2) {
            dip2px(60f)
        } else if (seconds <= 10) {
            //90~170
            dip2px((60 + 8 * seconds).toFloat())
        } else {
            //170~220
            dip2px((140 + 10 * (seconds / 10)).toFloat())
        }
    }

    fun getScreenWidth(): Int {
        return LibBase.application.resources.displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return LibBase.application.resources.displayMetrics.heightPixels
    }

    fun showPopupWindow(
        view: View,
        gravity: Int = Gravity.END.or(Gravity.CENTER_VERTICAL)
    ): PopupWindow {
        return PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).let {
            RtcXLogUtil.logE(CommonUtils.currentActivity!!.window.decorView.rootView)
            ui {
                it.showAtLocation(
                    CommonUtils.currentActivity!!.window.decorView.rootView,
                    gravity,
                    0,
                    0
                )
            }
            it
        }
    }
}