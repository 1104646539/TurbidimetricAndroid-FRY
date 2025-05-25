package com.wl.turbidimetric

import com.wl.turbidimetric.ex.matchingArg
import com.wl.turbidimetric.util.CurveFitterUtil
import com.wl.turbidimetric.util.FitterFactory
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.util.FourFun
import com.wl.turbidimetric.util.LinearFun
import com.wl.turbidimetric.util.ThreeFun
import org.junit.Before
import org.junit.Test
import kotlin.math.abs


class CurveFitterTest {

    lateinit var ratsFour: DoubleArray
    lateinit var tarsFour: DoubleArray

    @Before
    fun create() {
        ratsFour = doubleArrayOf(0.1, 15.9, 37.5, 178.8, 329.3, 437.7)
        tarsFour = doubleArrayOf(0.0, 25.0, 50.0, 200.0, 500.0, 1000.0)
    }

    /**
     * 三次多项式拟合测�?
     */
    @Test
    fun threadFunTest() {
        val p1 = 5.toDouble()
        val p2 = 28.toDouble()
        val p3 = 50.toDouble()
        val p4 = 210.toDouble()
        val p5 = 342.toDouble()
        val p6 = 435.toDouble()


        val m1 = 0.toDouble()
        val m2 = 25.toDouble()
        val m3 = 50.toDouble()
        val m4 = 200.toDouble()
        val m5 = 500.toDouble()
        val m6 = 1000.toDouble()

        val ps = doubleArrayOf(p1, p2, p3, p4, p5,p6)
        val ms = doubleArrayOf(m1, m2, m3, m4, m5,m6)
//        FitterType.values().forEach { type ->

        val fitter = FitterFactory.create(FitterType.Three)
        fitter.calcParams(ps, ms)
//            println("type=$type \n参数")
        fitter.params.forEach {
            print("it=$it")
        }
        println("\n验算")
        ps.forEach { v ->
            val con = fitter.ratCalcCon(fitter.params, v)
            print("con=$con")
        }
        println()
    }
    /**
     * 三次多项式拟合测�?
     */
    @Test
    fun threadFunTest3() {
        val p1 = 5.toDouble()
        val p2 = 54.toDouble()
        val p3 = 205.toDouble()
        val p4 = 343.toDouble()
        val p5 = 422.toDouble()


        val m1 = 0.toDouble()
        val m2 = 50.toDouble()
        val m3 = 200.toDouble()
        val m4 = 500.toDouble()
        val m5 = 1000.toDouble()

        val ps = doubleArrayOf(p1, p2, p3, p4, p5)
        val ms = doubleArrayOf(m1, m2, m3, m4, m5)
//        FitterType.values().forEach { type ->

        val fitter = FitterFactory.create(FitterType.Three)
        fitter.calcParams(ps, ms)
//            println("type=$type \n参数")
        fitter.params.forEach {
            print("it=$it")
        }
        println("\n验算")
        ps.forEach { v ->
            val con = fitter.ratCalcCon(fitter.params, v)
            println("con=$con")
        }
        println()
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
        println("--------------开始重新计�?-----------------------")
        var dif50 = 50 - con50fs
        println("50差值：$dif50")
        dif50 = if (dif50 >= 0) {
            dif50 * 2
        } else {
            dif50 / 2
        }
        println("50差值计算后�?$dif50")

        val dif0 = p1 - dif50
        println("0原始吸光�?-50差值计算后�?$dif0")

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
     * 线性拟合测�?
     */
    @Test
    fun linearFunTest() {
        val p1 = 1.toDouble()
        val p2 = 16.toDouble()
        val p3 = 35.toDouble()
        val p4 = 140.toDouble()
        val p5 = 310.toDouble()
        val p6 = 623.toDouble()


        val m1 = 0.toDouble()
        val m2 = 25.toDouble()
        val m3 = 50.toDouble()
        val m4 = 200.toDouble()
        val m5 = 500.toDouble()
        val m6 = 1000.toDouble()

        val ps = doubleArrayOf(p1, p2, p3, p4, p5, p6)
        val ms = doubleArrayOf(m1, m2, m3, m4, m5, m6)

        val fitter = FitterFactory.create(FitterType.Linear)
        fitter.calcParams(ps, ms)

        for (p in fitter.params) {
            println("params=$p")
        }

        for (p in ps) {
            val con = fitter.ratCalcCon(fitter.params, p)
            println("con=$con")
        }
        println("fitGoodness${fitter.fitGoodness}")
    }

    /**
     * 四参数拟合测�?
     */
    @Test
    fun fourFunTest2() {
        val fitter = FitterFactory.create(FitterType.Four)
        fitter.calcParams(ratsFour, tarsFour)
        println("参数")
        fitter.params.forEach {
            print("it=$it ")
        }
        val k = abs(fitter.params[0] - (-1.818473100987009)) < 0.0000001
        assert(k)
        println("\n验算")
        ratsFour.forEach { v ->
            val con = fitter.ratCalcCon(fitter.params, v)
            print("con=$con ")
        }
        println()

        tarsFour.forEach {
            val rat = fitter.conCalcRat(fitter.params, it)
            print("rat=$rat ")
        }
    }

    /**
     * 四参数拟合测�?
     */
    @Test
    fun fourFunTest() {
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
        val ms = doubleArrayOf(m1, m2, m3, m4, m5, m6)

        val fitter = FitterFactory.create(FitterType.Four)
        fitter.calcParams(ps, ms)
        println("参数")
        fitter.params.forEach {
            print("it=$it ")
        }
        println("\n验算")
        ps.forEach { v ->
            val con = fitter.ratCalcCon(fitter.params, v)
            print("con=$con ")
        }
        println()
    }


    @Test
    fun fitterFactory() {
//        val p1 = 0.1.toDouble()
////        val p2 = 15.9.toDouble()
//        val p3 = 37.5.toDouble()
//        val p4 = 178.8.toDouble()
//        val p5 = 329.3.toDouble()
//        val p6 = 437.7.toDouble()
//
//
//        val m1 = 0.toDouble()
////        val m2 = 25.toDouble()
//        val m3 = 50.toDouble()
//        val m4 = 200.toDouble()
//        val m5 = 500.toDouble()
//        val m6 = 1000.toDouble()
//        val ps = doubleArrayOf(p1, p3, p4, p5, p6)
//        val ms = doubleArrayOf(m1, m3, m4, m5, m6)
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


//        FitterType.values().forEach { type ->
        val fitter = FitterFactory.create(FitterType.Four)
        fitter.calcParams(ps, ms)
//            println("type=$type \n参数")
        fitter.params.forEach {
            print("it=$it")
        }
        println("\n验算")
        ps.forEach { v ->
            val con = fitter.ratCalcCon(fitter.params, v)
            print("con=$con")
        }
        println()
//        }

    }


    @Test
    fun testFourFunCalc() {
        val f0 = -1.818473101
        val f1 = 1.253206105
        val f2 = 374.2468309
        val f3 = 564.4612105

        val abss = doubleArrayOf(43.0, 41.0, -16.0)
        abss.forEach {
            val con = FourFun.f2(doubleArrayOf(f0, f1, f2, f3), it)
            println("con=$con")
        }
    }

    @Test
    fun testThreeFunCalc() {
        val f0 = 24.756992920270594
        val f1 = 0.8582045209676992
        val f2 = -5.707122868757991E-4
        val f3 = 1.950833853427454E-7

        val abss = doubleArrayOf(43.0, 41.0, -16.0)
        abss.forEach {
            val con = ThreeFun.f2(doubleArrayOf(f0, f1, f2, f3), it)
            println("con=$con")
        }
    }

    @Test
    fun testLinearFunCalc() {
        val f0 = 1.614742835
        val f1 = -6.930948268
        val f2 = 0.0
        val f3 = 0.0

        val abss = doubleArrayOf(43.0, 41.0, -16.0)
        abss.forEach {
            val con = LinearFun.f2(doubleArrayOf(f0, f1, f2, f3), it)
            println("con=$con")
        }
    }

    fun solveCubic(a: Double, b: Double, c: Double, d: Double, y: Double, guess: Double = 0.0): Double {
        // Ŀ�꺯��
        fun f(x: Double) = a + b * x + c * x * x + d * x * x * x - y
        // ����
        fun df(x: Double) = b + 2 * c * x + 3 * d * x * x

        var x = guess
        repeat(100) {
            val fx = f(x)
            val dfx = df(x)
            if (dfx == 0.0) return x // �������
            val x1 = x - fx / dfx
            if (abs(x1 - x) < 1e-8) return x1
            x = x1
        }
        return x
    }
}
