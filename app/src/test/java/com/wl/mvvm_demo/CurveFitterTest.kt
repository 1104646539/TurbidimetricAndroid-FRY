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
            doubleArrayOf(0.0, 50.0, 200.0, 500.0, 1000.0),
            doubleArrayOf(-1.0, 39.0, 225.0, 811.0, 1842.0)
        )
        repeat(CurveFitter.fitList.size) {

            cf1.doFit(it)
            repeat(cf1.params.size) {
                i->
                print(" v=${cf1.params[i]}")
            }
            print("\n")
            println("---------")
        }


//        val f0 = String.format("%.6f", cf1.params[0]).toDouble()
//        val f1 = String.format("%.6f", cf1.params[1]).toDouble()
//        val f2 = String.format("%.6f", cf1.params[2]).toDouble()
//        val f3 = String.format("%.6f", cf1.params[3]).toDouble()

//        println("f0=$f0 f1=$f1 f2=$f2 f3=$f3 ")
//        println("f0=$f0 f1=$f1 f2=$f2 f3=$f3 ")
//        Assert.assertEquals(a1, -1.82824, 0.1)
    }

    @Test
    fun threadFunTest() {
        val p1 = -13.toDouble()
        val p2 = 1796.toDouble()
        val p3 = 767.toDouble()
        val p4 = 211.toDouble()
        val p5 = 35.toDouble()


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
