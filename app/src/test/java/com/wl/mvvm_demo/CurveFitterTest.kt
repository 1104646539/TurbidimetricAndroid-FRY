package com.wl.mvvm_demo

import com.wl.turbidimetric.ex.calcCon
import com.wl.turbidimetric.ex.matchingArg
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.util.CurveFitter
import com.wl.turbidimetric.util.CurveFitterUtil
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import org.junit.Test


class CurveFitterTest {

    @Test
    fun createCurve() {
        val cf1 = CurveFitter(
            doubleArrayOf(0.0, 50.0, 200.0, 500.0, 1000.0),
            doubleArrayOf(-1.0, 39.0, 225.0, 811.0, 1842.0)
        )
        repeat(CurveFitter.fitList.size) {

            cf1.doFit(it)
            repeat(cf1.params.size) { i ->
                print(" v=${cf1.params[i]}")
            }
            print("\n")
            println("---------")
        }

    }

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

        val cf = CurveFitter(ps, doubleArrayOf(m1, m2, m3, m4, m5))
        cf.doFitCon()

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
            val con2 = cf.f(cf.params, p)
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

        val cf = CurveFitter(ps, doubleArrayOf(m1, m2, m3, m4, m5))
        cf.doFitCon()

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
        val cf2 = CurveFitter(ps2, doubleArrayOf(m1, m2, m3, m4, m5))
        cf2.doFitCon()
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





    @Test
    fun testfj() {

        val p1 = 0.1349.toDouble()
        val p2 = 49.8117.toDouble()
        val p3 = 200.0589.toDouble()
        val p4 = 499.9939.toDouble()
        val p5 = 1000.0006.toDouble()

        val m1 = 0.toDouble()
        val m2 = 50.toDouble()
        val m3 = 200.toDouble()
        val m4 = 500.toDouble()
        val m5 = 1000.toDouble()

        val xs = doubleArrayOf(p1, p2, p3, p4, p5)
        val guess = doubleArrayOf(m1, m2, m3, m4, m5)

        val CORR = CurveFitterUtil.calcRSquared(xs, guess)
        System.out.println("运算结果是：")
        System.out.printf("CORR = " + CORR)
    }
}
