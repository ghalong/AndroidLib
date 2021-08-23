package net.o5g.android.lib

import android.app.Activity
import android.content.Intent
import android.os.Bundle


/**
 * Author:  王海宇<br>
 * Date:    2021/04/27 13:33<br>
 * Desc:    <br>
 * Edit History:<br>
 */
internal const val KEY_PERMISSION_COUNT = "KEY_PERMISSION_COUNT"

internal class PermissionRequestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val count = intent.getIntExtra(KEY_PERMISSION_COUNT, -1)
        PermissionUtils.checkPermissions(this, count) {
            finish()
        }
    }

    companion object {
        fun start(requestCount: Int) {
            LibBase.application.startActivity(
                Intent(
                    LibBase.application,
                    PermissionRequestActivity::class.java
                ).apply {
                    putExtra(KEY_PERMISSION_COUNT, requestCount)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
        }
    }
}