package com.wl.turbidimetric.upload.model

data class ConnectConfig(
    var autoUpload: Boolean = true,//是否自动上传结果
    var ip: String = "",//网络连接的IP
    var port: Int = 22222,//网络连接的IP
    var charset: String = "UTF-8",//字符集
    var reconnectionTimeout: Long = 15000,//重新建立连接的间隔
    var timeout: Long = 5000,//消息的超时时间
    var retryCount: Int = 1,//发送消息超时的超时重试次数
    var serialPortName: String = "",//串口名
    var serialPortBaudRate: Int = 9600,//串口波特率
    var serialPortDataBit: Int = 8,//串口波特率
    var serialPortStopBit: Int = 1,//串口波特率
    var serialPort: Boolean = false,//是串口连接还是网络连接
    var isReconnection: Boolean = false,//是否断开重连
    var getPatient: Boolean = false,//是否开启获取样本申请信息
    var realTimeGetPatient: Boolean = false,//是否实时获取样本申请信息
    var getPatientType: GetPatientType = GetPatientType.BC,//实时获取时的类型
)

