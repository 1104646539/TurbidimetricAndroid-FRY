package com.wl.turbidimetric.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.AppIntent
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.app.MachineState
import com.wl.turbidimetric.app.PrinterState
import com.wl.turbidimetric.app.UploadState
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.weiqianwllib.upan.StorageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(private val appViewModel: AppViewModel) : BaseViewModel() {
    /**
     * 关机命令
     */
    private fun shutdown() {
        appViewModel.serialPort.shutdown()
    }

    /**
     * 不允许发送命令
     */
    private fun allowRunning() {
        appViewModel.serialPort.allowRunning()
    }

    private var curIndex = 0
    private val _uiState = MutableStateFlow<MainState>(MainState.None)
    val uiState = _uiState.asStateFlow()

    fun processIntent(intent: MainIntent) {
        viewModelScope.launch {
            when (intent) {
                MainIntent.AllowRunning -> allowRunning()
                is MainIntent.ChangeNavCurIndex -> changeNavCurIndex(intent.index)
                is MainIntent.ChangeStorageState -> changeStorageState(intent.state)
                MainIntent.ListenerTime -> listenerTime()
                MainIntent.ShowOpenDocumentTree -> showOpenDocumentTree()
                MainIntent.ShutDown -> shutdown()
                MainIntent.ShowPopupViewForUploadState -> {
                    showPopupViewForUploadState()
                }

                MainIntent.ShowPopupViewForStorageState -> {
                    showPopupViewForStorageState()
                }

                MainIntent.ShowPopupViewForMachineState -> {
                    showPopupViewForMachineState()
                }

                MainIntent.ShowPopupViewForPrinterState -> {
                    showPopupViewForPrinterState()
                }
            }
        }
    }

    private fun showPopupViewForUploadState() {
        viewModelScope.launch {
            _uiState.emit(MainState.ShowPopupViewForUploadState(appViewModel.uploadState.first()))
        }
    }

    private fun showPopupViewForStorageState() {
        viewModelScope.launch {
            _uiState.emit(MainState.ShowPopupViewForStorageState(appViewModel.storageState.first()))
        }
    }

    private fun showPopupViewForMachineState() {
        viewModelScope.launch {
            _uiState.emit(MainState.ShowPopupViewForMachineState(appViewModel.machineState.first()))
        }
    }

    private fun showPopupViewForPrinterState() {
        viewModelScope.launch {
            _uiState.emit(
                MainState.ShowPopupViewForPrinterState(
                    appViewModel.printerState.first(),
                    appViewModel.printNum.first()
                )
            )
        }
    }


    private fun showOpenDocumentTree() {
        viewModelScope.launch {
            _uiState.emit(MainState.ShowOpenDocumentTree)
        }
    }

    private fun listenerTime() {
        appViewModel.processIntent(AppIntent.ListenerTime)
    }

    private fun changeNavCurIndex(index: Int) {
        curIndex = index
        viewModelScope.launch {
            _uiState.emit(MainState.CurIndex(index))
        }
    }

    private fun changeStorageState(state: StorageState) {
        appViewModel.processIntent(AppIntent.StorageStateChange(state))
    }
}

sealed class MainState {
    object None : MainState()
    object ShowOpenDocumentTree : MainState()
    data class CurIndex(val index: Int) : MainState()
    data class ShowPopupViewForUploadState(val uploadState: UploadState) : MainState()
    data class ShowPopupViewForStorageState(val storageState: com.wl.turbidimetric.app.StorageState) :
        MainState()

    data class ShowPopupViewForMachineState(val machineState: MachineState) : MainState()
    data class ShowPopupViewForPrinterState(val printerState: PrinterState, val printNum: Int) :
        MainState()
}

sealed class MainIntent {
    object AllowRunning : MainIntent() //机械故障，不允许继续运行
    object ShutDown : MainIntent() //关机
    object ListenerTime : MainIntent() //开始监听时间
    object ShowOpenDocumentTree : MainIntent() //显示u盘授权对话框
    data class ChangeNavCurIndex(val index: Int) : MainIntent() //变更当前导航页
    data class ChangeStorageState(val state: StorageState) : MainIntent() //变更u盘状态

    object ShowPopupViewForUploadState : MainIntent() //显示上传状态气泡
    object ShowPopupViewForStorageState : MainIntent() //显示u盘状态气泡
    object ShowPopupViewForMachineState : MainIntent() //显示仪器状态气泡
    object ShowPopupViewForPrinterState : MainIntent() //显示打印机状态气泡
}

class MainViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                appViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
