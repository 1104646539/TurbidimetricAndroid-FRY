package com.wl.wllib

import android.app.Activity
import androidx.annotation.LayoutRes
import java.util.regex.Pattern
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/**
 * 判断是否是ip格式
 *
 * @param addr
 * @return
 */
fun isIP(addr: String): Boolean {
    if (addr.length < 7 || addr.length > 15 || "" == addr) {
        return false
    }
    /**              * 判断IP格式和范围               */
    val rexp =
        "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}"
    val pat = Pattern.compile(rexp)
    val mat = pat.matcher(addr)
    return mat.find()
}


