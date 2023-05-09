package com.wl.turbidimetric

import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.CurveFitter
import com.wl.turbidimetric.util.SerialPortUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.*
import kotlin.math.log10
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

//        assertEquals(4, 2 + 2)

//        val returnData =
//            arrayOf<UByte>(
//                0x00u,
//                0x00u,
//                0x00u,
//                0x00u,
//                0x00u,
//                0x00u,
//                0x01u,
//                0x01u,
//                0x01u,
//                0x00u,
//                0x00u,
//                0x00u,
//                0x3Du,
//                0xF6u,
//                0xF6u,
//                0xF6u,
//            )
        var returnData: MutableList<UByte> = mutableListOf<UByte>(
            0x00u,
            0x00u,
            0x00u,
            0x00u,
            0x00u,
            0x00u,
            0x01u,
            0x01u,
            0x01u,
            0x00u,
            0x00u,
            0x00u,
            0x3Du,
            0xF6u,
            0xF6u,
        )
        val allCount = 7
        i@ for (i in returnData.indices) {
            if (returnData.size >= SerialPortUtil.Instance.hCount + allCount && returnData[i] == SerialPortUtil.Instance.header[0]) {
                var k = i;
                var count = 0
                j@ for (element in SerialPortUtil.Instance.header) {
                    if (returnData[k] == element) {
                        count++
                        if (SerialPortUtil.Instance.hCount == count) {
                            println("date2 ${Date().time}")
                            println("匹配了")

                            //找到了前缀
                            val temp: UByteArray =
                                returnData.toUByteArray()
                                    .copyOfRange(0, k + allCount + 1)
                            val ready =
                                temp.copyOfRange(temp.size - allCount, temp.size)
                            if (temp.size < returnData.size) {
                                val remaining =
                                    returnData.toUByteArray()
                                        .copyOfRange(
                                            k + allCount + 1,
                                            returnData.size
                                        )
                                returnData.clear()
                                returnData.addAll(remaining)
                            } else {
                                returnData.clear()
                            }
                            println("带前缀的")
                            parse(ready)
                            break@i
                        }
                    } else {
                        println("不对了")
                        continue@i
                    }
                    k++
                }
            } else {
                println("data.size ${returnData.size} hCount + allCount=${SerialPortUtil.Instance.hCount + allCount}")
                break@i
            }
        }
    }

    fun parse(ready: UByteArray) {
        println("parse ready=${ready.toHex()}");


        when (ready[0]) {
            SerialGlobal.CMD_GetMachineState -> {
                parseModel(
                    ReplyModel(
                        SerialGlobal.CMD_GetMachineState,
                        ready[1].toInt(),
                        GetMachineStateModel()
                    )
                )
            }
            SerialGlobal.CMD_GetState -> {
                parseModel(
                    ReplyModel(
                        SerialGlobal.CMD_GetState,
                        ready[1].toInt(),
                        GetStateModel()
                    )
                )
            }
        }
    }

    private fun <T> parseModel(replyModel: ReplyModel<T>) {
        println("parseModel replyModel=${replyModel}");

    }

    @Test
    fun parseL() {
//        val num: UByte = 0xF0u
        val num: UByte = 0x70u
        val nums = mutableListOf<Int>()
        nums.add(0)
        nums.add(0)
        nums.add(0)
        nums.add(0)

        val f1: UByte = 0x80u
        val temp1 = num and f1
        nums[0] = if (temp1 == f1) 1 else 0

        val f2: UByte = 0x40u
        val temp2 = num and f2
        nums[1] = if (temp2 == f2) 1 else 0

        val f3: UByte = 0x20u
        val temp3 = num and f3
        nums[2] = if (temp3 == f3) 1 else 0

        val f4: UByte = 0x10u
        val temp4 = num and f4
        nums[3] = if (temp4 == f4) 1 else 0

        println("nums=${nums}")
    }

    fun getStep(uByte: UByte, pos: Int): Int {
        val temp: Int = 1 shl (pos)
        return if (uByte.toInt() and temp == temp) 1 else 0
    }

    @Test
    fun testGetStep() {
        val num: UByte = 0xAu

        val p0 = getStep(num, 0)
        val p1 = getStep(num, 1)
        val p2 = getStep(num, 2)

        println("p0=$p0 p1=$p1 p2=$p2 ")
    }

    @Test
    fun merge() {
        //0000 0000 0x00u
        //0000 0000 0x00u
        //1111 1101 0xFDu
        //1110 1000 0x00u
        val ms = ubyteArrayOf(0x03u, 0xFFu, 0xFFu, 0xFFu)

        var sum = 0
        for (i in 0 until 4) {
            sum += (ms[i].toInt() shl 8 * (3 - i))
        }
        println("sum=${sum}")

        val sum2 = merge(ms)
        println("sum2=${sum2}")
    }

    @Test
    fun getCrc() {
        val re = CRC16(
            ubyteArrayOf(
                0x01u,
                0x01u,
                0x00u,
                0x00u,
                0x00u
            )
        )
        println("1=${re[0]} 2=${re[1]}")
    }

    @Test
    fun verify() {
        val ready = ubyteArrayOf(
            0x01u,
            0x01u,
            0x00u,
            0x00u,
            0x00u,
            0x18u,
            0x3Cu,
        )
        val verify = VerifyCrc(ready)
        println("verify=$verify")
    }

    @Test
    fun getResponse() {
        val ready = ubyteArrayOf(
            0x01u,
            0x01u,
            0x00u,
            0x00u,
            0x00u,
            0x18u,
            0x3Cu,
        )
        val response = ubyteArrayOf(
            SerialPortUtil.Instance.responseCommand1,
            ready[0],
            0x00u,
            0x00u,
            0x00u
        )
        val re = CRC16(response)
        val k = response + re
        println("getResponse finish")


        val verify = VerifyCrc(k)
        println("verify=$verify")
    }

    @Test
    fun testMachineState() {
        val data = 0x3C0FFF9u


        val num1 = (data shr 0) and 0x03u
        for (i in 0..10) {
            val num = (data shr (i * 2)) and 0x03u
            println("num=$num i =$i ")
        }
    }


    @Test
    fun testK() {

//        val yData = doubleArrayOf(0.008972,
//            0.014625948,
//            0.023926898,
//            0.09966074,
//            0.22472552,
//            0.244949189,
//        )

        val xData = doubleArrayOf(
            0.0,
            25.0,
            50.0,
            200.0,
            500.0,
            1000.0,
        )
        val yData =
            doubleArrayOf(0.07725, 15.9416750, 37.5354200, 178.8007500, 329.27181000, 437.7187)
        val curveFitter = CurveFitter(xData, yData)
        curveFitter.doFitCon()
        val res = curveFitter.params
        for (i in res.indices) {
            println(res[i])
        }
        println("------")
        println(curveFitter.fitGoodness)
        println(curveFitter.resultString)


        //a-a1
        //b-p
        //c-x0
        //d-a2
//        val a1 = -1.8288
//        val a2 = 564.784
//        val x0 = 374.6459
//        val p = 1.2522
        val a1 = res[0]
        val a2 = res[3]
        val x0 = res[2]
        val p = res[1]
        var con = 0.0
        val tc = 15.9
        con = x0 * Math.pow((a2 - a1) / (a2 - tc) - 1, 1 / p)

        println("tcToCon:con=$con tc=$tc")
    }

    @Test
    fun testXGD() {
        val resultTest1 = arrayListOf<Int>(30000, 33000, 36000, 39000, 42000)
        val resultTest4 = arrayListOf<Int>(10000, 12000, 14000, 16000, 20000)
        var result = arrayListOf<Double>()
        for (i in 0..4) {
            result.add(log10(((resultTest1[i] / resultTest4[i]).toDouble())))
        }
        println("result=$result")
    }

    @Test
    fun fd() {
        var d1 = 1;
        var d2 = 1;
        var volume = 2000;


        println("volumn=${volume.toUByte()}")
        println("shr=${(volume shr 8).toUByte()}")
    }

}
