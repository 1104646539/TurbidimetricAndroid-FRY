package com.wl.turbidimetric.test

import com.wl.turbidimetric.ex.CRC16
import com.wl.turbidimetric.ex.toHex
import com.wl.turbidimetric.global.SerialGlobal
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.LinkedBlockingQueue

/**
 * 仅测试回复用
 */
object TestSerialPort {
    var callback: ((ready: UByteArray) -> Unit)? = null

    /**
     * 测试回复
     * @param data UByteArray
     */
    public fun testReply(data: UByteArray) = runBlocking {
//        GlobalScope.launch {
//            launch {

        val state: UByte = 0x00u
        var reply = ubyteArrayOf(data[0], state)
        when (data[0]) {
            SerialGlobal.CMD_GetMachineState -> {
//                reply = reply.plus(ubyteArrayOf(0x3Fu, 0xFFu, 0xFFu, 0xFFu))
                reply = reply.plus(ubyteArrayOf(0x00u, 0x00u, 0x00u, 0x00u))
            }
            SerialGlobal.CMD_GetState -> {
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x2u, 0x33u, 0xffu))// 0011 0011
                reply = reply.plus(ubyteArrayOf(0x0u, 0x2u, 0x11u, 0xffu))//0001 0001
            }
            SerialGlobal.CMD_MoveSample -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x1u))
            }
            SerialGlobal.CMD_Test -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x23u, 0x0u))
            }
            SerialGlobal.CMD_SampleDoor -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))
            }
            SerialGlobal.CMD_CuvetteDoor -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))
            }
            SerialGlobal.CMD_GetSetTemp -> {
                reply = reply.plus(ubyteArrayOf(0x1u, 0x13u, 0x01u, 0x45u))// 275 325
            }
            SerialGlobal.CMD_TakeReagent -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x01u, 0x1u))// 存在r1试剂 1，r2试剂量 1
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x00u, 0x0u))// 不存在r1试剂 0，r2试剂量 0
            }
            SerialGlobal.CMD_StirProbeCleaning -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 有清洗液 1
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 无清洗液 0
            }
            SerialGlobal.CMD_SamplingProbeCleaning -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 有清洗液 1
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x01u))// 无清洗液 0
            }
            else -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))
            }
        }
        val crc = CRC16(reply)
        reply = reply.plus(crc)
        Timber.d("回复 ${reply.toHex()}")
//        block(reply)
        results.add(reply)
    }

    //        }
//    }
    val results = LinkedBlockingQueue<UByteArray>()

    init {
        GlobalScope.launch {
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
