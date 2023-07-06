package com.wl.turbidimetric.ex

import java.text.SimpleDateFormat
import java.util.*

private val DateFormatLong = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
private val DateFormatBigLong = SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS")

/**
 * Date转换为长字符串
 * yyyy-MM-dd HH:mm:ss SSS
 * @see DateFormatLong
 * @receiver Date
 */
fun Date.toLongString(): String {
    return toString(DateFormatLong)
}

/**
 * Date转换为超长字符串
 * @see DateFormatBigLong
 * @receiver Date
 */
fun Date.toBigLongString(): String {
    return toString(DateFormatBigLong)
}

/**
 * Date转换为字符串
 * @receiver Date
 * @param format SimpleDateFormat 格式
 */
fun Date.toString(format: SimpleDateFormat): String {
    return format.format(this)
}

/**
 * Long转时间
 * @receiver Long
 * @return Date
 */
fun Long.toDate(): Date {
    return Date(this)
}

/**
 * 超长字符串转时间
 * yyyy-MM-dd HH:mm:ss SSS to Date
 * @receiver Long
 * @return Date
 */
fun String.bigLongStrToDate(): Date {
    return strToDate(DateFormatBigLong)
}

/**
 * 长字符串转时间
 * yyyy-MM-dd HH:mm:ss to Date
 * @receiver Long
 * @return Date
 */
fun String.longStrToDate(): Date {
    return strToDate(DateFormatLong)
}

/**
 * 字符串转时间
 * str to Date
 * @receiver Long
 * @return Date
 */
fun String.strToDate(format: SimpleDateFormat): Date {
    return format.parse(this)
}
/**
 * 字符串转时间戳
 * str to Date
 * @receiver Long
 * @return Date
 */
fun String.strToLong(format: SimpleDateFormat= DateFormatLong): Long {
    return format.parse(this).time
}

/**
 * 时间戳转字符串
 * @receiver Long
 * @param format SimpleDateFormat
 * @return String
 */
fun Long.longToStr(format: SimpleDateFormat = DateFormatLong): String {
    return Date(this).toString(format)
}
