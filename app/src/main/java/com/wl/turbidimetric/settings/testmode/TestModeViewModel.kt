package com.wl.turbidimetric.settings.testmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.repository.DefaultLocalDataDataSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.settings.params.ParamsViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TestModeViewModel(private val localDataSource: LocalDataSource) : BaseViewModel() {
    private val _testModeUiState = MutableSharedFlow<TestModeUiState>()
    val testModeUiState = _testModeUiState.asSharedFlow()

    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _testModeUiState.emit(
                TestModeUiState(
                    MachineTestModel.valueOf(LocalData.CurMachineTestModel),
                    localDataSource.getSampleExist(),
                    localDataSource.getScanCode()
                )
            )
        }
    }

    fun change(
        machineTestModel: MachineTestModel,
        sampleExist: Boolean,
        scanCode: Boolean,
    ) {
        localDataSource.setCurMachineTestModel(machineTestModel.name)
        localDataSource.setSampleExist(sampleExist)
        localDataSource.setScanCode(scanCode)
        viewModelScope.launch {
            _hiltText.emit("修改成功")
        }
    }

}

class TestModeViewModelFactory(
    private val localDataSource: LocalDataSource = DefaultLocalDataDataSource()
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestModeViewModel::class.java)) {
            return TestModeViewModel(
                localDataSource
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class TestModeUiState(
    val machineTestModel: MachineTestModel,
    val sampleExist: Boolean,
    val scanCode: Boolean,
)
