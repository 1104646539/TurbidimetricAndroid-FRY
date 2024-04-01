package com.wl.turbidimetric.test.debug.debugSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.model.CuvetteDoorModel
import com.wl.turbidimetric.model.DripReagentModel
import com.wl.turbidimetric.model.DripSampleModel
import com.wl.turbidimetric.model.GetMachineStateModel
import com.wl.turbidimetric.model.GetStateModel
import com.wl.turbidimetric.model.GetVersionModel
import com.wl.turbidimetric.model.MoveCuvetteDripReagentModel
import com.wl.turbidimetric.model.MoveCuvetteDripSampleModel
import com.wl.turbidimetric.model.MoveCuvetteShelfModel
import com.wl.turbidimetric.model.MoveCuvetteTestModel
import com.wl.turbidimetric.model.MoveSampleModel
import com.wl.turbidimetric.model.MoveSampleShelfModel
import com.wl.turbidimetric.model.PiercedModel
import com.wl.turbidimetric.model.ReplyModel
import com.wl.turbidimetric.model.SampleDoorModel
import com.wl.turbidimetric.model.SampleType
import com.wl.turbidimetric.model.SamplingModel
import com.wl.turbidimetric.model.SamplingProbeCleaningModel
import com.wl.turbidimetric.model.SqueezingModel
import com.wl.turbidimetric.model.StirModel
import com.wl.turbidimetric.model.StirProbeCleaningModel
import com.wl.turbidimetric.model.TakeReagentModel
import com.wl.turbidimetric.model.TempModel
import com.wl.turbidimetric.model.TestModel
import com.wl.turbidimetric.model.TestType
import com.wl.turbidimetric.util.Callback2
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Timer
import kotlin.concurrent.timer

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
