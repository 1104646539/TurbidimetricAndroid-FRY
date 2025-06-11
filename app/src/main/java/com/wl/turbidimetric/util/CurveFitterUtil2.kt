package com.wl.turbidimetric.util

import android.util.Log
import com.wl.turbidimetric.ex.isNotValid
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import org.apache.commons.math3.stat.regression.SimpleRegression
import org.apache.log4j.helpers.FormattingInfo
import kotlin.math.abs
import kotlin.math.pow


interface Fitter {
    var params: DoubleArray //参数
    var fitGoodness: Double //拟合度
    var yss: DoubleArray //验算值
    fun calcParams(values: DoubleArray, guess: DoubleArray)
    fun ratCalcCon(p: DoubleArray, x: Double): Double
    fun conCalcRat(p: DoubleArray, x: Double): Double
}

enum class FitterType(val showName: String) {
    Three("三次多项式拟合"), Linear("线性拟合"), Four("四参数曲线拟合");

    companion object {
        fun toValue(index: Int): FitterType {
            values().forEachIndexed { i, fitterType ->
                if (index == i) {
                    return fitterType
                }
            }
            return Three
        }
    }
}

class FitterFactory {
    companion object {
        @JvmStatic
        fun create(fitterType: FitterType): Fitter {
            return when (fitterType) {
                FitterType.Three -> ThreeFun()
                FitterType.Linear -> LinearFun()
                FitterType.Four -> FourFun()
                else -> {
                    throw Exception("没有这个类型")
                }
            }
        }
    }
}

/**
 * 三次方多项式拟合
 */
class ThreeFun : Fitter {
    var guess: DoubleArray = doubleArrayOf()
    var values: DoubleArray = doubleArrayOf()

    override var params: DoubleArray = doubleArrayOf()//参数
    override var fitGoodness = 0.0//R方
    override var yss: DoubleArray = doubleArrayOf()//反算验证的


    override fun calcParams(values: DoubleArray, guess: DoubleArray) {
        this.guess = guess
        this.values = values
        //step1、拟合曲线
        val points = WeightedObservedPoints()

        for (i in guess.indices) {
            points.add(guess[i], values[i])
        }

        val fitter = PolynomialCurveFitter.create(3) //指定多项式阶数

        params = fitter.fit(points.toList()) // 曲线拟合，结果保存于数组


        yss = DoubleArray(guess.size)
        //step2、验算
        for (i in guess.indices) {
            yss[i] = ratCalcCon(params, values[i])
        }

        //step3、计算R方
        fitGoodness = calcRSquared2()
    }

    override fun ratCalcCon(p: DoubleArray, x: Double): Double {
        return f2(p, x, guess, values)
    }

    override fun conCalcRat(p: DoubleArray, x: Double): Double {
        return f2(p, x, guess, values)
    }


    private fun calcRSquared2(): Double {
        return (CurveFitterUtil.calcuteNumerator(
            guess, yss
        ) / CurveFitterUtil.calculateDenominator(guess, yss)).isNotValid()
    }

    companion object {
        //        @JvmStatic
//        fun f2(p: DoubleArray, x: Double): Double {
//            return p[0] + p[1] * x + p[2] * x * x + p[3] * x * x * x
//        }
        fun f2(p: DoubleArray, y: Double, guess: DoubleArray, values: DoubleArray): Double {
            // 目标函数
            fun f(x: Double) = p[0] + p[1] * x + p[2] * x * x + p[3] * x * x * x - y

            // 导数
            fun df(x: Double) = p[1] + 2 * p[2] * x + 3 * p[3] * x * x

            var x = y
            val index = values.indexOfLast { d -> d <= y }
            if (index >= 0 && guess.size > index) {
                x = guess[index]
            }
            if (x < 0) {
                x = 0.0
            }
//            for (value in values) {
//                print("value=${value}")
//            }
//            println("初始猜想 反应度=${y} 浓度=${x} index=${index}")
            repeat(5000) {
                val fx = f(x)
                val dfx = df(x)
                if (dfx == 0.0) {// 避免除零
//                    println("浓度=${x}")
                    return x
                }
                val x1 = x - fx / dfx
                if (abs(x1 - x) < 1e-6) {
//                    println("浓度=${x1}")
                    return x1
                }
                x = x1
            }
//            println("浓度=${x}")
            return x
        }

    }}
/**
 * 线性拟合
 */
class LinearFun : Fitter {
    override var params: DoubleArray = doubleArrayOf()//参数
    override var fitGoodness = 0.0//R方
    override var yss: DoubleArray = doubleArrayOf()//反算验证的

    override fun calcParams(values: DoubleArray, guess: DoubleArray) {
        //step1、拟合曲线
        val simple = SimpleRegression()
        guess.forEachIndexed { i, v ->
            simple.addData(values[i], guess[i])
        }
        params = DoubleArray(2)
        params[0] = simple.slope
        params[1] = getParamK(simple)

        //step2、验算
        yss = DoubleArray(values.size)
        for (i in yss.indices) {
            yss[i] = ratCalcCon(params, values[i])
        }

        //step3、计算R方
        fitGoodness = simple.rSquare.isNotValid()
    }

    override fun ratCalcCon(p: DoubleArray, x: Double): Double {
        return f2(p, x)
    }

    override fun conCalcRat(p: DoubleArray, x: Double): Double {
        return x - p[1] / p[0]
    }

    /**
     * 通过反射执行私有方法
     */
    private fun getParamK(simple: SimpleRegression): Double {
        val clazz: Class<*> = SimpleRegression::class.java
        val method = clazz.getDeclaredMethod("getIntercept", Double::class.java)
        method.isAccessible = true
        val re = method.invoke(simple, simple.slope)
        return re as Double
    }


    companion object {
        @JvmStatic
        fun f2(p: DoubleArray, x: Double): Double {
            return p[0] * x + p[1]
        }
    }
}

/**
 * 四参数拟合
 */
class FourFun : Fitter {
    override var params: DoubleArray = doubleArrayOf()//参数
    override var fitGoodness = 0.0//R方
    override var yss: DoubleArray = doubleArrayOf()//反算验证的

    override fun calcParams(values: DoubleArray, guess: DoubleArray) {
        //step1、拟合曲线
        val cf = CurveFitter(guess, values)
        cf.doFit(7)
        params = cf.params

        //step2、验算
        yss = DoubleArray(values.size)
        for (i in yss.indices) {
            yss[i] = ratCalcCon(params, values[i])
        }

        //step3、计算R方
        fitGoodness = cf.fitGoodness.isNotValid()

    }

    override fun ratCalcCon(p: DoubleArray, x: Double): Double {
        return f2(p, x)
    }

    override fun conCalcRat(p: DoubleArray, x: Double): Double {
        //y = (A - D) / [1 + (x/C)^B] + D
        return ((p[0] - p[3]) / (1 + (x / p[2]).pow(p[1]))) + p[3]
    }


    companion object {
        @JvmStatic
        fun f2(p: DoubleArray, x: Double): Double {
            return (p[2] * (((p[3] - p[0]) / (p[3] - x)) - 1).pow((1 / p[1])))
//            return CurveFitter.f(7, p, x)
        }
    }

}
