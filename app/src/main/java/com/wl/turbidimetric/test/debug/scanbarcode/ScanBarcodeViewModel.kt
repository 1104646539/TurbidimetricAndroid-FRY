package com.wl.turbidimetric.test.debug.scanbarcode

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.ex.isSample
import com.wl.turbidimetric.home.HomeDialogUiState
import com.wl.turbidimetric.model.CuvetteDoorModel
import com.wl.turbidimetric.model.DripReagentModel
import com.wl.turbidimetric.model.DripSampleModel
import com.wl.turbidimetric.model.FullR1Model
import com.wl.turbidimetric.model.GetMachineStateModel
import com.wl.turbidimetric.model.GetStateModel
import com.wl.turbidimetric.model.GetVersionModel
import com.wl.turbidimetric.model.Item
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
import com.wl.turbidimetric.model.SampleState
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
import com.wl.turbidimetric.util.OnScanResult
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.wllib.LogToFile
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.math.absoluteValue

class ScanBarcodeViewModel(private val appViewModel: AppViewModel) : BaseViewModel(), Callback2,
    OnScanResult {
    val testMsg = MutableStateFlow("")
    val hiltMsg = MutableStateFlow("")
    var needRunning = false

    /**样本架状态 1有 0无 顺序是从中间往旁边
     *
     */
    private var sampleShelfStates: IntArray = IntArray(4)

    /**当前排所有样本的状态
     *
     */
    private var mSamplesStates = initSampleStates()

    /**当前样本架位置
     *
     */
    private var sampleShelfPos = -1

    /**最后一排可使用的样本架的位置
     *
     */
    private var lastSampleShelfPos = -1


    /**当前样本位置
     *
     */
    private var samplePos = -1

    /**样本最大步数
     *
     */
    private val sampleMax = 10

    /**
     * 总扫码次数
     */
    private var scanCount = 0

    /**
     * 扫码成功次数
     */
    private var scanSuccessCount = 0

    /**
     * 扫码失败次数
     */
    private var scanFailedCount = 0


    fun listener() {
        appViewModel.serialPort.addCallback(this)
        appViewModel.scanCodeUtil.onScanResult = this
        appViewModel.testType = TestType.Debug
        i("appViewModel.serialPort.callback listener")
    }

    fun clearListener() {
        appViewModel.serialPort.removeCallback(this)
        i("appViewModel.serialPort.callback onCleared")
    }

    /**
     * 重置样本状态
     * @return MutableList<CuvetteState>
     */
    private fun initSampleStates(): Array<Array<Item>?> {
        val arrays = mutableListOf<Array<Item>?>()
        for (j in 0 until 4) {
            arrays.add(null)
        }

        return arrays.toTypedArray()
    }

    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {

    }

    /**
     * 获取初始位置
     */
    private fun getInitialPos() {
        getInitSampleShelfPos()
    }

    /**
     * 获取样本架初始位置和最后一排的位置
     */
    private fun getInitSampleShelfPos() {
        val arrays: Array<Array<Item>?> = arrayOfNulls(4)
        for (i in sampleShelfStates.indices) {
            var array: Array<Item>? = null
            if (sampleShelfStates[i] == 1) {
                array = arrayOf(
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                    Item(SampleState.None),
                )
                if (sampleShelfPos == -1) {
                    sampleShelfPos = i
                }
                lastSampleShelfPos = i
            }
            arrays[i] = array
        }
        mSamplesStates = arrays
        i("getInitSampleShelfPos sampleShelfPos=$sampleShelfPos lastSampleShelfPos=$lastSampleShelfPos")
    }

    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        if (!isVerifyRunning()) {
            resetMoveSampleShelf()
            return
        }
        sampleShelfStates = reply.data.sampleShelfs
        getInitialPos()
        if (sampleShelfPos == -1) {
            i("没有样本架")
            viewModelScope.launch {
                hiltMsg.value = "样本不足，请添加"
            }
            return
        }

        moveSampleShelf(sampleShelfPos)

    }

    /**
     * 样本架复位
     */
    private fun resetMoveSampleShelf() {
        sampleShelfPos = -1
        moveSampleShelf(sampleShelfPos)
        resetState()
    }

    private fun resetState() {
        samplePos = 0
        sampleShelfPos = -1
        scanFailedCount = 0
        scanCount = 0
        scanSuccessCount = 0
    }

    /**
     * 移动样本架
     * @param pos Int
     */
    private fun moveSampleShelf(pos: Int) {
        LogToFile.c("发送 移动样本架 pos=$pos ")
        appViewModel.serialPort.moveSampleShelf(pos + 1)
    }

    private fun isVerifyRunning(): Boolean {
        return needRunning
    }


    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        if (!isVerifyRunning() && sampleShelfPos != -1) {
            resetMoveSampleShelf()
            return
        } else if (sampleShelfPos == -1) {
            //结束
            changeHilt("扫码测试结束")
            return
        }
        samplePos = 0
        moveSample()

    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {

    }

    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        if (!isVerifyRunning()) {
            resetMoveSampleShelf()
            return
        }
        viewModelScope.launch {
            scanCount++
            appViewModel.scanCodeUtil.startScan()
        }
//        if (!reply.data.type.isSample()) {
//
//        }

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


    /**
     * 移动样本
     * @param step Int
     */
    private fun moveSample(step: Int = 1) {
        LogToFile.c("发送 移动样本 step=$step samplePos=$samplePos")
        samplePos += step
        appViewModel.serialPort.moveSample(step > 0, step.absoluteValue)
    }

    fun startTest() {
        needRunning = true
        resetState()
        getState()
    }

    fun stopTest() {
        needRunning = false
        resetState()
    }


    /**
     * 获取状态
     */
    fun getState() {
        appViewModel.serialPort.getState()
    }


    private fun changeTestMsg(msg: String) {
        testMsg.value = msg
    }

    private fun changeHilt(msg: String) {
        hiltMsg.value = msg
    }

    override fun scanSuccess(str: String) {
        scanSuccessCount++
        changeScanInfo()
        needNextSample()
    }

    private fun needNextSample() {
        if (samplePos >= sampleMax) {
            //已经是最后一个样本位置了
            if (sampleShelfPos == lastSampleShelfPos) {
                //最后一排了，结束
                resetMoveSampleShelf()
            } else {
                //还有，移动到下一排
                moveSampleShelfNext()
            }
        } else {
            //还有，继续移动
            moveSample()
        }
    }

    /**
     * 移动到下一排,第一次的时候不能调用，
     * 因为在只有一排时调用，会直接显示样本不足
     */
    private fun moveSampleShelfNext() {

        val oldPos = sampleShelfPos
        for (i in sampleShelfPos + 1 until sampleShelfStates.size) {
            if (sampleShelfStates[i] == 1) {
                if (sampleShelfPos == oldPos) {
                    sampleShelfPos = i
                }
            }
        }

        moveSampleShelf(sampleShelfPos)
        i("moveSampleShelfNext sampleShelfPos=$sampleShelfPos oldPos=$oldPos")
    }

    private fun changeScanInfo() {
        changeTestMsg(
            "样本架状态:${
                sampleShelfStates.map { "$it" }.joinToString { it }
            } 总共扫码了$scanCount 次，成功$scanSuccessCount 次，失败$scanFailedCount 次"
        )
    }

    override fun scanFailed() {
        scanFailedCount++
        changeScanInfo()
        needNextSample()
    }
}

class ScanBarcodeViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanBarcodeViewModel::class.java)) {
            return ScanBarcodeViewModel(
                appViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
