package com.wl.turbidimetric

import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.model.CurveModel
import com.wl.wllib.toLongTimeStr
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class ToolExTest {

    @Test
    fun crc16Test() {
//        Assert.assertTrue(
//            CRC16(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u)).toByteArray()
//                .contentEquals(ubyteArrayOf(0x18u, 0x3cu).toByteArray())
//        )
//
//        Assert.assertFalse(
//            CRC16(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u)).toByteArray()
//                .contentEquals(ubyteArrayOf(0x00u, 0x00u).toByteArray())
//        )
//        val c1 = crc2(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u))
//
//        println("crc16Test c1=$c1")
//
//        val a: List<Int> = listOf(100, 100)
//        val b: List<Int> = listOf(100, 100)
//
//        println(a == b)

    }

    fun crc2(data: UByteArray): UByteArray {
//        val polynomial = 0x8005
        val polynomial = 0x00
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (i in 0 until 8) {
                crc = if (crc and 0x0001 != 0) {
                    crc shr 1 xor polynomial
                } else {
                    crc shr 1
                }
            }
        }
        val lowByte: UByte = (crc shr 8 and 0xFF).toUByte()
        val highByte: UByte = (crc and 0xFF).toUByte()
        return ubyteArrayOf(highByte, lowByte)
    }

    @Test
    fun verifyCrcTest() {
//        Assert.assertTrue(VerifyCrc(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u, 0x18u, 0x3cu)))
//
//        Assert.assertFalse(VerifyCrc(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u, 0x11u, 0x11u)))
//
//        Assert.assertThrows("错误 数组长度错误 ba.size==8", IndexOutOfBoundsException::class.java) {
//            VerifyCrc(ubyteArrayOf(0x01u, 0x01u, 0x00u, 0x00u, 0x00u, 0x11u, 0x11u, 0x12u))
//        }
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

//        Assert.assertEquals(longStr.longStrToDate().toLongString(), "2023-04-23 10:20:30")
//        Assert.assertEquals(longStr.bigLongStrToDate().toBigLongString(), "2023-04-23 10:20:30 252")
//        Assert.assertEquals(longStr.bigLongStrToDate().time, 1682216430252)
//
//        val longDate = 1682827630000L
//        Assert.assertEquals(longDate.toDate().toBigLongString(), "2023-04-30 12:07:10 000")
//        Assert.assertEquals(longDate.toDate().toLongString(), "2023-04-30 12:07:10")
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
        val a1 = 9.702673786
        val a2 = 0.7425860767
        val x0 = -4.513632E-4
        val p = 1.406E-7
        val absorbance = 0.2253
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

//        val a1 = -0.00283;
//        val a2 = 9.07;
//        val x0 = 34909;
//        val p = 0.82816;
//        val absorbance = 0.1
//        var d1 = a2 - absorbance
//        var d2 = p
//        if (d1 == 0.0 || d2 == 0.0) {
//            println("newCalcBigD dividend1==0||dividend2==0 d1=$d1 d2=$d2")
//            return
//        }
//        var temp11 = (a2 - a1) / d1 - 1
//        var temp12 = temp11.pow(1 / d2).scale(10)
////        var con1: Double = x0 * ((a2 - a1) / (a2 - absorbance) - 1).pow(1 / p).scale(5)
//        var con1: Double = x0 * temp12
//        println("newCalcBigD con1=$con1 d1=$d1 d2=$d2 temp11=$temp11 temp12=$temp12")

//        var dividend1 = BigDecimal(a2)
//            .subtract(BigDecimal(absorbance)).setScale(10, BigDecimal.ROUND_HALF_UP)
//        var dividend2 = p
//        if (dividend1.compareTo(BigDecimal(0)) == 0 || dividend2 == 0.0) {
//            println("newCalcBigD dividend1==0||dividend2==0 dividend1=$dividend1 dividend2=$dividend2")
//            return
//        }
//        val temp21 = BigDecimal(a2).subtract(BigDecimal(a1)).divide(
//            dividend1, 10, BigDecimal.ROUND_HALF_UP
//        ).subtract(BigDecimal(1))
//
//        val temp22 = temp21.toDouble().pow((1 / dividend2)).scale(10)
//        val con2 = (x0 * temp22)
//
//        println("newCalcBigD con2=$con2 dividend1=$dividend1 dividend2=$dividend2 temp21=$temp21 temp22=$temp22")
    }

    @Test
    fun testCon() {
        val pm = CurveModel().apply {
            reagentNO = "5452"
            reactionValues = intArrayOf(60, 91, 2722, 11722, 27298)
            f0 = 9.702673786
            f1 = 0.7425860767
            f2 = -4.513632E-4
            f3 = 1.406E-7
            projectLjz = 100
            fitGoodness = 0.9998
            createTime = Date().toLongTimeStr()
        }
        val con1 = calcCon(BigDecimal(0.27858), pm)
        println("con1=$con1")
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

    @Test
    fun intArrayJoinTest() {
        val tst1 = intArrayOf(100, 200, 300, 400)
        val str = tst1.joinToString()
        println("str=$str")

        val newValue = str.replace(" ", "")
        val st = newValue.split(",").toTypedArray()
        val res = if (st.isEmpty()) {
            null
        } else {
            val ins = IntArray(st.size)
            for (i in st.indices) {
                ins[i] = st[i].toInt()
            }
            ins
        }
        res?.forEach {
            println("it=$it")
        }
    }

    @Test
    fun calcTarget() {
        var target = -15.625
        if (target < 0) {
            var range = 15
            val random = Random()
            range = random.nextInt(10) + range
            target = target + range / 100.0 * target
            println("target=$target")
        }
    }

}
