package com.wl.turbidimetric.settings.testmode

import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.model.MachineTestModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TestModeViewModel : BaseViewModel() {
    private val _testModeUiState = MutableSharedFlow<TestModeUiState>()
    val testModeUiState = _testModeUiState.asSharedFlow()

    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _testModeUiState.emit(
                TestModeUiState(
                    MachineTestModel.valueOf(LocalData.CurMachineTestModel),
                    LocalData.SampleExist,
                    LocalData.ScanCode
                )
            )
        }
    }

    fun change(
        machineTestModel: MachineTestModel,
        sampleExist: Boolean,
        scanCode: Boolean,
    ) {
        LocalData.CurMachineTestModel = machineTestModel.name
        LocalData.SampleExist = sampleExist
        LocalData.ScanCode = scanCode
        viewModelScope.launch {
            _hiltText.emit("修改成功")
        }
    }

}

data class TestModeUiState(
    val machineTestModel: MachineTestModel,
    val sampleExist: Boolean,
    val scanCode: Boolean,
)
