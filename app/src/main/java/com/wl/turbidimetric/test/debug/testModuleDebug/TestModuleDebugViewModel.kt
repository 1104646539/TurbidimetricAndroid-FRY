package com.wl.turbidimetric.test.debug.testModuleDebug

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
import com.wl.turbidimetric.model.GetMachineStateModel
import com.wl.turbidimetric.model.GetStateModel
import com.wl.turbidimetric.model.GetVersionModel
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
import com.wl.wllib.DateUtil
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.log4j.helpers.FormattingInfo
import java.util.Date
import java.util.Timer
import kotlin.concurrent.timer

class TestModuleDebugViewModel(private val appViewModel: AppViewModel) : BaseViewModel(),
    Callback2 {
    val testMsg = MutableStateFlow("")
    val resultMsg = MutableStateFlow("")
    var debugType: DebugType? = null

    enum class DebugType {
        IntervalTestModule
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

    var cuvetteShelfPos = 0
    var cuvettePos = -1
    var result = mutableListOf<Int>()

    //上一次移动比色皿的时间
    var prevMoveCuvetteTime = 0L

    var reset = false
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        debugType?.let {
            i("接收到 获取状态")
            val firstIndex = reply.data.cuvetteShelfs.firstOrNull { it == 1 } ?: -1
            if (firstIndex == -1) {
                changeHilt("请放入比色皿架")
                testFinish()
            } else {
                //开始移动比色皿架
                moveCuvetteShelf("${firstIndex + 1}")
            }
        }
    }


    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {

    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        debugType?.let {
            i("接收到 移动比色皿架 $cuvetteShelfPos")
            if (cuvetteShelfPos == 0) {
                //复位
                changeHilt(testMsg.value.plus("\n检测完成\n"))
                testFinish()
            } else {
                //去移动，检测
                moveCuvetteTest("2", true)
            }
        }
    }

    private fun testFinish() {
        debugType = null
    }

    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {

    }

    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {

    }

    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {

    }

    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        debugType?.let {
            i("接收到 移动比色到检测位 cuvettePos=$cuvettePos waitDuration=$waitDuration")
            viewModelScope.launch {
                delay(waitDuration.toLong())
                test()
            }
        }
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
        debugType?.let {
            i("接收到 检测${reply.data.value}")
//            result.add(reply.data.value)
            changeHilt(testMsg.value.plus("${reply.data.value},"))
            if (cuvettePos == 10) {
                //结束了
                cuvetteShelfPos = 0
                moveCuvetteShelf("$cuvetteShelfPos")

            } else {
                viewModelScope.launch {
                    val temp = intervalDuration - (Date().time - prevMoveCuvetteTime)
                    delay(temp)
                    moveCuvetteTest("1", true)
                }
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

    var intervalDuration = 0
    var waitDuration = 0
    fun startIntervalTest(interval: String, wait: String) {
        if (interval.isNullOrEmpty()) {
            changeHilt("间隔错误")
            return
        }
        if (wait.isNullOrEmpty()) {
            changeHilt("等待间隔错误")
            return
        }
        intervalDuration = interval.toIntOrNull() ?: 0
        if (intervalDuration !in 1..500000) {
            changeHilt("间隔错误,必须为1-500000")
            return
        }
        waitDuration = wait.toIntOrNull() ?: 0
        if (waitDuration !in 1..500000) {
            changeHilt("等待间隔错误,必须为1-500000")
            return
        }
        if (intervalDuration - waitDuration < 1000) {
            changeHilt("移动间隔必须比检测间隔大")
            return
        }

        initState()
        getState()
    }

    private fun initState() {
        debugType = DebugType.IntervalTestModule
        cuvetteShelfPos = 0
        cuvettePos = -1
        result.clear()
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
        if (step !in 0..4) {
            changeHilt("移动位置错误,必须为0-4")
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
        if (step !in 0..4) {
            changeHilt("移动位置错误,必须为0-4")
            return
        }
        cuvetteShelfPos = step
        i("发送 移动比色皿架 $cuvetteShelfPos")
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
        if (step !in 1..10) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        cuvettePos += step
        prevMoveCuvetteTime = Date().time
        i("发送 移动比色皿 cuvettePos=$cuvettePos step=$step prevMoveCuvetteTime=$prevMoveCuvetteTime")
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
        i("发送 检测")
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
        i("发送 获取状态")
        appViewModel.serialPort.getState()
    }


    private fun changeHilt(msg: String) {
        testMsg.value = msg
    }

    private fun changeResult(msg: String) {
        resultMsg.value = msg
    }

    fun clearMsg() {
        changeHilt("")
    }
}

class TestModuleDebugViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestModuleDebugViewModel::class.java)) {
            return TestModuleDebugViewModel(
                appViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
