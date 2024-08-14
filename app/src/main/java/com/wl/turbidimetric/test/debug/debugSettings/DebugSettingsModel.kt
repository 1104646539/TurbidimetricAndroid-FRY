package com.wl.turbidimetric.test.debug.debugSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalDataGlobal
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.repository.if2.LocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow

class DebugSettingsViewModel(
    private val appViewModel: AppViewModel,
    private val localDataSource: LocalDataSource,
) : BaseViewModel() {
    private val testMsg = MutableStateFlow("")
    val looperTest = MutableLiveData(appViewModel.getLooperTest())
    val tempLowLimit = MutableLiveData(localDataSource.getTempLowLimit().toString())
    val tempUpLimit = MutableLiveData(localDataSource.getTempUpLimit().toString())

    fun saveConfig() {
        localDataSource.setLooperTest(looperTest.value ?: false)
        localDataSource.setTempLowLimit(
            tempLowLimit.value?.toIntOrNull() ?: LocalDataGlobal.Default.TempLowLimit
        )
        localDataSource.setTempUpLimit(tempUpLimit.value?.toIntOrNull() ?: LocalDataGlobal.Default.TempUpLimit)
    }


}

class DebugSettingsViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
    private val localDataDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!),

    ) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebugSettingsViewModel::class.java)) {
            return DebugSettingsViewModel(
                appViewModel, localDataDataSource
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
