package com.wl.turbidimetric.matchingargs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.calcAbsorbance
import com.wl.turbidimetric.ex.calcAbsorbanceDifferences
import com.wl.turbidimetric.ex.isSample
import com.wl.turbidimetric.ex.machineStateNormal
import com.wl.turbidimetric.ex.matchingArg
import com.wl.turbidimetric.ex.nds
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.global.SystemGlobal.testType
import com.wl.turbidimetric.home.CurveRepository
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.CuvetteDoorModel
import com.wl.turbidimetric.model.CuvetteState
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
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.ReplyModel
import com.wl.turbidimetric.model.ReplyState
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
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.model.TestType
import com.wl.turbidimetric.model.convertReplyState
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.CurveFitterUtil
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wllib.LogToFile.i
import com.wl.wllib.toTimeStr
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.math.BigDecimal
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * 曲线拟合和质控
 *
 * @property quality Boolean
 */
class MatchingArgsViewModel(
    private val projectRepository: ProjectRepository, private val curveRepository: CurveRepository
) : BaseViewModel(), Callback2 {

    init {
        listener()
    }

    private fun listener() {
        SerialPortUtil.callback.add(this)
        viewModelScope.launch {
            projectRepository.getProjects().collectLatest {
                projects.clear()
                projects.addAll(it)
            }
        }
    }

    /**
     * 序号、质控是否可输入
     */
    val configEnable = MutableLiveData(true)

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
    private var result = arrayListOf<BigDecimal>()

    /**
     * 加稀释液的量
     */
    private val dripDiluentVolumes = arrayListOf(300, 300, 300)

    /**
     * 加标准品的量
     */
    private val dripStandardVolumes = arrayListOf(300, 200, 100)

    /**
     * 移动已混匀的样本的下标顺序
     * 由样本架的样本移动到比色皿
     * 样本  比色皿
     * 0        0
     * 1000     50
     * 500      200
     * 200      500
     * 50       1000
     * qc(l)    qc(l)
     * qc(h)    qc(h)
     */
    private val moveBlendingPos = arrayListOf(0, 5, 4, 3, 2, 6, 7)

    /**
     * 是否要同时质控
     */
    var quality: Boolean = false

    /**
     * 当前取样的步骤
     */
    private var sampleStep: Int = 0

    /**
     * 取样步骤最大需要步数
     */
    private var sampleStepMax = 3

    /**比色皿架是否移动完毕
     *
     */
    private var cuvetteShelfMoveFinish = false

    /**样本架是否移动完毕
     *
     */
    private var sampleShelfMoveFinish = false

    /**
     * 当前排所有比色皿的状态
     */
    private var cuvetteStates = initCuvetteStates()

    /**
     * 当前样本架位置
     */
    private var sampleShelfPos = -1

    /**
     * 最后一排可使用的样本架的位置
     */
    private var lastSampleShelfPos = -1

    /**
     * 当前比色皿架位置
     */
    private var cuvetteShelfPos = -1

    /**
     * 最后一排可使用的比色皿架的位置
     */
    private var lastCuvetteShelfPos = -1

    /**
     * 当前样本位置
     * 0-10 一共11次。其中每个样本的刺破位隔挤压取样位一个位置，所以第0个位置只能用来刺破，第10个位置不能刺破，只能挤压取样
     */
    private var samplePos = -1

    /**
     * 当前比色皿位置
     */
    private var cuvettePos = -1

    /**
     * 比色皿是否移动完毕
     */
    private var cuvetteMoveFinish = false

    /**
     * 样本是否移动完毕
     */
    private var sampleMoveFinish = false

    /**
     * 取样是否完成
     */
    private var samplingFinish = false

    /**
     * 加样是否完成
     */
    private var dripSamplingFinish = false

    /**
     * 加试剂是否完成
     */
    private var dripReagentFinish = false

    /**
     * 取试剂是否完成
     */
    private var takeReagentFinish = false

    /**
     * 搅拌是否完成
     */
    private var stirFinish = true

    /**
     * 检测是否完成
     */
    private var testFinish = true

    /**
     * 搅拌针清洗是否完成
     */
    private var stirProbeCleaningFinish = true

    /**
     * 样本架状态 1有 0无 顺序是从中间往旁边
     */
    private var sampleShelfStates: IntArray = IntArray(4)

    /**
     * 比色皿架状态 1有 0无 顺序是从中间往旁边
     */
    private var cuvetteShelfStates: IntArray = IntArray(4)

    private val _dialogUiState = MutableSharedFlow<MatchingArgsDialogUiState>()
    val dialogUiState: SharedFlow<MatchingArgsDialogUiState> = _dialogUiState.asSharedFlow()

    private val _curveUiState = MutableStateFlow(MatchingArgsCurveUiState("", ""))
    val curveUiState = _curveUiState.asStateFlow()

    val testMsg = MutableLiveData("")


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
     * 试剂序号
     */
    var reagentNOStr = ""

    /**
     * 要覆盖的项目  只有在项目超过最大限制时使用
     */
    var coverCurveModel: CurveModel? = null

    /**
     * 当前选择的项目参数
     */
    var selectProject: CurveModel? = null

    /**
     * 当前拟合好的项目参数
     */
    var curProject: CurveModel? = null

    /**
     * 意外停止的编号
     *
     * 比如取样失败，加试剂失败、是采便管等
     */
    var accidentState = ReplyState.SUCCESS

    /**
     * 开始检测前的清洗取样针
     */
    var cleaningBeforeStartTest = false

    /**
     * 记录每个比色皿的搅拌时间
     */
    val stirTimes = longArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /**
     * 全部可选择的项目
     */
    var projects = mutableListOf<ProjectModel>()

    /**
     * 是否自动稀释
     */
    var autoAttenuation = true

    /**
     * 拟合梯度数量
     */
    var matchingNum = 5

    /**
     * 选择用来拟合的项目
     */
    var selectMatchingProject: ProjectModel? = null

    /**
     * 选择用来拟合的方程
     */
    var selectFitterType: FitterType = FitterType.Three


    /**
     * 拟合梯度对应的浓度
     */
    var targetCons = mutableListOf<Int>()

    /**
     * 拟合后的浓度
     */
    var resultCons = mutableListOf<MutableList<Int>>()

    /**
     * 测试用的 start
     */
    private val testValues1 = doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.3, 0.3, 0.3)
    private val testValues2 = doubleArrayOf(0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3)
    private val testValues3 = doubleArrayOf(0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3)
    private val testValues4 =
        doubleArrayOf(0.9998, 1.0035, 1.0178, 1.0642, 1.1586, 1.0141, 1.0585, 1.0, 1.0, 1.0)
    private val testOriginalValues1 =
        intArrayOf(65532, 65532, 65532, 65532, 65532, 65532, 65532, 65532, 65532, 65532)
    private val testOriginalValues2 =
        intArrayOf(65520, 65520, 65520, 65520, 65520, 65520, 65520, 65520, 65520, 65520)
    private val testOriginalValues3 =
        intArrayOf(60000, 60000, 60000, 60000, 60000, 60000, 60000, 60000, 60000, 60000)
    private val testOriginalValues4 =
        intArrayOf(56000, 56000, 56000, 56000, 56000, 56000, 56000, 56000, 56000, 56000)

    //测试用 每排之间的检测间隔
    val testS: Long = 100

    //测试用 每个比色皿之间的检测间隔
    val testP: Long = 1000

    val datas = curveRepository.allDatas


    /**
     * 更新比色皿状态
     * @param pos Int
     * @param state CuvetteState
     */
    private fun updateCuvetteState(pos: Int, state: CuvetteState) {
        cuvetteStates[pos] = state
        i("updateCuvetteState pos=$pos state=$state cuvetteStates=$cuvetteStates")
    }

    /**
     * 重置比色皿状态
     * @return MutableList<CuvetteState>
     */
    private fun initCuvetteStates(): MutableList<CuvetteState> {
        val array = mutableListOf<CuvetteState>()
        for (i in 0 until 10) {
            array.add(CuvetteState.None)
        }
        return array
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
                        MatchingArgsDialogUiState(
                            dialogState = DialogState.StateFailed,
                            stateFailedText.plus("，请停止使用仪器并联系供应商维修")
                        )
                    )
                }
            }
        }
        return stateFailedText.isEmpty()
    }

    /**
     * 停止运行
     */
    private fun stateErrorStopRunning() {
        testState = TestState.RunningError
    }

    /**
     * 挤压
     * @param reply ReplyModel<SqueezingModel>
     */
    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 挤压 reply=$reply")
    }

    /**
     * 显示拟合配置对话框
     */
    fun showMatchingSettingsDialog() {
        viewModelScope.launch {
            _dialogUiState.emit(
                MatchingArgsDialogUiState(
                    dialogState = DialogState.MatchingSettings, msg = ""
                )
            )
        }
    }

    /**
     * 点击开始拟合
     */
    fun clickStart(curveModel: CurveModel?) {
        if (!machineStateNormal()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState(
                        dialogState = DialogState.Accident, msg = "请重新自检或重启仪器"
                    )
                )
            }
            return
        }
        if (testState != TestState.Normal) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState(
                        dialogState = DialogState.Accident, msg = "正在检测，请勿操作"
                    )
                )
            }
            return
        }

        if (reagentNOStr.isNullOrEmpty()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState(
                        dialogState = DialogState.Accident, msg = "请输入试剂序号"
                    )
                )
            }
            return
        }


        start(curveModel)
    }

    fun start(curveModel: CurveModel?) {
        this.coverCurveModel = curveModel
        initState()
        testState = TestState.GetState
        testType = TestType.MatchingArgs
        getState()
    }

    private fun initState() {
        curProject = null
        accidentState = ReplyState.SUCCESS
        resultTest1.clear()
        resultTest2.clear()
        resultTest3.clear()
        resultTest4.clear()
        resultOriginalTest1.clear()
        resultOriginalTest2.clear()
        resultOriginalTest3.clear()
        resultOriginalTest4.clear()
        testState = TestState.None
        sampleStep = 0
        testMsg.postValue("")
        samplePos = -1
        cuvettePos = -1
        sampleShelfPos = -1
        cuvetteShelfPos = -1

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
    }

    /**
     * 接收到自检
     * @param reply ReplyModel<GetMachineStateModel>
     */
    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        if (!runningMatching()) return
//        i("接收到 样本舱门状态 reply=$reply")
    }

    /**
     * 接收到获取状态
     * @param reply ReplyModel<GetStateModel>
     */
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 获取状态 reply=$reply")
        cuvetteShelfStates = reply.data.cuvetteShelfs
        sampleShelfStates = reply.data.sampleShelfs
        val r1Reagent = reply.data.r1Reagent
        val r2Reagent = reply.data.r2Reagent
        val cleanoutFluid = reply.data.cleanoutFluid

        getInitialPos()

        i("cuvetteShelfPos=${cuvetteShelfPos} sampleShelfPos=${sampleShelfPos}")
        val errorMsg = if (cuvetteShelfPos == -1) {
            i("没有比色皿架")
            "比色皿不足，请添加"
        } else if (sampleShelfPos == -1) {
            i("没有样本架")
            "样本不足，请添加"
        } else if (!r1Reagent) {
            i("没有R1试剂")
            "R1试剂不足，请添加"
        } else if (!r2Reagent) {
            i("没有R2试剂")
            "R2试剂不足，请添加"
        } else if (!cleanoutFluid) {
            i("没有清洗液试剂")
            "清洗液不足，请添加"
        } else {
            ""
        }
        if (errorMsg.isNotEmpty()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState(
                        DialogState.GetStateNotExistMsg, errorMsg
                    )
                )
            }
            return
        }

        testState = TestState.DripDiluentVolume
        //开始检测
//        moveSampleShelf(sampleShelfPos)
        cleaningBeforeStartTest = true
        samplingProbeCleaning()

//        moveCuvetteShelf(cuvetteShelfPos)
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
        for (i in sampleShelfStates.indices) {
            if (sampleShelfStates[i] == 1) {
                if (sampleShelfPos == -1) {
                    sampleShelfPos = i
                }
                lastSampleShelfPos = i
            }
        }
        i("getInitSampleShelfPos sampleShelfPos=$sampleShelfPos lastSampleShelfPos=$lastSampleShelfPos")
    }

    /**
     * 获取比色皿架初始位置和最后一排的位置
     */
    private fun getInitCuvetteShelfPos() {
        for (i in cuvetteShelfStates.indices) {
            if (cuvetteShelfStates[i] == 1) {
                if (cuvetteShelfPos == -1) {
                    cuvetteShelfPos = i
                }
                lastCuvetteShelfPos = i
            }
        }
        i("getInitCuvetteShelfPos cuvetteShelfPos=$cuvetteShelfPos lastCuvetteShelfPos=$lastCuvetteShelfPos")
    }

    /**
     * 接收到移动比色皿架
     * @param reply ReplyModel<MoveCuvetteShelfModel>
     */
    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        if (testState == TestState.TestFinish && testType.isMatchingArgs()) {
            cuvetteShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 移动比色皿架 reply=$reply")
        cuvettePos = 0
        when (testState) {
            TestState.MoveSample -> {
                moveCuvetteDripSample()
            }

            TestState.Test1 -> {

            }

            TestState.Test2, TestState.Test3, TestState.Test4 -> {
                moveCuvetteTest()
            }

            else -> {}
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
        i("发送 开样本仓门")
        SerialPortUtil.openSampleDoor()
    }

    /**
     * 发送 开比色皿仓门
     */
    private fun openCuvetteDoor() {
        i("发送 开比色皿仓门")
        SerialPortUtil.openCuvetteDoor()
    }

    /**
     * 获取样本仓门状态
     */
    private fun getSampleDoor() {
        i("发送 获取样本仓门状态")
        SerialPortUtil.getSampleDoorState()
    }

    private fun isMatchingFinish(): Boolean {
        return testState == TestState.TestFinish && cuvetteShelfMoveFinish && sampleShelfMoveFinish
    }

    /**
     * 已取样 在取|加稀释液时和取|加标准品使用
     * false时代表应该去取样了，反之加样
     */
    var sampled = false

    /**
     * 接收到移动样本
     * @param reply ReplyModel<MoveSampleModel>
     */
    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 移动样本 reply=$reply samplePos=$samplePos testState=$testState sampleStep=$sampleStep")
        sampleMoveFinish = true

        //如果是样本管，直接检测结束，并报错，因为质控不允许使用样本管
        if (reply.data.type.isSample()) {
            accidentState = ReplyState.ORDER
            matchingFinish()
            return
        }
        when (testState) {
            TestState.DripDiluentVolume, TestState.DripStandardVolume -> {//去取稀释液、标准品
                if (!sampled) {
                    sampleStep++
                    val volume = getSamplingVolume(testState, sampleStep - 1)
                    sampling(volume)

                } else {
                    val volume = getSamplingVolume(testState, sampleStep - 1)
                    dripSample(
                        testState == TestState.DripStandardVolume, inplace = true, volume
                    )
                }
                sampled = !sampled
            }

            TestState.MoveSample -> {//去取需要移动的已混匀的样本
                sampleStep++
                sampling(LocalData.SamplingVolume)
            }

            else -> {

            }
        }

    }

    /**
     * 获取当前位置取样加样的量
     * @param testState
     * @param step
     * @return Int
     */
    private fun getSamplingVolume(testState: TestState, step: Int): Int {
        return if (testState == TestState.DripDiluentVolume) {
            dripDiluentVolumes[step]
        } else {
            dripStandardVolumes[step]
        }
    }

    /**
     * 接收到移动比色皿到加样位
     * @param reply ReplyModel<MoveCuvetteDripSampleModel>
     */
    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 移动比色皿到加样位 reply=$reply cuvetteStates=$cuvetteStates")

        cuvetteMoveFinish = true

        goDripSample()
    }

    /**
     * 去比色皿加样
     */
    private fun goDripSample() {
        if (cuvetteMoveFinish && samplingFinish) {
            dripSample(autoBlending = false, inplace = false, LocalData.SamplingVolume)
        }
    }

    /**
     * 接收到移动比色皿到加试剂位
     * @param reply ReplyModel<MoveCuvetteDripReagentModel>
     */
    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 移动比色皿到加试剂位 reply=$reply cuvettePos=$cuvettePos stirFinish=$stirFinish takeReagentFinish=$takeReagentFinish cuvetteStates=$cuvetteStates")

        cuvetteMoveFinish = true

        goDripReagentAndStirAndTest()
    }

    /**
     * 开始加试剂，搅拌，检测
     */
    private fun goDripReagentAndStirAndTest() {
        if (cuvetteNeedDripReagent(cuvettePos) && cuvetteMoveFinish && takeReagentFinish) {
            dripReagent()
        }
        if (cuvettePos > 1 && cuvetteNeedStir(cuvettePos - 2) && stirFinish) {
            stir()
        }
        if (cuvettePos > 4 && cuvetteNeedTest(cuvettePos - 5)) {
            testFinish = false
            val stirTime = stirTimes[cuvettePos - 5]
            val testInterval = testShelfInterval1 - (Date().time - stirTime)
            i("goDripReagentAndStirAndTest testInterval=$testInterval stirTime=$stirTime")
            viewModelScope.launch {
                delay(testInterval)
                test()
            }
        }
    }

    /**
     * 加试剂，搅拌，检测结束后调用
     *
     * 判断是否这排结束，结束开始下一个步骤，检测第二次
     * 如果未结束，则继续移动比色皿，检测
     */
    private fun dripReagentAndStirAndTestFinish() {
        i("dripReagentAndStirAndTestFinish cuvettePos=$cuvettePos dripReagentFinish=$dripReagentFinish takeReagentFinish=$takeReagentFinish")
        if ((cuvetteNeedDripReagent(cuvettePos) && !dripReagentFinish)) {
            return
        }
        if (cuvettePos > 1 && ((cuvetteNeedStir(cuvettePos - 2) && !stirFinish) || !stirProbeCleaningFinish)) {
            return
        }
        if (cuvettePos > 4 && (cuvetteNeedTest(cuvettePos - 5) && !testFinish)) {
            return
        }

        if ((cuvettePos == 11 && quality) || (cuvettePos == 9 && !quality)) {
            //最后一个也检测结束了
            testState = TestState.Test2
            cuvettePos = -1
            moveCuvetteTest()
        } else {
            //如果没有结束，移动比色皿，判断是否需要加试剂
            moveCuvetteDripReagent()
            if (cuvetteNeedDripReagent(cuvettePos)) {
                takeReagent()
            }
        }
    }

    /**
     * 接收到移动样本架
     * @param reply ReplyModel<MoveSampleShelfModel>
     */
    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        i("接收到 移动样本架 reply=$reply testState=$testState")
        if (testState == TestState.TestFinish && testType.isMatchingArgs()) {
            sampleShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        sampleShelfMoveFinish = true
        if (testState != TestState.TestFinish) {
            //一开始就要移动到第二个位置去取第一个位置的稀释液
            moveSample(2)
        }
    }


    /**
     * 接收到检测
     * @param reply ReplyModel<TestModel>
     */
    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 检测 reply=$reply cuvettePos=$cuvettePos testState=$testState 检测值=${reply.data.value}")

        calcTestResult(reply.data.value)
    }

    private fun calcTestResult(value: Int) {
        when (testState) {
            TestState.DripReagent -> {
                updateCuvetteState(cuvettePos - 5, CuvetteState.Test1)
                resultTest1.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest1.add(value)
                updateResult()
                dripReagentAndStirAndTestFinish()
            }

            TestState.Test2 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test2)
                resultTest2.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest2.add(value)
                updateResult()
                //检测结束,开始检测第三次
                if ((cuvettePos == 4 && !quality) || (cuvettePos == 6 && quality)) {
                    testState = TestState.Test3

                    moveCuvetteTest(-cuvettePos)
                } else {
                    moveCuvetteTest()
                }

            }

            TestState.Test3 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test3)
                resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest3.add(value)
                updateResult()
                //检测结束,开始检测第四次
                if ((cuvettePos == 4 && !quality) || (cuvettePos == 6 && quality)) {
                    testState = TestState.Test4

                    moveCuvetteTest(-cuvettePos)
                } else {
                    moveCuvetteTest()
                }

            }

            TestState.Test4 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test4)
                resultTest4.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest4.add(value)
                updateResult()
                //检测结束,计算拟合参数
                if ((cuvettePos == 4 && !quality) || (cuvettePos == 6 && quality)) {
                    matchingFinish()
                    calcMatchingArg()
                } else {
                    moveCuvetteTest()
                }
            }

            else -> {}
        }


    }

    /**
     * 拟合结束，复位
     */
    private fun matchingFinish() {
        testState = TestState.TestFinish
        moveCuvetteShelf(-1)
        moveSampleShelf(-1)
    }

    /**
     * 测试用，更新每次检测的值
     */
    private fun updateResult() {
        testMsg.postValue(
            "第一次原始:$resultOriginalTest1 \n" + "第一次:$resultTest1 \n" + "第二次原始:$resultOriginalTest2 \n" + " 第二次:$resultTest2 \n" + "第三次原始:$resultOriginalTest3 \n" + " 第三次:$resultTest3 \n" + "第四次原始:$resultOriginalTest4 \n" + " 第四次:$resultTest4 \n"
        )
    }

    var absorbancys: List<Double> = mutableListOf()
    var yzs: List<Double> = mutableListOf()

    fun roundResult(): BigDecimal {
        return Random(Date().time).nextFloat().toBigDecimal()
    }

    /**
     * 计算拟合曲线
     */
    private fun calcMatchingArg() {
        if (SystemGlobal.isCodeDebug) {
            resultTest1.clear()
            resultTest2.clear()
            resultTest3.clear()
            resultTest4.clear()
            resultOriginalTest1.clear()
            resultOriginalTest2.clear()
            resultOriginalTest3.clear()
            resultOriginalTest4.clear()
            val size = if (quality) 7 else 5
            repeat(size) {
                resultTest1.add(roundResult())
                resultTest2.add(roundResult())
                resultTest3.add(roundResult())
                resultTest4.add(roundResult())

                resultOriginalTest1.add(testOriginalValues1[it])
                resultOriginalTest2.add(testOriginalValues2[it])
                resultOriginalTest3.add(testOriginalValues3[it])
                resultOriginalTest4.add(testOriginalValues4[it])
            }

        }
        i("开始计算拟合曲线 $resultTest1 $resultTest2 $resultTest3 $resultTest4")

        result = calcAbsorbanceDifferences(resultTest1, resultTest2, resultTest3, resultTest4)

        absorbancys = result.map { it.toDouble() }
        val cf = matchingArg(absorbancys)
        val res = cf.params
        for (i in res.indices) {
            println(res[i])
        }
        i("拟合度：${cf.fitGoodness}")
//        i("四参数：a1=${res[0]} a2=${res[3]} x0=${res[2]} p=${res[1]}")
        val f0 = res[0]
        val f1 = res[1]
        val f2 = res[2]
        val f3 = res[3]

        i("四参数：f0=${f0} f1=${f1} f2=${f2} f3=${f3}")

        yzs = absorbancys.map {
            val yz = CurveFitterUtil.f(res, it).scale(2)
            yz
        }
        //**修正start**
        var dif50 = 50 - yzs[1]
        println("50差值：$dif50")
        dif50 = if (dif50 >= 0) {
            dif50 * 2
        } else {
            dif50 / 2
        }
        println("50差值计算后：$dif50")

        val dif0 = absorbancys[0] - dif50
        println("0原始吸光度-50差值计算后：$dif0")

        val np1 = dif0
        var absorbancys2 = absorbancys.toMutableList()
        absorbancys2[0] = np1

        val cf2 = matchingArg(absorbancys2)
        val res2 = cf2.params
        val yzs2 = absorbancys2.map {
            val yz2 = CurveFitterUtil.f(res2, it).scale(2)
            yz2
        }

        //**修正end**
        var msg: StringBuilder = StringBuilder(
            "第一次原始:$resultOriginalTest1 \n" + "第一次:$resultTest1 \n" + "第二次原始:$resultOriginalTest2 \n" + "第二次:$resultTest2 \n" + "第三次原始:$resultOriginalTest3 \n" + "第三次:$resultTest3 \n" + "第四次原始:$resultOriginalTest4 \n" + "第四次:$resultTest4 \n" +
//                    "吸光度:$result \n" +
//                    "拟合度：${cf.fitGoodness} \n" +
//                    "四参数：f0=${f0} f1=${f1} f2=${f2} f3=${f3} \n " +
//                    "验算 ${yzs}\n" +
                    "吸光度:$absorbancys2 \n" + "拟合度：${cf2.fitGoodness} \n" + "四参数：f0=${cf2.params[0]} f1=${cf2.params[1]} f2=${cf2.params[2]} f3=${cf2.params[3]} \n" + "验算 ${yzs2}\n"
        )
        curProject = CurveModel().apply {
            this.f0 = cf2.params[0]
            this.f1 = cf2.params[1]
            this.f2 = cf2.params[2]
            this.f3 = cf2.params[3]
            this.fitGoodness = cf2.fitGoodness
            this.createTime = Date().toTimeStr()
            this.projectLjz = 100
            this.reagentNO = reagentNOStr
            this.reactionValues = absorbancys2.map { it.toInt() }.toIntArray()
            this.yzs = yzs2.map { it.toInt() }.toIntArray()
        }
//        print()
        testMsg.postValue(msg.toString())

        //添加到参数列表，刷新
//        projectRepository.addProject(project)
    }

    /**
     * 打印上次的
     */
    fun print() {
        selectProject?.let {
            if (it.reactionValues?.isNotEmpty() == true && it.yzs?.isNotEmpty() == true) {
                PrintUtil.printMatchingQuality(
                    it.reactionValues!!.toList(),
                    nds,
                    it.yzs!!.toList(),
                    mutableListOf(it.f0, it.f1, it.f2, it.f3),
                    it.createTime,
                    it.projectName,
                    it.reagentNO
                )
            }
        }
    }

    /**
     * 接收到搅拌
     * @param reply ReplyModel<StirModel>
     */
    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 搅拌 reply=$reply cuvettePos=$cuvettePos")
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
     * @param reply ReplyModel<DripReagentModel>
     */
    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 加试剂 reply=$reply cuvettePos=$cuvettePos")
        dripReagentFinish = true
        updateCuvetteState(cuvettePos, CuvetteState.DripReagent)

        dripReagentAndStirAndTestFinish()
    }

    /**
     * 接收到加样
     * @param reply ReplyModel<DripSampleModel>
     */
    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 加样 reply=$reply cuvettePos=$cuvettePos testState=$testState sampleStep=$sampleStep samplePos=$samplePos")

        dripSamplingFinish = true
        samplingFinish = false
        if (reply.state == ReplyState.CUVETTE_NOT_EMPTY) {//比色皿非空
            accidentState = ReplyState.CUVETTE_NOT_EMPTY
            matchingFinish()
            return
        }
        when (testState) {
            TestState.DripDiluentVolume -> {//加稀释液
                if (sampleStep == sampleStepMax) {
                    //已经加完三个了，清洗取样针，然后进行下一个步骤，取标准品
                    samplingProbeCleaning()
                } else {
                    moveSample(-samplePos + 1)
                }
            }

            TestState.DripStandardVolume -> {//加标准品
                samplingProbeCleaning()
            }

            TestState.MoveSample -> {//加已混匀的样本
                updateCuvetteState(cuvettePos - 1, CuvetteState.DripSample)
                samplingProbeCleaning()
            }

            else -> {}
        }

        i("加样 cuvetteStates=$cuvetteStates")
    }

    /**
     * 接收到取样
     * @param reply ReplyModel<SamplingModel>
     */
    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 取样 reply=$reply testState=$testState sampleStep=$sampleStep samplePos=$samplePos")

        samplingFinish = true
        if (reply.state == ReplyState.SAMPLING_FAILED) {//取样失败
            accidentState = ReplyState.SAMPLING_FAILED
            matchingFinish()
            return
        }
        when (testState) {
            TestState.DripDiluentVolume -> {//去加稀释液
                moveSample(sampleStep + 1)
            }

            TestState.DripStandardVolume -> {//去加标准品
                moveSample(1)
            }

            TestState.MoveSample -> {//去加已混匀的样本
                goDripSample()
            }

            else -> {}
        }
    }

    private fun cuvetteNeed(cuvettePos: Int, state: CuvetteState): Boolean {
        if (cuvettePos < 0 || cuvettePos >= cuvetteStates.size) return false
        return cuvetteStates[cuvettePos] == state
    }

    /**
     * 当前的比色皿位置是否需要加试剂
     * @param cuvettePos Int
     * @return Boolean
     */
    private fun cuvetteNeedDripReagent(cuvettePos: Int) =
        cuvetteNeed(cuvettePos, CuvetteState.DripSample)

    /**
     * 当前的比色皿位置是否需要搅拌
     * @param cuvettePos Int
     * @return Boolean
     */
    private fun cuvetteNeedStir(cuvettePos: Int) = cuvetteNeed(cuvettePos, CuvetteState.DripReagent)

    /**
     * 当前的比色皿位置是否需要检测
     * @param cuvettePos Int
     * @return Boolean
     */
    private fun cuvetteNeedTest(cuvettePos: Int) = cuvetteNeed(cuvettePos, CuvetteState.Stir)

    /**
     * 接收到移动比色皿到检测位
     * @param reply ReplyModel<MoveCuvetteTestModel>
     */
    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 移动比色皿到检测位 reply=$reply cuvetteStates=$cuvetteStates testState=$testState")

        when (testState) {
            TestState.Test2, TestState.Test3, TestState.Test4 -> {
                if (cuvettePos in 0 until 10) {
                    val targetTime =
                        if (testState == TestState.Test2) testShelfInterval2 else if (testState == TestState.Test3) testShelfInterval3 else testShelfInterval4
                    val stirTime = stirTimes[cuvettePos]
                    val intervalTemp = targetTime - (Date().time - stirTime) - 3
                    i("intervalTemp=$intervalTemp stirTime=$stirTime targetTime=$targetTime")
                    viewModelScope.launch {
                        delay(intervalTemp)
                        test()
                    }
                }
            }

            else -> {}
        }
    }

    /**
     * 接收到取试剂
     * @param reply ReplyModel<TakeReagentModel>
     */
    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 取试剂 reply=$reply cuvettePos=$cuvettePos cuvetteMoveFinish=$cuvetteMoveFinish")
        takeReagentFinish = true
        if (reply.state == ReplyState.TAKE_REAGENT_FAILED) {//取试剂失败
            accidentState = ReplyState.TAKE_REAGENT_FAILED
            matchingFinish()
            return
        }
        //取完试剂判断是否移动比色皿完成，完成就直接取样
        if (cuvetteNeedDripReagent(cuvettePos) && cuvetteMoveFinish && takeReagentFinish) {
            dripReagent()
        }
    }

    /**
     * 接收到取样针清洗
     * @param reply ReplyModel<SamplingProbeCleaningModel>
     */
    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 取样针清洗 reply=$reply samplePos=$samplePos sampleStep=$sampleStep")
        if (cleaningBeforeStartTest) {
            //是开始检测前的清洗，清洗完才开始检测
            cleaningBeforeStartTest = false
            moveSampleShelf(sampleShelfPos)
            return
        }
        if (testState == TestState.DripDiluentVolume) {//加完稀释液后的清洗
            sampleStep = 0
            testState = TestState.DripStandardVolume
            moveSample(-samplePos + 2)//直接到取标准品的位置
        } else if (testState == TestState.DripStandardVolume) {//加标准后的清洗
            if (sampleStep == sampleStepMax) {
                //开始移动已混匀样本的步骤
                //第一步、先复位样本和比色皿
                testState = TestState.MoveSample
                sampleStep = 0
                moveSample(-samplePos + 1)
                moveCuvetteShelf(cuvetteShelfPos)
            } else {//继续加标准品
                moveSample(0)
            }
        } else if (testState == TestState.MoveSample) {//加已混匀的样本的清洗
            if ((sampleStep == 5 && !quality) || (sampleStep == 7 && quality)) {
                //开始加试剂的步骤
                testState = TestState.DripReagent
                sampleStep = 0
                cuvettePos = -1
                moveSample(-samplePos + 1)
                moveCuvetteDripReagent()

                takeReagent()
            } else {//继续移动已混匀的样本
                i("sampleStep=$sampleStep")
                val targetIndex = moveBlendingPos[sampleStep]
                moveSample(targetIndex - samplePos)
                moveCuvetteDripSample()
            }
        }
    }


    /**
     * 接收到搅拌针清洗
     * @param reply ReplyModel<StirProbeCleaningModel>
     */
    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 搅拌针清洗 reply=$reply")
        stirProbeCleaningFinish = true
        dripReagentAndStirAndTestFinish()
    }


    /**
     * 接收到刺破
     * @param reply ReplyModel<PiercedModel>
     */
    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 刺破 reply=$reply")
    }

    /**
     * 接收到获取版本号
     * @param reply ReplyModel<GetVersionModel>
     */
    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        i("接收到 获取版本号 reply=$reply")
    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {
    }


    /**
     * 接收到 样本架锁状态
     * @param reply ReplyModel<SampleDoorModel>
     */
    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {


    }

    /**
     * 接收到 比色皿架锁状态
     * @param reply ReplyModel<CuvetteDoorModel>
     */
    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {

    }

    /**
     * 显示拟合结束对话框
     */
    private fun showMatchingDialog() {
        if (accidentState != ReplyState.SUCCESS) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState(
                        DialogState.Accident, getAccidentFinishDialog()
                    )
                )
            }
        } else {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState(
                        DialogState.MatchingFinishMsg, testMsg.value ?: ""
                    )
                )
            }
        }
        testState = TestState.Normal
    }

    /**
     * 当发生意外导致拟合结束时的提示信息
     */
    private fun getAccidentFinishDialog(): String {
        return when (accidentState) {
            ReplyState.SUCCESS -> {
                "拟合结束"
            }

            ReplyState.INVALID_PARAMETER -> {
                "拟合结束"
            }

            ReplyState.MOTOR_ERR -> {
                "拟合结束"
            }

            ReplyState.SENSOR_ERR -> {
                "拟合结束"
            }

            ReplyState.SAMPLING_FAILED -> {
                "拟合结束，取样失败"
            }

            ReplyState.CUVETTE_NOT_EMPTY -> {
                "拟合结束，比色皿非空"
            }

            ReplyState.TAKE_REAGENT_FAILED -> {
                "拟合结束，加试剂失败"
            }

            ReplyState.ORDER -> {
                "拟合结束，检测到样本管"
            }

            else -> {
                "其他"
            }
        }
    }

    /**
     * 保存标曲记录
     */
    fun saveProject() {
        selectProject = curProject
        //添加到参数列表，刷新
        viewModelScope.launch {
            selectProject?.let {
                curveRepository.addCurve(it)
                coverCurveModel?.let { curve ->
                    curveRepository.updateCurve(curve.apply {
                        isValid = false
                    }).let { ret ->
                        i("更新替换曲线成功 $ret")
                    }
                    coverCurveModel = null
                }
                print()
                EventBus.getDefault().post(EventMsg<String>(EventGlobal.WHAT_PROJECT_ADD))
            }
        }
    }

    /**
     * 不保存标曲记录
     */
    fun notSaveProject() {
        viewModelScope.launch {
//            curProject =
            coverCurveModel = null
        }
    }

    /**
     * 获取样本架，比色皿架状态，试剂，清洗液状态
     */
    private fun getState() {
        i("发送 获取样本架，比色皿架状态，试剂，清洗液状态")
        SerialPortUtil.getState()
    }


    /**
     * 移动样本架·
     * @param pos Int
     */
    private fun moveSampleShelf(pos: Int) {
        i("发送 移动样本架 pos=$pos")
        sampleShelfMoveFinish = false
        SerialPortUtil.moveSampleShelf(pos + 1)
    }

    /**
     * 移动比色皿架
     * @param pos Int
     */
    private fun moveCuvetteShelf(pos: Int) {
        i("发送 移动比色皿架 pos=$pos")
        cuvetteShelfMoveFinish = false
        SerialPortUtil.moveCuvetteShelf(pos + 1)
    }

    /**
     * 移动样本
     * @param step Int
     */
    private fun moveSample(step: Int = 1) {
        i("发送 移动样本 step=$step")
        sampleMoveFinish = false
        samplePos += step
        SerialPortUtil.moveSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 加样位
     */
    private fun moveCuvetteDripSample(step: Int = 1) {
        i("发送 移动比色皿到 加样位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.moveCuvetteDripSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 滴试剂位
     */
    private fun moveCuvetteDripReagent(step: Int = 1) {
        i("发送 移动比色皿到 滴试剂位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.moveCuvetteDripReagent(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 检测位
     */
    private fun moveCuvetteTest(step: Int = 1) {
        i("发送 移动比色皿到 检测位 testState=$testState step=$step")
        cuvettePos += step
        SerialPortUtil.moveCuvetteTest(step > 0, step.absoluteValue)
    }

    /**
     * 取样
     */
    private fun sampling(volume: Int) {
        i("发送 取样 volume=$volume")
        samplingFinish = false
        SerialPortUtil.sampling(volume, SampleType.CUVETTE)
    }

    /**
     * 取试剂
     */
    private fun takeReagent() {
        i("发送 取试剂")
        takeReagentFinish = false
        dripReagentFinish = false
        SerialPortUtil.takeReagent()
    }

    /**
     * 取样针清洗
     */
    private fun samplingProbeCleaning() {
        i("发送 取样针清洗")
        SerialPortUtil.samplingProbeCleaning()
    }

    /**
     * 搅拌
     */
    private fun stir() {
        i("发送 搅拌 cuvettePos=$cuvettePos")
        stirFinish = false
        stirProbeCleaningFinish = false
        SerialPortUtil.stir()
    }

    /**
     * 搅拌针清洗
     */
    private fun stirProbeCleaning() {
        i("发送 搅拌针清洗 cuvettePos=$cuvettePos")
        stirProbeCleaningFinish = false
        SerialPortUtil.stirProbeCleaning()
    }

    /**
     * 加样
     */
    private fun dripSample(autoBlending: Boolean = false, inplace: Boolean = true, volume: Int) {
        i("发送 加样 volume=$volume")
        dripSamplingFinish = false
        SerialPortUtil.dripSample(
            autoBlending = autoBlending, inplace = inplace, volume = volume
        )
    }

    /**
     * 加试剂
     */
    private fun dripReagent() {
        i("发送 加试剂 cuvettePos=$cuvettePos")
        dripReagentFinish = false
        SerialPortUtil.dripReagent()
    }

    /**
     * 检测
     */
    private fun test() {
        i("发送 检测 cuvettePos=$cuvettePos")
        testFinish = false
        SerialPortUtil.test()
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
        matchingFinish()
    }

    /**
     * 是否正在拟合
     */
    fun runningMatching(): Boolean {
        return testState.isRunning() && testType.isMatchingArgs()
    }

    fun changeSelectProject(project: CurveModel) {
        selectProject = project
        _curveUiState.update {
            it.copy(
                equationText = "Y=${project.f0.scale(8)}+${project.f1.scale(8)}x+${
                    project.f2.scale(
                        8
                    )
                }x²+${project.f3.scale(8)}x³",
                fitGoodnessText = "R²=${project.fitGoodness.scale(6)}"
            )
        }
    }

    /**
     * 拟合配置完毕
     */
    fun matchingConfigFinish(
        matchingNum: Int,
        autoAttenuation: Boolean,
        selectProject: ProjectModel?,
        selectFitterType: FitterType,
        cons: List<Int>
    ) {
        this.matchingNum = matchingNum
        this.autoAttenuation = autoAttenuation
        this.selectMatchingProject = selectProject
        this.selectFitterType = selectFitterType
        this.targetCons.clear()
        this.targetCons.addAll(cons)


    }
}

class MatchingArgsViewModelFactory(
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val curveRepository: CurveRepository = CurveRepository()
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchingArgsViewModel::class.java)) {
            return MatchingArgsViewModel(projectRepository, curveRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
