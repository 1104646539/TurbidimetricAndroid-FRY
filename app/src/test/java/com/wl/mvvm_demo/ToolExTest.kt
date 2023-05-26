package com.wl.mvvm_demo

import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.util.CurveFitter
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class ToolExTest {

    @Test
    fun crc16Test() {
        Assert.assertTrue(
            CRC16(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u)).toByteArray()
                .contentEquals(ubyteArrayOf(0x18u, 0x3cu).toByteArray())
        )

        Assert.assertFalse(
            CRC16(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u)).toByteArray()
                .contentEquals(ubyteArrayOf(0x00u, 0x00u).toByteArray())
        )

    }

    @Test
    fun verifyCrcTest() {
        Assert.assertTrue(VerifyCrc(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u, 0x18u, 0x3cu)))

        Assert.assertFalse(VerifyCrc(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u, 0x11u, 0x11u)))

        Assert.assertThrows("错误 数组长度错误 ba.size==8", IndexOutOfBoundsException::class.java) {
            VerifyCrc(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u, 0x11u, 0x11u, 0x12u))
        }
    }

    @Test
    fun getStepTest() {
        Assert.assertEquals(getStep(0xffu, 0), 1)
        Assert.assertEquals(getStep(0xffu, 1), 1)
        Assert.assertEquals(getStep(0xffu, 2), 1)
        Assert.assertEquals(getStep(0xffu, 3), 1)
        Assert.assertEquals(getStep(0xffu, 4), 1)
        Assert.assertEquals(getStep(0xffu, 5), 1)
        Assert.assertEquals(getStep(0xffu, 6), 1)
        Assert.assertEquals(getStep(0xffu, 7), 1)

        Assert.assertEquals(getStep(0xf0u, 0), 0)
        Assert.assertEquals(getStep(0xf0u, 1), 0)
        Assert.assertEquals(getStep(0xf0u, 2), 0)
        Assert.assertEquals(getStep(0xf0u, 3), 0)
        Assert.assertEquals(getStep(0xf0u, 4), 1)
        Assert.assertEquals(getStep(0xf0u, 5), 1)
        Assert.assertEquals(getStep(0xf0u, 6), 1)
        Assert.assertEquals(getStep(0xf0u, 7), 1)

        Assert.assertNotEquals(getStep(0x0fu, 0), 0)
        Assert.assertNotEquals(getStep(0x0fu, 1), 0)
        Assert.assertNotEquals(getStep(0x0fu, 2), 0)
        Assert.assertNotEquals(getStep(0x0fu, 3), 0)
        Assert.assertNotEquals(getStep(0x0fu, 4), 1)
        Assert.assertNotEquals(getStep(0x0fu, 5), 1)
        Assert.assertNotEquals(getStep(0x0fu, 6), 1)
        Assert.assertNotEquals(getStep(0x0fu, 7), 1)

        Assert.assertThrows("错误 pos下标错误 pos=-1", IndexOutOfBoundsException::class.java) {
            getStep(0x0fu, -1)
        }
        Assert.assertThrows("错误 pos下标错误 pos=8", IndexOutOfBoundsException::class.java) {
            getStep(0x0fu, 8)
        }
    }

    @Test
    fun mergeTest() {
        Assert.assertEquals(merge(ubyteArrayOf(0xffu, 0xffu)), 65535)

        Assert.assertEquals(merge(ubyteArrayOf(0xf0u, 0x52u)), 61522)
    }


    @Test
    fun scaleTest() {
        val re: Double = 380.251125412.scale(2)
        Assert.assertEquals(re, 380.25, 0.01)
    }

    @Test
    fun dateTest() {
        val longStr = "2023-04-23 10:20:30 252"

        Assert.assertEquals(longStr.bigLongStrToDate().toLongString(), "2023-04-23 10:20:30")
        Assert.assertEquals(longStr.bigLongStrToDate().toBigLongString(), "2023-04-23 10:20:30 252")
        Assert.assertEquals(longStr.bigLongStrToDate().time, 1682216430252)

        val longDate = 1682827630000L
        Assert.assertEquals(longDate.toDate().toBigLongString(), "2023-04-30 12:07:10 000")
        Assert.assertEquals(longDate.toDate().toLongString(), "2023-04-30 12:07:10")
//        println(longStr.bigLongStrToDate().toLongString())
//        println(longStr.bigLongStrToDate().toBigLongString())
//        println(longStr.bigLongStrToDate().time)

    }

    @Test
    fun localDataTest() {
        val num1 = "9"
        Assert.assertEquals(LocalData.getDetectionNumInc(num1), "10")
        val num2 = "0022"
        Assert.assertEquals(LocalData.getDetectionNumInc(num2), "0023")

    }

    @Test
    fun aTest() {
        val base = 65536.toDouble()
        val sourceValue1 = 65000.toDouble()
        val sourceValue2 = 58888.toDouble()
        val sourceValue3 = 46666.toDouble()
        val sourceValue4 = 32000.toDouble()

        val re1 = log10(base / sourceValue1)
        val re2 = log10(base / sourceValue2)
        val re3 = log10(base / sourceValue3)
        val re4 = log10(base / sourceValue4)

        val re = re4 - re1

        println("re1=$re1 re2=$re2 re3=$re3 re4=$re4 re=$re ")
        val oldRe = calcAbsorbance(
            sourceValue1.toBigDecimal()
        )
        println("oldRe=$oldRe ")
    }

    @Test
    fun doubleTest() {
        val v1 = 123.3 / 100

        println("v1=$v1")

        val d1 = BigDecimal(123.3)
        val d2 = BigDecimal(100.0)
        val v2 = d1.divide(d2, 10, BigDecimal.ROUND_HALF_UP).toDouble()
        println("v2=$v2")
    }


    @Test
    fun newCalcBigD() {
        val a1 = -0.00283;
        val a2 = 9.07;
        val x0 = 34909;
        val p = 0.82816;
        val absorbance = 0.1
        var d1 = a2 - absorbance
        var d2 = p
        if (d1 == 0.0 || d2 == 0.0) {
            println("newCalcBigD dividend1==0||dividend2==0 d1=$d1 d2=$d2")
            return
        }
        var temp11 = (a2 - a1) / d1 - 1
        var temp12 = temp11.pow(1 / d2).scale(10)
//        var con1: Double = x0 * ((a2 - a1) / (a2 - absorbance) - 1).pow(1 / p).scale(5)
        var con1: Double = x0 * temp12
        println("newCalcBigD con1=$con1 d1=$d1 d2=$d2 temp11=$temp11 temp12=$temp12")

        var dividend1 = BigDecimal(a2)
            .subtract(BigDecimal(absorbance)).setScale(10, BigDecimal.ROUND_HALF_UP)
        var dividend2 = p
        if (dividend1.compareTo(BigDecimal(0)) == 0 || dividend2 == 0.0) {
            println("newCalcBigD dividend1==0||dividend2==0 dividend1=$dividend1 dividend2=$dividend2")
            return
        }
        val temp21 = BigDecimal(a2).subtract(BigDecimal(a1)).divide(
            dividend1, 10, BigDecimal.ROUND_HALF_UP
        ).subtract(BigDecimal(1))

        val temp22 = temp21.toDouble().pow((1 / dividend2)).scale(10)
        val con2 = (x0 * temp22)

        println("newCalcBigD con2=$con2 dividend1=$dividend1 dividend2=$dividend2 temp21=$temp21 temp22=$temp22")
    }

    @Test
    fun cvTest() {

        val cons = mutableListOf(
            495.0,
            489.0,
            481.0,
            490.0,
            469.0,
            473.0,
            465.0,
            470.0,
            459.0,
            471.0,
//            1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0
        )

        val sd = calculateSD(cons.toDoubleArray())
        val mean = calculateMean(cons.toDoubleArray())
        val cv = sd / mean
        println("nsd=$sd ncv=$cv")

        val pi = NumberFormat.getPercentInstance()
        pi.maximumFractionDigits = 2
        val ncv = pi.format(cv)

        println("ncv=$ncv")
    }

}
