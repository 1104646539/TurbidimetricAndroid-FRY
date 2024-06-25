package com.wl.turbidimetric.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.repository.if2.LocalDataSource

class SettingsViewModel constructor(
    private val localDataSource: LocalDataSource
) : BaseViewModel() {

    fun setTestModel(
        debugMode: Boolean
    ) {
        localDataSource.setDebugMode(debugMode)
    }
    fun getTestModel(
    ) :Boolean{
       return localDataSource.getDebugMode()
    }

}

class SettingsViewModelFactory(
    private val localDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource()
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                localDataSource
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

