package com.wl.turbidimetric.test

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.global.SystemGlobal.testType
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.home.TestResultRepository
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.kotlin.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue
import com.wl.wllib.LogToFile.i

/**
 * 重复性测试
 *
 * @property quality Boolean
 */
class RepeatabilityViewModel(
    val projectRepository: ProjectRepository,
    val testResultRepository: TestResultRepository
) : BaseViewModel(),
    Callback2 {

    init {
//        listener()
    }

    fun listener() {
        SerialPortUtil.callback.add(this)
        i("SerialPortUtil.callback listener")
    }

    fun clearListener() {
        SerialPortUtil.callback.remove(this)
        i("SerialPortUtil.callback onCleared")
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
     * 移动已混匀样本的量
     */
    private val moveSampleVolume = 25


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

    /**
     * 开始检测 比色皿，样本，试剂,清洗液不存在
     */
    val getStateNotExistMsg = MutableLiveData("")

    /**
     * 拟合质控结束提示
     */
    val matchingFinishMsg = MutableLiveData("")


    val testMsg = MutableLiveData("")
    val toastMsg = MutableLiveData("")

    val projectDatas = projectRepository.allDatas.flow()

    var selectProject: ProjectModel? = null

    /**
     * 每排之间的检测间隔
     */
    var testShelfInterval: Long = 1000 * 0;

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
    val testS: Long = 1000

    //测试用 每个比色皿之间的检测间隔
    val testP: Long = 1000


    /**
     * 测试用的 end
     */
    override fun init() {
        super.init()

    }


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
     * 报错
     * @param cmd UByte
     * @param state UByte
     */
    override fun readDataStateFailed(cmd: UByte, state: UByte) {
        if (!runningRepeatability()) return
        testState = TestState.RunningError
        i("报错了，cmd=$cmd state=$state")
        testMsg.postValue("报错了，cmd=$cmd state=$state")
    }


    /**
     * 点击开始拟合
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
        if (testState != TestState.Normal) {
            toastMsg.postValue("正在检测，请勿操作！")
            return
        }
        initState()
        testState = TestState.GetState;
        testType = TestType.Repeatability
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
                        detectionNum = LocalData.getDetectionNumInc(),
                        concentration = cons[index],
                        absorbances = item,
                        testTime = Date().time,
                        testValue1 = resultTest1[index],
                        testValue2 = resultTest2[index],
                        testValue3 = resultTest3[index],
                        testValue4 = resultTest4[index],
                        testOriginalValue1 = testOriginalValues1[index],
                        testOriginalValue2 = testOriginalValues2[index],
                        testOriginalValue3 = testOriginalValues3[index],
                        testOriginalValue4 = testOriginalValues4[index],
                    ).apply {
                        project.target = selectProject
                    }
                )
            }
            with(Dispatchers.Main) {
                toast("转移完成，请勿多次点击")
            }
        }

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
        testState = TestState.None
        sampleStep = 0
        testMsg.postValue("")
        samplePos = -1;
        cuvettePos = -1;
        sampleShelfPos = -1;
        cuvetteShelfPos = -1;

        if (SystemGlobal.isCodeDebug) {
            testShelfInterval = testS;
            testPosInterval = testP;

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

    /**
     * 接收到获取状态
     * @param reply ReplyModel<GetStateModel>
     */
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 获取状态 reply=$reply")
        cuvetteShelfStates = reply.data.cuvetteShelfs
        sampleShelfStates = reply.data.sampleShelfs
        val r1Reagent = reply.data.r1Reagent
        val r2Reagent = reply.data.r2Reagent
        val cleanoutFluid = reply.data.cleanoutFluid

        getInitialPos()

        i("cuvetteShelfPos=${cuvetteShelfPos} sampleShelfPos=${sampleShelfPos}")
        if (cuvetteShelfPos == -1) {
            i("没有比色皿架")
            getStateNotExistMsg.postValue("比色皿不足，请添加")
            return
        }
        if (sampleShelfPos == -1) {
            i("没有样本架")
            getStateNotExistMsg.postValue("样本不足，请添加")
            return
        }
        if (!r1Reagent) {
            i("没有R1试剂")
            getStateNotExistMsg.postValue("R1试剂不足，请添加")
            return
        }
        if (!r2Reagent) {
            i("没有R2试剂")
            getStateNotExistMsg.postValue("R2试剂不足，请添加")
            return
        }
        if (!cleanoutFluid) {
            i("没有清洗液试剂")
            getStateNotExistMsg.postValue("清洗液不足，请添加")
            return
        }
        testState = TestState.MoveSample
        //开始检测
        moveSampleShelf(sampleShelfPos)
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
        if (testState == TestState.TestFinish) {
            cuvetteShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 移动比色皿架 reply=$reply")
        cuvettePos = 0
        when (testState) {
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
//        openSampleDoor()
//        openCuvetteDoor()
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
     * 接收到移动样本
     * @param reply ReplyModel<MoveSampleModel>
     */
    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 移动样本 reply=$reply samplePos=$samplePos testState=$testState sampleStep=$sampleStep")
        sampleMoveFinish = true

        when (testState) {
            TestState.MoveSample -> {//去取需要移动的已混匀的样本
                sampleStep++
                sampling(moveSampleVolume)
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
            dripSample(autoBlending = false, inplace = false, moveSampleVolume)
        }
    }

    /**
     * 接收到移动比色皿到加试剂位
     * @param reply ReplyModel<MoveCuvetteDripReagentModel>
     */
    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
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
        i("dripReagentAndStirAndTestFinish dripReagentFinish=$dripReagentFinish cuvettePos=$cuvettePos ")
        if ((cuvetteNeedDripReagent(cuvettePos) && !dripReagentFinish)) {
            return
        }
        if (cuvettePos > 1 && ((cuvetteNeedStir(cuvettePos - 2) && !stirFinish) || !stirProbeCleaningFinish)) {
            return
        }
        if (cuvettePos > 2 && (cuvetteNeedTest(cuvettePos - 3) && !testFinish)) {
            return
        }

        if (cuvettePos == 12) {
            //最后一个也检测结束了
            testState = TestState.Test2
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
     * 接收到移动样本架
     * @param reply ReplyModel<MoveSampleShelfModel>
     */
    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        i("接收到 移动样本架 reply=$reply testState=$testState")
        if (testState == TestState.TestFinish) {
            sampleShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningRepeatability()) return
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
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 检测 reply=$reply cuvettePos=$cuvettePos testState=$testState 检测值=${reply.data.value}")

        calcTestResult(reply.data.value)
    }

    private fun calcTestResult(value: Int) {
        when (testState) {
            TestState.DripReagent -> {
                updateCuvetteState(cuvettePos - 3, CuvetteState.Test1)
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
                if (cuvettePos == 9) {
                    testState = TestState.Test3

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
            TestState.Test3 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test3)
                resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest3.add(value)
                updateResult()
                //检测结束,开始检测第四次
                if (cuvettePos == 9) {
                    testState = TestState.Test4
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
            TestState.Test4 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test4)
                resultTest4.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest4.add(value)
                updateResult()
                //检测结束,计算拟合参数
                if (cuvettePos == 9) {
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
        testState = TestState.TestFinish
        moveCuvetteShelf(-1)
        moveSampleShelf(-1)
        samplingProbeCleaning()
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
        if (!machineStateNormal()) return
        i("接收到 搅拌 reply=$reply cuvettePos=$cuvettePos")

        stirFinish = true
        updateCuvetteState(cuvettePos - 2, CuvetteState.Stir)
        stirProbeCleaning()
    }

    /**
     * 接收到加试剂
     * @param reply ReplyModel<DripReagentModel>
     */
    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
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
        if (!machineStateNormal()) return
        i("接收到 加样 reply=$reply cuvettePos=$cuvettePos testState=$testState sampleStep=$sampleStep samplePos=$samplePos")

        dripSamplingFinish = true

        when (testState) {
            TestState.MoveSample -> {//加完样判断是否结束
                updateCuvetteState(cuvettePos - 1, CuvetteState.DripSample)
//                samplingProbeCleaning()
                if ((sampleStep == 10)) {
                    //开始加试剂的步骤
                    testState = TestState.DripReagent
                    sampleStep = 0
                    cuvettePos = -1
//                moveSample(-samplePos + 1)
                    moveCuvetteDripReagent()

                    takeReagent()
                } else {//继续取样。同一个样本位置
//                    moveSample()
                    sampleStep++
                    sampling(moveSampleVolume)
                    moveCuvetteDripSample()
                }
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
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 取样 reply=$reply testState=$testState sampleStep=$sampleStep samplePos=$samplePos")

        samplingFinish = true

        when (testState) {
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
        if (!machineStateNormal()) return
        i("接收到 移动比色皿到检测位 reply=$reply cuvetteStates=$cuvetteStates testState=$testState")

        when (testState) {
            TestState.Test2,
            TestState.Test3,
            TestState.Test4 -> {
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
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 取试剂 reply=$reply")
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
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 取样针清洗 reply=$reply samplePos=$samplePos sampleStep=$sampleStep")
        if (testState == TestState.MoveSample) {//加已混匀的样本的清洗
            if ((sampleStep == 9)) {
                //开始加试剂的步骤
                testState = TestState.DripReagent
                sampleStep = 0
                cuvettePos = -1
//                moveSample(-samplePos + 1)
                moveCuvetteDripReagent()

                takeReagent()
            } else {//继续移动已混匀的样本
                moveSample()
                moveCuvetteDripSample()
            }
        }
    }


    /**
     * 接收到搅拌针清洗
     * @param reply ReplyModel<StirProbeCleaningModel>
     */
    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        if (!runningRepeatability()) return
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
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        i("接收到 刺破 reply=$reply")
    }

    /**
     * 接收到获取版本号
     * @param reply ReplyModel<GetVersionModel>
     */
    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        if (!runningRepeatability()) return
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
//        i("接收到 样本门状态 reply=$reply")
//        sampleDoorLocked = reply.data.isOpen
//        sampleDoorLockedLD.postValue(reply.data.isOpen)
//        //拟合完成后的开门，不成功代表有故障
//        if (testState == TestState.TestFinish) {
//            if (cuvetteDoorLocked && sampleDoorLocked) {
//                showMatchingDialog()
//            } else if (!sampleDoorLocked) {
//                i("样本门打开失败")
//                testMsg.postValue("样本门打开失败")
//            }
//        }
//        if (!runningRepeatability()) return
//        if (!machineStateNormal()) return
        //开始检测前的检测状态，必须开门
//        if (testState == TestState.None) {
//
//        }

    }

    /**
     * 接收到 比色皿架锁状态
     * @param reply ReplyModel<CuvetteDoorModel>
     */
    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
//        i("接收到 比色皿门状态 reply=$reply")
//        cuvetteDoorLocked = reply.data.isOpen
//        cuvetteDoorLockedLD.postValue(reply.data.isOpen)
//        if (testState == TestState.TestFinish && cuvetteDoorLocked && sampleDoorLocked) {
//            showMatchingDialog()
//        }
//        if (!runningRepeatability()) return
//        if (!machineStateNormal()) return
    }

    /**
     * 显示拟合结束对话框
     */
    fun showMatchingDialog() {
        matchingFinishMsg.postValue("重复性检测结束")
        testState = TestState.Normal
    }

    /**
     * 获取样本架，比色皿架状态，试剂，清洗液状态
     */
    private fun getState() {
        i("发送 获取样本架，比色皿架状态，试剂，清洗液状态")
        SerialPortUtil.getState()
    }


    /**
     * 移动样本架
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
        SerialPortUtil.sampling(volume, squeezing = false)
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

    }

    /**
     * 是否正在测试重复性
     */
    fun runningRepeatability(): Boolean {
        return isTestRunning()
    }
}

class RepeatabilityViewModelFactory(
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val testResultRepository: TestResultRepository = TestResultRepository()
) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RepeatabilityViewModel::class.java)) {
            return RepeatabilityViewModel(projectRepository, testResultRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
