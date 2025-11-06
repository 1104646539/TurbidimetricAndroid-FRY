package com.wl.wllib

import java.text.SimpleDateFormat
import java.util.Date

/**
 * 时间戳 日期相关
 */
object DateUtil {
    /**
     * 例：2022-05-20 15:30:21
     */
    @JvmStatic
    val PATTERN_TIME_1 = "yyyy-MM-dd HH:mm:ss"

    /**
     * 例：2022-05-20 15-30-21
     */
    @JvmStatic
    val PATTERN_TIME_2 = "yyyy-MM-dd HH-mm-ss"

    /**
     * 例：2022-05-20 15:30:21 200
     */
    @JvmStatic
    val PATTERN_TIME_3 = "yyyy-MM-dd HH:mm:ss SSS"

    /**
     * 例：2022-05-20 15:30
     */
    @JvmStatic
    val PATTERN_TIME_4 = "yyyy-MM-dd HH:mm"

    /**
     * 例：2022-05-20
     */
    @JvmStatic
    val PATTERN_DATE_1 = "yyyy-MM-dd"

    /**
     * 例：20220520153050
     */
    @JvmStatic
    val PATTERN_TIME_5 = "yyyyMMddHHmmss"
    /**
     * 例：15:30
     */
    @JvmStatic
    val PATTERN_TIME_6 = "HH:mm"
    /**
     * 例：10-25 15:30
     */
    @JvmStatic
    val PATTERN_TIME_7 = "MM-dd HH:mm"

    /**
     * @see PATTERN_TIME_1
     */
    @JvmStatic
    val Time1Format by lazy { SimpleDateFormat(PATTERN_TIME_1) }

    /**
     * @see PATTERN_TIME_2
     */
    @JvmStatic
    val Time2Format by lazy { SimpleDateFormat(PATTERN_TIME_2) }

    /**
     * @see PATTERN_TIME_3
     */
    @JvmStatic
    val Time3Format by lazy { SimpleDateFormat(PATTERN_TIME_3) }

    /**
     * @see PATTERN_TIME_4
     */
    @JvmStatic
    val Time4Format by lazy { SimpleDateFormat(PATTERN_TIME_4) }
    /**
     * @see PATTERN_TIME_5
     */
    @JvmStatic
    val Time5Format by lazy { SimpleDateFormat(PATTERN_TIME_5) }
    /**
     * @see PATTERN_TIME_6
     */
    @JvmStatic
    val Time6Format by lazy { SimpleDateFormat(PATTERN_TIME_6) }
    /**
     * @see PATTERN_TIME_7
     */
    @JvmStatic
    val Time7Format by lazy { SimpleDateFormat(PATTERN_TIME_7) }

    /**
     * @see PATTERN_DATE_1
     */
    @JvmStatic
    val Date1Format by lazy { SimpleDateFormat(PATTERN_DATE_1) }

    @JvmStatic
    fun date2Str(date: Date, format: SimpleDateFormat): String {
        return format.format(date)
    }

    @JvmStatic
    fun long2Str(timestamp: Long, format: SimpleDateFormat): String {
        return date2Str(Date(timestamp), format)
    }
}
