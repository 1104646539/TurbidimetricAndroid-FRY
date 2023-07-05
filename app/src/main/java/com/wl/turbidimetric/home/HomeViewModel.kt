package com.wl.turbidimetric.home

import android.view.View
import androidx.lifecycle.*
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.machineArgState
import com.wl.turbidimetric.global.SystemGlobal.matchingTestState
import com.wl.turbidimetric.global.SystemGlobal.repeatabilityState
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.OnScanResult
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.kotlin.flow
import kotlinx.coroutines.*
import timber.log.Timber
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.absoluteValue


class HomeViewModel(
    private val projectRepository: ProjectRepository,
    private val testResultRepository: TestResultRepository
) : BaseViewModel(), Callback2, OnScanResult {

    init {
        listener()
    }

    /**
     * 自检
     */
    fun goGetMachineState() {
        dialogGetMachine.postValue(true)
        testState = TestState.GetMachineState
        getMachineState()
    }


    private fun listener() {
        SerialPortUtil.Instance.callback.add(this)
        ScanCodeUtil.Instance.onScanResult = this

        listenerTempState()
    }

    /**
     * 检测结果
     */
    val resultModels = arrayListOf<TestResultModel?>()

    /**
     * 四次检测的吸光度值
     */
    private var resultTest1 = arrayListOf<BigDecimal>()
    private var resultTest2 = arrayListOf<BigDecimal>()
    private var resultTest3 = arrayListOf<BigDecimal>()
    private var resultTest4 = arrayListOf<BigDecimal>()

    /**
     * 四次检测的原始值
     */
    private var resultOriginalTest1 = arrayListOf<Int>()
    private var resultOriginalTest2 = arrayListOf<Int>()
    private var resultOriginalTest3 = arrayListOf<Int>()
    private var resultOriginalTest4 = arrayListOf<Int>()

    /**
     * 吸光度
     */
    private var absorbances = arrayListOf<BigDecimal>()

    /**
     * 浓度
     */
    private var cons = arrayListOf<Int>()

    /**
     * 自检中对话框
     */
    val dialogGetMachine = MutableLiveData(false);

    /**
     * 自检失败对话框
     */
    val getMachineFailedMsg = MutableLiveData("");

    /**
     * 检测结束 比色皿不足对话框
     */
    val dialogTestFinishCuvetteDeficiency = MutableLiveData(false)

    /**
     * 检测结束提示
     */
    val dialogTestFinish = MutableLiveData(false)

    /**
     * 正常检测 样本不足对话框
     */
    val dialogTestSampleDeficiency = MutableLiveData(false)

    /**
     * 开始检测 比色皿，样本，试剂不存在
     */
    val getStateNotExistMsg = MutableLiveData("")

    /**
     * 选择项目是否可用
     */
    val selectProjectEnable = MutableLiveData(true)

    /**
     * 跳过比色皿是否可用
     */
    val skipCuvetteEnable = MutableLiveData(true)

    /**
     * 编辑编号是否可用
     */
    val editDetectionNumEnable = MutableLiveData(true)

    /**样本架状态 1有 0无 顺序是从中间往旁边
     *
     */
    private var sampleShelfStates: IntArray = IntArray(4)

    /**比色皿架状态 1有 0无 顺序是从中间往旁边
     *
     */
    private var cuvetteShelfStates: IntArray = IntArray(4)


    /**当前排所有比色皿的状态
     *
     */
    var cuvetteStates = MutableLiveData(initCuvetteStates())

    /**当前排所有比色皿的状态
     *
     */
    private var mCuvetteStates = initCuvetteStates()


    /**当前排所有样本的状态
     *
     */
    var samplesStates = MutableLiveData(initSampleStates())


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

    /**当前比色皿架位置
     *
     */
    private var cuvetteShelfPos = -1

    /**最后一排可使用的比色皿架的位置
     *
     */
    private var lastCuvetteShelfPos = -1

    /**当前比色皿位置
     *
     *
     */
    private var cuvettePos = -1

    /**当前样本位置
     *
     */
    private var samplePos = -1

    /**样本最大步数
     *
     */
    private val sampleMax = 10

    /**比色皿架是否移动完毕
     *
     */
    private var cuvetteShelfMoveFinish = false

    /**样本架是否移动完毕
     *
     */
    private var sampleShelfMoveFinish = false

    /**比色皿是否移动完毕
     *
     */
    private var cuvetteMoveFinish = false

    /**样本是否移动完毕
     *
     */
    private var sampleMoveFinish = false

    /**取样是否完成
     *
     */
    private var samplingFinish = false

    /**加样是否完成
     *
     */
    private var dripSampleFinish = true

    /**加试剂是否完成
     *
     */
    private var dripReagentFinish = true

    /**搅拌是否完成
     *
     */
    private var stirFinish = true

//    /**
//     * 输入的跳过的比色皿数量
//     */
//    var skipCuvetteNum = 0;

    /**
     *比色皿起始的位置，只限于第一排比色皿用来跳过前面的几个已经使用过的比色皿
     */
    var cuvetteStartPos = 0;


    /**继续检测后获取状态 判断是否是来自比色皿不足
     *
     */
    private var continueTestCuvetteState = false

    /**继续检测后获取状态 判断是否是来自样本不足
     *
     */
    private var continueTestSampleState = false

    /**比色皿不足
     *
     */
    private var cuvetteDeficiency = false

    /**样本不足
     *
     */
    private var sampleDeficiency = false

    /**正在检测
     *
     */
    private var testing = false

    /**单次检测是否结束
     *
     */
    private var testFinish = true

    /**扫码是否完成
     *
     */
    private var scanFinish = false

    /**刺破是否完成
     *
     */
    private var piercedFinish = false

    /**取样针清洗是否完成
     *
     */
    private var samplingProbeCleaningFinish = true

    /**搅拌针清洗是否完成
     *
     */
    private var stirProbeCleaningFinish = true

    /**取样针清洗完成后，需要恢复取样？
     *
     */
    private var samplingProbeCleaningRecoverSampling = false

    /**
     * 搅拌针清洗完成后，需要恢复搅拌？
     */
    private var stirProbeCleaningRecoverStir = false

    /**
     * 是否是点击了开始检测
     * 为true时，在下一次获取状态时，会自动进行下一步骤
     */
    private var clickStart = false

    /**
     * 取试剂是否完成
     */
    private var takeReagentFinish = false


    val testMsg = MutableLiveData("");
    val toastMsg = MutableLiveData("");

    val projectDatas = projectRepository.allDatas.flow()

    /**
     * 选择的检测项目
     */
    var selectProject: ProjectModel? = null

    /**
     * 输入的起始编号，在第一次开始的时候才赋值 ,使用过就会为空
     */
    var detectionNumInput = ""


    /**
     * 每排之间的检测间隔
     */
    var testShelfInterval: Long = 1000 * 0

    /**
     * 每个比色皿之间的检测间隔
     */
    var testPosInterval: Long = 1000 * 10

    /**
     * 扫码结果
     */
    var scanResults = arrayListOf<String?>()


    /**
     * r1状态
     */
    val r1State = MutableLiveData(false)

    /**
     * r2状态
     */
    val r2State = MutableLiveData(false)

    /**
     * 清洗液状态
     */
    val cleanoutFluidState = MutableLiveData(false)

    /**
     * r1状态
     */
    val r2VolumeState = MutableLiveData(0)

    /**
     * 反应槽温度
     */
    val reactionTemp = MutableLiveData(0.0)

    /**
     * r1温度
     */
    val r1Temp = MutableLiveData(0.0)

    /**
     * 手动模式下，需要取样的样本数量
     */
    var needSamplingNum = 0

    /**
     * 手动模式下，已经取样的样本数量
     */
    var samplingNum = 0

    /**
     * 是否是检测完一排比色皿后，准备检测下一排时却因为清洗液|R1|R2状态不符合时获取的状态
     */
    var continueTestGetState = false

    var r1Reagent: Boolean = false
        set(value) {
            field = value
            r1State.postValue(value)
        }
    var r2Reagent: Boolean = false
        set(value) {
            field = value
            r2State.postValue(value)
        }
    var r2Volume: Int = 0
        set(value) {
            field = value
            r2VolumeState.postValue(value)
        }
    var cleanoutFluid: Boolean = false
        set(value) {
            field = value
            cleanoutFluidState.postValue(value)
        }

    /**
     * 测试用的 start
     */
    //检测的值
    private val testValues1 = doubleArrayOf(
        0.5441, 0.5555, 0.2525, 0.22492, 0.46042, 0.24542, 0.66332, 0.74412, 0.0122, 0.12142
    )
    private val testValues2 = doubleArrayOf(0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3)
    private val testValues3 = doubleArrayOf(0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3)
    private val testValues4 = doubleArrayOf(0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4)
    private val testOriginalValues1 =
        intArrayOf(65532, 65532, 65532, 65532, 65532, 65532, 65532, 65532, 65532, 65532)
    private val testOriginalValues2 =
        intArrayOf(65520, 65520, 65520, 65520, 65520, 65520, 65520, 65520, 65520, 65520)
    private val testOriginalValues3 =
        intArrayOf(60000, 60000, 60000, 60000, 60000, 60000, 60000, 60000, 60000, 60000)
    private val testOriginalValues4 =
        intArrayOf(56000, 56000, 56000, 56000, 56000, 56000, 56000, 56000, 56000, 56000)

    //测试用，扫码是否成功
    val tempSampleState = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
//  val tempSampleState = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 1)

    //测试用，样本是否存在
    val sampleExists = mutableListOf(true, true, true, true, true, true, true, true, true, true)

    //测试用 每排之间的检测间隔
    val testS: Long = 100;

    //测试用 每个比色皿之间的检测间隔
    val testP: Long = 100;

    /**
     * 一直获取温度状态
     */
    private fun listenerTempState() {
        viewModelScope.launch {
            timer("", true, Date(), 30000) {
                SerialPortUtil.Instance.getTemp()
            }
        }
    }


    /**
     * 测试用的 end
     */
    fun clickStart() {
//        if (DoorAllOpen()) {
//            toast("舱门未关")
//            return
//        }
        if (!machineStateNormal()) {
            toastMsg.postValue("请重新自检或重启仪器！")
            return
        }
        if (testState != TestState.None) {
            toastMsg.postValue("正在检测，请勿操作！")
            return
        }
        if (matchingTestState != MatchingArgState.None) {
            toastMsg.postValue("正在拟合质控，请勿操作！")
            return
        }
        if (repeatabilityState != RepeatabilityState.None) {
            toastMsg.postValue("正在进行重复性检测，请勿操作！")
            return
        }
        clickStart = true
        initState()
        getState()
    }


    private fun initState() {
        testMsg.value = ""
        testState = TestState.DripSample
        cuvetteShelfPos = -1
        sampleShelfPos = -1
        samplingNum = 0
        if (detectionNumInput.isNotEmpty()) {//如果更改了起始编号就使用输入的
            LocalData.DetectionNum = detectionNumInput
            detectionNumInput = ""
        }


        mSamplesStates = initSampleStates()
        mCuvetteStates = initCuvetteStates()

        Timber.d("跳过 $cuvetteStartPos 个比色皿")

        if (SystemGlobal.isCodeDebug) {
            testShelfInterval = testS;
            testPosInterval = testP;
        }
        resultTest1.clear()
        resultTest2.clear()
        resultTest3.clear()
        resultTest4.clear()
        resultOriginalTest1.clear()
        resultOriginalTest2.clear()
        resultOriginalTest3.clear()
        resultOriginalTest4.clear()
        cons.clear()
    }

    /**
     * 重置比色皿状态
     * @return MutableList<CuvetteState>
     */
    private fun initCuvetteStates(): Array<Array<CuvetteItem>?> {
        val arrays = mutableListOf<Array<CuvetteItem>?>()
        for (j in 0 until 4) {
            val array = null
//            for (i in 0 until 10) {
//                array.add(SampleState.None)
//            }
            arrays.add(null)
        }

        return arrays.toTypedArray()
    }

    /**
     * 重置样本状态
     * @return MutableList<CuvetteState>
     */
    private fun initSampleStates(): Array<Array<SampleItem>?> {
        val arrays = mutableListOf<Array<SampleItem>?>()
        for (j in 0 until 4) {
            val array = null
//            for (i in 0 until 10) {
//                array.add(SampleState.None)
//            }
            arrays.add(null)
        }

        return arrays.toTypedArray()
    }


    /**
     * 接收到取试剂
     * @param reply ReplyModel<TakeReagentModel>
     */
    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        r1Reagent = reply.data.r1Reagent
        r2Volume = reply.data.r2Volume
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取试剂 reply=$reply")
        takeReagentFinish = true
        goDripReagent()
    }


    /**
     * 去加试剂
     */
    private fun goDripReagent() {
        Timber.d("goDripReagent cuvettePos=$cuvettePos takeReagentFinish=$takeReagentFinish cuvetteMoveFinish=$cuvetteMoveFinish")
        if (cuvettePos < 10 && cuvetteNeedDripReagent(cuvettePos) && takeReagentFinish && cuvetteMoveFinish) {
            dripReagent()
        }
    }

    /**
     * 接收到取样针清洗
     * @param reply ReplyModel<SamplingProbeCleaningModel>
     */
    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {
        cleanoutFluid = reply.data.cleanoutFluid
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取样针清洗 reply=$reply samplingProbeCleaningRecoverSampling=$samplingProbeCleaningRecoverSampling")
        samplingProbeCleaningFinish = true

        //恢复取样
        /**
         * @sample readDataMoveSampleModel
         */
        if (samplingProbeCleaningRecoverSampling) {
            samplingProbeCleaningRecoverSampling = false
            sampling()
        }
    }

    /**
     * 接收到搅拌针清洗
     * @param reply ReplyModel<StirProbeCleaningModel>
     */
    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        cleanoutFluidState.postValue(reply.data.cleanoutFluid)
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 搅拌针清洗 reply=$reply stirProbeCleaningRecoverStir=$stirProbeCleaningRecoverStir")
        stirProbeCleaningFinish = true
        nextDripReagent()

        if (stirProbeCleaningRecoverStir) {
            stirProbeCleaningRecoverStir = false
            goStir()
        }
    }


    /**
     * 接收到 刺破
     * @param reply ReplyModel<PiercedModel>
     */
    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 刺破 reply=$reply samplePos=$samplePos")
        piercedFinish = true
        updateSampleState(samplePos, SampleState.Pierced)
        //当不需要取样时，直接下一步
        if (!sampleNeedSampling(samplePos - 1)) {
            samplingFinish = true
            dripSampleFinish = true
        }
        nextStepDripReagent()
    }


    /**
     * 去加样
     */
    private fun goDripSample() {
        Timber.d("sampleMoveFinish=$sampleMoveFinish samplingFinish=$samplingFinish cuvetteMoveFinish=$cuvetteMoveFinish")
        if (samplePos > 0 && sampleMoveFinish && samplingFinish && cuvetteMoveFinish && sampleNeedDripSampling(
                samplePos - 1
            )
        ) {
            dripSample()

        }
    }

    /**
     * 接收到 获取版本号
     * @param reply ReplyModel<GetVersionModel>
     */
    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 获取版本号 reply=$reply")

    }

    /**
     * 接收到 获取设置温度
     * @param reply ReplyModel<TempModel>
     */
    override fun readDataTempModel(reply: ReplyModel<TempModel>) {
        Timber.d("接收到 获取设置温度 reply=$reply")

        reactionTemp.postValue(reply.data.reactionTemp / 10.0)
        r1Temp.postValue(reply.data.r1Temp / 10.0)
    }

    /**
     * 报错
     * @param cmd UByte
     * @param state UByte
     */
    override fun readDataStateFailed(cmd: UByte, state: UByte) {
        if (!runningTest()) return
        if (cmd == SerialGlobal.CMD_TakeReagent) {
            Timber.d("报错了，cmd=$cmd state=$state 是取试剂的暂时不管")
            readDataTakeReagentModel(
                ReplyModel(
                    SerialGlobal.CMD_TakeReagent, 0, TakeReagentModel()
                )
            )
            return
        }
        machineArgState = MachineState.RunningError
        Timber.d("报错了，cmd=$cmd state=$state")
        testMsg.postValue("报错了，cmd=$cmd state=$state")
    }

    /**
     * 接收到 样本门状态
     * @param reply ReplyModel<SampleDoorModel>
     */
    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {
        Timber.d("接收到 样本门状态 reply=$reply")
//        sampleDoorIsOpen.postValue(reply.data.isOpen)

//        sampleDoorLocked = reply.data.isOpen
//        sampleDoorLockedLD.postValue(reply.data.isOpen)
//        if (testState == TestState.TestFinish && cuvetteDoorLocked && sampleDoorLocked) {
//            showFinishDialog()
//        }
        if (!runningTest()) return
        if (!machineStateNormal()) return

    }

    /**
     * 接收到 比色皿门状态
     * @param reply ReplyModel<CuvetteDoorModel>
     */
    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
        Timber.d("接收到 比色皿门状态 reply=$reply")
//        cuvetteDoorIsOpen.postValue(reply.data.isOpen)
//        cuvetteDoorLocked = reply.data.isOpen
//        cuvetteDoorLockedLD.postValue(reply.data.isOpen)
//        if (testState == TestState.TestFinish && cuvetteDoorLocked && sampleDoorLocked) {
//            showFinishDialog()
//        }
        if (!runningTest()) return
        if (!machineStateNormal()) return
    }

    /**
     * 接收到移动样本
     */
    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动样本 reply=$reply cuvettePos=$cuvettePos lastCuvetteShelfPos=$lastCuvetteShelfPos cuvetteShelfPos=$cuvetteShelfPos samplePos=$samplePos samplingProbeCleaningFinish=$samplingProbeCleaningFinish")
        sampleMoveFinish = true
        samplingFinish = false
        dripSampleFinish = false
        scanFinish = false

        var exist = reply.data.exist

        //最后一个位置不需要扫码，直接取样
        if (samplePos < sampleMax) {
//            if ((SystemGlobal.isCodeDebug && !sampleExists[samplePos]) || (isAuto() && LocalData.SampleExist && !exist)) {
            //如果是自动模式并且已开启样本传感器,并且是未识别到样本，才是没有样本
            if ((isAuto() && LocalData.SampleExist && !exist)) {
                //没有样本，移动到下一个位置
                Timber.d("没有样本,移动到下一个位置")
                scanFinish = true
                scanResults.add(null)
                Timber.d("scanResults=$scanResults")
                nextStepDripReagent()
            } else {
                //有样本
                Timber.d("有样本")
                updateSampleState(samplePos, SampleState.Exist)
                startScan(samplePos)
            }
        }
        //判断是否需要取样。取样位和扫码位差一个位置
        if (sampleNeedSampling(samplePos - 1)) {
            //注：如果上次取样针清洗未结束，就等待清洗结束后再加样,否则直接加样
            if (samplingProbeCleaningFinish) {
                sampling()
            } else {
                samplingProbeCleaningRecoverSampling = true
            }
        } else if (samplePos == sampleMax && sampleMoveFinish) {
            //加入sampleMoveFinish的判断是为了防止在上面的nextStepDripReagent()之前samplePos=sampleMax-1，而移动了样本后，导致samplePos == sampleMax从而发生同时移动样本和比色皿的问题
            //最后一个样本，并且不需要取样时，下一步
            nextStepDripReagent()
        }
    }

    /**
     * 判断当前位置是否需要取样，只有扫码成功了的样本才需要取样
     * @param samplePos Int
     * @return Boolean
     */
    private fun sampleNeedSampling(samplePos: Int): Boolean {
        if (samplePos < 0) return false
        return mSamplesStates[sampleShelfPos]!![samplePos].state == SampleState.Pierced
    }

    /**
     * 判断当前位置是否需要加样，只有取样完成并且比色皿已经移动到位了才需要加样
     * @param samplePos Int
     * @return Boolean
     */
    private fun sampleNeedDripSampling(samplePos: Int): Boolean {
        if (samplePos < 0) return false
        return mSamplesStates[sampleShelfPos]!![samplePos].state == SampleState.Sampling
    }

    /**
     * 更新样本状态
     * @param samplePos Int
     * @param state SampleState
     */
    private fun updateSampleState(
        samplePos: Int,
        state: SampleState? = null,
        testResult: TestResultModel? = null,
        cuvettePos: String? = null
    ) {
        if (samplePos < 0) {
            return
        }
        state?.let {
            mSamplesStates[sampleShelfPos]!![samplePos].state = state
        }
        testResult?.let {
            mSamplesStates[sampleShelfPos]!![samplePos].testResult = testResult
        }
        samplePos?.let {
            mSamplesStates[sampleShelfPos]!![samplePos].cuvetteID = cuvettePos
        }
        samplesStates.postValue(mSamplesStates)
        Timber.d("updateSampleState samplePos=${samplePos} sampleShelfPos=$sampleShelfPos")
        Timber.d("updateSampleState samplesState=${mSamplesStates.print()}")
    }

    /**
     * 更新比色皿状态
     * @param cuvettePos Int
     * @param state SampleState
     */
    private fun updateCuvetteState(
        cuvettePos: Int,
        state: CuvetteState,
        testResult: TestResultModel? = null,
        samplePos: String? = null
    ) {
        if (cuvettePos < 0) {
            return
        }
        mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state = state
        testResult?.let { mCuvetteStates[cuvetteShelfPos]!![cuvettePos].testResult = testResult }
        samplePos?.let { mCuvetteStates[cuvetteShelfPos]!![cuvettePos].sampleID = samplePos }
        cuvetteStates.postValue(mCuvetteStates)
        Timber.d("updateCuvetteState  cuvetteShelfPos=$cuvetteShelfPos")
        Timber.d("updateCuvetteState cuvetteStates=${mCuvetteStates.print()}")
    }

    /**
     * 开始扫码
     */
    private fun startScan(samplePos: Int) {
        scanFinish = false
        piercedFinish = false

        if (!SystemGlobal.isCodeDebug && (isAuto() && LocalData.ScanCode)) {
            viewModelScope.launch {
                ScanCodeUtil.Instance.startScan()
            }
        } else {
            //如果是测试用的。。。或者是自动模式，但是未开启扫码、或者是手动模式时。走扫码成功路线
            if ((SystemGlobal.isCodeDebug && tempSampleState[samplePos] == 1) || (isAuto() && !LocalData.ScanCode) || (!isAuto())) {
                scanSuccess("")
            } else {
                scanFailed()
            }
        }
    }

    /**
     * 扫码成功
     */
    override fun scanSuccess(str: String) {
        Timber.d("扫码成功 str=$str")
        updateSampleState(samplePos, SampleState.ScanSuccess)
        scanFinish = true
        pierced()
        scanResults.add(str ?: "")
        Timber.d("scanResults=$scanResults")
    }

    /**
     * 扫码失败
     */
    override fun scanFailed() {
        Timber.d("扫码失败")
        updateSampleState(samplePos, SampleState.ScanFailed)
        scanFinish = true
        piercedFinish = true
        //当不需要取样时，直接下一步
        if (!sampleNeedSampling(samplePos - 1)) {
            samplingFinish = true
            dripSampleFinish = true
        }
        nextStepDripReagent()
        scanResults.add(null)
        Timber.d("scanResults=$scanResults")
    }


    /**
     * 新建检测记录
     * @param str String?
     */
    private fun createResultModel(str: String?): TestResultModel {
        val resultModel = TestResultModel(
            sampleQRCode = str ?: "",
            createTime = Date().toLongString(),
            detectionNum = LocalData.getDetectionNumInc()
        )

        resultModels.add(resultModel)
        return resultModel
    }

    /**
     * 刺破
     */
    private fun pierced() {
        Timber.d("发送 刺破")
        piercedFinish = false
        SerialPortUtil.Instance.pierced()
    }


    /**
     * 接收到检测完成
     */
    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 检测完成 reply=$reply cuvettePos=$cuvettePos testState=$testState")
        testFinish = true

        calcTestResult(reply.data.value)
    }

    /**
     * 计算结果
     */
    private fun calcTestResult(value: Int) {
        when (testState) {
            TestState.DripReagent -> {
                updateCuvetteState(cuvettePos - 3, CuvetteState.Test1)
                updateTestResultModel(value, cuvettePos - 3, CuvetteState.Test1)
                nextDripReagent()
            }
            TestState.Test2 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test2)
                updateTestResultModel(value, cuvettePos, CuvetteState.Test2)
                if (lastNeed(cuvettePos, CuvetteState.Test1)) {
                    //检测结束，下一个步骤，检测第三次
                    viewModelScope.launch {
                        delay(testShelfInterval)
                        stepTest(TestState.Test3)
                    }
                } else {
                    //继续检测
                    viewModelScope.launch {
                        delay(testPosInterval)
                        moveCuvetteTest()
                    }
                }
            }
            TestState.Test3 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test3)
                updateTestResultModel(value, cuvettePos, CuvetteState.Test3)
                if (lastNeed(cuvettePos, CuvetteState.Test2)) {
                    //检测结束，下一个步骤，检测第四次
                    viewModelScope.launch {
                        delay(testShelfInterval)
                        stepTest(TestState.Test4)
                    }
                } else {
                    //继续检测
                    viewModelScope.launch {
                        delay(testPosInterval)
                        moveCuvetteTest()
                    }
                }
            }
            TestState.Test4 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test4)
                updateTestResultModel(value, cuvettePos, CuvetteState.Test4)
                if (lastNeed(cuvettePos, CuvetteState.Test3)) {
                    //检测结束，下一个步骤，计算值
                    calcResult()
                } else {
                    viewModelScope.launch {
                        delay(testPosInterval)
                        moveCuvetteTest()
                    }
                }
            }

            else -> {}
        }

    }

    /**
     * 更新检测结果
     * @param pos Int
     * @param state CuvetteState
     */
    private fun updateTestResultModel(value: Int, index: Int, state: CuvetteState) {
        var pos = index
        Timber.d("updateTestResultModel pos=$pos resultModels=$resultModels")
        if (cuvetteStartPos > 0 && isFirstCuvetteShelf()) {
            pos -= cuvetteStartPos
        }
        if (pos < 0 || pos >= resultModels.size) {
            return
        }
        when (state) {
            CuvetteState.Test1 -> {
                if (SystemGlobal.isCodeDebug) {
                    resultTest1.add(testValues1[pos].toBigDecimal())
                    resultOriginalTest1.add(testOriginalValues1[pos])
                } else {
                    resultOriginalTest1.add(value)
                    resultTest1.add(calcAbsorbance(value.toBigDecimal()))
                }
                resultModels[pos]?.testValue1 = resultTest1[pos]
                resultModels[pos]?.testOriginalValue1 = resultOriginalTest1[pos]

                mCuvetteStates[cuvetteShelfPos]?.get(pos)?.testResult = resultModels[pos]
                cuvetteStates.postValue(mCuvetteStates)
            }
            CuvetteState.Test2 -> {
                if (SystemGlobal.isCodeDebug) {
                    resultTest2.add(testValues2[pos].toBigDecimal())
                    resultOriginalTest2.add(testOriginalValues2[pos])
                } else {
                    resultOriginalTest2.add(value)
                    resultTest2.add(calcAbsorbance(value.toBigDecimal()))
                }
                resultModels[pos]?.testValue2 = resultTest2[pos]
                resultModels[pos]?.testOriginalValue2 = resultOriginalTest2[pos]
            }
            CuvetteState.Test3 -> {
                if (SystemGlobal.isCodeDebug) {
                    resultTest3.add(testValues3[pos].toBigDecimal())
                    resultOriginalTest3.add(testOriginalValues3[pos])
                } else {
                    resultOriginalTest3.add(value)
                    resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                }
                resultModels[pos]?.testValue3 = resultTest3[pos]
                resultModels[pos]?.testOriginalValue3 = resultOriginalTest3[pos]
            }
            CuvetteState.Test4 -> {
                if (SystemGlobal.isCodeDebug) {
                    resultTest4.add(testValues4[pos].toBigDecimal())
                    resultOriginalTest4.add(testOriginalValues4[pos])
                } else {
                    resultOriginalTest4.add(value)
                    resultTest4.add(calcAbsorbance(value.toBigDecimal()))
                }
                resultModels[pos]?.testValue4 = resultTest4[pos]
                resultModels[pos]?.testOriginalValue4 = resultOriginalTest4[pos]
                resultModels[pos]?.testTime = Date().toLongString()

                //计算单个结果浓度
                val abs = calcAbsorbanceDifference(resultTest1[pos], resultTest4[pos])
                absorbances.add(abs)
                selectProject?.let { project ->
                    var con = calcCon(abs, project)
                    con = if (con.toDouble() < 0.0) 0 else con
                    cons.add(con)
                    resultModels[pos]?.absorbances = abs
                    resultModels[pos]?.concentration = con
                }
            }
            else -> {}
        }
        resultModels[pos]?.let {
            testResultRepository.updateTestResult(it)
        }
        Timber.d("updateTestResultModel resultModels=$resultModels")
    }


    /**
     * 计算结果
     */
    private fun calcResult() {
        testMsg.postValue(
            testMsg.value?.plus("这排比色皿检测结束 比色皿位置=$cuvetteShelfPos 样本位置=$sampleShelfPos \n 第一次:$resultTest1 \n 第二次:$resultTest2 \n 第三次:$resultTest3 \n 第四次:$resultTest4 \n  吸光度:$absorbances \n 浓度=$cons \n选择的四参数为${selectProject ?: "未选择"}\n\n")
        )

        resultModels.forEach {
            Timber.d("resultModel=$it")
        }
        Timber.d("这排比色皿检测结束")

        resultTest1.clear()
        resultTest2.clear()
        resultTest3.clear()
        resultTest4.clear()

        resultOriginalTest1.clear()
        resultOriginalTest2.clear()
        resultOriginalTest3.clear()
        resultOriginalTest4.clear()

        cons.clear()
        absorbances.clear()
        cuvetteStartPos = 0
        resultModels.clear()


        continueTestNextCuvette()
    }

//    /**
//     * 检测下一排比色皿前的检测状态
//     */
//    private fun checkTestStateInContinueTest(
//        r2Volume: Int = r2VolumeState.value ?: 0,
//        r1Reagent: Boolean = r1State.value ?: false,
//        r2Reagent: Boolean = r2State.value ?: false,
//        cleanoutFluid: Boolean = cleanoutFluidState.value ?: false,
//        accord: () -> Unit
//    ) {
//        checkTestState(
//            r2Volume,
//            r1Reagent,
//            r2Reagent,
//            cleanoutFluid,
//            accord = accord,
//            discrepancy = { str ->
//                continueTestGetState = true
//                getStateNotExistMsg.postValue(str)
//            })
//    }

    /**
     * 在开始检测下一排比色皿之前，检查试剂和清洗液的状态
     * @param r2Volume Int
     * @param r1Reagent Boolean
     * @param r2Reagent Boolean
     * @param cleanoutFluid Boolean
     * @param accord Function0<Unit> 符合后执行的命令
     * @param discrepancy Function1<String, Unit> 不符合后执行的命令
     */
    private fun checkTestState(
        accord: () -> Unit, discrepancy: (String) -> Unit
    ) {
        if (!r1Reagent) {
            Timber.d("没有R1试剂")
            discrepancy.invoke("R1试剂不足，请添加")
            return
        }
        if (!r2Reagent) {
            Timber.d("没有R2试剂")
            discrepancy.invoke("R2试剂不足，请添加")
            return
        }
        if (r2Volume == 0) {
            Timber.d("没有R2试剂")
            discrepancy.invoke("R2试剂不足，请添加")
            return
        }
        if (!cleanoutFluid) {
            Timber.d("没有清洗液")
            discrepancy.invoke("清洗液不足，请添加")
            return
        }
        accord()
    }

    /**
     * 检测完这排比色皿，继续检测下一排
     */
    private fun continueTestNextCuvette() {
        //重置这排比色皿架的位置和检测状态
        testState = TestState.DripSample
        cuvettePos = -1

//        if ((isAuto() && lastSamplePos(samplePos)) || !isAuto()) {//这排最后一个样本
//            if (!isAuto() && manualModelSamplingFinish()) {
//                Timber.d("手动模式，样本加样完成！")
//                stepDripReagent()
//            }

        if ((isAuto() && lastSamplePos(samplePos)) || !isAuto()) {//这排最后一个样本
            if (!isAuto() && manualModelSamplingFinish()) {
                //手动模式，检测完了
                testFinishAction();
            } else if (lastSampleShelf(sampleShelfPos)) {//最后一排
                //结束检测，样本已经取完样了
                testFinishAction();
            } else {
                if (lastCuvetteShelf(cuvetteShelfPos)) {//最后一排比色皿
                    //提示，比色皿不足了
                    dialogTestFinishCuvetteDeficiency.postValue(true)
                } else {
                    checkTestState(accord = {
                        //还有比色皿。继续移动比色皿，检测
                        moveCuvetteShelfNext()
                        //移动样本架
                        moveSampleShelfNext()
                    }, discrepancy = { str ->
                        continueTestGetState = true
                        getStateNotExistMsg.postValue(str)
                    })
//                    //还有比色皿。继续移动比色皿，检测
//                    moveCuvetteShelfNext()
//                    //移动样本架
//                    moveSampleShelfNext()
                }
            }
        } else {
            if (lastCuvetteShelf(cuvetteShelfPos)) {//最后一排比色皿
                //提示，比色皿不足了
                dialogTestFinishCuvetteDeficiency.postValue(true)
            } else {
                //还有比色皿。继续移动比色皿，检测
                checkTestState(accord = {
                    moveSample()
                    moveCuvetteShelfNext()
                }, discrepancy = { str ->
                    continueTestGetState = true
                    getStateNotExistMsg.postValue(str)
                })
//                moveSample()
//                moveCuvetteShelfNext()
            }
        }
    }

    /**
     * 判断是否进行下一次加试剂
     */
    private fun nextDripReagent() {
        if (testState != TestState.DripReagent) return;
        Timber.d("nextDripReagent cuvettePos=$cuvettePos dripReagentFinish=$dripReagentFinish testFinish=$testFinish stirFinish=$stirFinish stirProbeCleaningFinish=$stirProbeCleaningFinish takeReagentFinish=$takeReagentFinish cuvetteMoveFinish=$cuvetteMoveFinish");
        if (cuvettePos < 13) {
            //当取试剂完成，检测完成，搅拌完成，加试剂完成，移动比色皿完成时，
            //去取试剂，移动比色皿
            if (dripReagentFinish && testFinish && stirFinish && takeReagentFinish && cuvetteMoveFinish && stirProbeCleaningFinish) {
//                //是否是最后一个需要加试剂的比色皿了
                if (lastNeedDripReagent(cuvettePos)) {
                    Timber.d("加试剂完成")
                } else {
                    //取试剂，移动比色皿
                    takeReagent()
                }
                if (cuvettePos >= 3 && lastNeedTest1(cuvettePos - 3)) {
                    Timber.d("重新计算间隔时间  之前 testShelfInterval=$testShelfInterval")
                    if (!SystemGlobal.isCodeDebug) {
                        testShelfInterval =
                            ((10 - (cuvettePos - cuvetteStartPos - 2)) * 10 * 1000).toLong()
                    }
                    Timber.d("重新计算间隔时间 之后 testShelfInterval=$testShelfInterval cuvettePos=$cuvettePos cuvetteStartPos=$cuvetteStartPos")
                    Timber.d("已经检测最后一个了,进行下一个步骤，检测第二次 testShelfInterval=$testShelfInterval")
                    viewModelScope.launch {
                        delay(testShelfInterval)
                        stepTest(TestState.Test2)
                    }
                    return
                } else {
                    moveCuvetteDripReagent()
                }
            }
        } else {
            Timber.d("cuvettePos >= 13 $cuvettePos")
        }
    }

    /**
     *
     * 从其他步骤切换到检测步骤
     * 除了 TestState.Test1
     * @param state TestState 要切换到的状态
     */
    private fun stepTest(state: TestState) {
        Timber.d(" ———————— stepTest state=$state  cuvettePos=$cuvettePos————————————————————————————————————————————————————————————————————————————————————————————————=")
        testState = state
        //获取跳过的步数
        val needMoveStep = getFirstCuvetteStartPos();
        //切换到TestState.Test2时，当前下位机的比色皿位置cuvettePos == 0，则不需要计算需要回退多少格，直接往前移动
        if (state == TestState.Test2) {
            Timber.d("stepTest cuvettePos=$cuvettePos needMoveStep=$needMoveStep")
            if (needMoveStep > -1) {//需要跳过
                cuvettePos = -1
                moveCuvetteTest(needMoveStep + 2)
            } else {//不需要跳过
                cuvettePos = getNextStepCuvetteStartPos()
                Timber.d("stepTest cuvettePos=$cuvettePos needMoveStep=$needMoveStep")
                moveCuvetteTest()
            }
        } else {
            //当切换状态时，当前下位机的比色皿位置 cuvettePos != 0，则需要计算需要回退多少格才回到可使用的第一个比色皿的位置
            //直接移动到可使用的第一个比色皿的位置
            //如果 nextStartPos > -1 说明这一排比色皿有跳过的 ，那么就直接移动到可使用的第一个比色皿的位置
            Timber.d("stepTest cuvettePos=$cuvettePos needMoveStep=$needMoveStep moveStep=${-(mCuvetteStates[cuvetteShelfPos]!!.size - needMoveStep)} $cuvetteStartPos")
            if (needMoveStep > -1) {
                if (isFirstCuvetteShelf()) {
                    if (cuvetteStartPos > 0) {
                        repeat(cuvetteStartPos) {
                            updateCuvetteState(it, CuvetteState.Skip, null, null)
                        }
                    }
                }
                moveCuvetteTest(-(mCuvetteStates[cuvetteShelfPos]!!.size - needMoveStep - 2))
            } else {
                val nextStartPos = getNextStepCuvetteStartPos()
                val moveStep =
                    if (nextStartPos > -1) -(mCuvetteStates[cuvetteShelfPos]!!.size - (nextStartPos + 2)) else -cuvettePos
                moveCuvetteTest(moveStep)
            }
        }
    }

    /**
     * 获取下一个步骤时，起始的比色皿的位置(只应用在第一排存在的比色皿时，才有跳过的)
     * 当需要跳过时，起始的位置不是-1
     * @see cuvetteStartPos
     * @return Int
     */
    private fun getNextStepCuvetteStartPos(): Int {
        return if (isFirstCuvetteShelf() && cuvetteStartPos > 0) {
            cuvetteStartPos - 1
        } else {
            -1
        }
    }

    /**
     * 是否是最后一个需要检测的比色皿了
     */
    private fun lastNeedTest1(pos: Int): Boolean {
        return lastNeed(pos, CuvetteState.Stir) && lastNeed(
            pos, CuvetteState.DripSample
        ) && lastNeed(pos, CuvetteState.DripReagent)
    }

    /**
     * 是否是最后一个需要加试剂的比色皿了
     */
    private fun lastNeedDripReagent(pos: Int): Boolean {
        return lastNeed(pos, CuvetteState.DripSample)
    }

    /**
     * 是否是最后一个需要**的比色皿
     * @param cuvettePos Int
     * @param state CuvetteState
     * @return Boolean
     */
    private fun lastNeed(pos: Int, state: CuvetteState): Boolean {
        if (pos > mCuvetteStates[cuvetteShelfPos]!!.size) {
            return true;
        }
        for (i in pos + 1 until mCuvetteStates[cuvetteShelfPos]!!.size) {
            if (mCuvetteStates[cuvetteShelfPos]!![i].state == state) {
                return false
            }
        }
        return true
    }

    /**
     * 接收到搅拌
     */
    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 搅拌 reply=$reply cuvettePos=$cuvettePos")
        stirFinish = true
        updateCuvetteState(cuvettePos - 2, CuvetteState.Stir)
        stirProbeCleaning()
    }


    /**
     * 接收到加试剂
     */
    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 加试剂 reply=$reply cuvettePos=$cuvettePos")
        dripReagentFinish = true
        updateCuvetteState(cuvettePos, CuvetteState.DripReagent)
        nextDripReagent()
    }

    /**
     * 判断当前位置的比色皿是否需要搅拌
     */
    private fun cuvetteNeedStir(cuvettePos: Int): Boolean {
        if (cuvettePos >= mCuvetteStates[cuvetteShelfPos]!!.size) return false
        return mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state == CuvetteState.DripReagent
    }

    /**
     * 判断当前位置的比色皿是否需要检测第一次
     */
    private fun cuvetteNeedTest1(cuvettePos: Int): Boolean {
        if (cuvettePos >= mCuvetteStates[cuvetteShelfPos]!!.size) return false
        return mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state == CuvetteState.Stir
    }

    /**
     * 判断当前位置的比色皿是否需要加试剂
     */
    private fun cuvetteNeedDripReagent(cuvettePos: Int): Boolean {
        if (cuvettePos >= mCuvetteStates[cuvetteShelfPos]!!.size) return false
        return mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state == CuvetteState.DripSample
    }


    /**
     * 接收到加样
     */
    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 加样 reply=$reply cuvettePos=$cuvettePos samplePos=$samplePos")

        dripSampleFinish = true
        val result = createResultModel(scanResults[samplePos - 1])
        updateCuvetteState(
            cuvettePos, CuvetteState.DripSample, result, "${sampleShelfPos + 1}- $samplePos"
        )
        updateSampleState(
            samplePos - 1, null, result, "${cuvetteShelfPos + 1}- ${cuvettePos + 1}"
        )
        samplingProbeCleaning()

        nextStepDripReagent()
    }

    /**
     * 判断是否是这排最后一个比色皿了
     * @param pos Int
     * @return Boolean
     */
    private fun lastCuvettePos(pos: Int): Boolean {
        return mCuvetteStates[cuvetteShelfPos]!!.lastIndex == pos
    }

    /**
     * 判断是否是最后一排比色皿了
     * @param pos Int
     * @return Boolean
     */
    private fun lastCuvetteShelf(pos: Int): Boolean {
        return lastCuvetteShelfPos == pos
    }

    /**
     * 判断是否是这排最后一个样本了
     * @param pos Int
     * @return Boolean
     */
    private fun lastSamplePos(pos: Int): Boolean {
        return sampleMax == pos
    }

    /**
     * 判断是否是最后一排样本了
     * @param pos Int
     * @return Boolean
     */
    private fun lastSampleShelf(pos: Int): Boolean {
        return lastSampleShelfPos == pos
    }

    /**
     * 判断是否需要进行加试剂步骤了。
     * 如果已经加完一排比色皿或者样本已经完全加完样了，就去进行加试剂步骤，
     * 否则则去下一个样本和比色皿处取样加样
     */

    private fun nextStepDripReagent() {

        Timber.d("piercedFinish=$piercedFinish scanFinish=$scanFinish samplingFinish=$samplingFinish dripSampleFinish=$dripSampleFinish cuvettePos=$cuvettePos samplePos=$samplePos")
        //(刺破结果 && (扫码结束 || 不需要扫码) && (需要取样 && 取样结束 && 加样结束) || 不需要加样)
        if ((piercedFinish && (scanFinish || lastSamplePos(samplePos)) && (sampleNeedSampling(
                samplePos - 1
            ) && samplingFinish && dripSampleFinish) || (!sampleNeedSampling(
                samplePos - 1
            ))) || samplePos == 0
        ) {
            //是否是最后一个比色皿的位置，但是样本又不能是第一个，因为第一个样本代表还没取样
            if (lastCuvettePos(cuvettePos) && samplePos > 0) {
                //这排最后一个比色皿，需要去下一个步骤，加试剂
                Timber.d("这排最后一个比色皿，需要去下一个步骤，加试剂")
                stepDripReagent()
            }
//            else if (!isAuto()) {//手动模式
//
//                if (manualModelSamplingFinish()) {//取样结束了,开始下一个步骤
//                    stepDripReagent()
//                } else if (lastSamplePos(samplePos)) {//
//                    if (lastSampleShelf(sampleShelfPos)) {//最后一排
//                        //已经加完了最后一个样本了，加样结束，去下一个步骤，加试剂
//                        Timber.d("样本加样完成！")
//                        if (shelfNeedDripReagent()) {
//                            stepDripReagent()
//                        } else {
//                            Timber.d("不需要加试剂,检测结束！")
//                            testFinishAction()
//                        }
//                    } else {
//                        //这排样本已经取完样了，移动到下一排接着取样
//                        Timber.d("这排样本已经取完样了，移动到下一排接着取样")
//                        moveNextSampleAndCuvette()
//                    }
//                }
//            }
            else if ((isAuto() && lastSamplePos(samplePos)) || !isAuto()) {//这排最后一个样本
                if (!isAuto() && manualModelSamplingFinish()) {
                    Timber.d("手动模式，样本加样完成！")
                    stepDripReagent()
                } else if (lastSampleShelf(sampleShelfPos)) {//最后一排
                    //已经加完了最后一个样本了，加样结束，去下一个步骤，加试剂
                    Timber.d("样本加样完成！")
                    if (shelfNeedDripReagent()) {
                        stepDripReagent()
                    } else {
                        Timber.d("不需要加试剂,检测结束！")
                        testFinishAction()
                    }
                } else {
                    //这排样本已经取完样了，移动到下一排接着取样
                    Timber.d("这排样本已经取完样了，移动到下一排接着取样")
                    moveNextSampleAndCuvette()
                }
            } else {
                Timber.d("比色皿和样本都还有，继续")
                //比色皿和样本都还有，继续
                moveNextSampleAndCuvette()
            }
        }
    }

    /**
     * 判断这排比色皿是否需要加样
     * @return Boolean
     */
    private fun shelfNeedDripReagent(): Boolean {
        return mCuvetteStates[cuvetteShelfPos]!!.any { it.state == CuvetteState.DripSample }
    }

    /**
     * 去下一个步骤 加试剂
     * 因为跨命令的移动比色皿后重置位置，所以移动前，先把当前位置置为-1，这样移动后记录的位置才是真实的
     */
    private fun stepDripReagent() {
        Timber.d(" ———————— stepDripReagent testState=$testState ————————————————————————————————————————————————————————————————————————————————————————————————=")
        testState = TestState.DripReagent
        cuvettePos = -1
        val step = getNextStepCuvetteStartPos()
        moveCuvetteDripReagent(step + 2)
        takeReagent()
    }

    /**
     * 判断比色皿在取样时是否需要移动。
     * 因为比色皿和样本不一定是同步移动的，可能上一次移动样本时已经移动了比色皿，但这个样本不存在或扫码失败，下一次移动样本时就不需要再次移动比色皿了
     * @param cuvettePos Int
     * @return Boolean
     */
    private fun cuvetteNeedMove(cuvettePos: Int): Boolean {
        Timber.d("cuvetteNeedMove cuvetteShelfPos=$cuvetteShelfPos cuvettePos=$cuvettePos")
        return cuvettePos >= 0 && (mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state == CuvetteState.DripSample || mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state == CuvetteState.Skip)
    }


    /**
     * 接收到取样
     */
    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取样 reply=$reply cuvettePos=$cuvettePos samplePos=$samplePos cuvetteShelfPos=$cuvetteShelfPos sampleShelfPos=$sampleShelfPos")
        samplingFinish = true
        samplingNum++
        updateSampleState(samplePos - 1, SampleState.Sampling)
        goDripSample()
    }

    /**
     * 判断是否样本不足，是否需要提示
     */
    private fun sampleDeficiencyShowHint() {
        if (samplePos == sampleMax && sampleShelfPos == lastSampleShelfPos) {
            Timber.d("sampleDeficiencyShowHint 样本不足")
            sampleShelfPos = -1
            showTestSampleDeficiencyDialog()
        }
    }

    /**
     * 接收到移动比色皿 检测位
     */
    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        if (!runningTest()) return;
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿 检测位 reply=$reply")
        cuvetteMoveFinish = true

        test()
    }

    /**
     * 接收到移动比色皿 加试剂位，搅拌位
     */
    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿 加试剂位 reply=$reply cuvettePos=$cuvettePos stirProbeCleaningFinish=$stirProbeCleaningFinish stirProbeCleaningRecoverStir=$stirProbeCleaningRecoverStir")
        cuvetteMoveFinish = true
        goDripReagent()

        goStir()

        goTest()

        /**
         * 此步骤用于在跳过9个比色皿 && 正在跳过的这一排 && 在cuvettePos == 10时， 直接移动比色皿
         * 因为这个位置不需要加试剂，搅拌和检测。直接移动到下一个位置即可正常搅拌。下下个位置检测
         */
        if (cuvettePos == 10 && getFirstCuvetteStartPos() == 8 && getFirstCuvetteShelfIndex() == cuvetteShelfPos) {
            moveCuvetteDripReagent()
        }
    }

    /**
     * 去检测
     */
    private fun goTest() {
        if (cuvettePos > 2 && cuvetteNeedTest1(cuvettePos - 3)) {
            test()
        }
    }

    /**
     * 去搅拌
     */

    private fun goStir() {
        if (!stirProbeCleaningFinish) {
            stirProbeCleaningRecoverStir = true
            return
        }
        if (cuvettePos in 2..11 && cuvetteNeedStir(cuvettePos - 2)) {
            stir()
        }
    }


    /**
     * 接收到移动比色皿 加样位
     */
    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿 加样位 reply=$reply cuvetteStartPos=$cuvetteStartPos cuvettePos=$cuvettePos samplingFinish=$samplingFinish")
        cuvetteMoveFinish = true
        goDripSample()
    }

    /**
     * 接收到移动比色皿架
     */
    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        if (testState == TestState.TestFinish) {
            cuvetteShelfMoveFinish = true
            if (isTestFinish()) {
                showFinishDialog()
                openAllDoor()
            }
        }
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿架 reply=$reply cuvetteShelfPos=$cuvetteShelfPos cuvetteStartPos=$cuvetteStartPos")
        cuvetteShelfMoveFinish = true

        if (testState != TestState.TestFinish) {
            val needMoveStep = getFirstCuvetteStartPos();
            if (needMoveStep > -1) {
                moveCuvetteDripSample(needMoveStep + 2)
            } else {
                moveCuvetteDripSample()
            }

        }
    }

    /**
     * 获取第一排比色皿跳过的数量，如果不是第一排返回-1
     * @return Int
     */
    private fun getFirstCuvetteStartPos(): Int {
        return if (isFirstCuvetteShelf()) {
            getNextStepCuvetteStartPos()
        } else {
            -1
        }
    }


    /**
     * 检测结束动作完成后提示
     */
    public fun showFinishDialog() {
        dialogTestFinish.postValue(true)
        testState = TestState.None
    }

    private fun isTestFinish(): Boolean {
        return testState == TestState.TestFinish && sampleShelfMoveFinish && cuvetteShelfMoveFinish
    }


    /**
     * 接收到移动样本架
     */
    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        Timber.d("接收到移动样本架 reply=$reply sampleShelfPos=$sampleShelfPos $testState")
        if (testState == TestState.TestFinish) {
            sampleShelfMoveFinish = true
            if (isTestFinish()) {
                showFinishDialog()
                openAllDoor()
            }
        }
        if (!runningTest()) return
        if (!machineStateNormal()) return
        sampleShelfMoveFinish = true

        if (testState != TestState.TestFinish) {
            samplePos = -1;
            scanResults?.clear()
//            samplesStates = initSampleStates()
            moveSample()
        }
    }

    private fun openAllDoor() {
//        openSampleDoor()
//        openCuvetteDoor()
    }

    /**
     * 发送 开样本仓门
     */
    private fun openSampleDoor() {
        Timber.d("发送 开样本仓门")
        SerialPortUtil.Instance.openSampleDoor()
    }

    /**
     * 发送 开比色皿仓门
     */
    private fun openCuvetteDoor() {
        Timber.d("发送 开比色皿仓门")
        SerialPortUtil.Instance.openCuvetteDoor()
    }

    /**
     * 移动到下一个位置 样本和比色皿
     */
    private fun moveNextSampleAndCuvette() {
        if (samplePos < sampleMax) {
            //如果不是最后一个
            moveSample()
        } else {
            //如果还没全部取完样就该换下一排样本去取样了
            moveSampleShelfNext()
        }
        //如果需要移动
        if (cuvetteNeedMove(cuvettePos)) {
            moveCuvetteDripSample()
        }
    }


    /**
     * 移动到下一排,第一次的时候不能调用，
     * 因为在只有一排时调用，会直接显示样本不足
     */
    private fun moveSampleShelfNext() {
        if (lastSampleShelfPos == sampleShelfPos) {
            //已经是最后一排了，结束检测
            Timber.d("样本取样结束")
            showTestSampleDeficiencyDialog()
            return
        }

        val oldPos = sampleShelfPos
        for (i in sampleShelfPos + 1 until sampleShelfStates.size) {
            if (sampleShelfStates[i] == 1) {
                if (sampleShelfPos == oldPos) {
                    sampleShelfPos = i
                }
            }
        }

        moveSampleShelf(sampleShelfPos)
        Timber.d("moveSampleShelfNext sampleShelfPos=$sampleShelfPos oldPos=$oldPos")
    }

    /**
     * 显示样本不足的对话框
     */
    private fun showTestSampleDeficiencyDialog() {
        sampleDeficiency = true
        moveSampleShelf(sampleShelfPos)
    }

    /**
     * 接收到自检
     * @param reply ReplyModel<GetMachineStateModel>
     */
    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        Timber.d("接收到 自检 reply=$reply")
        dialogGetMachine.postValue(false)
        val errorInfo = reply.data.errorInfo
        testState = TestState.None
        if (errorInfo.isNullOrEmpty()) {
            Timber.d("自检完成")
            machineArgState = MachineState.Normal
            //自检成功后获取一下r1,r2，清洗液状态
            getState()
        } else {
            machineArgState = MachineState.NotGetMachineState
            val sb = StringBuffer()
            for (error in errorInfo) {
                sb.append(error.errorMsg)
                sb.append(error.motorMsg)
                sb.append("\n")
            }
            Timber.d("自检失败，错误信息=${sb}")
            getMachineFailedMsg.postValue(sb.toString())
        }
    }

    /**
     * 手动模式下，已经取样结束了
     * @return Boolean
     */
    fun manualModelSamplingFinish(): Boolean {
        return needSamplingNum <= samplingNum
    }


    /**
     * 接收到获取状态
     */
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        r1Reagent = reply.data.r1Reagent
        r2Reagent = reply.data.r2Reagent
        r2Volume = reply.data.r2Volume
        cleanoutFluid = reply.data.cleanoutFluid
        Timber.d("接收到 获取状态 reply=$reply continueTestCuvetteState=$continueTestCuvetteState continueTestSampleState=$continueTestSampleState clickStart=$clickStart r1Reagent=$r1Reagent r2Reagent=$r2Reagent cleanoutFluid=$cleanoutFluid continueTestGetState=$continueTestGetState")
        if (!runningTest()) return
        if (!machineStateNormal()) return
//        Timber.d("接收到 获取状态 reply=$reply continueTestCuvetteState=$continueTestCuvetteState continueTestSampleState=$continueTestSampleState clickStart=$clickStart r1Reagent=$r1Reagent r2Reagent=$r2Reagent cleanoutFluid=$cleanoutFluid continueTestGetState=$continueTestGetState")
        //比色皿不足才获取的状态
        //如果还是没有比色皿，继续弹框，如果有就移动比色皿架，继续检测
        if (continueTestCuvetteState) {
            cuvetteShelfStates = reply.data.cuvetteShelfs
            cuvetteShelfPos = -1
            getInitCuvetteShelfPos()
            if (cuvetteShelfPos == -1) {
                Timber.d("没有比色皿架")
                cuvetteDeficiency = true
                moveCuvetteShelfNext()
                return
            }
            continueTestCuvetteState = false
            moveCuvetteShelf(cuvetteShelfPos)
            moveSampleShelfNext()
            return
        }
        //样本不足才获取的状态
        //如果还是没有样本，继续弹框，如果有就移动比色皿架，继续检测
        if (continueTestSampleState) {
            sampleShelfStates = reply.data.sampleShelfs
            sampleShelfPos = -1
            getInitSampleShelfPos()
            if (sampleShelfPos == -1) {
                Timber.d("没有样本架")
                sampleDeficiency = true
                showTestSampleDeficiencyDialog()
                return
            }
            continueTestSampleState = false
            moveSampleShelf(sampleShelfPos)
            return
        }
        //开始下一排时R1|R2试剂不足|清洗液不足 才获取的状态
        if (continueTestGetState) {
            continueTestGetState = false
            //还有比色皿。继续移动比色皿，检测
            continueTestNextCuvette()
//            checkTestState(r2Volume, r1Reagent, r2Reagent, cleanoutFluid, accord = {
//                continueTestNextCuvette()
//            }, discrepancy = { str ->
//                continueTestGetState = true
//                getStateNotExistMsg.postValue(str)
//            })
            return
        }
        cuvetteShelfStates = reply.data.cuvetteShelfs
        sampleShelfStates = reply.data.sampleShelfs

        //如果是手动开始检测，就进行下一步骤，而且就直接返回
//        if (clickStart) {
//            clickStart = false
//        } else {
//            return
//        }

        getInitialPos()
        Timber.d("cuvetteShelfPos=${cuvetteShelfPos} sampleShelfPos=${sampleShelfPos}")
        if (cuvetteShelfPos == -1) {
            Timber.d("没有比色皿架")
            getStateNotExistMsg.postValue("比色皿不足，请添加")
            return
        }
        if (sampleShelfPos == -1) {
            Timber.d("没有样本架")
            getStateNotExistMsg.postValue("样本不足，请添加")
            return
        }

        checkTestState(accord = {
            //开始检测
            moveSampleShelf(sampleShelfPos)
            moveCuvetteShelf(cuvetteShelfPos)
        }, discrepancy = { str ->
            getStateNotExistMsg.postValue(str)
            return@checkTestState
        })
    }


    /**
     * 移动到下一排 第一次的时候不能调用，因为在只有一排时，会直接显示样本不足
     */
    private fun moveCuvetteShelfNext() {
        if (cuvetteShelfPos == lastCuvetteShelfPos) {
            Timber.d("已经是最后一排比色皿了")
            cuvetteDeficiency = true
            cuvetteShelfPos = -1
            moveCuvetteShelf(cuvetteShelfPos)
            return
        }
        val oldPos = cuvetteShelfPos
        for (i in cuvetteShelfPos + 1 until cuvetteShelfStates.size) {
            if (cuvetteShelfStates[i] == 1) {
                if (cuvetteShelfPos == oldPos) {
                    cuvetteShelfPos = i
                }
            }
        }
        moveCuvetteShelf(cuvetteShelfPos)
    }

    /**
     * 获取初始位置
     */
    private fun getInitialPos() {
        getInitCuvetteShelfPos()
        getInitSampleShelfPos()
    }

    /**
     * 获取样本架初始位置和最后一排的位置
     */
    private fun getInitSampleShelfPos() {
        val arrays: Array<Array<SampleItem>?> = arrayOfNulls(4)
        for (i in sampleShelfStates.indices) {
            var array: Array<SampleItem>? = null
            if (sampleShelfStates[i] == 1) {
                array = arrayOf(
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                    SampleItem(SampleState.None),
                )
                if (sampleShelfPos == -1) {
                    sampleShelfPos = i
                }
                lastSampleShelfPos = i
            }
            arrays[i] = array
        }
        mSamplesStates = arrays
        samplesStates.postValue(mSamplesStates)
        Timber.d("getInitSampleShelfPos sampleShelfPos=$sampleShelfPos lastSampleShelfPos=$lastSampleShelfPos")
    }

    /**
     * 获取比色皿架初始位置和最后一排的位置
     */
    private fun getInitCuvetteShelfPos() {
        val arrays: Array<Array<CuvetteItem>?> = arrayOfNulls(4)
        for (i in cuvetteShelfStates.indices) {
            var array: Array<CuvetteItem>? = null
            if (cuvetteShelfStates[i] == 1) {
                array = arrayOf(
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                    CuvetteItem(CuvetteState.None),
                )
                if (cuvetteShelfPos == -1) {
                    cuvetteShelfPos = i
                }
                lastCuvetteShelfPos = i
            }
            arrays[i] = array
        }
        mCuvetteStates = arrays
        cuvetteStates.postValue(mCuvetteStates)
        Timber.d("getInitCuvetteShelfPos cuvetteShelfPos=$cuvetteShelfPos lastCuvetteShelfPos=$lastCuvetteShelfPos")
    }

    /**
     * 是否是第一排比色皿
     */
    private fun isFirstCuvetteShelf(): Boolean {
        return cuvetteShelfPos == getFirstCuvetteShelfIndex()
    }

    /**
     * 获取第一排比色皿架的下标
     * @return Int
     */
    private fun getFirstCuvetteShelfIndex(): Int {
        for (i in cuvetteShelfStates.indices) {
            if (cuvetteShelfStates[i] == 1) {
                return i
            }
        }
        return -1;
    }

    override fun onMessageEvent(event: EventMsg<Any>) {
        super.onMessageEvent(event)
        when (event.what) {
            EventGlobal.WHAT_INIT_QRCODE -> {
            }
            else -> {}
        }
    }

    /**
     * 执行检测结束动作
     */
    private fun testFinishAction() {
        testState = TestState.TestFinish
        sampleShelfPos = -1
        cuvetteShelfPos = -1
        cuvetteStartPos = 0
        moveCuvetteShelf(cuvetteShelfPos)
        moveSampleShelf(sampleShelfPos)
    }


    /**
     * 对话框
     * 自检失败
     * 点击重新自检
     */
    fun dialogGetMachineFailedConfirm() {
        Timber.d("dialogGetMachineFailedConfirm 点击重新自检")
        goGetMachineState()
    }

    /**
     * 对话框
     * 自检失败
     * 点击我知道了
     */
    fun dialogGetMachineFailedCancel() {
        Timber.d("dialogGetMachineFailedCancel 点击我知道了")
    }


    /**
     * 检测结束
     * 比色皿不足
     * 点击继续检测
     */
    fun dialogTestFinishCuvetteDeficiencyConfirm() {
        Timber.d("dialogTestFinishCuvetteDeficiencyConfirm 点击继续检测")
        continueTestCuvetteState = true
        getState()
    }

    /**
     * 检测结束
     * 比色皿不足
     * 点击结束检测
     */
    fun dialogTestFinishCuvetteDeficiencyCancel() {
        Timber.d("dialogTestFinishCuvetteDeficiencyCancel 点击结束检测")

        testFinishAction();
    }

    /**
     * 正常检测中
     * 样本不足
     * 点击我已添加
     */
    fun dialogTestSampleDeficiencyConfirm() {
        Timber.d("dialogDripSampleSampleDeficiencyConfirm 点击我已添加")
        continueTestSampleState = true
        getState()
    }

    /**
     * 正常检测中
     * 样本不足
     * 点击结束检测
     */
    fun dialogTestSampleDeficiencyCancel() {
        Timber.d("dialogDripSampleSampleDeficiencyCancel 点击结束检测")
        testFinishAction();
    }

    /**
     * 开始检测
     * 比色皿,样本，试剂不足
     * 点击我已添加，继续检测
     */
    fun dialogGetStateNotExistConfirm() {
        Timber.d("dialogGetStateNotExistConfirm 点击我已添加，继续检测")
//        r1State.postValue(true)
//        r2State.postValue(true)
//        cleanoutFluidState.postValue(true)
//        r2VolumeState.postValue(2)

        getState()
    }

    /**
     * 开始检测
     * 比色皿,样本，试剂不足
     * 点击结束检测
     */
    fun dialogGetStateNotExistCancel() {
        Timber.d("dialogGetStateNotExistCancel 点击结束检测")
        testFinishAction();
    }

    /**
     * 获取样本架，比色皿架状态，试剂，清洗液状态
     */

    private fun getState() {
        Timber.d("发送 获取状态")
        SerialPortUtil.Instance.getState()
    }

    /**
     * 自检
     */
    private fun getMachineState() {
        Timber.d("发送 自检")
        SerialPortUtil.Instance.getMachineState()
    }

    /**
     * 移动样本架
     * @param pos Int
     */
    private fun moveSampleShelf(pos: Int) {
        Timber.d("发送 移动样本架 pos=$pos ")
        sampleShelfMoveFinish = false
        SerialPortUtil.Instance.moveSampleShelf(pos + 1)
    }

    /**
     * 移动比色皿架
     * @param pos Int
     */
    private fun moveCuvetteShelf(pos: Int) {
        Timber.d("发送 移动比色皿架 pos=$pos ")
        cuvetteShelfMoveFinish = false
        SerialPortUtil.Instance.moveCuvetteShelf(pos + 1)
    }

    /**
     * 移动样本
     * @param step Int
     */
    private fun moveSample(step: Int = 1) {
        Timber.d("发送 移动样本 step=$step samplePos=$samplePos")
        sampleMoveFinish = false
        samplePos += step;
        SerialPortUtil.Instance.moveSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 滴样位
     */
    private fun moveCuvetteDripSample(step: Int = 1) {
        Timber.d("发送 移动比色皿到 加样位 step=$step cuvettePos=$cuvettePos")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.Instance.moveCuvetteDripSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 加试剂位
     */
    private fun moveCuvetteDripReagent(step: Int = 1) {
        Timber.d("发送 移动比色皿到 加试剂位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.Instance.moveCuvetteDripReagent(step > 0, step.absoluteValue)
    }


    /**
     * 移动比色皿到 检测位
     */
    private fun moveCuvetteTest(step: Int = 1) {
        Timber.d("发送 移动比色皿到 检测位 testState=$testState step=$step")
        cuvettePos += step
        testFinish = false
        testing = true
        SerialPortUtil.Instance.moveCuvetteTest(step > 0, step.absoluteValue)
    }

    /**
     * 取样
     */
    private fun sampling() {
        Timber.d("发送 取样")
        samplingFinish = false
        dripSampleFinish = false
        SerialPortUtil.Instance.sampling()
    }

    /**
     * 取试剂
     */
    private fun takeReagent() {
        Timber.d("发送 取试剂")
        takeReagentFinish = false
        SerialPortUtil.Instance.takeReagent()
    }

    /**
     * 取样针清洗
     */
    private fun samplingProbeCleaning() {
        Timber.d("发送 取样针清洗")
        samplingProbeCleaningFinish = false
        SerialPortUtil.Instance.samplingProbeCleaning()
    }

    /**
     * 搅拌
     */
    private fun stir() {
        Timber.d("发送 搅拌")
        stirFinish = false
        SerialPortUtil.Instance.stir()
    }

    /**
     * 搅拌针清洗
     */
    private fun stirProbeCleaning() {
        Timber.d("发送 搅拌针清洗")
        stirProbeCleaningFinish = false
        SerialPortUtil.Instance.stirProbeCleaning()
    }

    /**
     * 加样
     */
    private fun dripSample() {
        Timber.d("发送 加样")
        dripSampleFinish = false
        SerialPortUtil.Instance.dripSample()
    }

    /**
     * 加试剂
     */
    private fun dripReagent() {
        Timber.d("发送 加试剂")
        dripReagentFinish = false
        SerialPortUtil.Instance.dripReagent()
    }

    /**
     * 检测
     */
    private fun test() {
        Timber.d("发送 检测 $cuvettePos")
        testFinish = false
        SerialPortUtil.Instance.test()
    }

    /**
     * 是否正在检测
     */
    private fun runningTest(): Boolean {
        return testState != TestState.None && matchingTestState == MatchingArgState.None
    }

    fun clickTest1(view: View) {

    }

    /**
     * 更改了配置
     * @param projectModel ProjectModel
     * @param skipNum Int
     * @param detectionNum String
     * @param sampleNum Int
     */
    fun changeConfig(
        projectModel: ProjectModel, skipNum: Int, detectionNum: String, sampleNum: Int
    ) {
        selectProject = projectModel
        detectionNumInput = detectionNum
        cuvetteStartPos = skipNum
        needSamplingNum = sampleNum
    }

    /**
     * 控制开始检测时按钮不可用
     * @param enable Boolean
     */
    fun enableView(enable: Boolean) {
        selectProjectEnable.postValue(enable)
        editDetectionNumEnable.postValue(enable)
        skipCuvetteEnable.postValue(enable)
    }

    data class CuvetteItem(
        var state: CuvetteState, var testResult: TestResultModel? = null, var sampleID: String? = ""
    )

    data class SampleItem(
        var state: SampleState, var testResult: TestResultModel? = null, var cuvetteID: String? = ""
    )
}

class HomeViewModelFactory(
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val testResultRepository: TestResultRepository = TestResultRepository()
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(projectRepository, testResultRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
