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
    val toast: Toast by lazy { Toast.makeText(context!!.applicationContext, "", Toast.LENGTH_SHORT) }

    fun init(context: Context) {
        this.context = context
    }

    @JvmStatic
    fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        if (msg.isNullOrEmpty()) return
        toast.setText(msg)
        toast.duration = duration
        toast.show()
    }

    @JvmStatic
    fun showToast(msg: String) {
        showToast(msg, Toast.LENGTH_SHORT)
    }

}
