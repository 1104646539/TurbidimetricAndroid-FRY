package com.wl.turbidimetric.test

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.matchingTestState
import com.wl.turbidimetric.global.SystemGlobal.repeatabilityState
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.kotlin.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Format
import java.text.NumberFormat
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 重复性测试
 *
 * @property quality Boolean
 */
class RepeatabilityViewModel(val projectRepository: ProjectRepository) : BaseViewModel(),
    Callback2 {

    init {
//        listener()
    }

    fun listener() {
        SerialPortUtil.Instance.callback.add(this)
        Timber.d("SerialPortUtil.Instance.callback listener")
    }

    fun clearListener() {
        SerialPortUtil.Instance.callback.remove(this)
        Timber.d("SerialPortUtil.Instance.callback onCleared")
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
        if (!runningRepeatability()) return
        SystemGlobal.machineArgState = MachineState.RunningError
        Timber.d("报错了，cmd=$cmd state=$state")
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
        if (testState != TestState.None) {
            toastMsg.postValue("正在检测，请勿操作！")
            return
        }
        if (matchingTestState != MatchingArgState.None) {
            toast("正在拟合质控，请勿操作！")
            return
        }
        if (repeatabilityState != RepeatabilityState.None && repeatabilityState != RepeatabilityState.Finish) {
            toastMsg.postValue("正在质控，请勿操作！")
            return
        }
        initState()
        repeatabilityState = RepeatabilityState.GetState;
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
        repeatabilityState = RepeatabilityState.None
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
//        Timber.d("接收到 样本舱门状态 reply=$reply")
    }

    /**
     * 接收到获取状态
     * @param reply ReplyModel<GetStateModel>
     */
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 获取状态 reply=$reply")
        cuvetteShelfStates = reply.data.cuvetteShelfs
        sampleShelfStates = reply.data.sampleShelfs
        val r1Reagent = reply.data.r1Reagent
        val r2Reagent = reply.data.r2Reagent
        val cleanoutFluid = reply.data.cleanoutFluid

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
        repeatabilityState = RepeatabilityState.MoveSample
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
        Timber.d("getInitSampleShelfPos sampleShelfPos=$sampleShelfPos lastSampleShelfPos=$lastSampleShelfPos")
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
        if (repeatabilityState == RepeatabilityState.Finish) {
            cuvetteShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动比色皿架 reply=$reply")
        cuvettePos = 0
        when (repeatabilityState) {
            RepeatabilityState.MoveSample -> {
                moveCuvetteDripSample()
            }
            RepeatabilityState.Test1 -> {

            }
            RepeatabilityState.Test2,
            RepeatabilityState.Test3,
            RepeatabilityState.Test4 -> {
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
     * 获取样本仓门状态
     */
    private fun getSampleDoor() {
        Timber.d("发送 获取样本仓门状态")
        SerialPortUtil.Instance.getSampleDoorState()
    }

    private fun isMatchingFinish(): Boolean {
        return repeatabilityState == RepeatabilityState.Finish && cuvetteShelfMoveFinish && sampleShelfMoveFinish
    }


    /**
     * 接收到移动样本
     * @param reply ReplyModel<MoveSampleModel>
     */
    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动样本 reply=$reply samplePos=$samplePos repeatabilityState=$repeatabilityState sampleStep=$sampleStep")
        sampleMoveFinish = true

        when (repeatabilityState) {
            RepeatabilityState.MoveSample -> {//去取需要移动的已混匀的样本
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
        if (!runningRepeatability()) return
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
        Timber.d("dripReagentAndStirAndTestFinish dripReagentFinish=$dripReagentFinish cuvettePos=$cuvettePos ")
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
            repeatabilityState = RepeatabilityState.Test2
            cuvettePos = -1
            viewModelScope.launch {
                delay(testShelfInterval)
                moveCuvetteTest()
            }
        } else {
            //如果没有结束，移动比色皿，判断是否需要加试剂
            moveCuvetteDripReagent()
            if (cuvetteNeedDripReagent(cuvettePos) ) {
                takeReagent()
            }
        }
    }

    /**
     * 接收到移动样本架
     * @param reply ReplyModel<MoveSampleShelfModel>
     */
    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        Timber.d("接收到 移动样本架 reply=$reply repeatabilityState=$repeatabilityState")
        if (repeatabilityState == RepeatabilityState.Finish) {
            sampleShelfMoveFinish = true
            if (isMatchingFinish()) {
                showMatchingDialog()
                openAllDoor()
            }
        }
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        sampleShelfMoveFinish = true
        if (repeatabilityState != RepeatabilityState.Finish) {
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
        Timber.d("接收到 检测 reply=$reply cuvettePos=$cuvettePos repeatabilityState=$repeatabilityState 检测值=${reply.data.value}")

        calcTestResult(reply.data.value)
    }

    private fun calcTestResult(value: Int) {
        when (repeatabilityState) {
            RepeatabilityState.DripReagent -> {
                updateCuvetteState(cuvettePos - 3, CuvetteState.Test1)
                resultTest1.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest1.add(value)
                updateResult()
                dripReagentAndStirAndTestFinish()
            }
            RepeatabilityState.Test2 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test2)
                resultTest2.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest2.add(value)
                updateResult()
                //检测结束,开始检测第三次
                if (cuvettePos == 9) {
                    repeatabilityState = RepeatabilityState.Test3

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
            RepeatabilityState.Test3 -> {
                updateCuvetteState(cuvettePos, CuvetteState.Test3)
                resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                resultOriginalTest3.add(value)
                updateResult()
                //检测结束,开始检测第四次
                if (cuvettePos == 9) {
                    repeatabilityState = RepeatabilityState.Test4
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
            RepeatabilityState.Test4 -> {
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
        repeatabilityState = RepeatabilityState.Finish
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
        Timber.d("开始计算重复性 $resultTest1 $resultTest2 $resultTest3 $resultTest4")

        result = calcAbsorbanceDifferences(resultTest1, resultTest2, resultTest3, resultTest4)

        //计算吸光度
        val cons = mutableListOf<Double>()
        //计算浓度
        selectProject?.let { it ->
            for (i in result.indices) {
                val con = calcCon(result[i], it)
                cons.add(con.setScale(2, RoundingMode.HALF_UP).toDouble())
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

        val sd = calculateSD(cons.toDoubleArray())
        val mean = calculateMean(cons.toDoubleArray())
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
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 加试剂 reply=$reply cuvettePos=$cuvettePos")
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
        Timber.d("接收到 加样 reply=$reply cuvettePos=$cuvettePos repeatabilityState=$repeatabilityState sampleStep=$sampleStep samplePos=$samplePos")

        dripSamplingFinish = true

        when (repeatabilityState) {
            RepeatabilityState.MoveSample -> {//加完样判断是否结束
                updateCuvetteState(cuvettePos - 1, CuvetteState.DripSample)
//                samplingProbeCleaning()
                if ((sampleStep == 10)) {
                    //开始加试剂的步骤
                    repeatabilityState = RepeatabilityState.DripReagent
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

        Timber.d("加样 cuvetteStates=$cuvetteStates")
    }

    /**
     * 接收到取样
     * @param reply ReplyModel<SamplingModel>
     */
    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取样 reply=$reply repeatabilityState=$repeatabilityState sampleStep=$sampleStep samplePos=$samplePos")

        samplingFinish = true

        when (repeatabilityState) {
            RepeatabilityState.MoveSample -> {//去加已混匀的样本
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
        Timber.d("接收到 移动比色皿到检测位 reply=$reply cuvetteStates=$cuvetteStates repeatabilityState=$repeatabilityState")

        when (repeatabilityState) {
            RepeatabilityState.Test2,
            RepeatabilityState.Test3,
            RepeatabilityState.Test4 -> {
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
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取样针清洗 reply=$reply samplePos=$samplePos sampleStep=$sampleStep")
        if (repeatabilityState == RepeatabilityState.MoveSample) {//加已混匀的样本的清洗
            if ((sampleStep == 9)) {
                //开始加试剂的步骤
                repeatabilityState = RepeatabilityState.DripReagent
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
        Timber.d("接收到 搅拌针清洗 reply=$reply")
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
        Timber.d("接收到 刺破 reply=$reply")
    }

    /**
     * 接收到获取版本号
     * @param reply ReplyModel<GetVersionModel>
     */
    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        if (!runningRepeatability()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 获取版本号 reply=$reply")
    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {


    }


    /**
     * 接收到 样本架锁状态
     * @param reply ReplyModel<SampleDoorModel>
     */
    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {
//        Timber.d("接收到 样本门状态 reply=$reply")
//        sampleDoorLocked = reply.data.isOpen
//        sampleDoorLockedLD.postValue(reply.data.isOpen)
//        //拟合完成后的开门，不成功代表有故障
//        if (repeatabilityState == RepeatabilityState.Finish) {
//            if (cuvetteDoorLocked && sampleDoorLocked) {
//                showMatchingDialog()
//            } else if (!sampleDoorLocked) {
//                Timber.d("样本门打开失败")
//                testMsg.postValue("样本门打开失败")
//            }
//        }
//        if (!runningRepeatability()) return
//        if (!machineStateNormal()) return
        //开始检测前的检测状态，必须开门
//        if (repeatabilityState == RepeatabilityState.None) {
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
//        if (repeatabilityState == RepeatabilityState.Finish && cuvetteDoorLocked && sampleDoorLocked) {
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
        repeatabilityState = RepeatabilityState.None
    }

    /**
     * 获取样本架，比色皿架状态，试剂，清洗液状态
     */
    private fun getState() {
        Timber.d("发送 获取样本架，比色皿架状态，试剂，清洗液状态")
        SerialPortUtil.Instance.getState()
    }


    /**
     * 移动样本架
     * @param pos Int
     */
    private fun moveSampleShelf(pos: Int) {
        Timber.d("发送 移动样本架 pos=$pos")
        sampleShelfMoveFinish = false
        SerialPortUtil.Instance.moveSampleShelf(pos + 1)
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
     * 移动样本
     * @param step Int
     */
    private fun moveSample(step: Int = 1) {
        Timber.d("发送 移动样本 step=$step")
        sampleMoveFinish = false
        samplePos += step
        SerialPortUtil.Instance.moveSample(step > 0, step.absoluteValue)
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
        Timber.d("发送 移动比色皿到 检测位 repeatabilityState=$repeatabilityState step=$step")
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
     * 比色皿,样本，试剂不足
     * 点击我已添加，继续检测
     */
    fun dialogGetStateNotExistConfirm() {
        Timber.d("dialogGetStateNotExistConfirm 点击我已添加，继续检测")
        getState()
    }

    /**
     * 开始检测
     * 比色皿,样本，试剂不足
     * 点击结束检测
     */
    fun dialogGetStateNotExistCancel() {
        Timber.d("dialogGetStateNotExistCancel 点击结束检测")

    }

    /**
     * 是否正在测试重复性
     */
    fun runningRepeatability(): Boolean {
        return repeatabilityState != RepeatabilityState.None && repeatabilityState != RepeatabilityState.Finish
    }
}

class RepeatabilityViewModelFactory(private val projectRepository: ProjectRepository = ProjectRepository()) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RepeatabilityViewModel::class.java)) {
            return RepeatabilityViewModel(projectRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
