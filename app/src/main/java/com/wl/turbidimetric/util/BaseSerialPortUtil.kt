package com.wl.turbidimetric.util

//import android_serialport_api.SerialPort
import weiqian.hardware.SerialPort
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.stream.IntStream

/**
 * 微嵌
 */
open class BaseSerialPortUtil(private val com: String, private val baud: Int) {

    lateinit var serialPort: SerialPort

    open fun open() {
        serialPort = SerialPort()
        serialPort.open(com, baud, 8, "\n", 1)

    }

    fun write(data: ByteArray) {
        serialPort?.write(data, data.size)
    }

    fun read(data: ByteArray): Int {
        return serialPort?.read(data, data.size) ?: -1
    }
}
/**
 * 浙江系能科技
 */
//open class BaseSerialPortUtil(private val com: String, private val baud: Int) {
//
//    lateinit var serialPort: SerialPort
//    lateinit var inputStream: InputStream
//    lateinit var outputStream: OutputStream
//    open fun open() {
//        serialPort = SerialPort(File(com), baud, 0)
//        inputStream = serialPort.inputStream
//        outputStream = serialPort.outputStream
//    }
//
//    fun write(data: ByteArray) {
//        outputStream.write(data)
//    }
//
//    fun read(data: ByteArray): Int {
//        return inputStream.read(data, 0, data.size)
//    }
//}
