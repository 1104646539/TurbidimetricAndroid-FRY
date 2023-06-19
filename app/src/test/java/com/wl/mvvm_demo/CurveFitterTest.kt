package com.wl.mvvm_demo

import com.wl.turbidimetric.ex.matchingArg
import com.wl.turbidimetric.util.CurveFitter
import org.junit.Assert
import org.junit.Test
import kotlin.math.pow

class CurveFitterTest {

    @Test
    fun createCurve() {
        val cf1 = CurveFitter(
            doubleArrayOf(0.0, 25.0, 50.0, 200.0, 500.0, 1000.0),
            doubleArrayOf(0.07725, 15.9416750, 37.5354200, 178.8007500, 329.27181000, 437.7187)
        )
        cf1.doFitCon()
        cf1.params

        val a1 = String.format("%.2f", cf1.params[0]).toDouble()
        val a12 = String.format("%.2f", -1.82824).toDouble()
        Assert.assertEquals(a1, -1.82824, 0.1)
    }

    @Test
    fun threadFunTest() {
        val p1 = 60.toDouble()
        val p2 = 27298.toDouble()
        val p3 = 11722.toDouble()
        val p4 = 2722.toDouble()
        val p5 = 91.toDouble()



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
}
