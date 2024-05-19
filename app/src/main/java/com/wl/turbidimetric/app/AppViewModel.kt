package com.wl.turbidimetric.app

import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.model.TestType
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.util.SerialPortIF
import com.wl.turbidimetric.util.SerialPortImpl
import com.wl.wllib.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.concurrent.timer

/**
 * 全局唯一的ViewModel，用来保存全局通用的属性
 */
class AppViewModel(val localDataSource: LocalDataSource) : BaseViewModel() {
    val serialPort: SerialPortIF = SerialPortImpl(SystemGlobal.isCodeDebug)

    /**
     * 仪器状态
     */
    private val _machineState = MutableStateFlow(MachineState.None)
    val machineState = _machineState.asSharedFlow()

    /**
     * 上传状态
     */
    private val _uploadState = MutableStateFlow(UploadState.None)
    val uploadState = _uploadState.asSharedFlow()

    /**
     * u盘状态
     */
    private val _storageState = MutableStateFlow(StorageState.None)
    val storageState = _storageState.asSharedFlow()

    /**
     * 当前时间
     */
    private val _nowTimeStr = MutableStateFlow("00:00")
    val nowTimeStr = _nowTimeStr.asSharedFlow()


    /**
     * 检测类型
     */
    var testType = TestType.None

    /**
     * 检测模式状态
     */
    var testState = TestState.None
        set(value) {
            field = value
            _obTestState.value = value
            changeMachineState(value)
        }
    private val _obTestState = MutableStateFlow(TestState.None)
    val obTestState = _obTestState.asStateFlow()

    /**
     * 仪器检测模式
     */
    private val _machineTestMode =
        MutableStateFlow(MachineTestModel.valueOf(localDataSource.getCurMachineTestModel()))
    val machineTestModel = _machineTestMode.asSharedFlow()

    /**
     * 检测编号
     */
    private val _detectionNum = MutableStateFlow(localDataSource.getDetectionNum().toLong())
    val detectionNum = _detectionNum.asSharedFlow()


    fun changeMachineTestModel(machineTestModel: MachineTestModel) {
        this._machineTestMode.value = machineTestModel
    }

    fun setLooperTest(looperTest: Boolean) {
        localDataSource.setLooperTest(looperTest)
    }

    fun getLooperTest(): Boolean {
        return localDataSource.getLooperTest()
    }

    fun changeDetectionNum(detectionNum: Long) {
        this._detectionNum.value = detectionNum
    }

    fun getAutoPrintReceipt():Boolean{
        return localDataSource.getAutoPrintReceipt()
    }
    /**
     * 更新当前时间
     */
    fun listenerTime() {
        timer("更新时间", false, Date(), 1000 * 10) {
            viewModelScope.launch {
                _nowTimeStr.emit(DateUtil.date2Str(Date(), DateUtil.Time6Format))
            }
        }
    }

    /**
     * 更新u盘状态
     */
    fun changeStorageState(state: com.wl.weiqianwllib.upan.StorageState) {
        val s = when (state) {
            com.wl.weiqianwllib.upan.StorageState.NONE -> StorageState.None
            com.wl.weiqianwllib.upan.StorageState.INSERTED -> StorageState.Inserted
            com.wl.weiqianwllib.upan.StorageState.EXIST -> StorageState.Exist
            com.wl.weiqianwllib.upan.StorageState.UNAUTHORIZED -> StorageState.Unauthorized
        }
        viewModelScope.launch {
            _storageState.emit(s)
        }
    }

    /**
     * 更新上传状态
     */
    fun changeUploadState(state: ConnectStatus) {
        val s = when (state) {
            ConnectStatus.NONE -> UploadState.None
            ConnectStatus.RECONNECTION -> UploadState.ReConnection
            ConnectStatus.CONNECTED -> UploadState.Connected
            ConnectStatus.DISCONNECTED -> UploadState.Disconnected
        }
        viewModelScope.launch {
            _uploadState.emit(s)
        }
    }

    /**
     * 更新上传状态
     */
    fun changeMachineState(state: TestState) {
        serialPort?.testStateChange(state)
        val s = when (state) {
            TestState.NotGetMachineState -> MachineState.MachineError
            TestState.RunningError -> MachineState.MachineRunningError
            TestState.None -> MachineState.None
            else -> MachineState.MachineNormal
        }
        viewModelScope.launch {
            _machineState.emit(s)
        }
    }
}

