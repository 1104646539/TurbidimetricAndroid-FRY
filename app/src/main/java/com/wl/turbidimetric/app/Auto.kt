package com.wl.turbidimetric.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wl.turbidimetric.main.MainActivity


class Auto : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val newIntent = Intent(context, MainActivity::class.java)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) //注意，必须添加这个标记，否则启动会失败
            context.startActivity(newIntent)
        }
    }
}
