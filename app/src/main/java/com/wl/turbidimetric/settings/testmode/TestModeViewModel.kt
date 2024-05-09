package com.wl.turbidimetric.settings.testmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.model.GetPatientType
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
                    MachineTestModel.valueOf(localDataSource.getCurMachineTestModel()),
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
        verifyChange(machineTestModel, sampleExist, scanCode).let { ret ->
            if (ret.isNotEmpty()) {
                viewModelScope.launch {
                    _hiltText.emit("修改失败,$ret")
                }
                return
            }
        }
        localDataSource.setCurMachineTestModel(machineTestModel.name)
        localDataSource.setSampleExist(sampleExist)
        localDataSource.setScanCode(scanCode)
        viewModelScope.launch {
            _hiltText.emit("修改成功")
        }
    }

    private fun verifyChange(
        machineTestModel: MachineTestModel,
        sampleExist: Boolean,
        scanCode: Boolean,
    ): String {
        if (machineTestModel != MachineTestModel.Auto || !scanCode) {
            val config = HL7Helper.getConfig()
            if (config.openUpload && config.twoWay && config.getPatientType == GetPatientType.BC) {
                return "当按条码匹配时，不允许更改检测模式或关闭扫码"
            }
        }
        return ""
    }
}

class TestModeViewModelFactory(
    private val localDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!)
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
