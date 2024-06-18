package com.wl.turbidimetric.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.ex.getAppViewModel

class MainViewModel(private val appViewModel: AppViewModel) : BaseViewModel() {
    /**
     * 关机命令
     */
    fun shutdown() {
        appViewModel.serialPort.shutdown()
    }

    /**
     * 不允许发送命令
     */
    fun allowRunning() {
        appViewModel.serialPort.allowRunning()
    }

    val curIndex = MutableLiveData<Int>(0)

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
