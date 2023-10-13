package com.wl.weiqianwllib

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import java.util.regex.Pattern

object OrderUtil {

    /**
     * 显示和隐藏导航栏
     * @param activity
     * @param showNav
     */
    @JvmStatic
    fun showHideNav(activity: Activity, showNav: Boolean) {
        val intent = Intent("com.android.intent.action.NAVBAR_SHOW")
        intent.putExtra("cmd", if (showNav) "show" else "hide")
        activity.sendBroadcast(intent)
    }

    /**
     * 获取打开桌面的intent
     * @return Intent
     */
    @JvmStatic
    fun getLauncher(): Intent {
        val showIntent = Intent()
        val cn = ComponentName("com.android.launcher3", "com.android.launcher3.Launcher")
        showIntent.component = cn
        return showIntent
    }
}
