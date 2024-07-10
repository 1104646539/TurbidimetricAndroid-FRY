package com.wl.turbidimetric.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.ex.toState
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.model.TestType
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.report.PrintHelper
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.util.SerialPortIF
import com.wl.wllib.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 全局唯一的ViewModel，用来保存全局通用的属性
 */
class AppViewModel(
    private val localDataSource: LocalDataSource,
    val serialPort: SerialPortIF,
    val printUtil: PrintUtil,
    val printHelper: PrintHelper,
) :
    ViewModel() {

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
     * 打印机状态
     */
    private val _printerState = MutableStateFlow(PrinterState.None)
    val printerState = _printerState.asSharedFlow()

    /**
     * 打印数量
     */
    private val _printNum = MutableStateFlow(0)
    val printNum = _printNum.asSharedFlow()

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

    fun getLooperTest(): Boolean {
        return localDataSource.getLooperTest()
    }


    fun getAutoPrintReceipt(): Boolean {
        return localDataSource.getAutoPrintReceipt()
    }

    fun getHospitalName(): String {
        return localDataSource.getHospitalName()
    }

    fun getReportFileNameBarcode(): Boolean {
        return localDataSource.getReportFileNameBarcode()
    }

    fun getAutoPrintReport(): Boolean {
        return localDataSource.getAutoPrintReport()
    }

    private fun changeMachineTestModel(machineTestModel: MachineTestModel) {
        this._machineTestMode.value = machineTestModel
    }

    private fun setLooperTest(looperTest: Boolean) {
        localDataSource.setLooperTest(looperTest)
    }

    private fun changeDetectionNum(detectionNum: Long) {
        this._detectionNum.value = detectionNum
    }

    /**
     * 更新当前时间
     */
    private fun listenerTime() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _nowTimeStr.emit(DateUtil.date2Str(Date(), DateUtil.Time6Format))
                delay(30000)
            }
        }
    }

    /**
     * 更新u盘状态
     */
    private fun changeStorageState(state: com.wl.weiqianwllib.upan.StorageState) {
        val s = state.toState()
        viewModelScope.launch {
            _storageState.emit(s)
        }
    }

    /**
     * 更新上传状态
     */
    private fun changeUploadState(state: ConnectStatus) {
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
     * 更新打印机状态
     */
    private fun changePrinterState(state: PrinterState) {
        viewModelScope.launch {
            _printerState.emit(state)
        }
    }

    /**
     * 更新打印机状态
     */
    private fun changePrintNum(num: Int) {
        viewModelScope.launch {
            _printNum.emit(num)
        }
    }

    /**
     * 更新上传状态
     */
    private fun changeMachineState(state: TestState) {
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

    fun processIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.ListenerTime -> {
                listenerTime()
            }

            is AppIntent.PrintNumChange -> {
                changePrintNum(intent.printNum)
            }

            is AppIntent.PrinterStateChange -> {
                changePrinterState(intent.printerState)
            }

            is AppIntent.StorageStateChange -> {
                changeStorageState(intent.storageState)
            }

            is AppIntent.UploadStateChange -> {
                changeUploadState(intent.uploadState)
            }

            is AppIntent.MachineStateChange -> {
                changeMachineState(intent.state)
            }

            is AppIntent.MachineTestModelChange -> {
                changeMachineTestModel(intent.machineTestModel)
            }

            is AppIntent.LooperTestChange -> {
                setLooperTest(intent.looperTest)
            }

            is AppIntent.DetectionNumChange -> {
                changeDetectionNum(intent.detectionNum)
            }
        }
    }

}

sealed class AppIntent {
    object ListenerTime : AppIntent()
    data class PrintNumChange(val printNum: Int) : AppIntent()
    data class PrinterStateChange(val printerState: PrinterState) : AppIntent()
    data class StorageStateChange(val storageState: com.wl.weiqianwllib.upan.StorageState) :
        AppIntent()

    data class UploadStateChange(val uploadState: ConnectStatus) : AppIntent()
    data class MachineStateChange(val state: TestState) : AppIntent()
    data class MachineTestModelChange(val machineTestModel: MachineTestModel) : AppIntent()
    data class LooperTestChange(val looperTest: Boolean) : AppIntent()
    data class DetectionNumChange(val detectionNum: Long) : AppIntent()

}
