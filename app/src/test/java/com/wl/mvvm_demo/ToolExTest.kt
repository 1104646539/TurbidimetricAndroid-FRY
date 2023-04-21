package com.wl.mvvm_demo

import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.util.CRC
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import java.util.Arrays

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
    fun scaleTest(){
        val re:Double  = 380.251125412.scale(2)
        Assert.assertEquals(re,380.25,0.01)
    }

}
