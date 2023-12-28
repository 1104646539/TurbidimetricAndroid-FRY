package com.wl.mvvm_demo

import com.wl.turbidimetric.ex.matchingArg
import com.wl.turbidimetric.util.CurveFitter
import com.wl.turbidimetric.util.CurveFitterUtil
import com.wl.turbidimetric.util.FourFun
import com.wl.turbidimetric.util.FourParameterFunction
import com.wl.turbidimetric.util.LinearFun
import org.apache.commons.math3.analysis.ParametricUnivariateFunction
import org.apache.commons.math3.fitting.SimpleCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import org.junit.Test


class CurveFitterTest {

    /**
     * 三次多项式拟合
     */
    @Test
    fun threadFunTest() {
        val p1 = -27.5.toDouble()
        val p2 = 28.8.toDouble()
        val p3 = 239.6.toDouble()
        val p4 = 975.6.toDouble()
        val p5 = 1979.toDouble()


        val m1 = 0.toDouble()
        val m2 = 1000.toDouble()
        val m3 = 500.toDouble()
        val m4 = 200.toDouble()
        val m5 = 50.toDouble()

        val ps = doubleArrayOf(p1, p2, p3, p4, p5)

        val cf = CurveFitterUtil()
        cf.calcParams(ps, doubleArrayOf(m1, m2, m3, m4, m5))

        val a = cf.params[0]
        val b = cf.params[1]
        val c = cf.params[2]
        val d = cf.params[3]

        for (p in cf.params) {
            println("params=$p")
        }
        println("df=${cf.fitGoodness}")
        for (p in ps) {
            val r3 = d.toBigDecimal().multiply(p.toBigDecimal().pow(3))
            val r2 = c.toBigDecimal().multiply(p.toBigDecimal().pow(2))
            val r = b.toBigDecimal().multiply(p.toBigDecimal())
            var con = a.toBigDecimal() + r + r2 + r3

            println("con=$con")
//            val con2 = cf.f(doubleArrayOf(4.26006, 1.27627, -0.00104, 0.0000003988), p)
            val con2 = CurveFitterUtil.f(cf.params, p)
            println("con2=$con2")
        }
        println("-------------------------------------")
        val cf2 = matchingArg(ps.map { it })

        for (p in cf2.params) {
            println("params=$p")
        }
        println("df=${cf2.fitGoodness}")
    }


    @Test
    fun testFixCurve() {
        val p1 = (-29.5).toDouble()
        val p2 = 28.8.toDouble()
        val p3 = 239.6.toDouble()
        val p4 = 975.6.toDouble()
        val p5 = 1979.toDouble()

        val m1 = 0.toDouble()
        val m2 = 50.toDouble()
        val m3 = 200.toDouble()
        val m4 = 500.toDouble()
        val m5 = 1000.toDouble()

        val ps = doubleArrayOf(p1, p2, p3, p4, p5)

        val cf = CurveFitterUtil()

        cf.calcParams(ps, doubleArrayOf(m1, m2, m3, m4, m5))

        val a = cf.params[0]
        val b = cf.params[1]
        val c = cf.params[2]
        val d = cf.params[3]

        for (p in cf.params) {
            println("params=$p")
        }
        println("df=${cf.fitGoodness}")
        var con50fs = 0.0
        for (i in ps.indices) {
            val r3 = d.toBigDecimal().multiply(ps[i].toBigDecimal().pow(3))
            val r2 = c.toBigDecimal().multiply(ps[i].toBigDecimal().pow(2))
            val r = b.toBigDecimal().multiply(ps[i].toBigDecimal())
            var con = a.toBigDecimal() + r + r2 + r3

            if (i == 1) {
                con50fs = con.toDouble()
            }
            print("con=$con")
//            val con2 = cf.f(doubleArrayOf(4.26006, 1.27627, -0.00104, 0.0000003988), p)
//            val con2 = cf.f(cf.params, p)
//            println("con2=$con2")

        }
        println("--------------开始重新计算-----------------------")
        var dif50 = 50 - con50fs
        println("50差值：$dif50")
        dif50 = if (dif50 >= 0) {
            dif50 * 2
        } else {
            dif50 / 2
        }
        println("50差值计算后：$dif50")

        val dif0 = p1 - dif50
        println("0原始吸光度-50差值计算后：$dif0")

        val np1 = dif0.toDouble()

        val ps2 = doubleArrayOf(np1, p2, p3, p4, p5)
        val cf2 = CurveFitterUtil()
        cf2.calcParams(ps2, doubleArrayOf(m1, m2, m3, m4, m5))

        for (p in cf2.params) {
            println("params2=$p")
        }

        val a2 = cf2.params[0]
        val b2 = cf2.params[1]
        val c2 = cf2.params[2]
        val d2 = cf2.params[3]

        for (i in ps2.indices) {
            val r3 = d2.toBigDecimal().multiply(ps2[i].toBigDecimal().pow(3))
            val r2 = c2.toBigDecimal().multiply(ps2[i].toBigDecimal().pow(2))
            val r = b2.toBigDecimal().multiply(ps2[i].toBigDecimal())
            var con = a2.toBigDecimal() + r + r2 + r3
            print("con2=$con")
//            val con2 = cf.f(doubleArrayOf(4.26006, 1.27627, -0.00104, 0.0000003988), p)
//            val con2 = cf.f(cf.params, p)
//            println("con2=$con2")

        }
    }

    /**
     * 线性拟合
     */
    @Test
    fun linearFunTest() {
        val p1 = -7.toDouble()
        val p2 = 22.toDouble()
        val p3 = 48.toDouble()
        val p4 = 200.toDouble()
        val p5 = 500.toDouble()
        val p6 = 1000.toDouble()


        val m1 = 0.toDouble()
        val m2 = 25.toDouble()
        val m3 = 50.toDouble()
        val m4 = 200.toDouble()
        val m5 = 500.toDouble()
        val m6 = 1000.toDouble()

        val ps = doubleArrayOf(p1, p2, p3, p4, p5, p6)

        val cf = LinearFun()
        cf.calcParams(ps, doubleArrayOf(m1, m2, m3, m4, m5, m6))

        for (p in cf.params) {
            println("params=$p")
        }

        for (p in ps) {
            val con = cf.f(cf.params, p)
            println("con=$con")
        }
        println("fitGoodness${cf.fitGoodness}")
    }

    @Test
    fun fourFunTest() {
        val p1 = 0.1.toDouble()
//        val p2 = 15.9.toDouble()
        val p3 = 37.5.toDouble()
        val p4 = 178.8.toDouble()
        val p5 = 329.3.toDouble()
        val p6 = 437.7.toDouble()


        val m1 = 0.toDouble()
//        val m2 = 25.toDouble()
        val m3 = 50.toDouble()
        val m4 = 200.toDouble()
        val m5 = 500.toDouble()
        val m6 = 1000.toDouble()

        val ps = doubleArrayOf(p1, p3, p4, p5, p6)

        val cf = FourFun()
        cf.calcParams(ps, doubleArrayOf(m1, m3, m4, m5, m6))

        for (p in cf.params) {
            println("params=$p")
        }

        for (p in ps) {
            val con = cf.f(cf.params, p)
            println("con=$con")
        }
        println("fitGoodness${cf.fitGoodness}")
    }

    @Test
    fun kl() {
        val p1 = 0.1.toDouble()
        val p2 = 15.9.toDouble()
        val p3 = 37.5.toDouble()
        val p4 = 178.8.toDouble()
        val p5 = 329.3.toDouble()
        val p6 = 437.7.toDouble()


        val m1 = 0.toDouble()
        val m2 = 25.toDouble()
        val m3 = 50.toDouble()
        val m4 = 200.toDouble()
        val m5 = 500.toDouble()
        val m6 = 1000.toDouble()

        val ps = doubleArrayOf(p1, p2, p3, p4, p5, p6)

        val cf = FourFun()
        val ppp = curveFit(
            arrayOf(
                doubleArrayOf(p1, m1),
                doubleArrayOf(p2, m2),
                doubleArrayOf(p3, m3),
                doubleArrayOf(p4, m4),
                doubleArrayOf(p5, m5),
                doubleArrayOf(p6, m6),
            ), FourParameterFunction()
        )

        for (p in ppp) {
            println("params=$p")
        }

//        for (p in ps) {
//            val con = cf.f(cf.params, p)
//            println("con=$con")
//        }
//        println("fitGoodness${cf.fitGoodness}")
    }


    fun curveFit(xy: Array<DoubleArray>, function: ParametricUnivariateFunction?): DoubleArray {
//        val m1 = 0.toDouble()
//        val m2 = 25.toDouble()
//        val m3 = 50.toDouble()
//        val m4 = 200.toDouble()
//        val m5 = 500.toDouble()
//        val m6 = 1000.toDouble()

        val guess = doubleArrayOf(1.0, 1.0, 1.0, 1.0)
//        val guess = doubleArrayOf(m1, m2, m3, m4, m5, m6)
        val curveFitter = SimpleCurveFitter.create(function, guess)
        val observedPoints = WeightedObservedPoints()
        for (i in xy.indices) {
            observedPoints.add(xy[i][0], xy[i][1])
        }
        return curveFitter.fit(observedPoints.toList())
    }

    @Test
    fun test2() {
        val p1 = 0.00001.toDouble()
        val p2 = 0.00159.toDouble()
        val p3 = 0.00375.toDouble()
        val p4 = 0.01788.toDouble()
        val p5 = 0.03293.toDouble()
        val p6 = 0.04377.toDouble()
//        val p1 = 0.1.toDouble()
//        val p2 = 15.9.toDouble()
//        val p3 = 37.5.toDouble()
//        val p4 = 178.8.toDouble()
//        val p5 = 329.3.toDouble()
//        val p6 = 437.7.toDouble()


        val m1 = 0.toDouble()
        val m2 = 25.toDouble()
        val m3 = 50.toDouble()
        val m4 = 200.toDouble()
        val m5 = 500.toDouble()
        val m6 = 1000.toDouble()
        val ps = doubleArrayOf(p1, p2, p3, p4, p5, p6)
        val ms = doubleArrayOf(m1, m2, m3, m4, m5, m6)
        val cf = CurveFitter(ps, ms)
        cf.doFit(7)

        println("params a1=${cf.params[0]} a2=${cf.params[3]} x0=${cf.params[2]} p=${cf.params[1]}")
        println("fitGoodness=${cf.fitGoodness}")
        cf.f(cf.params,0.00252)

    }

}
