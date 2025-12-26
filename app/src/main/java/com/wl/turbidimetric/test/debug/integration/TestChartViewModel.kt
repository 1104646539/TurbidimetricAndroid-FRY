package com.wl.turbidimetric.test.debug.integration

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

class TestChartViewModel(private val appViewModel: AppViewModel) : BaseViewModel(), Callback2 {
    val testMsg = MutableStateFlow("")
    val resultMsg = MutableStateFlow("")
    var debugType = DebugType.IntervalTest
    val point = MutableLiveData<Int>()

    enum class DebugType {
        IntervalTest
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
        if (debugType == DebugType.IntervalTest) {
//            var msg = "检测完成\n ${reply.data.value}"
//            changeResult(msg)
            viewModelScope.launch {
                point.postValue(reply.data.value)
            }
        }
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

    //    override fun readDataStateFailed(cmd: UByte, state: UByte) {
//
//    }
    override fun stateSuccess(cmd: Int, state: Int): Boolean {
        return true
    }

    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {

    }

    override fun readDataMotor(reply: ReplyModel<MotorModel>) {

    }

    override fun readDataOverloadParamsModel(reply: ReplyModel<OverloadParamsModel>) {


    }

    override fun readDataFullR1Model(reply: ReplyModel<FullR1Model>) {

    }

    override fun readDataKillAllModel(reply: ReplyModel<KillAllModel>) {

    }

    var intervalTestTimer: Timer? = null
    fun startIntervalTest(msg: String) {
        if (msg.isNullOrEmpty()) {
            changeHilt("间隔错误")
            return
        }
        val intervalDuration = msg.toIntOrNull() ?: 0
        if (intervalDuration !in 1..500000) {
            changeHilt("间隔错误,必须为1-500000")
            return
        }
        debugType = DebugType.IntervalTest
        viewModelScope.launch {
            stopIntervalTest()
            intervalTestTimer = timer("", true, Date(), intervalDuration.toLong()) {
                test()
            }
        }
    }

    fun stopIntervalTest() {
        intervalTestTimer?.cancel()
    }

    /**
     * 自检
     */
    fun getMachineState() {
        appViewModel.serialPort.getMachineState()
    }

    /**
     * 移动样本架位置
     * @param msg 位置
     */
    fun moveSampleShelf(msg: String) {
        if (msg.isNullOrEmpty()) {
            changeHilt("移动位置错误")
            return
        }
        val step = msg.toIntOrNull() ?: 0
        if (step !in 1..4) {
            changeHilt("移动位置错误,必须为1-4")
            return
        }
        appViewModel.serialPort.moveSampleShelf(step)
    }

    /**
     * 移动比色皿架位置
     * @param msg 位置
     */
    fun moveCuvetteShelf(msg: String) {
        if (msg.isNullOrEmpty()) {
            changeHilt("移动位置错误")
            return
        }
        val step = msg.toIntOrNull() ?: 0
        if (step !in 1..4) {
            changeHilt("移动位置错误,必须为1-4")
            return
        }
        appViewModel.serialPort.moveCuvetteShelf(step)
    }

    /**
     * 移动样本位置
     * @param msg 距离
     * @param forward true向前 false向后
     */
    fun moveSample(msg: String, forward: Boolean) {
        if (msg.isNullOrEmpty()) {
            changeHilt("移动位置错误")
            return
        }
        val step = msg.toIntOrNull() ?: 0
        if (step !in 1..4) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        appViewModel.serialPort.moveSample(forward, step)
    }

    /**
     * 移动比色皿（加样位）
     * @param msg 距离
     * @param forward true向前 false向后
     */
    fun moveCuvetteDripSample(msg: String, forward: Boolean) {
        if (msg.isNullOrEmpty()) {
            changeHilt("移动位置错误")
            return
        }
        val step = msg.toIntOrNull() ?: 0
        if (step !in 1..4) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        appViewModel.serialPort.moveCuvetteDripSample(forward, step)
    }

    /**
     * 移动比色皿（加试剂位）
     * @param msg 距离
     * @param forward true向前 false向后
     */
    fun moveCuvetteDripReagent(msg: String, forward: Boolean) {
        if (msg.isNullOrEmpty()) {
            changeHilt("移动位置错误")
            return
        }
        val step = msg.toIntOrNull() ?: 0
        if (step !in 1..4) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        appViewModel.serialPort.moveCuvetteDripReagent(forward, step)
    }

    /**
     * 移动比色皿（检测位）
     * @param msg 距离
     * @param forward true向前 false向后
     */
    fun moveCuvetteTest(msg: String, forward: Boolean) {
        if (msg.isNullOrEmpty()) {
            changeHilt("移动位置错误")
            return
        }
        val step = msg.toIntOrNull() ?: 0
        if (step !in 1..4) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        appViewModel.serialPort.moveCuvetteTest(forward, step)
    }

    /**
     * 取样
     * @param msg 取样量
     * @param sampleType 取样类型
     */
    fun sampling(msg: String, sampleType: SampleType) {
        if (msg.isNullOrEmpty()) {
            changeHilt("取样量错误")
            return
        }
        val volume = msg.toIntOrNull() ?: 0
        if (volume !in 1..500) {
            changeHilt("取样量错误,必须为1-500")
            return
        }
        appViewModel.serialPort.sampling(volume, sampleType)
    }

    /**
     * 加样
     * @param msg 加样量
     * @param blending true 混匀 false不混匀
     * @param inplace true 原地加样 false 去比色皿处加样
     */
    fun dripSample(msg: String, blending: Boolean, inplace: Boolean) {
        if (msg.isNullOrEmpty()) {
            changeHilt("加样量错误")
            return
        }
        val volume = msg.toIntOrNull() ?: 0
        if (volume !in 1..500) {
            changeHilt("加样量错误,必须为1-500")
            return
        }
        appViewModel.serialPort.dripSample(blending, inplace, volume)
    }

    /**
     * 取试剂
     * @param msg1 取试剂量 R1
     * @param msg2 取试剂量 R2
     */
    fun takeReagent(msg1: String, msg2: String) {
        if (msg1.isNullOrEmpty()) {
            changeHilt("R1取试剂量错误")
            return
        }
        val volumeR1 = msg1.toIntOrNull() ?: 0
        if (volumeR1 !in 1..100) {
            changeHilt("R1取试剂量错误,必须为1-100")
            return
        }
        if (msg2.isNullOrEmpty()) {
            changeHilt("R2取试剂量错误")
            return
        }
        val volumeR2 = msg1.toIntOrNull() ?: 0
        if (volumeR2 !in 1..500) {
            changeHilt("R2取试剂量错误,必须为1-100")
            return
        }
        appViewModel.serialPort.takeReagent(volumeR1, volumeR2)
    }

    /**
     * 加试剂
     * @param msg1 加试剂量 R1
     * @param msg2 加试剂量 R2
     */
    fun dripReagent(msg1: String, msg2: String) {
        if (msg1.isNullOrEmpty()) {
            changeHilt("R1加试剂量错误")
            return
        }
        val volumeR1 = msg1.toIntOrNull() ?: 0
        if (volumeR1 !in 1..100) {
            changeHilt("R1加试剂量错误,必须为1-100")
            return
        }
        if (msg2.isNullOrEmpty()) {
            changeHilt("R2加试剂量错误")
            return
        }
        val volumeR2 = msg1.toIntOrNull() ?: 0
        if (volumeR2 !in 1..500) {
            changeHilt("R2加试剂量错误,必须为1-100")
            return
        }
        appViewModel.serialPort.takeReagent(volumeR1, volumeR2)
    }

    /**
     * 搅拌
     * @param msg 搅拌时长 ms
     */
    fun stir(msg: String) {
        if (msg.isNullOrEmpty()) {
            changeHilt("搅拌时长错误")
            return
        }
        val volume = msg.toIntOrNull() ?: 0
        if (volume !in 1..10000) {
            changeHilt("搅拌时长错误,必须为1-10000")
            return
        }
        appViewModel.serialPort.stir(volume)
    }

    /**
     * 取样针清洗
     * @param msg 清洗时长
     */
    fun samplingProbeCleaning(msg: String) {
        if (msg.isNullOrEmpty()) {
            changeHilt("清洗时长错误")
            return
        }
        val volume = msg.toIntOrNull() ?: 0
        if (volume !in 1..10000) {
            changeHilt("清洗时长错误,必须为1-10000")
            return
        }
        appViewModel.serialPort.samplingProbeCleaning(volume)
    }


    /**
     * 搅拌针清洗
     * @param msg 清洗时长
     */
    fun stirProbeCleaning(msg: String) {
        if (msg.isNullOrEmpty()) {
            changeHilt("清洗时长错误")
            return
        }
        val volume = msg.toIntOrNull() ?: 0
        if (volume !in 1..10000) {
            changeHilt("清洗时长错误,必须为1-10000")
            return
        }
        appViewModel.serialPort.stirProbeCleaning(volume)
    }

    /**
     * 刺破
     */
    fun pierced() {
        appViewModel.serialPort.pierced(SampleType.SAMPLE)
    }

    /**
     * 挤压
     */
    fun squeezing() {
        appViewModel.serialPort.squeezing(true)
    }

    /**
     * 获取下位机版本号
     */
    fun getMcuVersion() {
        appViewModel.serialPort.getVersion()
    }

    /**
     * 获取温度
     */
    fun getTemp() {
        appViewModel.serialPort.getTemp()
    }


    /**
     * 检测
     */
    fun test() {
        appViewModel.serialPort.test()
    }

    /**
     * 关机
     */
    fun shutdown() {
        appViewModel.serialPort.shutdown()
    }

    /**
     * 获取状态
     */
    fun getState() {
        appViewModel.serialPort.getState()
    }


    private fun changeHilt(msg: String) {
        testMsg.value = msg
    }

    private fun changeResult(msg: String) {
        resultMsg.value = msg
    }
}

class TestChartViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestChartViewModel::class.java)) {
            return TestChartViewModel(
                appViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
