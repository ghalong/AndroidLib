package net.o5g.android.lib.utils

import android.content.Context
import net.o5g.android.lib.LibBase.application


/**
 * Version: V1.4.2<br>
 * Author:  王海宇<br>
 * Date:    2020/06/23 15:46<br>
 * Desc:    <br>
 * Edit History:<br>
 */

object SPUtil {
    private const val name = "im_core"

    /**
     * 保存数据，修改数据
     *
     * @param key
     * @param value
     * @param <V>
    </V> */
    fun <V> setValue(key: String, value: V) {
        val sp = application.getSharedPreferences(
            name,
            Context.MODE_PRIVATE
        )
        val editor = sp.edit()
        if (value is String) {
            editor.putString(key, value as String)
        } else if (value is Int) {
            editor.putInt(key, (value as Int))
        } else if (value is Long) {
            editor.putLong(key, (value as Long))
        } else if (value is Boolean) {
            editor.putBoolean(key, (value as Boolean))
        } else if (value is Float) {
            editor.putFloat(key, (value as Float))
        }
        editor.apply()
    }

    /**
     * 读取数据
     *
     * @param key
     * @param defaultValue
     * @param <V>
     * @return
    </V> */
    fun <V> getValue(key: String, defaultValue: V): V {
        val sp = application.getSharedPreferences(
            name,
            Context.MODE_PRIVATE
        )
        var value: Any? = defaultValue
        if (defaultValue is String) {
            value = sp.getString(key, defaultValue as String)
        } else if (defaultValue is Int) {
            value = sp.getInt(key, (defaultValue as Int))
        } else if (defaultValue is Long) {
            value = sp.getLong(key, (defaultValue as Long))
        } else if (defaultValue is Boolean) {
            value = sp.getBoolean(key, (defaultValue as Boolean))
        } else if (defaultValue is Float) {
            value = sp.getFloat(key, (defaultValue as Float))
        }
        return value as V
    }

    /**
     * 清空数据
     */
    fun clearData() {
        val editor = application.getSharedPreferences(
            name,
            Context.MODE_PRIVATE
        ).edit()
        editor.clear()
        editor.apply()
    }
}