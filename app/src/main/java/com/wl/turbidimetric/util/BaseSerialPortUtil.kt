package com.wl.turbidimetric.util

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import weiqian.hardware.SerialPort
import java.nio.Buffer

open class BaseSerialPortUtil(private val com: String, private val baud: Int) {

    lateinit var serialPort: SerialPort

    open fun open() {
        serialPort = SerialPort()
        serialPort.open(com, baud, 8, "\n", 1)

    }

    fun write(data: ByteArray) {
        serialPort?.write(data, data.size)
    }

    fun read(data: ByteArray) :Int{
        return serialPort?.read(data, data.size)
    }
}
