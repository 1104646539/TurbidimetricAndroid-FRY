package com.wl.turbidimetric.upload.hl7.service

import com.wl.weiqianwllib.serialport.BaseSerialPort
import java.io.InputStream

/**
 * 串口输入流
 * @property serialPort BaseSerialPort
 * @property ba ByteArray
 * @constructor
 */
class SerialPortInputStream(val serialPort: BaseSerialPort) : InputStream() {
    private val ba = ByteArray(3)
    override fun read(): Int {
        var len = 0
        while (true) {
            if (serialPort.read(ba, 1).also { len = it } > 0) {
                return ba[0].toInt().also {
                    ba[0] = 0
                }
            }
        }
    }
}
