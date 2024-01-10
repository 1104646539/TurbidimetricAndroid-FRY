package com.wl.turbidimetric.upload.hl7.service

import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.wllib.LogToFile.i
import java.io.IOException
import java.io.InputStream

/**
 * 串口输入流
 * @property serialPort BaseSerialPort
 * @property ba ByteArray
 * @constructor
 */
class SerialPortInputStream(val serialPort: BaseSerialPort) : InputStream() {
    private val TAG = "SerialPortInputStream"
    private val ba = ByteArray(2)
    var isOpen = true
    override fun read(): Int {
        while (isOpen) {
            if (serialPort.read(ba, 1) > 0) {
                return ba[0].toInt().also {
                    ba[0] = 0
                }
            }
        }
        throw IOException("$TAG read io error")
    }

    override fun close() {
        isOpen = false
        i(TAG, "close: ")
        super.close()
    }
}
