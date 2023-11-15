package com.wl.turbidimetric.upload.hl7.service

import com.wl.weiqianwllib.serialport.BaseSerialPort
import java.io.OutputStream
/**
 * 串口输出流
 * @property serialPort BaseSerialPort
 * @constructor
 */
class SerialPortOutputStream(private val serialPort: BaseSerialPort) : OutputStream() {
    override fun write(b: Int) {
        serialPort.write(byteArrayOf(b.toByte()))
    }

    override fun write(ba: ByteArray) {
        serialPort.write(ba)
    }
}
