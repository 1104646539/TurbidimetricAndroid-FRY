package com.wl.wllib

import java.math.BigDecimal
import java.math.RoundingMode


/**
 * 用来将double类型的值保留几位小数 默认规则为四舍五入
 * @receiver Double
 * @param scale Int
 * @return Double
 */

fun Double.scale(scale: Int,roundingMode: Int = BigDecimal.ROUND_HALF_UP): Double {
    return try {
        val two = BigDecimal(this)
        two.setScale(scale, roundingMode).toDouble()
    } catch (e: Exception) {
        this
    }
}
