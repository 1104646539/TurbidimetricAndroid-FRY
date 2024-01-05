package com.wl.turbidimetric.upload.view

import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import com.wl.turbidimetric.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class UploadSettingsViewModel : BaseViewModel() {

    var autUpload = MutableStateFlow(false)

    var ip = MutableStateFlow("0.0.0.0")
    var port = MutableStateFlow("")
    var timeout = MutableStateFlow("")
    var retryCount = MutableStateFlow("")
    var serialPortBaudRate = MutableStateFlow("")
    var serialPort = MutableStateFlow(false)
    var isReconnection = MutableStateFlow(false)
    var realTimeGetPatient = MutableStateFlow(false)
    var getPatientType = MutableStateFlow(GetPatientType.BC)
    var getPatient = MutableStateFlow(false)
    override fun init() {
        autUpload.value = SystemGlobal.uploadConfig.autoUpload
        ip.value = SystemGlobal.uploadConfig.ip
        port.value = SystemGlobal.uploadConfig.port.toString()
        timeout.value = SystemGlobal.uploadConfig.timeout.toString()
        retryCount.value = SystemGlobal.uploadConfig.retryCount.toString()
        serialPortBaudRate.value = SystemGlobal.uploadConfig.serialPortBaudRate.toString()
        isReconnection.value = SystemGlobal.uploadConfig.isReconnection
        serialPort.value = SystemGlobal.uploadConfig.serialPort
        realTimeGetPatient.value = SystemGlobal.uploadConfig.realTimeGetPatient
        getPatientType.value = SystemGlobal.uploadConfig.getPatientType
        getPatient.value = SystemGlobal.uploadConfig.getPatient
    }

    fun generateConfig(): ConnectConfig {
        return ConnectConfig(
            autoUpload = autUpload.value,
            serialPortName = WQSerialGlobal.COM4,
            ip = ip.value,
            port = port.value.toInt(),
            timeout = timeout.value.toLong(),
            retryCount = retryCount.value.toInt(),
            serialPortBaudRate = serialPortBaudRate.value.toInt(),
            serialPort = serialPort.value,
            isReconnection = isReconnection.value,
            realTimeGetPatient = realTimeGetPatient.value,
            getPatientType = getPatientType.value,
            getPatient = getPatient.value
        )
    }
}
