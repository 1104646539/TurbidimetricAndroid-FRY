package com.wl.turbidimetric.util

import org.apache.commons.math3.analysis.ParametricUnivariateFunction


/**
 * f(x) = d + ((a - d) / (1 + (x / c) ^ b))
 *
 *
 * 指数函数的求导： f(x) = a^x => f'(x) = a^x * ln(a)
 * 密函数的求导：f(x) = x^a => f'(x) = a * x^(a-1)
 * 复合函数求导：u =f(x) v = g(u) => v' = g'(u) * u'
 * 分数的求导公式：f(x) = u(x) / v(x) => f'(x) = (u' * v - u * v') / v^2
 *
 * @author 印隽
 * @date 2023/7/24 9:43
 */
class FourParameterFunction : ParametricUnivariateFunction {
    /**
     * 定义公式
     *
     * @param x
     * @param parameters
     * @return
     */
    override fun value(x: Double, vararg parameters: Double): Double {
        val a = parameters[0]
        val b = parameters[1]
        val c = parameters[2]
        val d = parameters[3]
        return d + (a - d) / (1 + Math.pow(x / c, b))
    }

    /**
     * 对变量求导，需要和value()的定义顺序一致
     *
     * @param x
     * @param parameters
     * @return
     */
    override fun gradient(x: Double, vararg parameters: Double): DoubleArray {
        val a = parameters[0]
        val b = parameters[1]
        val c = parameters[2]
        val d = parameters[3]
        val gradients = DoubleArray(4)
        val den = 1 + Math.pow(x / c, b)
        gradients[0] = 1 / den // 对 a 求导
        gradients[1] = -((a - d) * Math.pow(x / c, b) * Math.log(x / c)) / (den * den) // 对 b 求导
        gradients[2] = b * Math.pow(x / c, b - 1) * (x / (c * c)) * (a - d) / (den * den) // 对 c 求导
        gradients[3] = 1 - 1 / den // 对 d 求导
        return gradients
    }
}
