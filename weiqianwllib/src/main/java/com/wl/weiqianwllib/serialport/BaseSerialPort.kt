package com.wl.weiqianwllib.serialport

import android.util.Log
import weiqian.hardware.SerialPort
import kotlin.concurrent.thread

/**
 * 基础的串口库
 */
class BaseSerialPort {
    private var serialPort: SerialPort? = null;
//    private val BUFSIZE = 500
//    private val buf = ByteArray(BUFSIZE)
//    var readInterval:Long = 100
//
//    private val readThread = thread {
//        while (true) {
//            Thread.sleep(readInterval)
//            val retSize = read(buf)
//            if (retSize != null) {
//                if (retSize <= 0) {
//                    continue
//                }
//                val tempByteArray = buf.copyOf(retSize)
//                onParseListener?.onParseListener(tempByteArray)
//            }
//        }
//    }
//
//    private var onParseListener: OnParseListener? = null

    fun openSerial(
        addr: String,
        baudrate: Int,
        databits: Int = 8,
        parity: String = "\n",
        stopbits: Int = 1
    ) {
        serialPort = SerialPort()
        serialPort!!.open(addr, baudrate, databits, parity, stopbits)
    }

//    fun openReadListener(onParseListener: OnParseListener? = null) {
//        this.onParseListener = onParseListener
//    }
//
//
//    fun cancelReadListener() {
//        readThread.interrupt()
//    }

    fun close() {
//        cancelReadListener()
        serialPort?.close()
    }

    fun read(byteArray: ByteArray): Int {
        return serialPort?.read(byteArray, byteArray.size) ?: 0
    }



    fun write(byteArray: ByteArray): Int {
        return serialPort?.write(byteArray, byteArray.size) ?: 0
    }

//    interface OnParseListener {
//        fun onParseListener(byteArray: ByteArray): Unit
//    }

}
