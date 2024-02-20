package com.wl.turbidimetric.test.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.ex.toHex
import com.wl.turbidimetric.util.OriginalDataCall
import com.wl.wllib.LogToFile
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.repository.DefaultCurveDataSource
import com.wl.turbidimetric.repository.DefaultLocalDataDataSource
import com.wl.turbidimetric.repository.DefaultTestResultDataSource
import com.wl.turbidimetric.repository.if2.CurveSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.TestResultSource
import com.wl.turbidimetric.test.repeatablitylity.RepeatabilityViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DebugViewModel(private val appViewModel: AppViewModel) : BaseViewModel(), OriginalDataCall {
    val originalDataMsg = MutableStateFlow("")
    fun listener() {
        appViewModel.serialPort.addOriginalCallback(this)
        LogToFile.i("SerialPortUtil.callback listener")
    }

    fun clearListener() {
        appViewModel.serialPort.removeOriginalCallback(this)
        LogToFile.i("SerialPortUtil.callback onCleared")
    }

    override fun readDataOriginalData(ready: UByteArray) {
        originalDataMsg.value = "接收："+ready.toHex()+"\n"
    }

    override fun sendOriginalData(ready: UByteArray) {
        originalDataMsg.value = "发送："+ready.toHex()+"\n"
    }
}
class DebugViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebugViewModel::class.java)) {
            return DebugViewModel(
                appViewModel,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
