package com.wl.wllib

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * toast
 */
object ToastUtil {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
    }

    @JvmStatic
    fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        if (context == null) throw Exception("未设置context")
        Toast.makeText(context!!.applicationContext, msg, duration).show()
    }

    @JvmStatic
    fun showToast(msg: String) {
        showToast(msg, Toast.LENGTH_SHORT)
    }

}
