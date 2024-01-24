package com.wl.turbidimetric.home

import androidx.lifecycle.*
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.testType
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.OnScanResult
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wllib.LogToFile.c
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.greenrobot.eventbus.EventBus
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.absoluteValue

class HomeViewModel(
    private val appViewModel: AppViewModel,
    private val projectRepository: ProjectRepository,
    private val curveRepository: CurveRepository,
    private val testResultRepository: TestResultRepository
) : BaseViewModel(), Callback2, OnScanResult {

    init {
        listener()
    }

    /**
     * 自检
     */
    fun goGetMachineState() {
//        dialogGetMachine.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            _dialogUiState.emit(HomeDialogUiState.GetMachineShow)
        }
        appViewModel.testState = TestState.GetMachineState
        getMachineState()
    }

    fun goGetVersion() {
        SerialPortUtil.getVersion()
    }

    private fun listener() {
        SerialPortUtil.callback.add(this)
        ScanCodeUtil.onScanResult = this

        listenerTempState()
    }

    /**
     * 检测结果
     */
    val resultModels = arrayListOf<TestResultAndCurveModel?>()

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
     * 对话框状态，不包括调试的和不需要viewmodel处理的
     */
    private val _dialogUiState = MutableSharedFlow<HomeDialogUiState>()
    val dialogUiState = _dialogUiState.asSharedFlow()

    /**
     * 界面的其他状态
     */
    private val _testMachineUiState = MutableStateFlow(
        HomeMachineUiState(
            r1State = false,
            r2State = -1,
            cleanoutFluidState = false,
            reactionTemp = 0.0,
            r1Temp = 0.0,
        )
    )
    val testMachineUiState: StateFlow<HomeMachineUiState> = _testMachineUiState.asStateFlow()


    /**
     * 配置信息内的选择项目、编号、跳过比色皿等按钮是否可用，检测时不可用
     */
    val configViewEnable = MutableLiveData(true)

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
    private val _cuvetteStates = MutableStateFlow(initCuvetteStates())
    val cuvetteStates: StateFlow<Array<Array<CuvetteItem>?>> = _cuvetteStates.asStateFlow()

    /**当前排所有比色皿的状态
     *
     */
    private var mCuvetteStates = initCuvetteStates()


    /**当前排所有样本的状态
     *
     */
    private var _samplesStates = MutableStateFlow(initSampleStates())
    val sampleStates: StateFlow<Array<Array<SampleItem>?>> = _samplesStates.asStateFlow()


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

    /**
     *比色皿起始的位置，只限于第一排比色皿用来跳过前面的几个已经使用过的比色皿
     */
    var cuvetteStartPos = 0


    /**继续检测后获取状态 判断是否是来自比色皿不足
     *
     */
    private var continueTestCuvetteState = false

    /**继续检测后获取状态 判断是否是来自样本不足
     *
     */
    private var continueTestSampleState = false

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


    val testMsg = MutableLiveData("")
    val projectDatas = curveRepository.allDatas

    /**
     * 选择的检测项目
     */
    var selectProject: CurveModel? = null

    /**
     * 输入的起始编号，在第一次开始的时候才赋值 ,使用过就会为空
     */
    var detectionNumInput = ""


    /**
     * 第一次的检测间隔
     */
    var testShelfInterval1: Long = 1000 * 0

    /**
     * 第二次的检测间隔
     */
    var testShelfInterval2: Long = 1000 * 0

    /**
     * 第三次的检测间隔
     */
    var testShelfInterval3: Long = 1000 * 0

    /**
     * 第四次的检测间隔
     */
    var testShelfInterval4: Long = 1000 * 0


    /**
     * 扫码结果
     */
    var scanResults = arrayListOf<String?>()

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

    /**
     * 保存着当前样本位置的类型（如果有）
     */
    var sampleType: SampleType? = null

    /**
     * r1试剂
     */
    var r1Reagent: Boolean = false
        set(value) {
            field = value
            _testMachineUiState.update {
                it.copy(r1State = value)
            }
        }

    /**
     * r2试剂
     */
    var r2Reagent: Boolean = false

    /**
     * r2试剂量
     */
    var r2Volume: Int = 0
        set(value) {
            field = value
            _testMachineUiState.update {
                it.copy(r2State = value)
            }
        }

    /**
     * 清洗液量
     */
    var cleanoutFluid: Boolean = false
        set(value) {
            field = value
            _testMachineUiState.update {
                it.copy(cleanoutFluidState = value)
            }
        }

    /**
     * 开始检测前的清洗取样针
     */
    var cleaningBeforeStartTest = false

    /**
     * 记录每个比色皿的搅拌时间
     */
    val stirTimes = longArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /**
     * 是否允许获取温度（在调试时不自动获取）
     */
    var allowTemp: Boolean = true

    /**
     * 是否允许取试剂(在出现取试剂失败的情况下，只检测已经加好试剂的样本，不继续取试剂)
     */
    var allowTakeReagent = true

    /**
     * 是否允许加样(在出现加样时比色皿非空的情况下，只检测已经加好样的样本，不继续取样加样)
     */
    var allowDripSample = true

    /**
     * 测试用的 start
     */
    //检测的值
    //测试三次方拟合
//    private val testValues1 = doubleArrayOf(27.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
//    private val testValues2 =
//        doubleArrayOf(0.0, 28.8, 239.6, 975.6, 1979.0, 0.3, 0.3, 0.3, 0.3, 0.3)
    //测试线性拟合
//    private val testValues1 = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
//    private val testValues2 =
//        doubleArrayOf(1.0, 16.0, 35.0, 140.0, 310.0, 623.0, 0.3, 0.3, 0.3, 0.3)
    //测试四参数拟合
    private val testValues1 = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private val testValues2 =
        doubleArrayOf(0.1, 15.9, 37.5, 178.8, 329.3, 437.7, 0.3, 0.3, 0.3, 0.3)
    private val testValues3 = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private val testValues4 = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
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
    val testS: Long = 100

    //测试用 每个比色皿之间的检测间隔
    val testP: Long = 100

    /**
     * 一直获取温度状态
     */
    private fun listenerTempState() {
        viewModelScope.launch {
            timer("", true, Date(), 30000) {
                if (allowTemp) {
                    SerialPortUtil.getTemp()
                }
            }
        }
    }


    /**
     * 测试用的 end
     */
    fun clickStart() {
        val errorMsg = if (appViewModel.testState.isRunning()) {
            "正在检测，请勿操作！"
        } else if (appViewModel.testState.isRunningError()) {
            "请停止使用仪器并联系供应商维修"
        } else if (selectProject == null) {
            "未选择标曲"
        } else if (isManual() && needSamplingNum <= 0) {
            "未指定检测数量"
        } else if (isManualSampling() && needSamplingNum <= 0) {
            "未指定检测数量"
        } else {
            ""
        }
        if (errorMsg.isNotEmpty()) {
            viewModelScope.launch {
                _dialogUiState.emit(HomeDialogUiState.Notify(errorMsg))
            }
            return
        }
        //需要重新自检
        if (appViewModel.testState.isNotPrepare()) {
            goGetMachineState()
            return
        }
        testType = TestType.Test
        clickStart = true
        initState()
        getState()
    }


    private fun initState() {
        allowDripSample = true
        allowTakeReagent = true
        testMsg.value = ""
        appViewModel.testState = TestState.DripSample
        cuvetteShelfPos = -1
        sampleShelfPos = -1
        samplingNum = 0
        if (detectionNumInput.isNotEmpty()) {//如果更改了起始编号就使用输入的
            LocalData.DetectionNum = detectionNumInput
            detectionNumInput = ""
        }

        mSamplesStates = initSampleStates()
        mCuvetteStates = initCuvetteStates()
        _cuvetteStates.update {
            mCuvetteStates
        }
        _samplesStates.update {
            mSamplesStates
        }


        i("跳过 $cuvetteStartPos 个比色皿")

        if (SystemGlobal.isCodeDebug) {
            testShelfInterval1 = testS
            testShelfInterval2 = testS
            testShelfInterval3 = testS
            testShelfInterval4 = testS
        } else {
            testShelfInterval1 = LocalData.Test1DelayTime
            testShelfInterval2 = LocalData.Test2DelayTime
            testShelfInterval3 = LocalData.Test3DelayTime
            testShelfInterval4 = LocalData.Test4DelayTime
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
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 取试剂 reply=$reply cuvettePos=$cuvettePos")
        takeReagentFinish = true
        if (reply.state == ReplyState.TAKE_REAGENT_FAILED) {//取试剂失败
            allowTakeReagent = false
            dripReagentFinish = true
            updateCuvetteState(cuvettePos, CuvetteState.TakeReagentFailed)
            if (cuvetteMoveFinish) {//已经移动到位了，取试剂失败后不应该再取，只检测已经加完试剂的样本
                val takeReagentCuvetteNum = getTakeReagentCuvetteNum()
                if (takeReagentCuvetteNum == 0) {//第一个就失败，直接结束检测
                    testFinishAction()
                } else {
                    nextDripReagent()
                }
            }
        } else {//取试剂成功,去加试剂
            goDripReagent()
        }
    }

    /**
     * 获取当前比色皿架已经加好试剂的比色皿的数量
     */
    private fun getTakeReagentCuvetteNum(): Any {
        if (cuvetteShelfPos !in mCuvetteStates.indices) {
            return 0
        }
        return mCuvetteStates[cuvetteShelfPos]!!.filter { it.state == CuvetteState.DripReagent }.size
    }


    /**
     * 去加试剂
     */
    private fun goDripReagent() {
        i("goDripReagent cuvettePos=$cuvettePos takeReagentFinish=$takeReagentFinish cuvetteMoveFinish=$cuvetteMoveFinish allowTakeReagent=$allowTakeReagent")
        if (cuvettePos < 10 && cuvetteNeedDripReagent(cuvettePos) && takeReagentFinish && cuvetteMoveFinish && allowTakeReagent) {
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
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 取样针清洗 reply=$reply samplingProbeCleaningRecoverSampling=$samplingProbeCleaningRecoverSampling")

        samplingProbeCleaningFinish = true

        if (cleaningBeforeStartTest) {
            //是开始检测前的清洗，清洗完才开始检测
            cleaningBeforeStartTest = false
            if (!isManualSampling()) {
                moveSampleShelf(sampleShelfPos)
            }
            moveCuvetteShelf(cuvetteShelfPos)
        }
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
//        cleanoutFluidState.postValue(reply.data.cleanoutFluid)

        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 搅拌针清洗 reply=$reply stirProbeCleaningRecoverStir=$stirProbeCleaningRecoverStir")

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
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 刺破 reply=$reply samplePos=$samplePos")

        piercedFinish = true
        updateSampleState(samplePos, SampleState.Pierced)
        //当不需要取样时，直接下一步
        if (!sampleNeedSampling(samplePos - 1)) {
            samplingFinish = true
            dripSampleFinish = true
            i("不需要取样 下一步")
        }
        nextStepDripReagent()
    }


    /**
     * 去加样
     */
    private fun goDripSample() {
        i("sampleMoveFinish=$sampleMoveFinish samplingFinish=$samplingFinish cuvetteMoveFinish=$cuvetteMoveFinish")
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
//        if (!runningTest()) return
//        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 获取版本号 reply=$reply")
        SystemGlobal.mcuVersion = reply.data.version
    }

    /**
     * 接收到 获取设置温度
     * @param reply ReplyModel<TempModel>
     */
    override fun readDataTempModel(reply: ReplyModel<TempModel>) {
        c("接收到 获取设置温度 reply=$reply")

        _testMachineUiState.update {
            it.copy(
                reactionTemp = reply.data.reactionTemp / 10.0, r1Temp = reply.data.r1Temp / 10.0
            )
        }
    }


    /**
     * 在收到每个命令的时候判断是否是状态错误
     *
     * 成功：0
     *
     * 失败：1:非法参数 2:电机错误 3:传感器错误 4:取样失败（样本量不足） 5:比色皿非空 6:取试剂失败
     *
     * 其中 1 2 3 在每个命令都通用，在这里判断并提示
     *
     * 成功：0 继续执行下一步命令
     *
     * 失败：1 2 3 中断，其他看具体情况而定
     * @param cmd 命令
     * @param state 状态
     */
    override fun stateSuccess(cmd: Int, state: Int): Boolean {
        val isSuccess = cmd == ReplyState.SUCCESS.ordinal
        var stateFailedText = ""
        if (!isSuccess) {//状态失败、
            stateFailedText = when (convertReplyState(state)) {
                ReplyState.INVALID_PARAMETER -> "非法数据 命令号:${cmd}"
                ReplyState.MOTOR_ERR -> "电机错误 命令号:${cmd}"
                ReplyState.SENSOR_ERR -> "传感器错误 命令号:${cmd}"
                ReplyState.ORDER -> "意外的命令号"
                else -> {
                    ""
                }
            }
            if (stateFailedText.isNotEmpty()) {
                stateErrorStopRunning()
                viewModelScope.launch {
                    _dialogUiState.emit(
                        HomeDialogUiState.StateFailed(
                            stateFailedText.plus("，请停止使用仪器并联系供应商维修")
                        )
                    )
                }
            }
        }
        return stateFailedText.isEmpty()
    }

    /**
     * 挤压
     * @param reply ReplyModel<SqueezingModel>
     */
    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 挤压 reply=$reply")

        updateSampleState(samplePos - 1, SampleState.Squeezing)
        //注：如果上次取样针清洗未结束，就等待清洗结束后再加样,否则直接加样
        if (samplingProbeCleaningFinish) {
            sampling()
        } else {
            samplingProbeCleaningRecoverSampling = true
        }
    }

    /**
     * 接收到 样本门状态
     * @param reply ReplyModel<SampleDoorModel>
     */
    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {
        c("接收到 样本门状态 reply=$reply")
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return

    }

    /**
     * 接收到 比色皿门状态
     * @param reply ReplyModel<CuvetteDoorModel>
     */
    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
        c("接收到 比色皿门状态 reply=$reply")
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
    }

    /**
     * 接收到移动样本
     */
    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 移动样本 reply=$reply cuvettePos=$cuvettePos lastCuvetteShelfPos=$lastCuvetteShelfPos cuvetteShelfPos=$cuvetteShelfPos samplePos=$samplePos samplingProbeCleaningFinish=$samplingProbeCleaningFinish")

        sampleMoveFinish = true
        samplingFinish = false
        dripSampleFinish = false
        scanFinish = false
        sampleType = reply.data.type
        var isNonexistent = reply.data.type.isNonexistent()
        var isCuvette = reply.data.type.isCuvette()
        //最后一个位置不需要扫码，直接取样
        if (samplePos < sampleMax) {
//            if ((SystemGlobal.isCodeDebug && !sampleExists[samplePos]) || (isAuto() && LocalData.SampleExist && isNonexistent)) {
            //如果是自动模式并且已开启样本传感器,并且是未识别到样本，才是没有样本
            if ((isAuto() && LocalData.SampleExist && isNonexistent)) {
                //没有样本，移动到下一个位置
//                i("没有样本,移动到下一个位置")
                scanFinish = true
                scanResults.add(null)
                i("scanResults=$scanResults")
                updateSampleState(samplePos, SampleState.NONEXISTENT)
                nextStepDripReagent()
            } else {
                //有样本
                i("有样本")
                updateSampleState(samplePos, SampleState.Exist)
                startScan(samplePos)
            }
        }
        //判断是否需要取样。取样位和扫码位差一个位置
        if (sampleNeedSampling(samplePos - 1)) {
            val isSample =
                mSamplesStates[sampleShelfPos]?.get(samplePos - 1)?.sampleType?.isSample() == true
            squeezing(isSample)
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
        return mSamplesStates[sampleShelfPos]!![samplePos].state == SampleState.Pierced || mSamplesStates[sampleShelfPos]!![samplePos].state == SampleState.Squeezing
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
        sampleType: SampleType? = null,
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
        samplePos.let {
            mSamplesStates[sampleShelfPos]!![samplePos].cuvetteID = cuvettePos
        }
        sampleType?.let {
            mSamplesStates[sampleShelfPos]!![samplePos].sampleType = sampleType
        }
        _samplesStates.value = mSamplesStates.copyOf()
        i("updateSampleState ShelfPos=$sampleShelfPos samplePos=$samplePos  sampleType=$sampleType samplesState=${mSamplesStates.print()}")
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
        _cuvetteStates.value = mCuvetteStates.copyOf()
        i("updateCuvetteState ShelfPos=$cuvetteShelfPos cuvettePos=$cuvettePos cuvetteStates=${mCuvetteStates.print()}")
    }


    /**
     * 开始扫码
     */
    private fun startScan(samplePos: Int) {
        scanFinish = false
        piercedFinish = false


        /**
         * 以下情况需要扫码
         * 1、测试用的
         * 2、自动模式并且开启了扫码并且样本类型为样本管
         */
        if (!SystemGlobal.isCodeDebug && (isAuto() && LocalData.ScanCode && sampleType?.isSample() == true)) {
            viewModelScope.launch {
                ScanCodeUtil.startScan()
            }
        } else {
            /**
             * 以下情况不扫码，直接成功
             * 1、如果是测试用的并且测试数据是扫码成功
             * 2、是自动模式，但是未开启扫码
             * 3、是手动模式时
             * 4、是比色杯时
             */
            if ((SystemGlobal.isCodeDebug && tempSampleState[samplePos] == 1) || (isAuto() && !LocalData.ScanCode) || (!isManual()) || sampleType?.isCuvette() == true) {
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
        i("扫码成功 str=$str")
        updateSampleState(samplePos, SampleState.ScanSuccess, sampleType = sampleType)
        scanFinish = true
        pierced()
        scanResults.add(str)
        i("scanResults=$scanResults")
    }

    /**
     * 扫码失败
     */
    override fun scanFailed() {
        i("扫码失败")
        updateSampleState(samplePos, SampleState.ScanFailed, sampleType = sampleType)
        scanFinish = true
        piercedFinish = true
        //当不需要取样时，直接下一步
        if (!sampleNeedSampling(samplePos - 1)) {
            samplingFinish = true
            dripSampleFinish = true
            i("不需要取样 下一步")
        }
        nextStepDripReagent()
        scanResults.add(null)
        i("scanResults=$scanResults")
    }


    /**
     * 新建检测记录
     * @param str String?
     */
    private suspend fun createResultModel(str: String?, sampleItem: SampleItem?): TestResultModel {
        val resultModel = TestResultModel(
            sampleBarcode = str ?: "",
            createTime = Date().time,
            detectionNum = LocalData.getDetectionNumInc(),
            sampleType = sampleItem?.sampleType?.ordinal ?: SampleType.NONEXISTENT.ordinal,
            curveOwnerId = selectProject?.curveId ?: 0
        )
        val id = testResultRepository.addTestResult(resultModel)
        resultModel.resultId = id
        resultModels.add(TestResultAndCurveModel(resultModel, selectProject))
        return resultModel
    }

    /**
     * 刺破
     */
    private fun pierced() {
        val type =
            mSamplesStates.get(sampleShelfPos)?.get(samplePos)?.sampleType ?: SampleType.CUVETTE
        c("发送 刺破 type=$type")
        piercedFinish = false
        SerialPortUtil.pierced(type)
    }


    /**
     * 接收到检测完成
     */
    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 检测完成 reply=$reply cuvettePos=$cuvettePos appViewModel.testState=$appViewModel.testState")

        testFinish = true

        calcTestResult(reply.data.value)
    }

    /**
     * 计算结果
     */
    private fun calcTestResult(value: Int) {
        when (appViewModel.testState) {
            TestState.DripReagent -> {
                updateCuvetteState(cuvettePos - 5, CuvetteState.Test1)
                updateTestResultModel(value, cuvettePos - 5, CuvetteState.Test1)
                nextDripReagent()
            }

            TestState.Test2 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test2)
                updateTestResultModel(value, cuvettePos, CuvetteState.Test2)
                if (lastNeed(cuvettePos, CuvetteState.Test1)) {
                    //检测结束，下一个步骤，检测第三次
                    stepTest(TestState.Test3)
                } else {
                    //继续检测
                    moveCuvetteTest()
                }
            }

            TestState.Test3 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test3)
                updateTestResultModel(value, cuvettePos, CuvetteState.Test3)
                if (lastNeed(cuvettePos, CuvetteState.Test2)) {
                    //检测结束，下一个步骤，检测第四次
                    stepTest(TestState.Test4)
                } else {
                    //继续检测
                    moveCuvetteTest()
                }
            }

            TestState.Test4 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test4)
                updateTestResultModel(value, cuvettePos, CuvetteState.Test4)
                if (lastNeed(cuvettePos, CuvetteState.Test3)) {
                    //检测结束，下一个步骤，计算值
                    showResultFinishAndNext()
                } else {
                    moveCuvetteTest()
                }
            }

            else -> {}
        }

    }

    /**
     * 更新检测结果
     */
    private fun updateTestResultModel(value: Int, index: Int, state: CuvetteState) {
        var pos = index
        i("updateTestResultModel pos=$pos resultModels=$resultModels size=${resultModels.size}")
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
                resultModels[pos]?.result?.testValue1 = resultTest1[pos]
                resultModels[pos]?.result?.testOriginalValue1 = resultOriginalTest1[pos]

                mCuvetteStates[cuvetteShelfPos]?.get(pos)?.testResult = resultModels[pos]?.result
                _cuvetteStates.value = mCuvetteStates
            }

            CuvetteState.Test2 -> {
                if (SystemGlobal.isCodeDebug) {
                    resultTest2.add(testValues2[pos].toBigDecimal())
                    resultOriginalTest2.add(testOriginalValues2[pos])
                } else {
                    resultOriginalTest2.add(value)
                    resultTest2.add(calcAbsorbance(value.toBigDecimal()))
                }
                resultModels[pos]?.result?.testValue2 = resultTest2[pos]
                resultModels[pos]?.result?.testOriginalValue2 = resultOriginalTest2[pos]
            }

            CuvetteState.Test3 -> {
                if (SystemGlobal.isCodeDebug) {
                    resultTest3.add(testValues3[pos].toBigDecimal())
                    resultOriginalTest3.add(testOriginalValues3[pos])
                } else {
                    resultOriginalTest3.add(value)
                    resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                }
                resultModels[pos]?.result?.testValue3 = resultTest3[pos]
                resultModels[pos]?.result?.testOriginalValue3 = resultOriginalTest3[pos]
            }

            CuvetteState.Test4 -> {
                if (SystemGlobal.isCodeDebug) {
                    resultTest4.add(testValues4[pos].toBigDecimal())
                    resultOriginalTest4.add(testOriginalValues4[pos])
                } else {
                    resultOriginalTest4.add(value)
                    resultTest4.add(calcAbsorbance(value.toBigDecimal()))
                }
                resultModels[pos]?.result?.testValue4 = resultTest4[pos]
                resultModels[pos]?.result?.testOriginalValue4 = resultOriginalTest4[pos]
                resultModels[pos]?.result?.testTime = Date().time
                //计算单个结果浓度
                val abs = calcAbsorbanceDifference(resultTest1[pos], resultTest2[pos])
                absorbances.add(abs)
                selectProject?.let { project ->
                    var con = calcCon(abs, project)
                    con = if (con.toDouble() < 0.0) 0 else con
                    cons.add(con)
                    resultModels[pos]?.result?.absorbances = abs
                    resultModels[pos]?.result?.concentration = con


                    resultModels[pos]?.result?.testResult = calcShowTestResult(
                        con, resultModels[pos]?.curve?.projectLjz ?: 100
                    )
                }
                //单个检测完毕
                resultModels[pos]?.let {
                    singleTestResultFinish(it)
                }

            }

            else -> {}
        }

        resultModels[pos]?.result?.let {
            viewModelScope.launch {
                //因为结果内的姓名性别等信息可能在数据管理内修改过了，所以这里没有
                testResultRepository.getTestResultModelById(it.resultId).apply {
                    testOriginalValue1 = it.testOriginalValue1
                    testOriginalValue2 = it.testOriginalValue2
                    testOriginalValue3 = it.testOriginalValue3
                    testOriginalValue4 = it.testOriginalValue4
                    testTime = it.testTime
                    testState = it.testState
                    testResult = it.testResult
                    testValue1 = it.testValue1
                    testValue2 = it.testValue2
                    testValue3 = it.testValue3
                    testValue4 = it.testValue4
                    sampleBarcode = it.sampleBarcode
                    concentration = it.concentration
                    absorbances = it.absorbances
                    testResultRepository.updateTestResult(this)
                }
            }
        }
        i("updateTestResultModel resultModels=$resultModels")
    }

    /**
     * 单个检测完毕
     * 自动打印
     * 自动上传 等
     * @param testResultModel TestResultModel?
     */
    private fun singleTestResultFinish(testResultModel: TestResultAndCurveModel) {
        if (HL7Helper.getConfig().autoUpload && HL7Helper.isConnected()) {
            HL7Helper.uploadSingleTestResult(testResultModel) { count, success, failed ->
                i("singleTestResultFinish count=$count success=$success failed=$failed")
            }
        }


    }


    /**
     * 显示结果、清空数据并开始下一排检测
     */
    private fun showResultFinishAndNext() {
        testMsg.postValue(
            testMsg.value?.plus("这排比色皿检测结束 比色皿位置=$cuvetteShelfPos 样本位置=$sampleShelfPos \n 第一次:$resultTest1 \n 第二次:$resultTest2 \n 第三次:$resultTest3 \n 第四次:$resultTest4 \n  吸光度:$absorbances \n 浓度=$cons \n选择的四参数为${selectProject ?: "未选择"}\n\n")
        )
        resultModels.forEach {
            i("resultModel=$it")
        }
        i("这排比色皿检测结束")

        clearSingleShelf()

        continueTestNextCuvette()
    }

    /**
     * 单排比色皿结束结束后清理
     */
    private fun clearSingleShelf() {
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
    }


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
            i("没有R1试剂")
            discrepancy.invoke("R1试剂不足，请添加")
            return
        }
        if (!r2Reagent) {
            i("没有R2试剂")
            discrepancy.invoke("R2试剂不足，请添加")
            return
        }
        if (r2Volume == 0) {
            i("没有R2试剂")
            discrepancy.invoke("R2试剂不足，请添加")
            return
        }
        if (!cleanoutFluid) {
            i("没有清洗液")
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
        appViewModel.testState = TestState.DripSample
        cuvettePos = -1

        if (!allowTakeReagent || !allowDripSample) {//取试剂失败|比色皿非空，不允许再检测
            testFinishAction()
            return
        }
        if ((isAuto() && lastSamplePos(samplePos)) || isManual() || isManualSampling()) {//这排最后一个样本
            if (isManual() && manualModelSamplingFinish()) {//手动模式已经取完样
                //手动模式，检测完了
                testFinishAction()
            } else if (isManualSampling() && needSamplingNum <= 0) {//手动取样模式已经检测指定数量的比色皿
                //手动模式，检测完了
                testFinishAction()
            } else if ((isAuto() || isManual()) && lastSampleShelf(sampleShelfPos)) {//最后一排
                //结束检测，样本已经取完样了
                testFinishAction()
            } else {
                if (lastCuvetteShelf(cuvetteShelfPos)) {//最后一排比色皿
                    if (isManualSampling()) {
                        testFinishAction()
                    } else {
                        //提示，比色皿不足了
                        viewModelScope.launch {
                            _dialogUiState.emit(
                                HomeDialogUiState.CuvetteDeficiency
                            )
                        }
                    }
                } else {
                    checkTestState(accord = {
                        if (!isManualSampling()) {
                            //移动样本架
                            moveSampleShelfNext()
                        }
                        //还有比色皿。继续移动比色皿，检测
                        moveCuvetteShelfNext()
                    }, discrepancy = { str ->
                        continueTestGetState = true
                        viewModelScope.launch {
                            _dialogUiState.emit(
                                HomeDialogUiState.GetStateNotExist(str)
                            )
                        }
                    })
                }
            }
        } else {
            if (lastCuvetteShelf(cuvetteShelfPos)) {//最后一排比色皿
                if (isManualSampling()) {
                    testFinishAction()
                } else {
                    //提示，比色皿不足了
                    viewModelScope.launch {
                        _dialogUiState.emit(
                            HomeDialogUiState.CuvetteDeficiency
                        )
                    }
                }
            } else {
                //还有比色皿。继续移动比色皿，检测
                checkTestState(accord = {
                    moveSample()
                    moveCuvetteShelfNext()
                }, discrepancy = { str ->
                    continueTestGetState = true
                    viewModelScope.launch {
                        _dialogUiState.emit(
                            HomeDialogUiState.GetStateNotExist("试剂和清洗液不足")
                        )
                    }
                })
            }
        }
    }

    /**
     * 判断是否进行下一次加试剂
     */
    private fun nextDripReagent() {
        if (appViewModel.testState != TestState.DripReagent) return
        i("nextDripReagent cuvettePos=$cuvettePos dripReagentFinish=$dripReagentFinish testFinish=$testFinish stirFinish=$stirFinish stirProbeCleaningFinish=$stirProbeCleaningFinish takeReagentFinish=$takeReagentFinish cuvetteMoveFinish=$cuvetteMoveFinish allowTakeReagent=$allowTakeReagent")
        if (cuvettePos < 15) {
            //当取试剂完成，检测完成，搅拌完成，加试剂完成，移动比色皿完成时，
            //去取试剂，移动比色皿
            if (dripReagentFinish && testFinish && stirFinish && takeReagentFinish && cuvetteMoveFinish && stirProbeCleaningFinish) {
//                //是否是最后一个需要加试剂的比色皿了
                if (lastNeedDripReagent(cuvettePos)) {
                    i("加试剂完成")
                } else if (allowTakeReagent) {
                    //取试剂，移动比色皿
                    takeReagent()
                }
//                if (!allowTakeReagent) {//如果是取试剂失败，就不需要再取试剂了，检测完直接结束
//                    if (lastNeedTest1(cuvettePos-1)) {
//                        stepTest(TestState.Test2)
//                    } else {
//                        i("nextDripReagent 未知的")
//                    }
//
//                } else
                if (cuvettePos >= 5 && lastNeedTest1(cuvettePos - 5)) {
                    stepTest(TestState.Test2)
                } else {
                    moveCuvetteDripReagent()
                }
            }
        } else {
            i("cuvettePos >= 15 $cuvettePos")
        }
    }

    /**
     *
     * 从其他步骤切换到检测步骤
     * 除了 TestState.Test1
     * @param state TestState 要切换到的状态
     */
    private fun stepTest(state: TestState) {
        i(" ———————— stepTest state=$state  cuvettePos=$cuvettePos————————————————————————————————————————————————————————————————————————————————————————————————=")
        appViewModel.testState = state
        //获取跳过的步数
        val needMoveStep = getFirstCuvetteStartPos()
        //切换到TestState.Test2时，当前下位机的比色皿位置cuvettePos == 0，则不需要计算需要回退多少格，直接往前移动
        if (state == TestState.Test2) {
            i("stepTest cuvettePos=$cuvettePos needMoveStep=$needMoveStep")
            if (needMoveStep > -1) {//需要跳过
                cuvettePos = -1
                moveCuvetteTest(needMoveStep + 2)
            } else {//不需要跳过
                cuvettePos = getNextStepCuvetteStartPos()
                i("stepTest cuvettePos=$cuvettePos needMoveStep=$needMoveStep")
                moveCuvetteTest()
            }
        } else {
            //如果 nextStartPos > -1 代表要跳过，更新比色皿状态
            i("stepTest cuvettePos=$cuvettePos needMoveStep=$needMoveStep $cuvetteStartPos")
            if (needMoveStep > -1) {
                if (isFirstCuvetteShelf()) {
                    if (cuvetteStartPos > 0) {
                        repeat(cuvetteStartPos) {
                            updateCuvetteState(it, CuvetteState.Skip, null, null)
                        }
                    }
                }
            }
            //获取第一个需要检测的下标，计算当前位置到该位置的下标
            val prevState = if (state == TestState.Test3) CuvetteState.Test2 else CuvetteState.Test3
            val firstIndex =
                mCuvetteStates[cuvetteShelfPos]!!.indexOfFirst { it.state == prevState }
            moveCuvetteTest(firstIndex - cuvettePos)
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
        return (lastNeed(pos, CuvetteState.Stir) && lastNeed(
            pos, CuvetteState.DripSample
        ) && lastNeed(pos, CuvetteState.DripReagent)) || takeReagentFailedBeforeFinish(pos)
    }

    /**
     * 判断如果有取试剂失败的，那取试剂失败之前的是否都检测过第一次了
     */
    private fun takeReagentFailedBeforeFinish(pos: Int): Boolean {
        if (allowTakeReagent) {
            return false
        }
        if (pos > mCuvetteStates[cuvetteShelfPos]!!.size) {
            return true
        }
        val takeReagentFailedIndex =
            mCuvetteStates[cuvetteShelfPos]!!.indexOfFirst { it.state == CuvetteState.TakeReagentFailed }
        if (takeReagentFailedIndex <= 1) {
            return true
        }
        for (i in takeReagentFailedIndex - 1 downTo 0) {
            if (mCuvetteStates[cuvetteShelfPos]!![i].state != CuvetteState.Test1) {
                return false
            }
        }
        return true
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
            return true
        }
        for (i in pos + 1 until mCuvetteStates[cuvetteShelfPos]!!.size) {
            if (mCuvetteStates[cuvetteShelfPos]!![i].state == state) {
                return false
            }
        }
        return true
    }

    /**
     * 获取最后一个状态和 state 一样的index
     * @param state CuvetteState
     * @return Int
     */
    private fun getLastNeedIndex(state: CuvetteState): Int {
        for (i in mCuvetteStates[cuvetteShelfPos]!!.size - 1 downTo 0) {
            if (mCuvetteStates[cuvetteShelfPos]!![i].state == state) {
                return i
            }
        }
        return -1
    }

    /**
     * 接收到搅拌
     */
    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 搅拌 reply=$reply cuvettePos=$cuvettePos")

        updateStirTime()
        stirFinish = true
        updateCuvetteState(cuvettePos - 2, CuvetteState.Stir)
        stirProbeCleaning()
    }

    /**
     * 更新比色皿的搅拌时间
     */
    private fun updateStirTime() {
        if (cuvettePos < 2 || cuvettePos > 12) return
        stirTimes[cuvettePos - 2] = Date().time
    }

    /**
     * 接收到加试剂
     */
    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 加试剂 reply=$reply cuvettePos=$cuvettePos")

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
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 加样 reply=$reply cuvettePos=$cuvettePos samplePos=$samplePos")

        dripSampleFinish = true
        viewModelScope.launch {
            val result = createResultModel(
                scanResults[samplePos - 1], mSamplesStates[sampleShelfPos]?.get(samplePos - 1)
            )
            if (reply.state == ReplyState.CUVETTE_NOT_EMPTY) {//比色皿非空，不加样了
                allowDripSample = false
                updateCuvetteState(
                    cuvettePos,
                    CuvetteState.CuvetteNotEmpty,
                    result,
                    "${sampleShelfPos + 1}- $samplePos"
                )
            } else {//正常加样，继续
                updateCuvetteState(
                    cuvettePos, CuvetteState.DripSample, result, "${sampleShelfPos + 1}- $samplePos"
                )
            }
            updateSampleState(
                samplePos - 1, null, null, result, "${cuvetteShelfPos + 1}- ${cuvettePos + 1}"
            )
            samplingProbeCleaning()

            nextStepDripReagent()
        }
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

        i("piercedFinish=$piercedFinish scanFinish=$scanFinish samplingFinish=$samplingFinish dripSampleFinish=$dripSampleFinish cuvettePos=$cuvettePos samplePos=$samplePos")
        if (!allowDripSample) {
            i("这排最后一个比色皿，需要去下一个步骤，加试剂")
            val dripSampleCuvetteNum = getDripSampleCuvetteNum()
            if (dripSampleCuvetteNum == 0) {//没有已经加好样的就直接结束
                testFinishAction()
            } else {//有加好样的继续检测
                stepDripReagent()
            }
            return
        }
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
                i("这排最后一个比色皿，需要去下一个步骤，加试剂")
                stepDripReagent()
            } else if ((isAuto() && lastSamplePos(samplePos)) || !isAuto()) {//这排最后一个样本
                if (isManual() && manualModelSamplingFinish()) {
                    i("手动模式，样本加样完成！")
                    stepDripReagent()
                } else if (lastSampleShelf(sampleShelfPos)) {//最后一排
                    //已经加完了最后一个样本了，加样结束，去下一个步骤，加试剂
                    i("样本加样完成！")
                    if (shelfNeedDripReagent()) {
                        stepDripReagent()
                    } else {
                        i("不需要加试剂,检测结束！")
                        testFinishAction()
                    }
                } else {
                    //这排样本已经取完样了，移动到下一排接着取样
                    i("这排样本已经取完样了，移动到下一排接着取样")
                    moveNextSampleAndCuvette()
                }
            } else {
                i("比色皿和样本都还有，继续")
                //比色皿和样本都还有，继续
                moveNextSampleAndCuvette()
            }
        }
    }

    /**
     * 获取当前比色皿架已经加好样的比色皿的数量
     */
    private fun getDripSampleCuvetteNum(): Int {
        if (cuvetteShelfPos !in mCuvetteStates.indices) {
            return 0
        }
        return mCuvetteStates[cuvetteShelfPos]!!.filter { it.state == CuvetteState.DripSample }.size
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
        i(" ———————— stepDripReagent appViewModel.testState=$appViewModel.testState ————————————————————————————————————————————————————————————————————————————————————————————————=")
        appViewModel.testState = TestState.DripReagent
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
        i("cuvetteNeedMove cuvetteShelfPos=$cuvetteShelfPos cuvettePos=$cuvettePos")
        return cuvettePos >= 0 && (mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state == CuvetteState.DripSample || mCuvetteStates[cuvetteShelfPos]!![cuvettePos].state == CuvetteState.Skip)
    }


    /**
     * 接收到取样
     */
    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 取样 reply=$reply cuvettePos=$cuvettePos samplePos=$samplePos cuvetteShelfPos=$cuvetteShelfPos sampleShelfPos=$sampleShelfPos")
        samplingFinish = true
        samplingNum++
        if (reply.state == ReplyState.SAMPLING_FAILED) {//取样失败
            updateSampleState(samplePos - 1, SampleState.SamplingFailed)
            samplingProbeCleaning()
            nextStepDripReagent()
            return
        } else {//取样成功
            updateSampleState(samplePos - 1, SampleState.Sampling)
            goDripSample()
        }
    }


    /**
     * 接收到移动比色皿 检测位
     */
    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 移动比色皿 检测位 reply=$reply")

        cuvetteMoveFinish = true

        if (cuvettePos in 0 until 10) {
            val targetTime =
                if (appViewModel.testState == TestState.Test2) testShelfInterval2 else if (appViewModel.testState == TestState.Test3) testShelfInterval3 else testShelfInterval4
            val stirTime = stirTimes[cuvettePos]
            val intervalTemp = targetTime - (Date().time - stirTime) - 3
            i("intervalTemp=$intervalTemp stirTime=$stirTime targetTime=$targetTime")
            viewModelScope.launch {
                delay(intervalTemp)
                test()
            }
        }
    }

    /**
     * 接收到移动比色皿 加试剂位，搅拌位
     */
    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 移动比色皿 加试剂位 reply=$reply cuvettePos=$cuvettePos takeReagentFinish=$takeReagentFinish dripReagentFinish=$dripReagentFinish stirFinish=$stirFinish testFinish=$testFinish stirProbeCleaningFinish=$stirProbeCleaningFinish stirProbeCleaningRecoverStir=$stirProbeCleaningRecoverStir takeReagentFinish=$takeReagentFinish")

        cuvetteMoveFinish = true
        goDripReagent()

        goStir()

        goTest()

        if (takeReagentFinish && dripReagentFinish && stirFinish && testFinish) {//如果这一步，没有进行加样|搅拌|检测。又有需要加样|搅拌|检测的比色皿。说明还没到位置，继续移动
            if (mCuvetteStates[cuvetteShelfPos]?.filter { it.state == CuvetteState.DripReagent || it.state == CuvetteState.Stir }
                    ?.isNotEmpty() == true) {
                i("继续移动")
                viewModelScope.launch {
                    moveCuvetteDripReagent()
                }
            }
        }

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
        if (cuvettePos > 4 && cuvetteNeedTest1(cuvettePos - 5)) {
            testFinish = false //先置为未检测完成
            var testInterval = 0L
            //倒数第二个需要检测的
            val stirTime = stirTimes[cuvettePos - 5]
            testInterval = testShelfInterval1 - (Date().time - stirTime)
            i("goTest testInterval=$testInterval cuvettePos=$cuvettePos")
            viewModelScope.launch {
                delay(testInterval)
                test()
            }

        }
    }

    /**
     * 如果这排只有一个比色皿
     * @return Boolean
     */
    private fun cuvetteOnlyOne(): Boolean {
        var count = 0
        for (i in 0 until mCuvetteStates[cuvetteShelfPos]!!.size) {
            if (mCuvetteStates[cuvetteShelfPos]!![i].state == CuvetteState.None || mCuvetteStates[cuvetteShelfPos]!![i].state == CuvetteState.Skip) {
                count++
            }
        }
        return count == 9
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
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 移动比色皿 加样位 reply=$reply cuvetteStartPos=$cuvetteStartPos cuvettePos=$cuvettePos samplingFinish=$samplingFinish")
        cuvetteMoveFinish = true
        goDripSample()
    }


    /**
     * 停止运行
     */
    private fun stateErrorStopRunning() {
        appViewModel.testState = TestState.RunningError
    }

    /**
     * 接收到移动比色皿架
     */
    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        if (appViewModel.testState == TestState.TestFinish && testType.isTest()) {
            cuvetteShelfMoveFinish = true
            if (isTestFinish()) {
                showFinishDialog()
                openAllDoor()
            }
        }
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return
        c("接收到 移动比色皿架 reply=$reply cuvetteShelfPos=$cuvetteShelfPos cuvetteStartPos=$cuvetteStartPos")
        cuvetteShelfMoveFinish = true


        if (appViewModel.testState != TestState.TestFinish) {
            if (isManualSampling()) {//手动加样模式，不需要自动加样，直接设为已加样后加加试剂
                initManualSamplingCuvetteStatus()
                stepDripReagent()
            } else {//自动加样模式，去取样、加样
                val needMoveStep = getFirstCuvetteStartPos()
                if (needMoveStep > -1) {
                    moveCuvetteDripSample(needMoveStep + 2)
                } else {
                    moveCuvetteDripSample()
                }
            }
        }
    }

    /**
     * 初始化手动加样的比色皿状态，设为已加样
     */
    private fun initManualSamplingCuvetteStatus() {
        i("initManualSamplingCuvetteStatus cuvetteStartPos=$cuvetteStartPos")
        val need = needSamplingNum
        viewModelScope.launch {
            for (i in cuvetteStartPos until (mCuvetteStates[cuvetteShelfPos]?.size ?: 0)) {
                if (needSamplingNum > 0) {
                    mCuvetteStates[cuvetteShelfPos]?.get(i)?.state = CuvetteState.DripSample
                    needSamplingNum--
                    val result = createResultModel(
                        "", null
                    )
                    updateCuvetteState(
                        i, CuvetteState.DripSample, result, ""
                    )

                }
            }
        }
        i("needSamplingNum=$needSamplingNum")
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
    fun showFinishDialog() {
        var dialogMsg = getFinishDialogMsg()

        viewModelScope.launch {
            _dialogUiState.emit(HomeDialogUiState.TestFinish(dialogMsg))
        }
        appViewModel.testState = TestState.Normal
    }

    /**
     * 获取检测结束的问题提示
     */
    private fun getFinishDialogMsg(): String {
        var dialogMsg = ""
        if (!allowDripSample || !allowTakeReagent) {
            dialogMsg = "请检查"
        }
        if (!allowDripSample) {
            dialogMsg = dialogMsg.plus("比色皿非空")
        }
        if (!allowTakeReagent) {
            if (dialogMsg.length > 3) {
                dialogMsg = dialogMsg.plus(",")
            }
            dialogMsg = dialogMsg.plus("R2试剂是否存在")
        }
        return dialogMsg
    }

    private fun isTestFinish(): Boolean {
        return appViewModel.testState == TestState.TestFinish && testType.isTest() && sampleShelfMoveFinish && cuvetteShelfMoveFinish
    }


    /**
     * 接收到移动样本架
     */
    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        c("接收到 移动样本架 reply=$reply sampleShelfPos=$sampleShelfPos $appViewModel.testState")
        if (appViewModel.testState == TestState.TestFinish && testType.isTest()) {
            sampleShelfMoveFinish = true
            if (isTestFinish()) {
                showFinishDialog()
                openAllDoor()
            }
        }
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return

        sampleShelfMoveFinish = true

        if (appViewModel.testState != TestState.TestFinish) {
            samplePos = -1
            scanResults.clear()
//            samplesStates = initSampleStates()
            moveSample()
        }
    }

    private fun openAllDoor() {
        openSampleDoor()
        openCuvetteDoor()
    }

    /**
     * 发送 开样本仓门
     */
    private fun openSampleDoor() {
        c("发送 开样本仓门")
        SerialPortUtil.openSampleDoor()
    }

    /**
     * 发送 开比色皿仓门
     */
    private fun openCuvetteDoor() {
        c("发送 开比色皿仓门")
        SerialPortUtil.openCuvetteDoor()
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
            i("样本取样结束")
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
        i("moveSampleShelfNext sampleShelfPos=$sampleShelfPos oldPos=$oldPos")
    }

    /**
     * 显示样本不足的对话框
     */
    private fun showTestSampleDeficiencyDialog() {
        moveSampleShelf(sampleShelfPos)
    }

    var errorInfo: MutableList<ErrorInfo>? = null

    /**
     * 接收到自检
     * @param reply ReplyModel<GetMachineStateModel>
     */
    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        c("接收到 自检 reply=$reply")
        if (testType.isDebug()) {
            return
        }
        EventBus.getDefault().post(EventMsg<Any>(what = EventGlobal.WHAT_HIDE_SPLASH))
        errorInfo = reply.data.errorInfo
        appViewModel.testState = TestState.None
        if (errorInfo.isNullOrEmpty()) {
            appViewModel.testState = TestState.Normal
            //自检成功后获取一下r1,r2，清洗液状态
            getState()
        } else {
            viewModelScope.launch {
                _dialogUiState.emit(HomeDialogUiState.GetMachineDismiss)
            }
            appViewModel.testState = TestState.NotGetMachineState
            val sb = StringBuffer()
            errorInfo?.let {
                for (error in it) {
                    sb.append(error.errorMsg)
                    sb.append(error.motorMsg)
                    sb.append("\n")
                }
            }
            i("自检失败，错误信息=${sb}")
            viewModelScope.launch {
                _dialogUiState.emit(
                    HomeDialogUiState.GetMachineFailedShow(sb.toString())
                )
            }
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
        c("接收到 获取状态 appViewModel.testState=$appViewModel.testState reply=$reply continueTestCuvetteState=$continueTestCuvetteState continueTestSampleState=$continueTestSampleState clickStart=$clickStart r1Reagent=$r1Reagent r2Reagent=$r2Reagent cleanoutFluid=$cleanoutFluid continueTestGetState=$continueTestGetState")
        if (appViewModel.testState == TestState.Normal) {
            i("自检完成")
            viewModelScope.launch {
                _dialogUiState.emit(HomeDialogUiState.GetMachineDismiss)
            }
        }
        if (!runningTest()) return
        if (appViewModel.testState.isNotPrepare()) return

//        c("接收到 获取状态 reply=$reply continueTestCuvetteState=$continueTestCuvetteState continueTestSampleState=$continueTestSampleState clickStart=$clickStart r1Reagent=$r1Reagent r2Reagent=$r2Reagent cleanoutFluid=$cleanoutFluid continueTestGetState=$continueTestGetState")
        //比色皿不足才获取的状态
        //如果还是没有比色皿，继续弹框，如果有就移动比色皿架，继续检测
        if (continueTestCuvetteState) {
            cuvetteShelfStates = reply.data.cuvetteShelfs
            cuvetteShelfPos = -1
            getInitCuvetteShelfPos()
            if (cuvetteShelfPos == -1) {
                i("没有比色皿架")
//                cuvetteDeficiency = true
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
                i("没有样本架")
                viewModelScope.launch {
                    _dialogUiState.emit(
                        HomeDialogUiState.GetStateNotExist("样本不足，请添加")
                    )
                }
                return
            }
            continueTestSampleState = false
            moveSampleShelf(sampleShelfPos)
            return
        }
//        //开始下一排时R1|R2试剂不足|清洗液不足 才获取的状态
        if (continueTestGetState) {
            continueTestGetState = false
            //还有比色皿。继续移动比色皿，检测
            continueTestNextCuvette()
            return
        }
        cuvetteShelfStates = reply.data.cuvetteShelfs
        sampleShelfStates = reply.data.sampleShelfs


        getInitialPos()
        i("cuvetteShelfPos=${cuvetteShelfPos} sampleShelfPos=${sampleShelfPos}")
        if (cuvetteShelfPos == -1) {
            i("没有比色皿架")
            viewModelScope.launch {
                _dialogUiState.emit(
                    HomeDialogUiState.GetStateNotExist("比色皿不足，请添加")
                )
            }
            return
        }
        if (!isManualSampling()) {
            if (sampleShelfPos == -1) {
                i("没有样本架")
                viewModelScope.launch {
                    _dialogUiState.emit(
                        HomeDialogUiState.GetStateNotExist("样本不足，请添加")
                    )
                }
                return
            }
        }

        checkTestState(accord = {
            //开始检测前先清洗取样针
            cleaningBeforeStartTest = true
            samplingProbeCleaning()
        }, discrepancy = { str ->
            viewModelScope.launch {
                _dialogUiState.emit(
                    HomeDialogUiState.GetStateNotExist(str)
                )
            }
            return@checkTestState
        })
    }


    /**
     * 移动到下一排 第一次的时候不能调用，因为在只有一排时，会直接显示样本不足
     */
    private fun moveCuvetteShelfNext() {
        if (cuvetteShelfPos == lastCuvetteShelfPos) {
            i("已经是最后一排比色皿了")
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
        _samplesStates.value = mSamplesStates
        i("getInitSampleShelfPos sampleShelfPos=$sampleShelfPos lastSampleShelfPos=$lastSampleShelfPos")
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
//        cuvetteStates.postValue(mCuvetteStates)
        _cuvetteStates.value = mCuvetteStates
//        _testUiState.update {
//            it.copy(cuvetteStates = mCuvetteStates)
//        }
        i("getInitCuvetteShelfPos cuvetteShelfPos=$cuvetteShelfPos lastCuvetteShelfPos=$lastCuvetteShelfPos")
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
        return -1
    }


    /**
     * 执行检测结束动作
     */
    private fun testFinishAction() {
        clearSingleShelf()
        appViewModel.testState = TestState.TestFinish
        sampleShelfPos = -1
        cuvetteShelfPos = -1
        cuvetteStartPos = 0
        cuvettePos = -1
        samplePos = -1
        moveCuvetteShelf(cuvetteShelfPos)
        moveSampleShelf(sampleShelfPos)
    }


    /**
     * 对话框
     * 自检失败
     * 点击重新自检
     */
    fun dialogGetMachineFailedConfirm() {
        i("dialogGetMachineFailedConfirm 点击重新自检")
        goGetMachineState()
    }

    /**
     * 对话框
     * 自检失败
     * 点击我知道了
     */
    fun dialogGetMachineFailedCancel() {
        i("dialogGetMachineFailedCancel 点击我知道了")
    }


    /**
     * 检测结束
     * 比色皿不足
     * 点击继续检测
     */
    fun dialogTestFinishCuvetteDeficiencyConfirm() {
        i("dialogTestFinishCuvetteDeficiencyConfirm 点击继续检测")
        continueTestCuvetteState = true
        getState()
    }

    /**
     * 检测结束
     * 比色皿不足
     * 点击结束检测
     */
    fun dialogTestFinishCuvetteDeficiencyCancel() {
        i("dialogTestFinishCuvetteDeficiencyCancel 点击结束检测")

        testFinishAction()
    }

    /**
     * 开始检测
     * 比色皿,样本，试剂不足
     * 点击我已添加，继续检测
     */
    fun dialogGetStateNotExistConfirm() {
        i("dialogGetStateNotExistConfirm 点击我已添加，继续检测")
        getState()
    }

    /**
     * 开始检测
     * 比色皿,样本，试剂不足
     * 点击结束检测
     */
    fun dialogGetStateNotExistCancel() {
        i("dialogGetStateNotExistCancel 点击结束检测")
        testFinishAction()
    }

    /**
     * 获取样本架，比色皿架状态，试剂，清洗液状态
     */

    private fun getState() {
        c("发送 获取状态")
        SerialPortUtil.getState()
    }

    /**
     * 自检
     */
    private fun getMachineState() {
        c("发送 自检")
        SerialPortUtil.getMachineState()
    }

    /**
     * 移动样本架
     * @param pos Int
     */
    private fun moveSampleShelf(pos: Int) {
        c("发送 移动样本架 pos=$pos ")
        sampleShelfMoveFinish = false
        SerialPortUtil.moveSampleShelf(pos + 1)
    }

    /**
     * 移动比色皿架
     * @param pos Int
     */
    private fun moveCuvetteShelf(pos: Int) {
        c("发送 移动比色皿架 pos=$pos ")
        cuvetteShelfMoveFinish = false
        SerialPortUtil.moveCuvetteShelf(pos + 1)
    }

    /**
     * 移动样本
     * @param step Int
     */
    private fun moveSample(step: Int = 1) {
        c("发送 移动样本 step=$step samplePos=$samplePos")
        sampleMoveFinish = false
        samplePos += step
        SerialPortUtil.moveSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 滴样位
     */
    private fun moveCuvetteDripSample(step: Int = 1) {
        c("发送 移动比色皿到 加样位 step=$step cuvettePos=$cuvettePos")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.moveCuvetteDripSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 加试剂位
     */
    private fun moveCuvetteDripReagent(step: Int = 1) {
        c("发送 移动比色皿到 加试剂位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.moveCuvetteDripReagent(step > 0, step.absoluteValue)
    }


    /**
     * 移动比色皿到 检测位
     */
    private fun moveCuvetteTest(step: Int = 1) {
        c("发送 移动比色皿到 检测位 appViewModel.testState=$appViewModel.testState step=$step")
        cuvettePos += step
        testFinish = false
        testing = true
        SerialPortUtil.moveCuvetteTest(step > 0, step.absoluteValue)
    }

    /**
     * 取样
     */
    private fun sampling() {
        val type =
            mSamplesStates.get(sampleShelfPos)?.get(samplePos - 1)?.sampleType ?: SampleType.CUVETTE
        c("发送 取样 type=$type")
        samplingFinish = false
        dripSampleFinish = false
        SerialPortUtil.sampling(sampleType = type)
    }

    /**
     * 挤压
     * 比色杯不需要挤压（下位机收到不需要挤压会直接返回）。样本管才需要
     */
    private fun squeezing(enable: Boolean = true) {
        c("发送 挤压 enable=$enable")
        SerialPortUtil.squeezing(enable)
    }

    /**
     * 取试剂
     */
    private fun takeReagent() {
        c("发送 取试剂")
        if (allowTakeReagent) {
            takeReagentFinish = false
            dripReagentFinish = false
            SerialPortUtil.takeReagent()
        }
    }

    /**
     * 取样针清洗
     */
    private fun samplingProbeCleaning() {
        c("发送 取样针清洗")
        samplingProbeCleaningFinish = false
        SerialPortUtil.samplingProbeCleaning()
    }

    /**
     * 搅拌
     */
    private fun stir() {
        c("发送 搅拌")
        stirFinish = false
        SerialPortUtil.stir()
    }

    /**
     * 搅拌针清洗
     */
    private fun stirProbeCleaning() {
        c("发送 搅拌针清洗")
        stirProbeCleaningFinish = false
        SerialPortUtil.stirProbeCleaning()
    }

    /**
     * 加样
     */
    private fun dripSample() {
        c("发送 加样")
        dripSampleFinish = false
        SerialPortUtil.dripSample()
    }

    /**
     * 加试剂
     */
    private fun dripReagent() {
        c("发送 加试剂")
        dripReagentFinish = false
        SerialPortUtil.dripReagent()
    }

    /**
     * 检测
     */
    private fun test() {
        c("发送 检测 $cuvettePos")
        testFinish = false
        SerialPortUtil.test()
    }

    /**
     * 是否正在检测
     */
    private fun runningTest(): Boolean {
        return appViewModel.testState.isRunning() && testType.isTest()
    }

    /**
     * 更改了配置
     * @param curveModel ProjectModel
     * @param skipNum Int
     * @param detectionNum String
     * @param sampleNum Int
     */
    fun changeConfig(
        curveModel: CurveModel,
        skipNum: Int,
        detectionNum: String,
        sampleNum: Int,
    ) {
        selectProject = curveModel
        detectionNumInput = detectionNum
        cuvetteStartPos = skipNum
        needSamplingNum = sampleNum

        selectProject?.let {
            LocalData.SelectProjectID = it.curveId
        }
        i("changeConfig project=$curveModel")
    }

    /**
     * 控制开始检测时按钮不可用
     * @param enable Boolean
     */
    fun enableView(enable: Boolean) {
        configViewEnable.postValue(enable)
    }

    /**
     * 恢复上次选择的项目
     */
    fun recoverSelectProject(projects: MutableList<CurveModel>) {
        val selectId = LocalData.SelectProjectID
        val oldSelect = projects.filter { it.curveId == selectId }
        if (projects.isNotEmpty()) {
            if (oldSelect.isNotEmpty()) {//如果已经有选中的，就恢复
                selectProject = oldSelect.first()
            } else {//如果选中的不在里，就
                selectProject = projects.first()
            }
        }
    }

    /**
     * 获取曲线参数
     */
    suspend fun getCurveModels(): List<CurveModel> {
        return curveRepository.getCurveModels()
    }

    /**
     * 曲线更新了，选中最新的曲线
     */
    suspend fun selectLastProject() {
        var projects = getCurveModels()
        LocalData.SelectProjectID = projects.first().curveId
        recoverSelectProject(projects.toMutableList())
    }


    data class CuvetteItem(
        var state: CuvetteState, var testResult: TestResultModel? = null, var sampleID: String? = ""
    )

    data class SampleItem(
        var state: SampleState,
        var testResult: TestResultModel? = null,
        var cuvetteID: String? = "",
        var sampleType: SampleType? = null
    )
}

class HomeViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val curveRepository: CurveRepository = CurveRepository(),
    private val testResultRepository: TestResultRepository = TestResultRepository()
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(appViewModel,projectRepository, curveRepository, testResultRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
