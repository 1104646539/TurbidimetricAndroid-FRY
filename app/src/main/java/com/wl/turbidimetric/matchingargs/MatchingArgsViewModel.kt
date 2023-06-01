package com.wl.turbidimetric.matchingargs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.matchingTestState
import com.wl.turbidimetric.global.SystemGlobal.repeatabilityState
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.android.ObjectBoxDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import java.util.Date
import kotlin.math.absoluteValue

/**
 * 曲线拟合和质控
 *
 * @property quality Boolean
 */
class MatchingArgsViewModel(private val projectRepository: ProjectRepository) : BaseViewModel(),
    Callback2 {

    init {
        listener()
    }

    private fun listener() {
        SerialPortUtil.Instance.callback.add(this)
    }

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
    private val dripDiluentVolumes = arrayListOf(200, 320, 380)

    /**
     * 加标准品的量
     */
    private val dripStandardVolumes = arrayListOf(200, 80, 20)

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

//    /**
//     * 移动已混匀的样本
//     */
//    private val movePoss = arrayListOf(1, 5, 4, 3, 2);

    /**
     * 移动已混匀样本的量
     */
    private val moveSampleVolume = 25

    /**
     * 是否要同时质控
     */
    var quality: Boolean = false

//    //是否要同时质控
//    var isQuality = MutableLiveData(false)

    /**
     * 当前取样的步骤
     */
    private var sampleStep: Int = 0

    /**比色皿架是否移动完毕
     *
     */
    private var cuvetteShelfMoveFinish = false

    /**采便管架是否移动完毕
     *
     */
    private var shitTubeShelfMoveFinish = false

    /**
     * 当前排所有比色皿的状态
     */
    private var cuvetteStates = initCuvetteStates()

    /**
     * 当前采便管架位置
     */
    private var shitTubeShelfPos = -1

    /**
     * 最后一排可使用的采便管架的位置
     */
    private var lastShitTubeShelfPos = -1

    /**
     * 当前比色皿架位置
     */
    private var cuvetteShelfPos = -1

    /**
     * 最后一排可使用的比色皿架的位置
     */
    private var lastCuvetteShelfPos = -1

    /**
     * 当前采便管位置
     * 0-10 一共11次。其中每个采便管的刺破位隔挤压取样位一个位置，所以第0个位置只能用来刺破，第10个位置不能刺破，只能挤压取样
     */
    private var shitTubePos = -1

    /**
     * 当前比色皿位置
     */
    private var cuvettePos = -1

    /**
     * 比色皿是否移动完毕
     */
    private var cuvetteMoveFinish = false

    /**
     * 采便管是否移动完毕
     */
    private var shitTubeMoveFinish = false

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
     * 采便管架状态 1有 0无 顺序是从中间往旁边
     */
    private var shitTubeShelfStates: IntArray = IntArray(4)

    /**
     * 比色皿架状态 1有 0无 顺序是从中间往旁边
     */
    private var cuvetteShelfStates: IntArray = IntArray(4)

    /**
     * 开始检测 比色皿，采便管，试剂,清洗液不存在
     */
    val getStateNotExistMsg = MutableLiveData("")

    /**
     * 拟合质控结束提示
     */
    val matchingFinishMsg = MutableLiveData("")


    val testMsg = MutableLiveData("")
    val toastMsg = MutableLiveData("")

    /**
     * 比色皿锁状态
     */
    private val shitTubeDoorLockedLD = MutableLiveData(false)

    /**
     * 比色皿锁状态
     */
    private val cuvetteDoorLockedLD = MutableLiveData(false)


    private var shitTubeDoorLocked = false
    private var cuvetteDoorLocked = false

//    /**
//     * 点击获取采便管锁状态
//     */
//    private val clickShitTubeDoor = false;
//    /**
//     * 点击获取比色皿锁状态
//     */
//    private val clickCuvetteDoor = false;

    /**
     * 每排之间的检测间隔
     */
    var testShelfInterval: Long = 1000 * 60;

    /**
     * 每个比色皿之间的检测间隔
     */
    var testPosInterval: Long = 1000 * 10;

    /**
     * 测试用的 start
     */
    //检测的值
//    private val testValues1 = doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
//    private val testValues2 = doubleArrayOf(0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3)
//    private val testValues3 = doubleArrayOf(0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3)
//    private val testValues4 =
//        doubleArrayOf(0.9998, 1.1586, 1.0642, 1.0178, 1.0035, 1.0141, 1.0585, 1.0, 1.0, 1.0)
    private val testValues1 = doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
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
    val testS: Long = 100;

    //测试用 每个比色皿之间的检测间隔
    val testP: Long = 1000;

    /**
     * 测试用的 end
     */
    override fun init() {
        super.init()

    }

    val datas = projectRepository.paginDatas.flow.cachedIn(viewModelScope)


    /**
     * 更新比色皿状态
     * @param pos Int
     * @param state CuvetteState
     */
    private fun updateCuvetteState(pos: Int, state: CuvetteState) {
        cuvetteStates[pos] = state
        Timber.d("updateCuvetteState pos=$pos state=$state cuvetteStates=$cuvetteStates")
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
     * 报错
     * @param cmd UByte
     * @param state UByte
     */
    override fun readDataStateFailed(cmd: UByte, state: UByte) {
        if (!runningMatching()) return
        SystemGlobal.machineArgState = MachineState.RunningError
        Timber.d("报错了，cmd=$cmd state=$state")
        testMsg.postValue("报错了，cmd=$cmd state=$state")
    }

    /**
     * 仪器是否正常
     * @return Boolean
     */
    private fun machineStateNormal(): Boolean {
        return SystemGlobal.machineArgState == MachineState.Normal
    }

    /**
     * 点击开始拟合
     */
    fun clickStart() {
//        if (DoorAllOpen()) {
//            toast("舱门未关")
//            return
//        }
        if (testState != TestState.None) {
            toastMsg.postValue("正在检测，请勿操作！")
            return
        }
        if (repeatabilityState != RepeatabilityState.None) {
            toast("正在进行重复性检测，请勿操作！")
            return
        }

        if (matchingTestState != MatchingArgState.None && matchingTestState != MatchingArgState.Finish) {
            toastMsg.postValue("正在质控，请勿操作！")
            return
        }
        initState()
        matchingTestState = MatchingArgState.GetState;
        getState()
    }

    private fun initState() {
        resultTest1.clear()
        resultTest2.clear()
        resultTest3.clear()
        resultTest4.clear()
        resultOriginalTest1.clear()
        resultOriginalTest2.clear()
        resultOriginalTest3.clear()
        resultOriginalTest4.clear()
        matchingTestState = MatchingArgState.None
        sampleStep = 0
        testMsg.postValue("")
        shitTubePos = -1;
        cuvettePos = -1;
        shitTubeShelfPos = -1;
        cuvetteShelfPos = -1;

        if (SystemGlobal.isCodeDebug) {
            testShelfInterval = testS;
            testPosInterval = testP;

        } else {
//            testShelfInterval =
//                ((1000 * 10 * 10) - ((5 + (if (quality) 2 else 0)) * 10 * 1000)).toLong()
            testShelfInterval = if (quality) {
                33 * 1000
            } else {
                55 * 1000
            }
        }
    }

    /**
     * 接收到自检
     * @param reply ReplyModel<GetMachineStateModel>
     */
    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        if (!runningMatching()) return
//        Timber.d("接收到 采便管舱门状态 reply=$reply")
    }

    /**
     * 接收到获取状态
     * @param reply ReplyModel<GetStateModel>
     */
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 获取状态 reply=$reply")
        cuvetteShelfStates = reply.data.cuvetteShelfs
        shitTubeShelfStates = reply.data.shitTubeShelfs
        val r1Reagent = reply.data.r1Reagent
        val r2Reagent = reply.data.r2Reagent
        val cleanoutFluid = reply.data.cleanoutFluid

        getInitialPos()

        Timber.d("cuvetteShelfPos=${cuvetteShelfPos} shitTubeShelfPos=${shitTubeShelfPos}")
        if (cuvetteShelfPos == -1) {
            Timber.d("没有比色皿架")
            getStateNotExistMsg.postValue("比色皿不足，请添加")
            return
        }
        if (shitTubeShelfPos == -1) {
            Timber.d("没有采便管架")
            getStateNotExistMsg.postValue("采便管不足，请添加")
            return
        }
        if (!r1Reagent) {
            Timber.d("没有R1试剂")
            getStateNotExistMsg.postValue("R1试剂不足，请添加")
            return
        }
        if (!r2Reagent) {
            Timber.d("没有R2试剂")
            getStateNotExistMsg.postValue("R2试剂不足，请添加")
            return
        }
        if (!cleanoutFluid) {
            Timber.d("没有清洗液试剂")
            getStateNotExistMsg.postValue("清洗液不足，请添加")
            return
        }
        matchingTestState = MatchingArgState.DripDiluentVolume
        //开始检测
        moveShitTubeShelf(shitTubeShelfPos)
//        moveCuvetteShelf(cuvetteShelfPos)
    }

    /**
     * 获取初始位置
     */
    private fun getInitialPos() {
        getInitCuvetteShelfPos()
        getInitShitTubeShelfPos()
    }


    /**
     * 获取采便管架初始位置和最后一排的位置
     */
    private fun getInitShitTubeShelfPos() {
        for (i in shitTubeShelfStates.indices) {
            if (shitTubeShelfStates[i] == 1) {
                if (shitTubeShelfPos == -1) {
                    shitTubeShelfPos = i
                }
                lastShitTubeShelfPos = i
            }
        }
        Timber.d("getInitShitTubeShelfPos shitTubeShelfPos=$shitTubeShelfPos lastShitTubeShelfPos=$lastShitTubeShelfPos")
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
        Timber.d("getInitCuvetteShelfPos cuvetteShelfPos=$cuvetteShelfPos lastCuvetteShelfPos=$lastCuvetteShelfPos")
    }

    /**
     * 接收到移动比色皿架
     * @param reply ReplyModel<MoveCuvetteShelfModel>
     */
    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        if (matchingTestState == MatchingArgState.Finish) {
            cuvetteShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿架 reply=$reply")
        cuvettePos = 0
        when (matchingTestState) {
            MatchingArgState.MoveSample -> {
                moveCuvetteDripSample()
            }
            MatchingArgState.Test1 -> {

            }
            MatchingArgState.Test2,
            MatchingArgState.Test3,
            MatchingArgState.Test4 -> {
                moveCuvetteTest()
            }
            else -> {}
        }

    }

    private fun openAllDoor() {
//        openShitTubeDoor()
//        openCuvetteDoor()
    }

    /**
     * 发送 开样本仓门
     */
    private fun openShitTubeDoor() {
        Timber.d("发送 开样本仓门")
        SerialPortUtil.Instance.openShitTubeDoor()
    }

    /**
     * 发送 开比色皿仓门
     */
    private fun openCuvetteDoor() {
        Timber.d("发送 开比色皿仓门")
        SerialPortUtil.Instance.openCuvetteDoor()
    }

    /**
     * 获取样本仓门状态
     */
    private fun getShitTubeDoor() {
        Timber.d("发送 获取样本仓门状态")
        SerialPortUtil.Instance.getShitTubeDoorState()
    }

    private fun isMatchingFinish(): Boolean {
        return matchingTestState == MatchingArgState.Finish && cuvetteShelfMoveFinish && shitTubeShelfMoveFinish
    }


    /**
     * 接收到移动采便管
     * @param reply ReplyModel<MoveShitTubeModel>
     */
    override fun readDataMoveShitTubeModel(reply: ReplyModel<MoveShitTubeModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动采便管 reply=$reply shitTubePos=$shitTubePos matchingTestState=$matchingTestState sampleStep=$sampleStep")
        shitTubeMoveFinish = true

        when (matchingTestState) {
            MatchingArgState.DripDiluentVolume,
            MatchingArgState.DripStandardVolume
            -> {//去取稀释液、标准品
                if (shitTubePos == 1 || shitTubePos == 2) {
                    sampleStep++
                    val volume = getSamplingVolume(matchingTestState, sampleStep - 1)
                    sampling(volume)
                } else {
                    val volume = getSamplingVolume(matchingTestState, sampleStep - 1)
                    dripSample(
                        matchingTestState == MatchingArgState.DripStandardVolume,
                        inplace = true,
                        volume
                    )
                }
            }
            MatchingArgState.MoveSample -> {//去取需要移动的已混匀的样本
                sampleStep++
                sampling(moveSampleVolume)
            }
            else -> {

            }
        }

    }

    /**
     * 获取当前位置取样加样的量
     * @param matchingTestState
     * @param step
     * @return Int
     */
    private fun getSamplingVolume(matchingTestState: MatchingArgState, step: Int): Int {
        return if (matchingTestState == MatchingArgState.DripDiluentVolume) {
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
        Timber.d("接收到 移动比色皿到加样位 reply=$reply cuvetteStates=$cuvetteStates")

        cuvetteMoveFinish = true

        goDripSample()
    }

    /**
     * 去比色皿加样
     */
    private fun goDripSample() {
        if (cuvetteMoveFinish && samplingFinish) {
            dripSample(autoBlending = false, inplace = false, moveSampleVolume)
        }
    }

    /**
     * 接收到移动比色皿到加试剂位
     * @param reply ReplyModel<MoveCuvetteDripReagentModel>
     */
    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿到加试剂位 reply=$reply cuvetteStates=$cuvetteStates")

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
        if (cuvettePos > 2 && cuvetteNeedTest(cuvettePos - 3)) {
            test()
        }
    }

    /**
     * 加试剂，搅拌，检测结束后调用
     *
     * 判断是否这排结束，结束开始下一个步骤，检测第二次
     * 如果未结束，则继续移动比色皿，检测
     */
    private fun dripReagentAndStirAndTestFinish() {
        if ((cuvetteNeedDripReagent(cuvettePos) && !dripReagentFinish)) {
            return
        }
        if (cuvettePos > 1 && ((cuvetteNeedStir(cuvettePos - 2) && !stirFinish) || !stirProbeCleaningFinish)) {
            return
        }
        if (cuvettePos > 2 && (cuvetteNeedTest(cuvettePos - 3) && !testFinish)) {
            return
        }

        if ((cuvettePos == 9 && quality) || (cuvettePos == 7 && !quality)) {
            //最后一个也检测结束了
            matchingTestState = MatchingArgState.Test2
            cuvettePos = -1
            viewModelScope.launch {
                delay(testShelfInterval)
                moveCuvetteTest()
            }
        } else {
            //如果没有结束，移动比色皿，判断是否需要加试剂
            moveCuvetteDripReagent()
            if (cuvetteNeedDripReagent(cuvettePos)) {
                takeReagent()
            }
        }
    }

    /**
     * 接收到移动采便管架
     * @param reply ReplyModel<MoveShitTubeShelfModel>
     */
    override fun readDataMoveShitTubeShelfModel(reply: ReplyModel<MoveShitTubeShelfModel>) {
        Timber.d("接收到 移动采便管架 reply=$reply matchingTestState=$matchingTestState")
        if (matchingTestState == MatchingArgState.Finish) {
            shitTubeShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        shitTubeShelfMoveFinish = true
        if (matchingTestState != MatchingArgState.Finish) {
            //一开始就要移动到第二个位置去取第一个位置的稀释液
            moveShitTube(2)
        }
    }


    /**
     * 接收到检测
     * @param reply ReplyModel<TestModel>
     */
    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 检测 reply=$reply cuvettePos=$cuvettePos matchingTestState=$matchingTestState 检测值=${reply.data.value}")

        calcTestResult(reply.data.value)
    }

    private fun calcTestResult(value: Int) {
        when (matchingTestState) {
            MatchingArgState.DripReagent -> {
                updateCuvetteState(cuvettePos - 3, CuvetteState.Test1)
                resultTest1.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest1.add(value)
                updateResult()
                dripReagentAndStirAndTestFinish()
            }
            MatchingArgState.Test2 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test2)
                resultTest2.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest2.add(value)
                updateResult()
                //检测结束,开始检测第三次
                if ((cuvettePos == 4 && !quality) || (cuvettePos == 6 && quality)) {
                    matchingTestState = MatchingArgState.Test3

                    viewModelScope.launch {
                        delay(testShelfInterval)
                        moveCuvetteTest(-cuvettePos)
                    }
                } else {
                    viewModelScope.launch {
                        delay(testPosInterval)
                        moveCuvetteTest()
                    }
                }

            }
            MatchingArgState.Test3 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test3)
                resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest3.add(value)
                updateResult()
                //检测结束,开始检测第四次
                if ((cuvettePos == 4 && !quality) || (cuvettePos == 6 && quality)) {
                    matchingTestState = MatchingArgState.Test4
                    viewModelScope.launch {
                        delay(testShelfInterval)
                        moveCuvetteTest(-cuvettePos)
                    }
                } else {
                    viewModelScope.launch {
                        delay(testPosInterval)
                        moveCuvetteTest()
                    }
                }

            }
            MatchingArgState.Test4 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test4)
                resultTest4.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest4.add(value)
                updateResult()
                //检测结束,计算拟合参数
                if ((cuvettePos == 4 && !quality) || (cuvettePos == 6 && quality)) {
                    matchingFinish()
                    calcMatchingArg()
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
     * 拟合结束，复位
     */
    private fun matchingFinish() {
        matchingTestState = MatchingArgState.Finish
        moveCuvetteShelf(-1)
        moveShitTubeShelf(-1)
    }

    /**
     * 测试用，更新每次检测的值
     */
    private fun updateResult() {
        testMsg.postValue(
            "第一次原始:$resultOriginalTest1 \n" +
                    "第一次:$resultTest1 \n" +
                    "第二次原始:$resultOriginalTest2 \n" +
                    " 第二次:$resultTest2 \n" +
                    "第三次原始:$resultOriginalTest3 \n" +
                    " 第三次:$resultTest3 \n" +
                    "第四次原始:$resultOriginalTest4 \n" +
                    " 第四次:$resultTest4 \n"
        )
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
            repeat(testValues1.size) {
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
        Timber.d("开始计算拟合曲线 $resultTest1 $resultTest2 $resultTest3 $resultTest4")

        result = calcAbsorbanceDifferences(resultTest1, resultTest2, resultTest3, resultTest4)

        val absorbancys = result.map { it.toDouble() * 10000 }
        val cf = matchingArg(absorbancys)
        val res = cf.params
        for (i in res.indices) {
            println(res[i])
        }
        Timber.d("拟合度：${cf.fitGoodness}")
//        Timber.d("四参数：a1=${res[0]} a2=${res[3]} x0=${res[2]} p=${res[1]}")
        val f0 = res[0]
        val f1 = res[1]
        val f2 = res[2]
        val f3 = res[3]

        Timber.d("四参数：f0=${f0} f1=${f1} f2=${f2} f3=${f3}")

        val yzs = absorbancys.map {
            val yz = cf.f(res, it).scale(2)
            yz
        }

        var msg: StringBuilder = StringBuilder(
            "第一次原始:$resultOriginalTest1 \n" +
                    "第一次:$resultTest1 \n" +
                    "第二次原始:$resultOriginalTest2 \n" +
                    "第二次:$resultTest2 \n" +
                    "第三次原始:$resultOriginalTest3 \n" +
                    "第三次:$resultTest3 \n" +
                    "第四次原始:$resultOriginalTest4 \n" +
                    "第四次:$resultTest4 \n" +
                    "吸光度:$result 拟合度：${cf.fitGoodness} \n" +
                    "四参数：f0=${f0} f1=${f1} f2=${f2} f3=${f3} \n " +
                    "验算 ${yzs}\n"
        )
        val project = ProjectModel().apply {
            this.f0 = f0
            this.f1 = f1
            this.f2 = f2
            this.f3 = f3
            this.fitGoodness = cf.fitGoodness
            this.createTime = Date().toLongString()
            this.projectLjz = 100
        }
        PrintUtil.printMatchingQuality(absorbancys, nds, yzs, cf.params, quality)
//        if (quality) {
//            val hValue = result[5];
//            val lValue = result[6];
//
//            val hCon = calcCon(hValue, project);
//            val lCon = calcCon(lValue, project);
//            msg.append("质控(H):$hCon\n")
//            msg.append("质控(L):$lCon\n")
//        }

        testMsg.postValue(msg.toString())

        //添加到参数列表，刷新
        projectRepository.addProject(project)
    }

    /**
     * 接收到搅拌
     * @param reply ReplyModel<StirModel>
     */
    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 搅拌 reply=$reply cuvettePos=$cuvettePos")

        stirFinish = true
        updateCuvetteState(cuvettePos - 2, CuvetteState.Stir)
        stirProbeCleaning()
    }

    /**
     * 接收到加试剂
     * @param reply ReplyModel<DripReagentModel>
     */
    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 加试剂 reply=$reply cuvettePos=$cuvettePos")
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
        Timber.d("接收到 加样 reply=$reply cuvettePos=$cuvettePos matchingTestState=$matchingTestState sampleStep=$sampleStep shitTubePos=$shitTubePos")

        dripSamplingFinish = true
        samplingFinish = false

        when (matchingTestState) {
            MatchingArgState.DripDiluentVolume -> {//加稀释液
                if (sampleStep == 3) {
                    //已经加完三个了，清洗取样针，然后进行下一个步骤，取标准品
                    samplingProbeCleaning()
                } else {
                    moveShitTube(-shitTubePos + 1)
                }
            }
            MatchingArgState.DripStandardVolume -> {//加标准品
                samplingProbeCleaning()
            }
            MatchingArgState.MoveSample -> {//加已混匀的样本
                updateCuvetteState(cuvettePos - 1, CuvetteState.DripSample)
                samplingProbeCleaning()
            }
            else -> {}
        }

        Timber.d("加样 cuvetteStates=$cuvetteStates")
    }

    /**
     * 接收到取样
     * @param reply ReplyModel<SamplingModel>
     */
    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取样 reply=$reply matchingTestState=$matchingTestState sampleStep=$sampleStep shitTubePos=$shitTubePos")

        samplingFinish = true

        when (matchingTestState) {
            MatchingArgState.DripDiluentVolume -> {//去加稀释液
                moveShitTube(sampleStep + 1)
            }
            MatchingArgState.DripStandardVolume -> {//去加标准品
                moveShitTube(sampleStep)
            }
            MatchingArgState.MoveSample -> {//去加已混匀的样本
                goDripSample()
            }
            else -> {}
        }
    }

    private fun cuvetteNeed(cuvettePos: Int, state: CuvetteState): Boolean {
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
    private fun cuvetteNeedStir(cuvettePos: Int) =
        cuvetteNeed(cuvettePos, CuvetteState.DripReagent)

    /**
     * 当前的比色皿位置是否需要检测
     * @param cuvettePos Int
     * @return Boolean
     */
    private fun cuvetteNeedTest(cuvettePos: Int) =
        cuvetteNeed(cuvettePos, CuvetteState.Stir)

    /**
     * 接收到移动比色皿到检测位
     * @param reply ReplyModel<MoveCuvetteTestModel>
     */
    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿到检测位 reply=$reply cuvetteStates=$cuvetteStates matchingTestState=$matchingTestState")

        when (matchingTestState) {
            MatchingArgState.Test2,
            MatchingArgState.Test3,
            MatchingArgState.Test4 -> {
                test()
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
        Timber.d("接收到 取试剂 reply=$reply")
        takeReagentFinish = true

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
        Timber.d("接收到 取样针清洗 reply=$reply shitTubePos=$shitTubePos sampleStep=$sampleStep")
        if (matchingTestState == MatchingArgState.DripDiluentVolume) {//加完稀释液后的清洗
            sampleStep = 0
            matchingTestState = MatchingArgState.DripStandardVolume
            moveShitTube(-shitTubePos + 2)//直接到取标准品的位置
        } else if (matchingTestState == MatchingArgState.DripStandardVolume) {//加标准后的清洗
            if (sampleStep == 3) {
                //开始移动已混匀样本的步骤
                //第一步、先复位采便管和比色皿
                matchingTestState = MatchingArgState.MoveSample
                sampleStep = 0
                moveShitTube(-shitTubePos + 1)
                moveCuvetteShelf(cuvetteShelfPos)
            } else {//继续加标准品
                moveShitTube(-shitTubePos + 2)
            }
        } else if (matchingTestState == MatchingArgState.MoveSample) {//加已混匀的样本的清洗
            if ((sampleStep == 5 && !quality) || (sampleStep == 7 && quality)) {
                //开始加试剂的步骤
                matchingTestState = MatchingArgState.DripReagent
                sampleStep = 0
                cuvettePos = -1
                moveShitTube(-shitTubePos + 1)
                moveCuvetteDripReagent()

                takeReagent()
            } else {//继续移动已混匀的样本
                Timber.d("sampleStep=$sampleStep")
                val targetIndex = moveBlendingPos[sampleStep]
                moveShitTube(targetIndex - shitTubePos)
                moveCuvetteDripSample()
//                Timber.d("sampleStep=$sampleStep")
//                val targetIndex = moveBlendingPos[sampleStep]
//                moveShitTube()
//                moveCuvetteDripSample()
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
        Timber.d("接收到 搅拌针清洗 reply=$reply")
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
        Timber.d("接收到 刺破 reply=$reply")
    }

    /**
     * 接收到获取版本号
     * @param reply ReplyModel<GetVersionModel>
     */
    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        if (!runningMatching()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 获取版本号 reply=$reply")
    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {
    }


    /**
     * 接收到 样本架锁状态
     * @param reply ReplyModel<ShitTubeDoorModel>
     */
    override fun readDataShitTubeDoorModel(reply: ReplyModel<ShitTubeDoorModel>) {
//        Timber.d("接收到 样本门状态 reply=$reply")
//        shitTubeDoorLocked = reply.data.isOpen
//        shitTubeDoorLockedLD.postValue(reply.data.isOpen)
//        //拟合完成后的开门，不成功代表有故障
//        if (matchingTestState == MatchingArgState.Finish) {
//            if (cuvetteDoorLocked && shitTubeDoorLocked) {
//                showMatchingDialog()
//            } else if (!shitTubeDoorLocked) {
//                Timber.d("样本门打开失败")
//                testMsg.postValue("样本门打开失败")
//            }
//        }
//        if (!runningMatching()) return
//        if (!machineStateNormal()) return
        //开始检测前的检测状态，必须开门
//        if (matchingTestState == MatchingArgState.None) {
//
//        }

    }

    /**
     * 接收到 比色皿架锁状态
     * @param reply ReplyModel<CuvetteDoorModel>
     */
    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
//        Timber.d("接收到 比色皿门状态 reply=$reply")
//        cuvetteDoorLocked = reply.data.isOpen
//        cuvetteDoorLockedLD.postValue(reply.data.isOpen)
//        if (matchingTestState == MatchingArgState.Finish && cuvetteDoorLocked && shitTubeDoorLocked) {
//            showMatchingDialog()
//        }
//        if (!runningMatching()) return
//        if (!machineStateNormal()) return
    }

    /**
     * 显示拟合结束对话框
     */
    fun showMatchingDialog() {
        matchingFinishMsg.postValue("拟合结束")
        matchingTestState = MatchingArgState.None
    }

    /**
     * 获取采便管架，比色皿架状态，试剂，清洗液状态
     */
    private fun getState() {
        Timber.d("发送 获取采便管架，比色皿架状态，试剂，清洗液状态")
        SerialPortUtil.Instance.getState()
    }


    /**
     * 移动采便管架·
     * @param pos Int
     */
    private fun moveShitTubeShelf(pos: Int) {
        Timber.d("发送 移动采便管架 pos=$pos")
        shitTubeShelfMoveFinish = false
        SerialPortUtil.Instance.moveShitTubeShelf(pos + 1)
    }

    /**
     * 移动比色皿架
     * @param pos Int
     */
    private fun moveCuvetteShelf(pos: Int) {
        Timber.d("发送 移动比色皿架 pos=$pos")
        cuvetteShelfMoveFinish = false
        SerialPortUtil.Instance.moveCuvetteShelf(pos + 1)
    }

    /**
     * 移动采便管
     * @param step Int
     */
    private fun moveShitTube(step: Int = 1) {
        Timber.d("发送 移动采便管 step=$step")
        shitTubeMoveFinish = false
        shitTubePos += step
        SerialPortUtil.Instance.moveShitTube(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 加样位
     */
    private fun moveCuvetteDripSample(step: Int = 1) {
        Timber.d("发送 移动比色皿到 加样位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.Instance.moveCuvetteDripSample(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 滴试剂位
     */
    private fun moveCuvetteDripReagent(step: Int = 1) {
        Timber.d("发送 移动比色皿到 滴试剂位 step=$step")
        cuvetteMoveFinish = false
        cuvettePos += step
        SerialPortUtil.Instance.moveCuvetteDripReagent(step > 0, step.absoluteValue)
    }

    /**
     * 移动比色皿到 检测位
     */
    private fun moveCuvetteTest(step: Int = 1) {
        Timber.d("发送 移动比色皿到 检测位 matchingTestState=$matchingTestState step=$step")
        cuvettePos += step
        SerialPortUtil.Instance.moveCuvetteTest(step > 0, step.absoluteValue)
    }

    /**
     * 取样
     */
    private fun sampling(volume: Int) {
        Timber.d("发送 取样 volume=$volume")
        samplingFinish = false
        SerialPortUtil.Instance.sampling(volume, squeezing = false)
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
        SerialPortUtil.Instance.samplingProbeCleaning()
    }

    /**
     * 搅拌
     */
    private fun stir() {
        Timber.d("发送 搅拌 cuvettePos=$cuvettePos")
        stirFinish = false
        stirProbeCleaningFinish = false
        SerialPortUtil.Instance.stir()
    }

    /**
     * 搅拌针清洗
     */
    private fun stirProbeCleaning() {
        Timber.d("发送 搅拌针清洗 cuvettePos=$cuvettePos")
        stirProbeCleaningFinish = false
        SerialPortUtil.Instance.stirProbeCleaning()
    }

    /**
     * 加样
     */
    private fun dripSample(autoBlending: Boolean = false, inplace: Boolean = true, volume: Int) {
        Timber.d("发送 加样 volume=$volume")
        dripSamplingFinish = false
        SerialPortUtil.Instance.dripSample(
            autoBlending = autoBlending,
            inplace = inplace,
            volume = volume
        )
    }

    /**
     * 加试剂
     */
    private fun dripReagent() {
        Timber.d("发送 加试剂 cuvettePos=$cuvettePos")
        dripReagentFinish = false
        SerialPortUtil.Instance.dripReagent()
    }

    /**
     * 检测
     */
    private fun test() {
        Timber.d("发送 检测 cuvettePos=$cuvettePos")
        testFinish = false
        SerialPortUtil.Instance.test()
    }

    /**
     * 开始检测
     * 比色皿,采便管，试剂不足
     * 点击我已添加，继续检测
     */
    fun dialogGetStateNotExistConfirm() {
        Timber.d("dialogGetStateNotExistConfirm 点击我已添加，继续检测")
        getState()
    }

    /**
     * 开始检测
     * 比色皿,采便管，试剂不足
     * 点击结束检测
     */
    fun dialogGetStateNotExistCancel() {
        Timber.d("dialogGetStateNotExistCancel 点击结束检测")

    }

    /**
     * 是否正在拟合
     */
    fun runningMatching(): Boolean {
        return matchingTestState != MatchingArgState.None && matchingTestState != MatchingArgState.Finish
    }
}

class MatchingArgsViewModelFactory(private val projectRepository: ProjectRepository = ProjectRepository()) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchingArgsViewModel::class.java)) {
            return MatchingArgsViewModel(projectRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
