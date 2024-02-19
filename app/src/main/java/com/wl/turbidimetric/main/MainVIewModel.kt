package com.wl.turbidimetric.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.home.HomeViewModel

class MainViewModel constructor(private val appViewModel: AppViewModel) : BaseViewModel() {
    fun shutdown() {
        appViewModel.serialPort.shutdown()
    }

    fun allowRunning() {
        appViewModel.serialPort.allowRunning()
    }

    val curIndex = MutableLiveData<Int>(0)

    val navItems = SystemGlobal.navItems

    init {

    }
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
