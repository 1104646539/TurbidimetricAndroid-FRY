package com.wl.turbidimetric.upload.service

import com.wl.turbidimetric.upload.model.ConnectConfig

interface ConnectService {
    fun connect(config: ConnectConfig, onConnectListener: OnConnectListener?)
    fun setOnConnectListener2(onConnectListener: OnConnectListener?)
    fun disconnect()
    fun isConnected(): Boolean
}
