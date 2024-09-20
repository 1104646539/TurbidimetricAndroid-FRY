package com.wl.turbidimetric.test.repeatablitylity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.log.DbLogUtil
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.repository.if2.CurveSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.TestResultSource
import com.wl.turbidimetric.util.Callback2
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * 重复性测试
 *
 * @property quality Boolean
 */
class RepeatabilityViewModel(
    private val appViewModel: AppViewModel,
    val curveRepository: CurveSource,
    val testResultRepository: TestResultSource,
    val localDataRepository: LocalDataSource
) : BaseViewModel(),
    Callback2 {

    init {
//        listener()
    }

    fun listener() {
        appViewModel.serialPort.addCallback(this)
        i("appViewModel.serialPort.callback listener")
    }

    fun clearListener() {
        appViewModel.serialPort.removeCallback(this)
        i("appViewModel.serialPort.callback onCleared")
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
     * 浓度
     */
    var cons = mutableListOf<Int>()

    /**
     * 吸光度
     */
    private var result = arrayListOf<BigDecimal>()


    /**
     * 对话框状态，不包括调试的和不需要viewmodel处理的
     */
    private val _dialogUiState = MutableSharedFlow<RepeatabilityUiState>()
    val dialogUiState = _dialogUiState.asSharedFlow()


    /**
     * 当前取样的步骤
     */
    private var sampleStep: Int = 0

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

    val testMsg = MutableLiveData("")
    val toastMsg = MutableLiveData("")

    val projectDatas = curveRepository.listenerCurve()

    var selectProject: CurveModel? = null

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
     * 移动样本时检测到样本管的标记
     */
    var isDetectedSample = false


    /**
     * 开始检测前的清洗取样针
     */
    var cleaningBeforeStartTest = false

    /**
     * 记录每个比色皿的搅拌时间
     */
    val stirTimes = longArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /**
     * 结束时的异常提示信息
     */
    var finishErrorMsg = "";

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
    val testS: Long = 1000

    //测试用 每个比色皿之间的检测间隔
    val testP: Long = 1000

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

//    /**
//     * 报错
//     * @param cmd UByte
//     * @param state UByte
//     */
//    override fun readDataStateFailed(cmd: UByte, state: UByte) {
//        if (!runningRepeatability()) return
//        testState = TestState.RunningError
//        i("报错了，cmd=$cmd state=$state")
//        testMsg.postValue("报错了，cmd=$cmd state=$state")
//    }


    /**
     * 点击开始拟合
     */
    fun clickStart() {
//        if (DoorAllOpen()) {
//            toast("舱门未关")
//            return
//        }
        if (appViewModel.testState.isNotPrepare()) {
            toastMsg.postValue("请重新自检或重启仪器！")
            return
        }
        if (appViewModel.testState != TestState.Normal) {
            toastMsg.postValue("正在检测，请勿操作！")
            return
        }
        initState()
        appViewModel.testState = TestState.GetState
        appViewModel.testType = TestType.Repeatability
        getState()
    }

    /**
     * 添加检测数据到数据管理
     */
    fun clickMoveToRepository() {
        if (result.isEmpty()) {
            toast("未检测")
            return
        }
        viewModelScope.launch {
            result.forEachIndexed { index, item ->
                testResultRepository.addTestResult(
                    TestResultModel(
                        detectionNum = localDataRepository.getDetectionNumInc(),
                        concentration = cons[index],
                        absorbances = item,
                        testTime = Date().time,
                        testValue1 = resultTest1[index],
                        testValue2 = resultTest2[index],
                        testValue3 = resultTest3[index],
                        testValue4 = resultTest4[index],
                        testOriginalValue1 = resultOriginalTest1[index],
                        testOriginalValue2 = resultOriginalTest2[index],
                        testOriginalValue3 = resultOriginalTest3[index],
                        testOriginalValue4 = resultOriginalTest4[index],
                        curveOwnerId = selectProject?.curveId ?: 0
                    )
                )
            }
            withContext(Dispatchers.Main) {
                toast("转移完成，请勿多次点击")
            }
        }

    }

    private fun initState() {
        finishErrorMsg = ""
        isDetectedSample = false
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
            testShelfInterval1 = 10 * 1000
            testShelfInterval2 = 50 * 1000
            testShelfInterval3 = 110 * 1000
            testShelfInterval4 = 170 * 1000
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
        if (!runningRepeatability()) return
//        i("接收到 样本舱门状态 reply=$reply")
    }

    /**
     * 挤压
     * @param reply ReplyModel<SqueezingModel>
     */
    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {
        if (!runningRepeatability()) return
        i("接收到 挤压 reply=$reply")
    }

    override fun readDataMotor(reply: ReplyModel<MotorModel>) {

    }

    override fun readDataOverloadParamsModel(reply: ReplyModel<OverloadParamsModel>) {

    }

    /**
     * 接收到获取状态
     * @param reply ReplyModel<GetStateModel>
     */
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 获取状态 reply=$reply")
        cuvetteShelfStates = reply.data.cuvetteShelfs
        sampleShelfStates = reply.data.sampleShelfs
        val r1Reagent = reply.data.r1Reagent
        val r2Reagent = reply.data.r2Reagent
        val cleanoutFluid = reply.data.cleanoutFluid

        getInitialPos()

        i("cuvetteShelfPos=${cuvetteShelfPos} sampleShelfPos=${sampleShelfPos}")
        var failedText = if (cuvetteShelfPos == -1) {
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
        if (failedText.isNotEmpty()) {
            viewModelScope.launch {
                _dialogUiState.emit(
                    RepeatabilityUiState(
                        DialogState.GetStateNotExist,
                        failedText
                    )
                )
            }
            return
        }
        appViewModel.testState = TestState.MoveSample
        //开始检测
        cleaningBeforeStartTest = true
        samplingProbeCleaning()
//        moveSampleShelf(sampleShelfPos)
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
        if (appViewModel.testState == TestState.TestFinish) {
            cuvetteShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 移动比色皿架 reply=$reply")
        cuvettePos = 0
        when (appViewModel.testState) {
            TestState.MoveSample -> {
                moveCuvetteDripSample()
            }

            TestState.Test1 -> {

            }

            TestState.Test2,
            TestState.Test3,
            TestState.Test4 -> {
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
     * 接收到移动样本
     * @param reply ReplyModel<MoveSampleModel>
     */
    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 移动样本 reply=$reply samplePos=$samplePos testState=${appViewModel.testState} sampleStep=$sampleStep")
        sampleMoveFinish = true

        when (appViewModel.testState) {
            TestState.MoveSample -> {//因为需要检测第一个样本是否是比色杯，所以一次移动一个位置
                if (samplePos == 0) {
                    //如果是样本管，直接检测结束，并报错，因为质控不允许使用样本管
                    if (reply.data.type.isSample()) {
                        isDetectedSample = true
                        matchingFinish()
                        return
                    }
                    moveSample(1)
                } else if (samplePos == 1) {
                    sampleStep++
                    sampling(localDataRepository.getSamplingVolume().roundToInt())
                }
            }

            else -> {

            }
        }

    }


    /**
     * 接收到移动比色皿到加样位
     * @param reply ReplyModel<MoveCuvetteDripSampleModel>
     */
    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {
        if (!runningRepeatability()) return
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
            dripSample(
                autoBlending = false,
                inplace = false,
                localDataRepository.getSamplingVolume().roundToInt()
            )
        }
    }

    /**
     * 接收到移动比色皿到加试剂位
     * @param reply ReplyModel<MoveCuvetteDripReagentModel>
     */
    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 移动比色皿到加试剂位 reply=$reply cuvetteStates=$cuvetteStates")

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
            val intervalTemp = testShelfInterval1 - (Date().time - stirTime)
            i("intervalTemp=$intervalTemp stirTime=$stirTime testShelfInterval1=$testShelfInterval1")
            viewModelScope.launch {
                delay(intervalTemp)
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
        i("dripReagentAndStirAndTestFinish dripReagentFinish=$dripReagentFinish cuvettePos=$cuvettePos ")
        if ((cuvetteNeedDripReagent(cuvettePos) && !dripReagentFinish)) {
            return
        }
        if (cuvettePos > 1 && ((cuvetteNeedStir(cuvettePos - 2) && !stirFinish) || !stirProbeCleaningFinish)) {
            return
        }
        if (cuvettePos > 4 && (cuvetteNeedTest(cuvettePos - 5) && !testFinish)) {
            return
        }

        if (cuvettePos == 14) {
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
        if (appViewModel.testState == TestState.TestFinish) {
            sampleShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        sampleShelfMoveFinish = true
        if (appViewModel.testState != TestState.TestFinish) {
            //一开始就要移动到第二个位置去取第一个位置的稀释液
            moveSample(1)
        }
    }


    /**
     * 接收到检测
     * @param reply ReplyModel<TestModel>
     */
    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 检测 reply=$reply cuvettePos=$cuvettePos testState=${appViewModel.testState} 检测值=${reply.data.value}")

        calcTestResult(reply.data.value)
    }

    private fun calcTestResult(value: Int) {
        val tempCuvettePos = cuvettePos - 1
        when (appViewModel.testState) {
            TestState.DripReagent -> {
                updateCuvetteState(cuvettePos - 5, CuvetteState.Test1)
                resultTest1.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest1.add(value)
                updateResult()
                dripReagentAndStirAndTestFinish()
            }

            TestState.Test2 -> {
                updateCuvetteState(tempCuvettePos, CuvetteState.Test2)
                resultTest2.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest2.add(value)
                updateResult()
                //检测结束,开始检测第三次
                if (tempCuvettePos == 9) {
                    appViewModel.testState = TestState.Test3

                    moveCuvetteTest(-cuvettePos)
                } else {
                    delayMoveCuvetteTest(cuvettePos)
                }

            }

            TestState.Test3 -> {
                updateCuvetteState(tempCuvettePos, CuvetteState.Test3)
                resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest3.add(value)
                updateResult()
                //检测结束,开始检测第四次
                if (tempCuvettePos == 9) {
                    appViewModel.testState = TestState.Test4

                    moveCuvetteTest(-cuvettePos)
                } else {
                    delayMoveCuvetteTest(cuvettePos)
                }
            }

            TestState.Test4 -> {
                updateCuvetteState(tempCuvettePos, CuvetteState.Test4)
                resultTest4.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest4.add(value)
                updateResult()
                //检测结束,计算拟合参数
                if (tempCuvettePos == 9) {
                    matchingFinish()
                    calcMatchingArg()
                } else {
                    delayMoveCuvetteTest(cuvettePos)

                }
            }

            else -> {}
        }


    }
    /**
     * 根据检测次数 和 比色皿的位置计算还有多久才移动到下一个检测位
     * @param cuvettePos Int
     */
    private fun delayMoveCuvetteTest(cuvettePos: Int) {
        val targetTime =
            if (appViewModel.testState == TestState.Test2) testShelfInterval2 else if (appViewModel.testState == TestState.Test3) testShelfInterval3 else testShelfInterval4
        val stirTime = stirTimes[cuvettePos]
        val intervalTemp = targetTime - (Date().time - stirTime)
        i("intervalTemp=$intervalTemp stirTime=$stirTime targetTime=$targetTime")
        viewModelScope.launch {
            delay(intervalTemp)
            moveCuvetteTest()
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
     * 计算重复性
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
        i("开始计算重复性 $resultTest1 $resultTest2 $resultTest3 $resultTest4")

        result = calcAbsorbanceDifferences(resultTest1, resultTest2, resultTest3, resultTest4)

        cons.clear()
        //计算浓度
        selectProject?.let { it ->
            for (i in result.indices) {
                val con = calcCon(result[i], it)
                cons.add(con)
            }
        }

//        cons.clear()
//        cons.addAll(
//            mutableListOf(
//                495.0,
//                489.0,
//                481.0,
//                490.0,
//                469.0,
//                473.0,
//                465.0,
//                470.0,
//                459.0,
//                471.0,
//            )
//        )
        val temp2 = cons.toIntArray().map { it.toDouble() }.toDoubleArray()
        val sd = calculateSD(temp2)
        val mean = calculateMean(temp2)
        val cv = sd / mean

        val pi = NumberFormat.getPercentInstance()
        pi.maximumFractionDigits = 2
        val ncv = pi.format(cv)

        var msg: StringBuilder = StringBuilder(
            "第一次原始:$resultOriginalTest1 \n" +
                    "第一次:$resultTest1 \n" +
                    "第二次原始:$resultOriginalTest2 \n" +
                    "第二次:$resultTest2 \n" +
                    "第三次原始:$resultOriginalTest3 \n" +
                    "第三次:$resultTest3 \n" +
                    "第四次原始:$resultOriginalTest4 \n" +
                    "第四次:$resultTest4 \n" +
                    "吸光度:$result \n" +
                    "四参数：f0=${selectProject!!.f0} f1=${selectProject!!.f1} f2=${selectProject!!.f2} f3=${selectProject!!.f3} \n " +
                    "浓度 ${cons}\n" +
                    "标准方差=$sd \n cv=${ncv} \n"
        )

//        PrintUtil.printMatchingQuality(absorbancys, nds, yzs, cf.params, quality)
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
    }

    /**
     * 接收到搅拌
     * @param reply ReplyModel<StirModel>
     */
    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 搅拌 reply=$reply cuvettePos=$cuvettePos")
        stirFinish = true
        updateStirTime()
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
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 加试剂 reply=$reply cuvettePos=$cuvettePos")
        dripReagentFinish = true
        takeReagentFinish = false
        updateCuvetteState(cuvettePos, CuvetteState.DripReagent)

        dripReagentAndStirAndTestFinish()
    }

    /**
     * 接收到加样
     * @param reply ReplyModel<DripSampleModel>
     */
    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 加样 reply=$reply cuvettePos=$cuvettePos testState=${appViewModel.testState} sampleStep=$sampleStep samplePos=$samplePos cuvetteStates=$cuvetteStates")

        dripSamplingFinish = true

        if (reply.state == ReplyState.CUVETTE_NOT_EMPTY) {
            //比色皿非空
            finishErrorMsg = "比色皿非空"
            matchingFinish();
        } else {
            samplingProbeCleaning()
        }


    }

    /**
     * 接收到取样
     * @param reply ReplyModel<SamplingModel>
     */
    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 取样 reply=$reply testState=${appViewModel.testState} sampleStep=$sampleStep samplePos=$samplePos")

        samplingFinish = true
        if (reply.state == ReplyState.SAMPLING_FAILED) {//取样失败
            //取样失败
            finishErrorMsg = "取样失败"
            matchingFinish();
        } else {
            when (appViewModel.testState) {
                TestState.MoveSample -> {//去加已混匀的样本
                    goDripSample()
                }

                else -> {}
            }
//        }
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
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 移动比色皿到检测位 reply=$reply cuvetteStates=$cuvetteStates testState=${appViewModel.testState}")

        if (cuvettePos == 0) {
            //当前位置为等待检测的位置，到时间时才移动到第一个检测位置
            val firstTime = getFirstDelayTime()
            viewModelScope.launch {
                delay(firstTime)
                moveCuvetteTest()
            }
        } else {
            test()
        }
    }
    /**
     * 获取这次检测的第一个要检测的比色皿的间隔时间
     * @return Long
     */
    private fun getFirstDelayTime(): Long {
        val targetTime =
            if (appViewModel.testState == TestState.Test2) testShelfInterval2 else if (appViewModel.testState == TestState.Test3) testShelfInterval3 else testShelfInterval4
        val stirTime = stirTimes.first { it.toInt() != 0 }
        val intervalTemp = targetTime - (Date().time - stirTime) - 1000
        i("getFirstDelayTime intervalTemp=$intervalTemp stirTime=$stirTime targetTime=$targetTime")
        return intervalTemp
    }
    /**
     * 接收到取试剂
     * @param reply ReplyModel<TakeReagentModel>
     */
    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 取试剂 reply=$reply")
        takeReagentFinish = true

        if (reply.state == ReplyState.TAKE_REAGENT_FAILED) {
            //取试剂失败
            finishErrorMsg = "取试剂失败"
            matchingFinish();
        } else {
            //取完试剂判断是否移动比色皿完成，完成就直接取样
            if (cuvetteNeedDripReagent(cuvettePos) && cuvetteMoveFinish && takeReagentFinish) {
                dripReagent()
            }
        }
    }

    /**
     * 接收到取样针清洗
     * @param reply ReplyModel<SamplingProbeCleaningModel>
     */
    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 取样针清洗 reply=$reply samplePos=$samplePos sampleStep=$sampleStep")
        if (cleaningBeforeStartTest) {
            //是开始检测前的清洗，清洗完才开始检测
            cleaningBeforeStartTest = false
            moveSampleShelf(sampleShelfPos)
            moveCuvetteShelf(cuvetteShelfPos)
            return
        }
        when (appViewModel.testState) {
            TestState.MoveSample -> {//加完样判断是否结束
                updateCuvetteState(cuvettePos - 1, CuvetteState.DripSample)
                if ((sampleStep == 10)) {
                    //开始加试剂的步骤
                    appViewModel.testState = TestState.DripReagent
                    sampleStep = 0
                    cuvettePos = -1
                    moveCuvetteDripReagent()

                    takeReagent()
                } else {//去清洗
                    sampleStep++
                    sampling(localDataRepository.getSamplingVolume().roundToInt())
                    moveCuvetteDripSample()
                }
            }

            else -> {}
        }
    }


    /**
     * 接收到搅拌针清洗
     * @param reply ReplyModel<StirProbeCleaningModel>
     */
    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        if (!runningRepeatability()) return
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
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 刺破 reply=$reply")
    }

    /**
     * 接收到获取版本号
     * @param reply ReplyModel<GetVersionModel>
     */
    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        if (!runningRepeatability()) return
        if (appViewModel.testState.isNotPrepare()) return
        i("接收到 获取版本号 reply=$reply")
    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {


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
                        RepeatabilityUiState(
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
        appViewModel.testState = TestState.RunningError
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
    fun showMatchingDialog() {
        if (isDetectedSample) {
            isDetectedSample = false
            appViewModel.testState = TestState.Normal
            viewModelScope.launch {
                _dialogUiState.emit(
                    RepeatabilityUiState(
                        DialogState.TestFinish,
                        "检测到放入的是样本管，请在样本架上放置比色杯"
                    )
                )
            }
        } else {
            var msg = "重复性检测结束"
            if (finishErrorMsg.isNotEmpty()) {
                msg += ",$finishErrorMsg"
            }
            appViewModel.testState = TestState.Normal
            viewModelScope.launch {
                _dialogUiState.emit(
                    RepeatabilityUiState(
                        DialogState.TestFinish,
                        "$msg"
                    )
                )
            }
        }
    }

    /**
     * 获取样本架，比色皿架状态，试剂，清洗液状态
     */
    private fun getState() {
        i("发送 获取样本架，比色皿架状态，试剂，清洗液状态")
        appViewModel.serialPort.getState()
    }


    /**
     * 移动样本架
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
    private fun dripSample(
        autoBlending: Boolean = false,
        inplace: Boolean = true,
        volume: Int
    ) {
        i("发送 加样 volume=$volume")
        dripSamplingFinish = false
        appViewModel.serialPort.dripSample(
            autoBlending = autoBlending,
            inplace = inplace,
            volume = volume
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
     * 是否正在测试重复性
     */
    fun runningRepeatability(): Boolean {
        return appViewModel.testState.isTestRunning()
    }
}

class RepeatabilityViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
    private val curveRepository: CurveSource = ServiceLocator.provideCurveSource(App.instance!!),
    private val testResultRepository: TestResultSource = ServiceLocator.provideTestResultSource(
        App.instance!!
    ),
    private val localDataRepository: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!)
) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RepeatabilityViewModel::class.java)) {
            return RepeatabilityViewModel(
                appViewModel,
                curveRepository,
                testResultRepository,
                localDataRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
