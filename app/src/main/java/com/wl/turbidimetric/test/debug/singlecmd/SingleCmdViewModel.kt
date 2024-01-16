package com.wl.turbidimetric.test.debug.singlecmd

import androidx.lifecycle.MutableLiveData
import com.wl.turbidimetric.global.SystemGlobal.testType
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
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wllib.LogToFile.i
import com.wl.turbidimetric.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class SingleCmdViewModel : BaseViewModel(), Callback2 {
    val testMsg = MutableStateFlow("")
    val resultMsg = MutableStateFlow("")
    val enable = MutableLiveData(true)
    fun listener() {
        SerialPortUtil.callback.add(this)
        testType = TestType.Debug
        i("SerialPortUtil.callback listener")
    }

    fun clearListener() {
        SerialPortUtil.callback.remove(this)
        i("SerialPortUtil.callback onCleared")
    }

    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        var msg = "自检完成\n"
        reply.data.errorInfo.forEach { error ->
            msg += "错误信息：${error.errorMsg}" + "错误号:${error.motorMsg}" + "\n"
        }
        changeResult(msg)
    }

    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        val sampleShelfStr =reply.data.sampleShelfs.joinTo(StringBuffer())
        val cuvetteShelfStr =reply.data.cuvetteShelfs.joinTo(StringBuffer())
        var msg =
            "获取状态完成\n清洗液状态:${reply.data.cleanoutFluid.isExist()} R1状态:${reply.data.r1Reagent.isExist()} R2状态:${reply.data.r2Reagent.isExist()} ${reply.data.r2Volume}\n样本架状态:${sampleShelfStr} 比色皿架状态:${cuvetteShelfStr}"
        changeResult(msg)
    }

    private fun Boolean.isExist(): String {
        return if (this) {
            "有"
        } else {
            "无"
        }
    }

    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        var msg = "移动样本架完成"
        changeResult(msg)
    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        var msg = "移动比色皿架完成"
        changeResult(msg)
    }

    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        var msg = "移动样本完成\n state=${reply.data.type}"
        changeResult(msg)
    }

    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {
        var msg = "移动比色皿到加样位完成"
        changeResult(msg)
    }

    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        var msg = "移动比色皿到加试剂位完成"
        changeResult(msg)
    }

    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        var msg = "移动比色皿到检测位完成"
        changeResult(msg)
    }

    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        var msg = "取样完成"
        changeResult(msg)
    }

    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        var msg = "加试剂完成"
        changeResult(msg)
    }

    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        var msg = "加样完成"
        changeResult(msg)
    }

    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        var msg = "加试剂完成"
        changeResult(msg)
    }

    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        var msg = "搅拌完成"
        changeResult(msg)
    }

    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        var msg = "搅拌针清洗完成"
        changeResult(msg)
    }

    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {
        var msg = "取样针清洗完成"
        changeResult(msg)
    }

    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        var msg = "检测完成\n ${reply.data.value}"
        changeResult(msg)
    }

    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
    }

    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {

    }

    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {
        var msg = "刺破完成"
        changeResult(msg)
    }

    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        var msg = "获取下位机版本号完成\n ${reply.data.version}"
        changeResult(msg)
    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {
        var msg = "获取温度完成\n r1温度：${reply.data.r1Temp} 反应槽温度：${reply.data.reactionTemp}"
        changeResult(msg)
    }

//    override fun readDataStateFailed(cmd: UByte, state: UByte) {
//        var msg = "错误信息\n cmd=$cmd state=$state"
//        changeResult(msg)
//    }
override fun stateSuccess(cmd: Int, state: Int): Boolean {
    return true
}
    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {
        var msg = "挤压完成"
        changeResult(msg)
    }

    /**
     * 自检
     */
    fun getMachineState() {
        enable.postValue(false)
        SerialPortUtil.getMachineState()
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
        enable.postValue(false)
        SerialPortUtil.moveSampleShelf(step)
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
        enable.postValue(false)
        SerialPortUtil.moveCuvetteShelf(step)
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
        if (step !in 1..10) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        enable.postValue(false)
        SerialPortUtil.moveSample(forward, step)
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
        if (step !in 1..10) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        enable.postValue(false)
        SerialPortUtil.moveCuvetteDripSample(forward, step)
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
        if (step !in 1..10) {
            changeHilt("移动位置错误,必须为1-10")
            return
        }
        enable.postValue(false)
        SerialPortUtil.moveCuvetteDripReagent(forward, step)
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
        enable.postValue(false)
        SerialPortUtil.moveCuvetteTest(forward, step)
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
        enable.postValue(false)
        SerialPortUtil.sampling(volume, sampleType)
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
        enable.postValue(false)
        SerialPortUtil.dripSample(blending, inplace, volume)
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
        if (volumeR2 !in 1..100) {
            changeHilt("R2取试剂量错误,必须为1-100")
            return
        }
        enable.postValue(false)
        SerialPortUtil.takeReagent(volumeR1, volumeR2)
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
        if (volumeR1 !in 1..500) {
            changeHilt("R1加试剂量错误,必须为1-500")
            return
        }
        if (msg2.isNullOrEmpty()) {
            changeHilt("R2加试剂量错误")
            return
        }
        val volumeR2 = msg1.toIntOrNull() ?: 0
        if (volumeR2 !in 1..200) {
            changeHilt("R2加试剂量错误,必须为1-200")
            return
        }
        enable.postValue(false)
        SerialPortUtil.takeReagent(volumeR1, volumeR2)
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
        enable.postValue(false)
        SerialPortUtil.stir(volume)
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
        enable.postValue(false)
        SerialPortUtil.samplingProbeCleaning(volume)
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
        enable.postValue(false)
        SerialPortUtil.stirProbeCleaning(volume)
    }

    /**
     * 刺破
     */
    fun pierced() {
        enable.postValue(false)
        SerialPortUtil.pierced(SampleType.SAMPLE)
    }

    /**
     * 挤压
     */
    fun squeezing() {
        enable.postValue(false)
        SerialPortUtil.squeezing(true)
    }

    /**
     * 获取下位机版本号
     */
    fun getMcuVersion() {
        enable.postValue(false)
        SerialPortUtil.getVersion()
    }

    /**
     * 获取温度
     */
    fun getTemp() {
        enable.postValue(false)
        SerialPortUtil.getTemp()
    }


    /**
     * 检测
     */
    fun test() {
        enable.postValue(false)
        SerialPortUtil.test()
    }

    /**
     * 关机
     */
    fun shutdown() {
        enable.postValue(false)
        SerialPortUtil.shutdown()
    }

    /**
     * 获取状态
     */
    fun getState() {
        enable.postValue(false)
        SerialPortUtil.getState()
    }


    private fun changeHilt(msg: String) {
        testMsg.value = msg
    }

    private fun changeResult(msg: String) {
        enable.postValue(true)
        resultMsg.value = msg
    }
}
