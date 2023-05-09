package com.wl.mvvm_demo

import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.log
import kotlin.math.log10

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
            sourceValue1
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

}
