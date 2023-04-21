package com.wl.mvvm_demo

import com.wl.turbidimetric.util.CurveFitter
import org.junit.Assert
import org.junit.Test

class CurveFitterTest {

    @Test
    fun createCurve() {
        val cf1 = CurveFitter(
            doubleArrayOf(0.0,25.0, 50.0, 200.0, 500.0, 1000.0),
            doubleArrayOf(0.07725, 15.9416750, 37.5354200, 178.8007500, 329.27181000, 437.7187)
        )
        cf1.doFitCon()
        cf1.params

        val a1 = String.format("%.2f",cf1.params[0]).toDouble()
        val a12 = String.format("%.2f",-1.82824).toDouble()
        Assert.assertEquals(a1,-1.82824,0.1)
    }

}
