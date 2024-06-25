package com.wl.turbidimetric.upload.hl7.service

import android.util.Log
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.hl7.util.ErrorEnum
import com.wl.turbidimetric.upload.hl7.util.HL7Reader
import com.wl.turbidimetric.upload.hl7.util.HL7Write
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.service.OnConnectListener
import java.net.Socket
import java.nio.charset.Charset
import kotlin.concurrent.thread

/**
 * 实现基础的连接、断开 网络连接
 * @property socket Socket?
 * @constructor
 */
open class SocketConnectService(successListener: () -> Unit) : AbstractConnectService(
    successListener
) {
    private var socket: Socket? = null

    /**
     * 建立连接
     * @param config ConnectConfig
     * @param onConnectListener OnConnectListener?
     */
    override fun connect(
        config: ConnectConfig, onConnectListener: OnConnectListener?
    ) {
        cancelConnectionMsg()
        this.config = config
        this.onConnectListener = onConnectListener
        initContext(Charset.forName(config.charset))
        hl7Write = HL7Write(Charset.forName(config.charset))
        hl7Reader = HL7Reader(Charset.forName(config.charset))
        thread {
            try {
                socket = Socket(config.ip, config.port)
                output = socket!!.getOutputStream()
                input = socket!!.getInputStream()
                initSuccess()
                onConnectListener?.onConnectStatusChange(ConnectStatus.CONNECTED)
                onConnectListener?.onConnectResult(ConnectResult.Success())
                cancelConnectionMsg()
                isConnect = true
            } catch (e: Exception) {
                isConnect = false
                onConnectListener?.onConnectStatusChange(ConnectStatus.DISCONNECTED)
                onConnectListener?.onConnectResult(
                    ConnectResult.OrderError(
                        ErrorEnum.NOT_CONNECTED.code, "连接失败 ${e.message}"
                    )
                )
                cancelConnectionMsg()
                reconnection()
            }
        }
    }

    override fun setOnConnectListener2(onConnectListener: OnConnectListener?) {
        this.onConnectListener = onConnectListener
    }


    override fun disconnect() {
        isConnect = false
        socket?.close()
        cancelConnectionMsg()
        onConnectListener?.onConnectStatusChange(ConnectStatus.DISCONNECTED)
    }

    override fun isConnected(): Boolean {
        Log.d(TAG, "isConnected:${socket?.isBound} ${socket?.isClosed} $isConnect")
        return (socket != null) && socket!!.isConnected && isConnect
    }

}
