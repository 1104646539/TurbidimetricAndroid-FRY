package com.wl.turbidimetric.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.repository.DefaultLocalDataDataSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

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
    private val localDataSource: LocalDataSource = DefaultLocalDataDataSource()
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

