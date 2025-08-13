package com.wl.turbidimetric.test

import com.wl.turbidimetric.ex.toHex
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.mcuupdate.UpdateResult
import com.wl.wllib.CRC.CRC16
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue

/**
 * 仅测试回复用
 */
object TestSerialPort {
    var callback: ((ready: UByteArray) -> Unit)? = null
    var index = 0

    /**
     * 测试回复
     * @param data UByteArray
     */
    suspend fun testReply(data: UByteArray) {
//        GlobalScope.launch {
//            launch {
        val state: UByte = 0x00u
        var reply = ubyteArrayOf(data[0], state)
        when (data[0]) {
            SerialGlobal.CMD_GetMachineState -> {
//                delay(5000)
//                if (index <= 0) {
//                    reply = reply.plus(ubyteArrayOf(0x3Fu, 0xFFu, 0xFFu, 0xFFu))
//                } else {
                reply = reply.plus(ubyteArrayOf(0x00u, 0x00u, 0x00u, 0x00u))
//                }
//                index++
            }

            SerialGlobal.CMD_GetState -> {
                delay(200)
//                if (index < 2) {
                reply = reply.plus(ubyteArrayOf(0xffu, 0xffu, 0xffu, 0xffu))// 0011 0011
//                } else {
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x2u, 0x11u, 0xffu))//0001 0001
//                }
//                index++;
            }

            SerialGlobal.CMD_MoveSample -> {
                delay(100)
//                if (index in 0 .. 5) {
//                reply = ubyteArrayOf(data[0], 0x01u, 0x0u, 0x0u, 0x0u, 0x0u)
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x2u))//0不存在 1样本管 2比色杯
//                } else {
//                    reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))//0不存在 1样本管 2比色杯
//                }
//                index++
//                if (index == 10) {
//                    index = 0
//                }
            }

            SerialGlobal.CMD_MoveCuvetteShelf -> {
                delay(100)
//                reply = ubyteArrayOf(data[0], 0x01u, 0x0u, 0x0u, 0x0u, 0x0u)
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))
            }

            SerialGlobal.CMD_MoveSampleShelf -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))
            }

            SerialGlobal.CMD_Test -> {
//                delay(2000)//测试上传时需要
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x23u, 0x0u))
            }

            SerialGlobal.CMD_MoveCuvetteDripSample -> {
                delay(200)
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x23u, 0x0u))
            }

            SerialGlobal.CMD_SampleDoor -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))
            }

            SerialGlobal.CMD_Pierced -> {
//                delay(4000)//测试实时获取信息时需要
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))
            }

            SerialGlobal.CMD_Squeezing -> {
                delay(200)//测试实时获取信息时需要
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))
            }

            SerialGlobal.CMD_CuvetteDoor -> {
//                delay(3000)
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))
            }

            SerialGlobal.CMD_GetSetTemp -> {

                val symbolReaction = data[1]
                val reaction = data[2]
                val symbolR1 = data[3]
                val r1 = data[4]
                i("symbolReaction$symbolReaction reaction=$reaction symbolR1=$symbolR1 r1=$r1")
//                index++
//                if (index in 0..3) {
//                reply = reply.plus(ubyteArrayOf(0x1u, 0x13u, 0x01u, 0x45u))// 275 325
//                }else{
                reply = reply.plus(ubyteArrayOf(0x1u, 0x73u, 0x01u, 0x75u))// 370 325
//                }
            }

            SerialGlobal.CMD_TakeReagent -> {
//                delay(5000)
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x01u, 0x1u))// 存在r1试剂 1，r2试剂量 1
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))// 不存在r1试剂 0，r2试剂量 0
//                if (index == 6) {
//                    reply = ubyteArrayOf(data[0], 0x06u, 0x0u, 0x0u, 0x01u, 0x2u)//取试剂失败
//                } else {
                reply = ubyteArrayOf(data[0], 0x00u, 0x0u, 0x0u, 0x01u, 0x2u)//取试剂成功
//                }
//                index++
            }

            SerialGlobal.CMD_StirProbeCleaning -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 有清洗液 1
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 无清洗液 0
            }

            SerialGlobal.CMD_SamplingProbeCleaning -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 有清洗液 1
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 无清洗液 0
            }

            SerialGlobal.CMD_DripReagent -> {
//                delay(5000)
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))
            }

            SerialGlobal.CMD_Stir -> {
//                delay(2000)
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))
            }

            SerialGlobal.CMD_Sampling -> {
                delay(1000)
//                if (index == 3) {
//                    reply = ubyteArrayOf(data[0], 0x04u, 0x0u, 0x0u, 0x0u, 0x0u)//取样失败
//                } else {
                reply = ubyteArrayOf(data[0], 0x00u, 0x0u, 0x0u, 0x0u, 0x0u)//取样成功
//                }
//                index++
//                if (index == 10) {
//                    index = 0
//                }
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))//取样成功
//                reply = ubyteArrayOf(data[0], 0x01u, 0x0u, 0x0u, 0x0u, 0x0u)//取样失败
            }

            SerialGlobal.CMD_DripSample -> {
//                delay(4000)//测试实时获取信息时需要
//                if (index == 6) {
//                    reply = ubyteArrayOf(data[0], 0x02u, 0x0u, 0x0u, 0x0u, 0x0u)//加样失败
//                } else {
                reply = ubyteArrayOf(data[0], 0x00u, 0x0u, 0x0u, 0x0u, 0x0u)//加样成功
//                }
//                index++
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))//取样成功
//                reply = ubyteArrayOf(data[0], 0x04u, 0x0u, 0x0u, 0x0u, 0x0u)//取样失败
            }

            SerialGlobal.CMD_McuUpdate -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x1u))//是否准备好 1是 0否
            }

            SerialGlobal.CMD_Motor -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, data[3]))//
            }

            SerialGlobal.CMD_OverloadParams -> {
                reply = ubyteArrayOf(data[0], 0x00u, 0x0u, 0x0u, 0x0u, 0x0u)//成功
//                reply = ubyteArrayOf(data[0], 0x01u, 0x0u, 0x0u, 0x0u, 0x0u)//失败
            }

            SerialGlobal.CMD_FullR1 -> {
                delay(1000)
                var data1: UByte = 0x01u
//                if (index++ < 2) {
//                    data1 = 0x00u
//                }
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, data1))//
//                reply = ubyteArrayOf(data[0], 0x01u, 0x0u, 0x0u, 0x0u, 0x0u)//失败
            }

            else -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))
            }
        }
        val crc = CRC16(reply)
        reply = reply.plus(crc)
        i("回复 ${reply.toHex()}")
//        block(reply)
        results.add(reply)
    }

    fun mcuUpdateResult(onResult: ((UpdateResult) -> Unit)?) {
        onResult?.invoke(UpdateResult.Success("成功"))
//        onResult?.invoke(UpdateResult.Failed("失败"))
    }

    //        }
//    }
    val results = LinkedBlockingQueue<UByteArray>()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            launch {
                while (true) {
                    delay(50)
                    val re = results.take()
                    if (re != null) {
                        callback?.invoke(re)
                    }
                }
            }
        }
    }
}
