package com.wl.turbidimetric.test.debug.debugSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.getAppViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DebugSettingsViewModel(private val appViewModel: AppViewModel) : BaseViewModel() {
    private val testMsg = MutableStateFlow("")
    val looperTest = MutableLiveData(LocalData.LooperTest)

    fun changeLooperTest(looperTest: Boolean) {
        LocalData.LooperTest = looperTest
        appViewModel.looperTest = looperTest
    }


}

class DebugSettingsViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebugSettingsViewModel::class.java)) {
            return DebugSettingsViewModel(
                appViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
