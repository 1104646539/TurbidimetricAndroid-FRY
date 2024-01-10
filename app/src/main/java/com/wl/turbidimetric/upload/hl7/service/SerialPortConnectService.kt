package com.wl.turbidimetric.upload.hl7.service

import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.hl7.util.HL7Reader
import com.wl.turbidimetric.upload.hl7.util.HL7Write
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.weiqianwllib.serialport.BaseSerialPort
import java.nio.charset.Charset
import kotlin.concurrent.thread

/**
 * 实现基础的连接、断开 串口连接
 * @property serialPort BaseSerialPort?
 * @constructor
 */
class SerialPortConnectService(successListener: () -> Unit) :
    AbstractConnectService(successListener) {
    var serialPort: BaseSerialPort? = null

    /**
     * 建立连接
     * @param config ConnectConfig
     * @param onConnectListener OnConnectListener?
     */
    override fun connect(
        config: ConnectConfig, onConnectListener: OnConnectListener?
    ) {
        this.config = config
        this.onConnectListener = onConnectListener
        initContext(Charset.forName(config.charset))
        hl7Write =
            HL7Write(Charset.forName(config.charset))
        hl7Reader =
            HL7Reader(Charset.forName(config.charset))
        thread {
            try {
                serialPort = BaseSerialPort()
                serialPort!!.openSerial(
                    config.serialPortName,
                    config.serialPortBaudRate,
                    config.serialPortDataBit
                )
                if (serialPort!!.isOpen()) {
                    output = SerialPortOutputStream(serialPort!!)
                    input = SerialPortInputStream(serialPort!!)
                    initSuccess()
                    onConnectListener?.onConnectStatusChange(ConnectStatus.CONNECTED)
                    onConnectListener?.onConnectResult(ConnectResult.Success())
                    isConnect = true
                } else {
                    isConnect = false
                    onConnectListener?.onConnectStatusChange(ConnectStatus.DISCONNECTED)
                    onConnectListener?.onConnectResult(
                        ConnectResult.OrderError(
                            100,
                            "连接失败 串口打开失败"
                        )
                    )
                    reconnection()
                }
            } catch (e: Exception) {
                isConnect = false
                onConnectListener?.onConnectStatusChange(ConnectStatus.DISCONNECTED)
                onConnectListener?.onConnectResult(
                    ConnectResult.OrderError(
                        100,
                        "连接失败 ${e.message}"
                    )
                )
                reconnection()
            }
        }
    }


    override fun disconnect() {
        isConnect = false
        serialPort?.close()
        serialPort = null
        input?.close()
        output?.close()
        handler.removeCallbacksAndMessages(WHAT_RECONNECTION)
    }

    override fun isConnected(): Boolean {
        return (serialPort != null)
    }

}
