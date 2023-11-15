package com.wl.turbidimetric.upload.hl7.service

import com.wl.turbidimetric.upload.model.ConnectConfig

/**
 * 根据类型不同，返回网络连接和串口连接类
 */
object ConnectServiceFactory {
    @JvmStatic
    fun create(config: ConnectConfig, onSuccess: () -> Unit): AbstractConnectService {
        return if (config.serialPort) {
            SerialPortConnectService(onSuccess)
        } else {
            SocketConnectService(onSuccess)
        }
    }
}
