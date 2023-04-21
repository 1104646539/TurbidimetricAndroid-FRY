package com.wl.turbidimetric.util

import com.wl.turbidimetric.ex.CRC16
import com.wl.turbidimetric.ex.toHex
import com.wl.turbidimetric.global.SerialGlobal
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingDeque
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.reflect.KSuspendFunction1

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
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x0u))
            }
            SerialGlobal.CMD_GetState -> {
//                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x33u, 0xffu))// 0011 0011
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x37u, 0xffu))//0011 0111
            }
            SerialGlobal.CMD_MoveShitTube -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x0u, 0x1u))
            }
            SerialGlobal.CMD_Test -> {
                reply = reply.plus(ubyteArrayOf(0x0u, 0x0u, 0x23u, 0x0u))
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
                    delay(20)
                    val re = results.take()
                    if (re != null) {
                        callback?.invoke(re)
                    }
                }
            }
        }
    }
}
