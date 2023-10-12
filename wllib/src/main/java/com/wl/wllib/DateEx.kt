package com.wl.wllib

import java.text.SimpleDateFormat
import java.util.Date

/**
 * 时间戳转时间字符串
 * @receiver Long
 * @param format SimpleDateFormat
 * @return String
 */
fun Long.toTimeStr(format: SimpleDateFormat = DateUtil.Time1Format): String {
    return DateUtil.long2Str(this, format)
}

/**
 * 时间字符串转时间戳
 * @receiver String
 * @return String
 */
fun String.toLong(format: SimpleDateFormat = DateUtil.Time1Format): Long {
    return format.parse(this).time
}

/**
 * 时间字符串转Date
 * @receiver String
 * @return String
 */
fun String.toDate(format: SimpleDateFormat = DateUtil.Time1Format): Date {
    return format.parse(this)
}

/**
 * Date转时间字符串
 * @receiver Date
 * @param format SimpleDateFormat
 * @return String
 */
fun Date.toTimeStr(format: SimpleDateFormat = DateUtil.Time1Format): String {
    return format.format(this)
}

/**
 * 时间戳转时间字符串
 * @receiver Long
 * @return String
 */
fun Long.toLongTimeStr(): String {
    return toTimeStr(DateUtil.Time3Format)
}

/**
 * 时间字符串转时间戳
 * @receiver String
 * @return String
 */
fun String.longStrToLong(): Long {
    return toLong(DateUtil.Time3Format)
}
/**
 * 时间字符串转Date
 * @receiver String
 * @return String
 */
fun String.longStrToDate(): Date {
    return toDate(DateUtil.Time3Format)
}
/**
 * Date转时间字符串
 * @receiver Date
 * @return String
 */
fun Date.toLongTimeStr(): String {
    return toTimeStr(DateUtil.Time3Format)
}
