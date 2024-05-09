package com.wl.turbidimetric.upload.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.ProjectSource
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class UploadSettingsViewModel(
    private val projectSource: ProjectSource,
    private val localDataSource: LocalDataSource
) : BaseViewModel() {

    var openUpload = MutableStateFlow(false)
    var autoUpload = MutableStateFlow(false)

    var ip = MutableStateFlow("0.0.0.0")
    var port = MutableStateFlow("")
    var timeout = MutableStateFlow("")
    var retryCount = MutableStateFlow("")
    var serialPortBaudRate = MutableStateFlow("")
    var serialPort = MutableStateFlow(false)
    var isReconnection = MutableStateFlow(false)
    var twoway = MutableStateFlow(false)
    var realTimeGetPatient = MutableStateFlow(false)
    var getPatientType = MutableStateFlow(GetPatientType.BC)
    var getPatient = MutableStateFlow(false)
    override fun init() {
        openUpload.value = SystemGlobal.uploadConfig.openUpload
        autoUpload.value = SystemGlobal.uploadConfig.autoUpload
        ip.value = SystemGlobal.uploadConfig.ip
        port.value = SystemGlobal.uploadConfig.port.toString()
        timeout.value = SystemGlobal.uploadConfig.timeout.toString()
        retryCount.value = SystemGlobal.uploadConfig.retryCount.toString()
        serialPortBaudRate.value = SystemGlobal.uploadConfig.serialPortBaudRate.toString()
        isReconnection.value = SystemGlobal.uploadConfig.isReconnection
        serialPort.value = SystemGlobal.uploadConfig.serialPort
        realTimeGetPatient.value = SystemGlobal.uploadConfig.realTimeGetPatient
        twoway.value = SystemGlobal.uploadConfig.twoWay
        getPatientType.value = SystemGlobal.uploadConfig.getPatientType
        getPatient.value = SystemGlobal.uploadConfig.getPatient
    }

    suspend fun getProjects(): List<ProjectModel> {
        return projectSource.getProjects().first()
    }

    fun verifySave(): String {
        if (twoway.value) {
            if (getPatientType.value == GetPatientType.BC
                && (MachineTestModel.valueOf(localDataSource.getCurMachineTestModel()) != MachineTestModel.Auto || !localDataSource.getScanCode())
            ) {
                return "当按条码匹配时，请先更改检测模式为自动模式并开启扫码功能"
            }
        }
        return ""
    }

    fun generateConfig(): ConnectConfig {
        return ConnectConfig(
            openUpload = openUpload.value,
            autoUpload = autoUpload.value,
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
            getPatient = getPatient.value,
            twoWay = twoway.value
        )
    }

    fun getDetectionNumInc(): String {
        return localDataSource.getDetectionNumInc()
    }
}

class UploadSettingsViewModelFactory(
    private val projectSource: ProjectSource = ServiceLocator.provideProjectSource(App.instance!!),
    private val localDataRepository: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!)
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UploadSettingsViewModel::class.java)) {
            return UploadSettingsViewModel(
                projectSource,
                localDataRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
