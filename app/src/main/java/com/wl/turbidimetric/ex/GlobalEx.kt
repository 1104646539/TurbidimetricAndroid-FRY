package com.wl.turbidimetric.ex

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.wl.turbidimetric.App
import com.wl.turbidimetric.R
import timber.log.Timber
import java.math.BigDecimal

/**
 * 用来将double类型的值四舍五入下保留几位小数
 * @receiver Double
 * @param scale Int
 * @return Double
 */
fun Double.scale(scale: Int): Double {
    try {
        val two = BigDecimal(this)
        return two.setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()
    } catch (e: Exception) {
        return 0.0;
    }
}


fun snack(view: View, msg: String) {
    if (msg.isNullOrEmpty()) return
    Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
}
