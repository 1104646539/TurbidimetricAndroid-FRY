package com.wl.turbidimetric.util

import org.apache.commons.math3.analysis.ParametricUnivariateFunction
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.SimpleCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import org.apache.commons.math3.stat.regression.SimpleRegression


interface Fitter {
    fun calcParams(values: DoubleArray, guess: DoubleArray)
    fun f(p: DoubleArray, x: Double): Double
}

enum class FitterType(val showName: String) {
    Three("三次多项式拟合"), Linear("线性拟合"), Four("四参数曲线拟合");
}

class FitterFactory {
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


class ThreeFun : Fitter {
    var guess: DoubleArray = doubleArrayOf()
    var values: DoubleArray = doubleArrayOf()
    var params: DoubleArray = doubleArrayOf()//参数
    var yss: DoubleArray = doubleArrayOf()//反算验证的
    var fitGoodness = 0.0//R方


    override fun calcParams(values: DoubleArray, guess: DoubleArray) {
        this.guess = guess
        this.values = values
        //step1、拟合曲线
        val points = WeightedObservedPoints()

        for (i in values!!.indices) {
            points.add(values!![i], guess!![i])
        }

        val fitter = PolynomialCurveFitter.create(3) //指定多项式阶数

        params = fitter.fit(points.toList()) // 曲线拟合，结果保存于数组


        yss = DoubleArray(guess!!.size)
        //step2、验算
        for (i in guess!!.indices) {
            yss!![i] = f(params, values!![i])
        }

        //step3、计算R方
        fitGoodness = calcRSquared2()
    }

    override fun f(p: DoubleArray, x: Double): Double {
        return f2(p, x)
    }


    private fun calcRSquared2(): Double {
        return CurveFitterUtil.calcuteNumerator(
            guess, yss
        ) / CurveFitterUtil.calculateDenominator(guess, yss)
    }

    companion object {
        @JvmStatic
        fun f2(p: DoubleArray, x: Double): Double {
            return p[0] + p[1] * x + p[2] * x * x + p[3] * x * x * x
        }
    }
}

class LinearFun : Fitter {
    var params: DoubleArray = doubleArrayOf()//参数
    var yss: DoubleArray = doubleArrayOf()//反算验证的
    var fitGoodness = 0.0//R方

    override fun calcParams(values: DoubleArray, guess: DoubleArray) {
        //step1、拟合曲线
        val simple = SimpleRegression()
        values.forEachIndexed { i, v ->
            simple.addData(guess[i], values[i])
        }
        params = DoubleArray(2)
        params[0] = simple.slope
        params[1] = getParamK(simple)

        //step2、验算
        yss = DoubleArray(guess!!.size)
        for (i in guess!!.indices) {
            yss[i] = f(params, values!![i])
        }

        //step3、计算R方
        fitGoodness = simple.rSquare
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

    override fun f(p: DoubleArray, x: Double): Double {
        return f2(p, x)
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
    var params: DoubleArray = doubleArrayOf()//参数
    var yss: DoubleArray = doubleArrayOf()//反算验证的
    var fitGoodness = 0.0//R方

    override fun calcParams(values: DoubleArray, guess: DoubleArray) {
        //step1、拟合曲线
        val cf = CurveFitter(values, guess)
        cf.doFit(7)
        params = cf.params

        //step2、验算
        yss = DoubleArray(guess!!.size)
        for (i in guess!!.indices) {
            yss[i] = f(params, values!![i])
        }

        //step3、计算R方
        fitGoodness = cf.fitGoodness

    }

    override fun f(p: DoubleArray, x: Double): Double {
        return f2(p, x)
    }

    companion object {
        @JvmStatic
        fun f2(p: DoubleArray, x: Double): Double {
            return CurveFitter.f(7, p, x)
        }
    }

}
