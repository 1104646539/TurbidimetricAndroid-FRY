package com.wl.turbidimetric.upload.model

import java.nio.charset.Charset

data class ConnectConfig(
    val ip: String,//网络连接的IP
    val port: Int,//网络连接的IP
    val charset: String = "GB2312",//字符集
    val reconnectionTimeout: Long = 15000,//重新建立连接的间隔
    val timeout: Long = 5000,//消息的超时时间
    val retryCount: Int = 5,//发送消息超时的超时重试次数
    val serialPortName: String = "",//串口名
    val serialPortBaudRate: Int = 9600,//串口波特率
    val serialPort: Boolean = false,//是串口连接还是网络连接
    val isReconnection: Boolean = false,//是否断开重连
)

