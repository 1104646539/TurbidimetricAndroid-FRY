package com.wl.turbidimetric.test.debug.motorDebug

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.model.CuvetteDoorModel
import com.wl.turbidimetric.model.DripReagentModel
import com.wl.turbidimetric.model.DripSampleModel
import com.wl.turbidimetric.model.FullR1Model
import com.wl.turbidimetric.model.GetMachineStateModel
import com.wl.turbidimetric.model.GetStateModel
import com.wl.turbidimetric.model.GetVersionModel
import com.wl.turbidimetric.model.KillAllModel
import com.wl.turbidimetric.model.MotorModel
import com.wl.turbidimetric.model.MoveCuvetteDripReagentModel
import com.wl.turbidimetric.model.MoveCuvetteDripSampleModel
import com.wl.turbidimetric.model.MoveCuvetteShelfModel
import com.wl.turbidimetric.model.MoveCuvetteTestModel
import com.wl.turbidimetric.model.MoveSampleModel
import com.wl.turbidimetric.model.MoveSampleShelfModel
import com.wl.turbidimetric.model.OverloadParamsModel
import com.wl.turbidimetric.model.PiercedModel
import com.wl.turbidimetric.model.ReplyModel
import com.wl.turbidimetric.model.SampleDoorModel
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

class MotorDebugViewModel(private val appViewModel: AppViewModel) : BaseViewModel(), Callback2 {
    private val testMsg = MutableStateFlow("")
    val looperTest = MutableLiveData(appViewModel.getLooperTest())
    val params = MutableLiveData("")
    val enable = MutableLiveData(true)
    val chekckID = MutableLiveData(R.id.cb_forward)
    val motor = mutableListOf(
        "1(取样泵)",
        "2（样本架X）",
        "3（样本架Y）",
        "4（比色皿架X）",
        "5（比色皿架Y）",
        "6（试剂针X）",
        "7（试剂针Z）",
        "8（样本针X）",
        "9（样本针Z）",
        "10（试剂泵）",
        "11（搅拌）",
        "12（扶正）",
        "13（试剂）"
    )
    val motorIndex = MutableLiveData(0);

    fun action() {
        i("chekckID=${chekckID.value} enable=${enable.value} params=${params.value} motorIndex=${motorIndex.value}")
        enable.value = false

        val motorNum = motorIndex.value?.plus(1) ?: 1
        val direction =
            if (chekckID.value == R.id.cb_forward) 2 else if (chekckID.value == R.id.cb_backward) 3 else 1
        val param = params.value?.toIntOrNull() ?: 0
        appViewModel.serialPort.motor(
            motorNum, direction, param
        );
    }

    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {

    }

    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {

    }

    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {

    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {

    }

    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {

    }

    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {

    }

    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {

    }

    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {

    }

    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {

    }

    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {

    }

    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {

    }

    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {

    }

    override fun readDataStirModel(reply: ReplyModel<StirModel>) {

    }

    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {

    }

    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {

    }

    override fun readDataTestModel(reply: ReplyModel<TestModel>) {

    }

    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {

    }

    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {

    }

    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {

    }

    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {

    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {

    }

    override fun stateSuccess(cmd: Int, state: Int): Boolean {
        return false;
    }

    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {

    }

    override fun readDataMotor(reply: ReplyModel<MotorModel>) {
        enable.postValue(true)
        i("接收到 控制电机")
    }

    override fun readDataOverloadParamsModel(reply: ReplyModel<OverloadParamsModel>) {


    }

    override fun readDataFullR1Model(reply: ReplyModel<FullR1Model>) {

    }

    override fun readDataKillAllModel(reply: ReplyModel<KillAllModel>) {

    }

    fun listener() {
        appViewModel.serialPort.addCallback(this)
        appViewModel.testType = TestType.Debug
        i("appViewModel.serialPort.callback listener")
    }

    fun clearListener() {
        appViewModel.serialPort.removeCallback(this)
        i("appViewModel.serialPort.callback onCleared")
    }
}

class MotorDebugViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MotorDebugViewModel::class.java)) {
            return MotorDebugViewModel(
                appViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
