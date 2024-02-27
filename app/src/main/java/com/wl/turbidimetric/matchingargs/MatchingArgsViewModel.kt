package com.wl.turbidimetric.matchingargs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.calcAbsorbance
import com.wl.turbidimetric.ex.calcAbsorbanceDifferences
import com.wl.turbidimetric.ex.copyForProject
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.ex.getEquation
import com.wl.turbidimetric.ex.getFitGoodness
import com.wl.turbidimetric.ex.isSample
import com.wl.turbidimetric.ex.matchingArg
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
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
import com.wl.turbidimetric.repository.DefaultCurveDataSource
import com.wl.turbidimetric.repository.DefaultLocalDataDataSource
import com.wl.turbidimetric.repository.DefaultProjectDataSource
import com.wl.turbidimetric.repository.if2.CurveSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.ProjectSource
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.FitterType
import com.wl.wllib.LogToFile.i
import com.wl.wllib.toTimeStr
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * 曲线拟合和质控
 *
 * @property quality Boolean
 */
class MatchingArgsViewModel(
    private val appViewModel: AppViewModel,
    private val projectRepository: ProjectSource,
    private val curveRepository: CurveSource,
    private val localDataRepository: LocalDataSource
) : BaseViewModel(), Callback2 {

    init {
        listener()
    }

    private fun listener() {
        appViewModel.serialPort.addCallback(this)
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

    private val _curveUiState = MutableSharedFlow<MatchingArgsCurveUiState>()
    val curveUiState = _curveUiState.asSharedFlow()

    private val _matchingConfigUiState = MutableSharedFlow<MatchingConfigUiState>()
    val matchingConfigUiState = _matchingConfigUiState.asSharedFlow()

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
    var gradsNum = 5

    /**
     * 平均值
     */
    var means: MutableList<Double> = mutableListOf()

    /**
     * 全部吸光度
     */
    var abss: MutableList<MutableList<Double>> = mutableListOf()

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
    var targetCons = mutableListOf<Double>()


    /**
     * 测试用的 start
     */
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

    val datas = curveRepository.listenerCurve()


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
                        MatchingArgsDialogUiState.StateFailed(
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
        appViewModel.testState = TestState.RunningError
    }

    /**
     * 挤压
     * @param reply ReplyModel<SqueezingModel>
     */
    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {
        if (!runningMatching()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 挤压 reply=$reply")
    }

    /**
     * 显示拟合配置对话框
     */
    fun showMatchingSettingsDialog() {
        viewModelScope.launch {
            _dialogUiState.emit(
                MatchingArgsDialogUiState.MatchingSettings(
                    reagentNOStr,
                    quality,
                    projects,
                    autoAttenuation,
                    gradsNum,
                    selectMatchingProject,
                    selectFitterType,
                    targetCons,
                )
            )
        }
    }

    /**
     * 显示拟合状态对话框
     */
    fun showMatchingStateDialog() {
        viewModelScope.launch {
            _dialogUiState.emit(
                MatchingArgsDialogUiState.MatchingState(
                    gradsNum,
                    abss,
                    targetCons,
                    means,
                    selectFitterType,
                    curProject
                )
            )
        }
    }

    /**
     * 更改本次要覆盖的曲线
     */
    fun saveCoverCurve(curveModel: CurveModel?) {
        this.coverCurveModel = curveModel
    }

    /**
     * 点击开始拟合
     */
    fun clickStart() {
        if (appViewModel.testState.isNotPrepare()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState.Accident("请重新自检或重启仪器")
                )
            }
            return
        }
        if (appViewModel.testState != TestState.Normal) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState.Accident("正在检测，请勿操作")
                )
            }
            return
        }

        if (reagentNOStr.isNullOrEmpty()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState.Accident("请输入试剂序号")
                )
            }
            return
        }


        start()
    }

    fun start() {
        initState()
        appViewModel.testState = TestState.GetState
        appViewModel.testType = TestType.MatchingArgs
        getState()
    }

    private fun initState() {
        curProject = null
        sampled = false
        accidentState = ReplyState.SUCCESS
        resultTest1.clear()
        resultTest2.clear()
        resultTest3.clear()
        resultTest4.clear()
        resultOriginalTest1.clear()
        resultOriginalTest2.clear()
        resultOriginalTest3.clear()
        resultOriginalTest4.clear()
        appViewModel.testState = TestState.None
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
            testShelfInterval1 = localDataRepository.getTest1DelayTime()
            testShelfInterval2 = localDataRepository.getTest2DelayTime()
            testShelfInterval3 = localDataRepository.getTest3DelayTime()
            testShelfInterval4 = localDataRepository.getTest4DelayTime()
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
        if (appViewModel.testState.isNotPrepare()) return
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
                    MatchingArgsDialogUiState.GetStateNotExistMsg(errorMsg)
                )
            }
            return
        }

        appViewModel.testState = if (!autoAttenuation) {
            TestState.MoveSample
        } else {
            TestState.DripDiluentVolume
        }
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
        if (appViewModel.testState == TestState.TestFinish && appViewModel.testType.isMatchingArgs()) {
            cuvetteShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningMatching()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 移动比色皿架 reply=$reply")
        cuvettePos = 0
        when (appViewModel.testState) {
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
        appViewModel.serialPort.openSampleDoor()
    }

    /**
     * 发送 开比色皿仓门
     */
    private fun openCuvetteDoor() {
        i("发送 开比色皿仓门")
        appViewModel.serialPort.openCuvetteDoor()
    }

    /**
     * 获取样本仓门状态
     */
    private fun getSampleDoor() {
        i("发送 获取样本仓门状态")
        appViewModel.serialPort.getSampleDoorState()
    }

    private fun isMatchingFinish(): Boolean {
        return appViewModel.testState == TestState.TestFinish && cuvetteShelfMoveFinish && sampleShelfMoveFinish
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
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 移动样本 reply=$reply samplePos=$samplePos testState=${appViewModel.testState} sampleStep=$sampleStep")
        sampleMoveFinish = true

        //如果是样本管，直接检测结束，并报错，因为质控不允许使用样本管
        if (reply.data.type.isSample()) {
            accidentState = ReplyState.ORDER
            matchingFinish()
            return
        }
        when (appViewModel.testState) {
            TestState.DripDiluentVolume, TestState.DripStandardVolume -> {//去取稀释液、标准品
                if (!sampled) {
                    sampleStep++
                    val volume = getSamplingVolume(appViewModel.testState, sampleStep - 1)
                    sampling(volume)

                } else {
                    val volume = getSamplingVolume(appViewModel.testState, sampleStep - 1)
                    dripSample(
                        appViewModel.testState == TestState.DripStandardVolume,
                        inplace = true,
                        volume
                    )
                }
                sampled = !sampled
            }

            TestState.MoveSample -> {//去取需要移动的已混匀的样本
                sampleStep++
                sampling(localDataRepository.getSamplingVolume())
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
        return if (appViewModel.testState == TestState.DripDiluentVolume) {
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
        if (appViewModel.testState.isNotPrepare()) return
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
        if (appViewModel.testState.isNotPrepare()) return
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

        if ((cuvettePos == (gradsNum + 6) && quality) || (cuvettePos == (gradsNum + 4) && !quality)) {
            //最后一个也检测结束了
            appViewModel.testState = TestState.Test2
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
        i("接收到 移动样本架 reply=$reply testState=${appViewModel.testState}")
        if (appViewModel.testState == TestState.TestFinish && appViewModel.testType.isMatchingArgs()) {
            sampleShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningMatching()) return
        if (appViewModel.testState.isNotPrepare()) return
        sampleShelfMoveFinish = true
        if (appViewModel.testState != TestState.TestFinish) {
            //一开始就要移动到第二个位置去取第一个位置的稀释液
            val step = if (!autoAttenuation) {//自动稀释就去第二个位置加
                1
            } else {
                2
            }
            moveSample(step)
        }
    }


    /**
     * 接收到检测
     * @param reply ReplyModel<TestModel>
     */
    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        if (!runningMatching()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 检测 reply=$reply cuvettePos=$cuvettePos testState=${appViewModel.testState} 检测值=${reply.data.value}")

        calcTestResult(reply.data.value)
    }

    /**
     * 计算和保存检测结果
     */
    private fun calcTestResult(value: Int) {
        when (appViewModel.testState) {
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
                if ((cuvettePos == (gradsNum - 1) && !quality) || (cuvettePos == (gradsNum + 1) && quality)) {
                    appViewModel.testState = TestState.Test3

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
                if ((cuvettePos == (gradsNum - 1) && !quality) || (cuvettePos == (gradsNum + 1) && quality)) {
                    appViewModel.testState = TestState.Test4

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
                if ((cuvettePos == (gradsNum - 1) && !quality) || (cuvettePos == (gradsNum + 1) && quality)) {
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
        appViewModel.testState = TestState.TestFinish
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
        return Random(Date().time).nextFloat().toBigDecimal().multiply(10000.toBigDecimal())
            .setScale(1, RoundingMode.HALF_UP)
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
            val size = if (quality) gradsNum + 2 else gradsNum
            repeat(size) {
                resultTest1.add(testValues1[it].toBigDecimal())
                resultTest2.add(testValues2[it].toBigDecimal())
                resultTest3.add(testValues3[it].toBigDecimal())
                resultTest4.add(testValues4[it].toBigDecimal())

                resultOriginalTest1.add(testOriginalValues1[it])
                resultOriginalTest2.add(testOriginalValues2[it])
                resultOriginalTest3.add(testOriginalValues3[it])
                resultOriginalTest4.add(testOriginalValues4[it])
            }

        }
        i("开始计算反应度 $resultTest1 $resultTest2 $resultTest3 $resultTest4")

        result = calcAbsorbanceDifferences(resultTest1, resultTest2, resultTest3, resultTest4)

        absorbancys = result.map { it.toDouble() }

        //更新拟合参数
        abss.add(absorbancys.toMutableList())
        calcMean()
        matchingArg()
    }

    /**
     * 计算多组反应度的平均值
     */
    private fun calcMean() {
        means.clear()
        repeat(if (quality) gradsNum + 2 else gradsNum) {
            means.add(getMean(it).scale(2))
        }
    }

    private fun getMean(index: Int): Double {
        return abss.map {
            it[index]
        }.sum() / abss.size
    }

    fun matchingArg() {
        if (means.isEmpty()) return
        var cf = matchingArg(selectFitterType, means, targetCons.toDoubleArray())
        yzs = cf.yss.toList()
//        if (autoAttenuation && selectFitterType == FitterType.Three) {
//            //**修正start**
//            var dif50 = 50 - yzs[1]
//            println("50差值：$dif50")
//            dif50 = if (dif50 >= 0) {
//                dif50 * 2
//            } else {
//                dif50 / 2
//            }
//            println("50差值计算后：$dif50")
//
//            val dif0 = means[0] - dif50
//            println("0原始吸光度-50差值计算后：$dif0")
//
//            means[0] = dif0
//            cf = matchingArg(selectFitterType, means, targetCons.toDoubleArray())
//        }
        //**修正end**
        var msg: StringBuilder = StringBuilder(
            "拟合类型:${selectFitterType.showName} \n第一次原始:$resultOriginalTest1 \n" + "第一次:$resultTest1 \n" + "第二次原始:$resultOriginalTest2 \n" + "第二次:$resultTest2 \n" + "第三次原始:$resultOriginalTest3 \n" + "第三次:$resultTest3 \n" + "第四次原始:$resultOriginalTest4 \n" + "第四次:$resultTest4 \n" +
//                    "吸光度:$result \n" +
//                    "拟合度：${cf.fitGoodness} \n" +
//                    "四参数：f0=${f0} f1=${f1} f2=${f2} f3=${f3} \n " +
//                    "验算 ${yzs}\n" +
                    "吸光度:$means \n" + "拟合度:${cf.fitGoodness} \n" + "参数:${cf.params.joinToString()} \n" + "验算:${cf.yss.joinToString()}\n"
        )
        curProject = CurveModel().apply {
            this.f0 = cf.params.getOrNull(0) ?: 0.0
            this.f1 = cf.params.getOrNull(1) ?: 0.0
            this.f2 = cf.params.getOrNull(2) ?: 0.0
            this.f3 = cf.params.getOrNull(3) ?: 0.0
            this.fitterType = selectFitterType.ordinal
            this.fitGoodness = cf.fitGoodness
            this.createTime = Date().toTimeStr()
            this.reagentNO = reagentNOStr
            this.gradsNum = gradsNum
            this.targets = targetCons.toDoubleArray()
            this.reactionValues =
                means.subList(0, gradsNum).map { it.toInt() }.toIntArray()
            this.yzs = cf.yss.map { it.toInt() }.toIntArray()
        }.copyForProject(selectMatchingProject!!)
//        print()
        testMsg.postValue(msg.toString())
        showMatchingStateDialog()
    }

    /**
     * 打印上次的
     */
    fun print() {
        selectProject?.let {
            if (it.reactionValues.isNotEmpty() == true && it.yzs.isNotEmpty() == true) {
                PrintUtil.printMatchingQuality(
                    it.reactionValues.toList(),
                    it.targets,
                    it.yzs.toList(),
                    mutableListOf(it.f0, it.f1, it.f2, it.f3),
                    it.createTime,
                    it.projectName,
                    it.reagentNO,
                    it.gradsNum
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
        if (appViewModel.testState.isNotPrepare()) return
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
        if (appViewModel.testState.isNotPrepare()) return
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
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 加样 reply=$reply cuvettePos=$cuvettePos testState=${appViewModel.testState} sampleStep=$sampleStep samplePos=$samplePos")

        dripSamplingFinish = true
        samplingFinish = false
        if (reply.state == ReplyState.CUVETTE_NOT_EMPTY) {//比色皿非空
            accidentState = ReplyState.CUVETTE_NOT_EMPTY
            matchingFinish()
            return
        }
        when (appViewModel.testState) {
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
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 取样 reply=$reply testState=${appViewModel.testState} sampleStep=$sampleStep samplePos=$samplePos")

        samplingFinish = true
        if (reply.state == ReplyState.SAMPLING_FAILED) {//取样失败
            accidentState = ReplyState.SAMPLING_FAILED
            matchingFinish()
            return
        }
        when (appViewModel.testState) {
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
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 移动比色皿到检测位 reply=$reply cuvetteStates=$cuvetteStates testState=${appViewModel.testState}")

        when (appViewModel.testState) {
            TestState.Test2, TestState.Test3, TestState.Test4 -> {
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

            else -> {}
        }
    }

    /**
     * 接收到取试剂
     * @param reply ReplyModel<TakeReagentModel>
     */
    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        if (!runningMatching()) return
        if (appViewModel.testState.isNotPrepare()) return
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
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 取样针清洗 reply=$reply samplePos=$samplePos sampleStep=$sampleStep")
        if (cleaningBeforeStartTest) {
            //是开始检测前的清洗，清洗完才开始检测
            cleaningBeforeStartTest = false
            moveSampleShelf(sampleShelfPos)
            if (!autoAttenuation) {
                moveCuvetteShelf(cuvetteShelfPos)
            }
            return
        }
        if (appViewModel.testState == TestState.DripDiluentVolume) {//加完稀释液后的清洗
            sampleStep = 0
            appViewModel.testState = TestState.DripStandardVolume
            moveSample(-samplePos + 2)//直接到取标准品的位置
        } else if (appViewModel.testState == TestState.DripStandardVolume) {//加标准后的清洗
            if (sampleStep == sampleStepMax) {
                //开始移动已混匀样本的步骤
                //第一步、先复位样本和比色皿
                appViewModel.testState = TestState.MoveSample
                sampleStep = 0
                moveSample(-samplePos + 1)
                moveCuvetteShelf(cuvetteShelfPos)
            } else {//继续加标准品
                moveSample(0)
            }
        } else if (appViewModel.testState == TestState.MoveSample) {//加已混匀的样本的清洗
            if ((sampleStep == gradsNum && !quality) || (sampleStep == gradsNum + 2 && quality)) {
                //开始加试剂的步骤
                appViewModel.testState = TestState.DripReagent
                sampleStep = 0
                cuvettePos = -1
                moveSample(-samplePos + 1)
                moveCuvetteDripReagent()

                takeReagent()
            } else {//继续移动已混匀的样本
                i("sampleStep=$sampleStep")
                val targetIndex = if (autoAttenuation) {
                    moveBlendingPos[sampleStep] - samplePos
                } else {
                    1
                }
                moveSample(targetIndex)
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
        if (appViewModel.testState.isNotPrepare()) return
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
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 刺破 reply=$reply")
    }

    /**
     * 接收到获取版本号
     * @param reply ReplyModel<GetVersionModel>
     */
    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        if (!runningMatching()) return
        if (appViewModel.testState.isNotPrepare()) return
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
     * 显示异常拟合结束对话框
     */
    private fun showMatchingDialog() {
        if (accidentState != ReplyState.SUCCESS) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState.Accident(getAccidentFinishDialog())
                )
            }

        } else {
//            viewModelScope.launch {
//                _dialogUiState.emit(
//                    MatchingArgsDialogUiState(
//                        DialogState.MatchingFinishMsg, testMsg.value ?: ""
//                    )
//                )
//            }
        }
        appViewModel.testState = TestState.Normal
    }

    /**
     * 手动点击拟合结束
     */
    fun showSaveMatchingDialog() {
        if (appViewModel.testState.isRunning()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState.Accident("正在检测，请稍后")
                )
            }
            return
        }
        //没有开始质控就点击结束就直接结束
        if (abss.isEmpty() || means.isEmpty()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState.CloseMatchingStateDialog("")
                )
            }
            reagentNOStr = ""
            changeConfig(
                reagentNOStr,
                quality,
                gradsNum,
                autoAttenuation,
                selectMatchingProject,
                selectFitterType,
                targetCons
            )

            viewModelScope.launch {
                _dialogUiState.emit(
                    MatchingArgsDialogUiState.Accident("拟合质控结束")
                )
            }
            return
        }
        abss.clear()
        means.clear()

        viewModelScope.launch {
            _dialogUiState.emit(
                MatchingArgsDialogUiState.MatchingFinishMsg(testMsg.value ?: "")
            )
        }
        appViewModel.testState = TestState.Normal
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
        reagentNOStr = ""
        changeConfig(
            reagentNOStr,
            quality,
            gradsNum,
            autoAttenuation,
            selectMatchingProject,
            selectFitterType,
            targetCons
        )
    }

    /**
     * 不保存标曲记录
     */
    fun notSaveProject() {
        viewModelScope.launch {
//            curProject =
            coverCurveModel = null
        }
        reagentNOStr = ""
        changeConfig(
            reagentNOStr,
            quality,
            gradsNum,
            autoAttenuation,
            selectMatchingProject,
            selectFitterType,
            targetCons
        )
    }

    /**
     * 获取样本架，比色皿架状态，试剂，清洗液状态
     */
    private fun getState() {
        i("发送 获取样本架，比色皿架状态，试剂，清洗液状态")
        appViewModel.serialPort.getState()
    }


    /**
     * 移动样本架·
     * @param pos Int
     */
    private fun moveSampleShelf(pos: Int) {
        i("发送 移动样本架 pos=$pos")
        sampleShelfMoveFinish = false
        appViewModel.serialPort.moveSampleShelf(pos + 1)
    }

    /**
     * 移动比色皿架
     * @param pos Int
     */
    private fun moveCuvetteShelf(pos: Int) {
        i("发送 移动比色皿架 pos=$pos")
        cuvetteShelfMoveFinish = false
        appViewModel.serialPort.moveCuvetteShelf(pos + 1)
    }

    /**
     * 移动样本
     * @param step Int
     */
    private fun moveSample(step: Int = 1) {
        i("发送 移动样本 step=$step")
        sampleMoveFinish = false
        samplePos += step
        appViewModel.serialPort.moveSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 加样位
     */
    private fun moveCuvetteDripSample(step: Int = 1) {
        i("发送 移动比色皿到 加样位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        appViewModel.serialPort.moveCuvetteDripSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 滴试剂位
     */
    private fun moveCuvetteDripReagent(step: Int = 1) {
        i("发送 移动比色皿到 滴试剂位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        appViewModel.serialPort.moveCuvetteDripReagent(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 检测位
     */
    private fun moveCuvetteTest(step: Int = 1) {
        i("发送 移动比色皿到 检测位 testState=${appViewModel.testState} step=$step")
        cuvettePos += step
        appViewModel.serialPort.moveCuvetteTest(step > 0, step.absoluteValue)
    }

    /**
     * 取样
     */
    private fun sampling(volume: Int) {
        i("发送 取样 volume=$volume")
        samplingFinish = false
        appViewModel.serialPort.sampling(volume, SampleType.CUVETTE)
    }

    /**
     * 取试剂
     */
    private fun takeReagent() {
        i("发送 取试剂")
        takeReagentFinish = false
        dripReagentFinish = false
        appViewModel.serialPort.takeReagent(
            localDataRepository.getTakeReagentR1(),
            localDataRepository.getTakeReagentR2()
        )
    }

    /**
     * 取样针清洗
     */
    private fun samplingProbeCleaning() {
        i("发送 取样针清洗")
        appViewModel.serialPort.samplingProbeCleaning(localDataRepository.getSamplingProbeCleaningDuration())
    }

    /**
     * 搅拌
     */
    private fun stir() {
        i("发送 搅拌 cuvettePos=$cuvettePos")
        stirFinish = false
        stirProbeCleaningFinish = false
        appViewModel.serialPort.stir(localDataRepository.getStirDuration())
    }

    /**
     * 搅拌针清洗
     */
    private fun stirProbeCleaning() {
        i("发送 搅拌针清洗 cuvettePos=$cuvettePos")
        stirProbeCleaningFinish = false
        appViewModel.serialPort.stirProbeCleaning(localDataRepository.getStirProbeCleaningDuration())
    }

    /**
     * 加样
     */
    private fun dripSample(autoBlending: Boolean = false, inplace: Boolean = true, volume: Int) {
        i("发送 加样 volume=$volume")
        dripSamplingFinish = false
        appViewModel.serialPort.dripSample(
            autoBlending = autoBlending, inplace = inplace, volume = volume
        )
    }

    /**
     * 加试剂
     */
    private fun dripReagent() {
        i("发送 加试剂 cuvettePos=$cuvettePos")
        dripReagentFinish = false
        appViewModel.serialPort.dripReagent(
            localDataRepository.getTakeReagentR1(),
            localDataRepository.getTakeReagentR2()
        )
    }

    /**
     * 检测
     */
    private fun test() {
        i("发送 检测 cuvettePos=$cuvettePos")
        testFinish = false
        appViewModel.serialPort.test()
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
        return appViewModel.testState.isRunning() && appViewModel.testType.isMatchingArgs()
    }

    fun changeSelectProject(project: CurveModel) {
        selectProject = project
        viewModelScope.launch {
            _curveUiState.emit(
                MatchingArgsCurveUiState(
                    equationText = getEquation(
                        FitterType.toValue(project.fitterType),
                        mutableListOf(project.f0, project.f1, project.f2, project.f3)
                    ),
                    fitGoodnessText = getFitGoodness(
                        FitterType.toValue(project.fitterType),
                        project.fitGoodness
                    )
                )
            )
        }
    }

    /**
     * 拟合配置完毕
     */
    fun matchingConfigFinish(
        reagentNo: String,
        quality: Boolean,
        gradsNum: Int,
        autoAttenuation: Boolean,
        selectProject: ProjectModel?,
        selectFitterType: FitterType,
        cons: List<Double>
    ) {
        this.reagentNOStr = reagentNo
        this.quality = quality
        this.gradsNum = gradsNum
        this.autoAttenuation = autoAttenuation
        this.selectMatchingProject = selectProject
        this.selectFitterType = selectFitterType
        this.targetCons.clear()
        this.targetCons.addAll(cons)

        val hilt = if (selectMatchingProject == null) {
            "没有选择项目"
        } else if (reagentNOStr.isNullOrEmpty()) {
            "请输入序号"
        } else {
            ""
        }
        if (hilt.isNullOrEmpty()) {
            showMatchingStateDialog()
        } else {
            viewModelScope.launch {
                _dialogUiState.emit(MatchingArgsDialogUiState.Accident(hilt))
            }
        }
        changeConfig(
            reagentNOStr,
            quality,
            gradsNum,
            autoAttenuation,
            selectProject,
            selectFitterType,
            cons
        )

    }

    private fun changeConfig(
        reagentNOStr: String,
        quality: Boolean,
        gradsNum: Int,
        autoAttenuation: Boolean,
        selectProject: ProjectModel?,
        selectFitterType: FitterType,
        cons: List<Double>
    ) {
        viewModelScope.launch {
            _matchingConfigUiState.emit(
                MatchingConfigUiState(
                    selectProject,
                    autoAttenuation,
                    gradsNum,
                    selectFitterType,
                    cons.toMutableList(),
                    quality,
                    reagentNOStr,
                )
            )
        }
    }

    fun changeFitterType(fitterType: FitterType) {
        this.selectFitterType = fitterType
        matchingArg()
    }

    /**
     * 点击开始拟合
     */
    fun startMatching() {
        viewModelScope.launch {
            val hiltText = if (appViewModel.testState.isNotPrepare()) {
                "请重新自检"
            } else {
                ""
            }
            if (hiltText.isNotEmpty()) {
                //不能拟合
                _dialogUiState.emit(MatchingArgsDialogUiState.Accident(hiltText))
            } else if (curveRepository.getCurveModels().size >= SystemGlobal.showCurveSize) {
                //显示选择覆盖标曲对话框
                _dialogUiState.emit(MatchingArgsDialogUiState.MatchingCoverCurve(""))
            } else if (reagentNOStr.isNullOrEmpty() || selectMatchingProject == null) {
                //显示配置对话框
                saveCoverCurve(null)
                showMatchingSettingsDialog()
            } else {
                //开始拟合
                showMatchingStateDialog()
            }

        }
    }
}

class MatchingArgsViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
    private val projectRepository: ProjectSource = DefaultProjectDataSource(App.instance!!.mainDao),
    private val curveRepository: CurveSource = DefaultCurveDataSource(App.instance!!.mainDao),
    private val localDataRepository: LocalDataSource = DefaultLocalDataDataSource()
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchingArgsViewModel::class.java)) {
            return MatchingArgsViewModel(
                appViewModel,
                projectRepository,
                curveRepository,
                localDataRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
