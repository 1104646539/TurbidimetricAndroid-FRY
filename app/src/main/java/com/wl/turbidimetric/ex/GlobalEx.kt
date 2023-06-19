package com.wl.turbidimetric.ex

import android.content.res.Resources
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.wl.turbidimetric.App
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.home.HomeViewModel.*
import com.wl.wllib.ToastUtil
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

/**
 * 用来将double类型的值四舍五入下保留几位小数
 * @receiver Double
 * @param scale Int
 * @return Double
 */
fun Double.scale(scale: Int): Double {
    return try {
        val two = BigDecimal(this)
        two.setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()
    } catch (e: Exception) {
        0.0
    }
}

/**
 * 显示snack提示
 * @param view View
 * @param msg String
 */
fun snack(view: View, msg: String) {
    if (msg.isNullOrEmpty()) return
    Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
}

/**
 * 显示toast提示
 * @param msg String
 */
fun toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    if (msg.isNullOrEmpty()) return
    if (duration == Toast.LENGTH_LONG)
        ToastUtil.showToastLong(msg)
    else
        ToastUtil.showToast(msg)
}

fun getResource(): Resources {
    return App.instance!!.resources
}

inline fun Array<Array<CuvetteItem>?>.print(): String {
    val sb = StringBuffer()
    this.forEachIndexed { i, ts ->
        ts?.forEachIndexed { j, t ->
            sb.append("[${t.state},${t.testResult?.id}] ")
        }
        sb.append("\n")
    }
    return sb.toString();
}

inline fun Array<Array<SampleItem>?>.print(): String {
    val sb = StringBuffer()
    this.forEachIndexed { i, ts ->
        ts?.forEachIndexed { j, t ->
            sb.append("[${t.state},${t.testResult?.id}] ")
        }
        sb.append("\n")
    }
    return sb.toString();
}
