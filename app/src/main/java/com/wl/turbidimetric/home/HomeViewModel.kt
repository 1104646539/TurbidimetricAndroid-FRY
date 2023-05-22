package com.wl.turbidimetric.home

import android.view.View
import androidx.lifecycle.*
import androidx.lifecycle.livedata.core.R
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.cuvetteDoorIsOpen
import com.wl.turbidimetric.global.SystemGlobal.machineArgState
import com.wl.turbidimetric.global.SystemGlobal.matchingTestState
import com.wl.turbidimetric.global.SystemGlobal.shitTubeDoorIsOpen
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.OnScanResult
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.kotlin.flow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
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

        listenerDoorState()
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
    private var cons = arrayListOf<BigDecimal>()

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
     * 正常检测 采便管不足对话框
     */
    val dialogTestShitTubeDeficiency = MutableLiveData(false)

    /**
     * 开始检测 比色皿，采便管，试剂不存在
     */
    val getStateNotExistMsg = MutableLiveData("")

    /**采便管架状态 1有 0无 顺序是从中间往旁边
     *
     */
    private var shitTubeShelfStates: IntArray = IntArray(4)

    /**比色皿架状态 1有 0无 顺序是从中间往旁边
     *
     */
    private var cuvetteShelfStates: IntArray = IntArray(4)

    /**当前排所有比色皿的状态
     *
     */
    private var cuvetteStates = initCuvetteStates()


    /**当前排所有采便管的状态
     *
     */
    private var shitTubesStates = initShitTubeStates()

    /**当前采便管架位置
     *
     */
    private var shitTubeShelfPos = -1

    /**最后一排可使用的采便管架的位置
     *
     */
    private var lastShitTubeShelfPos = -1

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

    /**当前采便管位置
     *
     */
    private var shitTubePos = -1

    /**采便管最大步数
     *
     */
    private val shitTubeMax = 10

    /**比色皿架是否移动完毕
     *
     */
    private var cuvetteShelfMoveFinish = false

    /**采便管架是否移动完毕
     *
     */
    private var shitTubeShelfMoveFinish = false

    /**比色皿是否移动完毕
     *
     */
    private var cuvetteMoveFinish = false

    /**采便管是否移动完毕
     *
     */
    private var shitTubeMoveFinish = false

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
     * 输入的跳过的比色皿数量
     */
    var skipCuvetteNum = "0";

    /**
     *比色皿起始的位置，只限于第一排比色皿用来跳过前面的几个已经使用过的比色皿
     */
    var cuvetteStartPos = 0;


    /**继续检测后获取状态 判断是否是来自比色皿不足
     *
     */
    private var continueTestCuvetteState = false

    /**继续检测后获取状态 判断是否是来自采便管不足
     *
     */
    private var continueTestShitTubeState = false

    /**比色皿不足
     *
     */
    private var cuvetteDeficiency = false

    /**采便管不足
     *
     */
    private var shitTubeDeficiency = false

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
     * 取试剂是否完成
     */
    private var takeReagentFinish = false


    val testMsg = MutableLiveData("");

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
     * 扫码结果
     */
    var scanResults = arrayListOf<String?>()

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

    /**
     * 测试用的 start
     */
    //检测的值
    private val testValues1 = doubleArrayOf(
        0.544411,
        0.555225,
        0.252525,
        0.22498982,
        0.464510042,
        0.2454562,
        0.6633532,
        0.74412212,
        0.0122222,
        0.1213342
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
    val tempShitTubeState = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
//  val tempShitTubeState = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 1)

    //测试用，采便管是否存在
    val shitTubeExists = mutableListOf(true, true, true, true, true, true, true, true, true, true)

    //测试用 每排之间的检测间隔
    val testS: Long = 0;

    //测试用 每个比色皿之间的检测间隔
    val testP: Long = 100;

    /**
     * 一直获取舱门状态
     */
    private fun listenerDoorState() {
        viewModelScope.launch {
            timer("", true, Date(), 1500) {
                if ((testState == TestState.None || testState == TestState.TestFinish)
                    && (matchingTestState == MatchingArgState.None || matchingTestState == MatchingArgState.Finish)
                ) {
                    SerialPortUtil.Instance.getShitTubeDoorState()
                    SerialPortUtil.Instance.getCuvetteDoorState()
                }
            }
        }
    }

    /**
     * 测试用的 end
     */
    fun clickStart() {
        if (DoorAllOpen()) {
            toast("舱门未关")
            return
        }
        if (testState != TestState.None) {
            toast("正在检测，请勿操作！")
            return
        }
        if (matchingTestState != MatchingArgState.None) {
            toast("正在拟合质控，请勿操作！")
            return
        }
        initState()
        getState()
    }

    private fun initState() {
        testMsg.value = ""
        testState = TestState.DripSample
        cuvetteShelfPos = -1
        shitTubeShelfPos = -1
        cuvetteStartPos = if (skipCuvetteNum.isNullOrEmpty()) 0 else skipCuvetteNum.toInt()
        Timber.d("跳过 $cuvetteStartPos 个比色皿")
        //跳过的比色皿结果为-1
        repeat(cuvetteStartPos) {
//            resultTest1.add(-1)
//            resultTest2.add(-1)0
//            resultTest3.add(-1)
//            resultTest4.add(-1)
//            resultModels.add(null)
        }
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
    private fun initCuvetteStates(): MutableList<CuvetteState> {
        val array = mutableListOf<CuvetteState>()
        for (i in 0 until 10) {
            array.add(CuvetteState.None)
        }
        resultModels?.clear()
        return array
    }

    /**
     * 重置采便管状态
     * @return MutableList<CuvetteState>
     */
    private fun initShitTubeStates(): MutableList<ShitTubeState> {
        val array = mutableListOf<ShitTubeState>()
        for (i in 0 until 10) {
            array.add(ShitTubeState.None)
        }
        scanResults?.clear()
        return array
    }


    /**
     * 接收到取试剂
     * @param reply ReplyModel<TakeReagentModel>
     */
    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
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
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取样针清洗 reply=$reply samplingProbeCleaningRecoverSampling=$samplingProbeCleaningRecoverSampling")
        samplingProbeCleaningFinish = true

        //恢复取样
        /**
         * @sample readDataMoveShitTubeModel
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
        Timber.d("接收到 刺破 reply=$reply")
        piercedFinish = true
        updateShitTubeState(shitTubePos, ShitTubeState.Pierced)
        //当不需要取样时，直接下一步
        if (!shitTubeNeedSampling(shitTubePos - 1)) {
            samplingFinish = true
            dripSampleFinish = true
        }
        nextStepDripReagent()
    }


    /**
     * 去加样
     */
    private fun goDripSample() {
        Timber.d("shitTubeMoveFinish=$shitTubeMoveFinish samplingFinish=$samplingFinish cuvetteMoveFinish=$cuvetteMoveFinish")
        if (shitTubePos > 0 && shitTubeMoveFinish && samplingFinish && cuvetteMoveFinish && shitTubeNeedDripSampling(
                shitTubePos - 1
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
     * 报错
     * @param cmd UByte
     * @param state UByte
     */
    override fun readDataStateFailed(cmd: UByte, state: UByte) {
        if (!runningTest()) return
        machineArgState = MachineState.RunningError
        Timber.d("报错了，cmd=$cmd state=$state")
        testMsg.postValue("报错了，cmd=$cmd state=$state")
    }

    /**
     * 接收到 样本门状态
     * @param reply ReplyModel<ShitTubeDoorModel>
     */
    override fun readDataShitTubeDoorModel(reply: ReplyModel<ShitTubeDoorModel>) {
        Timber.d("接收到 样本门状态 reply=$reply")
        shitTubeDoorIsOpen.postValue(reply.data.isOpen)

//        shitTubeDoorLocked = reply.data.isOpen
//        shitTubeDoorLockedLD.postValue(reply.data.isOpen)
//        if (testState == TestState.TestFinish && cuvetteDoorLocked && shitTubeDoorLocked) {
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
        cuvetteDoorIsOpen.postValue(reply.data.isOpen)
//        cuvetteDoorLocked = reply.data.isOpen
//        cuvetteDoorLockedLD.postValue(reply.data.isOpen)
//        if (testState == TestState.TestFinish && cuvetteDoorLocked && shitTubeDoorLocked) {
//            showFinishDialog()
//        }
        if (!runningTest()) return
        if (!machineStateNormal()) return
    }

    /**
     * 接收到移动采便管
     */
    override fun readDataMoveShitTubeModel(reply: ReplyModel<MoveShitTubeModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 移动采便管 reply=$reply cuvettePos=$cuvettePos lastCuvetteShelfPos=$lastCuvetteShelfPos cuvetteShelfPos=$cuvetteShelfPos shitTubePos=$shitTubePos samplingProbeCleaningFinish=$samplingProbeCleaningFinish")
        shitTubeMoveFinish = true
        samplingFinish = false
        dripSampleFinish = false
        scanFinish = false


        //最后一个位置不需要扫码，直接取样
        if (shitTubePos < shitTubeMax) {
            if ((SystemGlobal.isCodeDebug && !shitTubeExists[shitTubePos]) || (!SystemGlobal.isCodeDebug && !reply.data.exist)) {
                //没有采便管，移动到下一个位置
                Timber.d("没有采便管,移动到下一个位置")
                scanFinish = true
                scanResults.add(null)
                Timber.d("scanResults=$scanResults")
                nextStepDripReagent()
            } else {
                //有采便管
                updateShitTubeState(shitTubePos, ShitTubeState.Exist)
                startScan(shitTubePos)
            }
        }
        //判断是否需要取样。取样位和扫码位差一个位置
        if (shitTubeNeedSampling(shitTubePos - 1)) {
            //注：如果上次取样针清洗未结束，就等待清洗结束后再加样,否则直接加样
            if (samplingProbeCleaningFinish) {
                sampling()
            } else {
                samplingProbeCleaningRecoverSampling = true
            }
        } else if (shitTubePos == shitTubeMax) {
            //最后一个采便管位置，并且不需要取样时，下一步
            nextStepDripReagent()
        }
    }

    /**
     * 判断当前位置是否需要取样，只有扫码成功了的采便管才需要取样
     * @param shitTubePos Int
     * @return Boolean
     */
    private fun shitTubeNeedSampling(shitTubePos: Int): Boolean {
        if (shitTubePos < 0) return false
        return shitTubesStates[shitTubePos] == ShitTubeState.Pierced
    }

    /**
     * 判断当前位置是否需要加样，只有取样完成并且比色皿已经移动到位了才需要加样
     * @param shitTubePos Int
     * @return Boolean
     */
    private fun shitTubeNeedDripSampling(shitTubePos: Int): Boolean {
        if (shitTubePos < 0) return false
        return shitTubesStates[shitTubePos] == ShitTubeState.Sampling
    }

    /**
     * 更新采便管状态
     * @param shitTubePos Int
     * @param state ShitTubeState
     */
    private fun updateShitTubeState(shitTubePos: Int, state: ShitTubeState) {
        if (shitTubePos < 0 || shitTubePos >= shitTubesStates.size) {
            return
        }
        shitTubesStates[shitTubePos] = state
        Timber.d("updateShitTubeState shitTubesStates=$shitTubesStates")

    }

    /**
     * 更新比色皿状态
     * @param cuvettePos Int
     * @param state ShitTubeState
     */
    private fun updateCuvetteState(cuvettePos: Int, state: CuvetteState) {
        if (cuvettePos < 0 || cuvettePos >= cuvetteStates.size) {
            return
        }
        cuvetteStates[cuvettePos] = state
        Timber.d("updateCuvetteState cuvetteStates=$cuvetteStates")
    }

    /**
     * 开始扫码
     */
    private fun startScan(shitTubePos: Int) {
        scanFinish = false
        piercedFinish = false

//        if (SystemGlobal.isCodeDebug) {
        if (tempShitTubeState[shitTubePos] == 1) {
            scanSuccess("ffaa$shitTubePos")
        } else {
            scanFailed()
        }
//        } else {
//            viewModelScope.launch {
//                ScanCodeUtil.Instance.startScan()
//            }
//        }
    }

    /**
     * 扫码成功
     */
    override fun scanSuccess(str: String) {
        Timber.d("扫码成功 str=$str")
        updateShitTubeState(shitTubePos, ShitTubeState.ScanSuccess)
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
        updateShitTubeState(shitTubePos, ShitTubeState.ScanFailed)
        scanFinish = true
        piercedFinish = true
        //当不需要取样时，直接下一步
        if (!shitTubeNeedSampling(shitTubePos - 1)) {
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
    private fun createResultModel(str: String?) {
        val resultModel = TestResultModel(
            sampleQRCode = str ?: "",
            createTime = Date().toLongString(),
            detectionNum = LocalData.getDetectionNumInc()
        )

        resultModels.add(resultModel)
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
                nextDripReagent()
                updateTestResultModel(value, cuvettePos - 3, CuvetteState.Test1)
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
                resultOriginalTest1.add(value)
                resultTest1.add(calcAbsorbance(value.toBigDecimal()))
                resultModels[pos]?.testValue1 = resultTest1[pos]
                resultModels[pos]?.testOriginalValue1 = resultOriginalTest1[pos]
            }
            CuvetteState.Test2 -> {
                resultOriginalTest2.add(value)
                resultTest2.add(calcAbsorbance(value.toBigDecimal()))
                resultModels[pos]?.testValue2 = resultTest2[pos]
                resultModels[pos]?.testOriginalValue2 = resultOriginalTest2[pos]
            }
            CuvetteState.Test3 -> {
                resultOriginalTest3.add(value)
                resultTest3.add(calcAbsorbance(value.toBigDecimal()))
                resultModels[pos]?.testValue3 = resultTest3[pos]
                resultModels[pos]?.testOriginalValue3 = resultOriginalTest3[pos]
            }
            CuvetteState.Test4 -> {
                resultOriginalTest4.add(value)
                resultTest4.add(calcAbsorbance(value.toBigDecimal()))
                resultModels[pos]?.testValue4 = resultTest4[pos]
                resultModels[pos]?.testOriginalValue4 = resultOriginalTest4[pos]
                resultModels[pos]?.testTime = Date().toLongString()
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
        if (SystemGlobal.isCodeDebug) {
            resultTest1.clear()
            resultTest2.clear()
            resultTest3.clear()
            resultTest4.clear()
            resultOriginalTest1.clear()
            resultOriginalTest2.clear()
            resultOriginalTest3.clear()
            resultOriginalTest4.clear()
            val size =
                if (cuvetteStartPos > 0) 10 - cuvetteStartPos else scanResults.filterNotNull().size
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
        //计算吸光度
        absorbances = calcAbsorbances(resultTest1, resultTest2, resultTest3, resultTest4)
        //计算浓度
        selectProject?.let { it ->
            for (i in absorbances.indices) {
                val con = calcCon(absorbances[i], it)
                cons.add(con)

                resultModels[i]?.absorbances = absorbances[i]
                if (SystemGlobal.isCodeDebug) {
                    resultModels[i]?.testValue1 = resultTest1[i]
                    resultModels[i]?.testValue2 = resultTest2[i]
                    resultModels[i]?.testValue3 = resultTest3[i]
                    resultModels[i]?.testValue4 = resultTest4[i]
                    resultModels[i]?.testOriginalValue1 = resultOriginalTest1[i]
                    resultModels[i]?.testOriginalValue2 = resultOriginalTest2[i]
                    resultModels[i]?.testOriginalValue3 = resultOriginalTest3[i]
                    resultModels[i]?.testOriginalValue4 = resultOriginalTest4[i]
                }

                resultModels[i]?.concentration = con
                resultModels[i]?.let {
                    testResultRepository.updateTestResult(it)
                }
            }
        }

        testMsg.postValue(
            testMsg.value?.plus("这排比色皿检测结束 比色皿位置=$cuvetteShelfPos 采便管位置=$shitTubeShelfPos \n 第一次:$resultTest1 \n 第二次:$resultTest2 \n 第三次:$resultTest3 \n 第四次:$resultTest4 \n  吸光度:$absorbances \n 浓度=$cons \n选择的四参数为${selectProject ?: "未选择"}\n\n")
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

        continueTestNextCuvette()

    }

    /**
     * 检测完这排比色皿，继续检测下一排
     */
    private fun continueTestNextCuvette() {
        //重置这排比色皿架的位置和检测状态
        testState = TestState.DripSample
        cuvettePos = -1

        if (lastShitTubePos(shitTubePos)) {//最后一个采便管
            if (lastShitTubeShelf(shitTubeShelfPos)) {//最后一排
                //结束检测，采便管已经取完样了
                testFinishAction();
            } else {
                //移动采便管架
                moveShitTubeShelfNext()

                if (lastCuvetteShelf(cuvetteShelfPos)) {//最后一排比色皿
                    //提示，比色皿不足了
                    dialogTestFinishCuvetteDeficiency.postValue(true)
                } else {
                    //还有比色皿。继续移动比色皿，检测
                    moveCuvetteShelfNext()
                }
            }
        } else {
            if (lastCuvetteShelf(cuvetteShelfPos)) {//最后一排比色皿
                //提示，比色皿不足了
                dialogTestFinishCuvetteDeficiency.postValue(true)
            } else {
                //还有比色皿。继续移动比色皿，检测
                moveShitTube()
                moveCuvetteShelfNext()
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
                    Timber.d("已经检测最后一个了,进行下一个步骤，检测第二次")
                    viewModelScope.launch {
                        delay(testShelfInterval)
                        stepTest(TestState.Test2)
                    }
                    return
                } else {
                    moveCuvetteDripReagent()
                }
            }
//            else if (!stirProbeCleaningFinish && dripReagentFinish && testFinish && stirFinish && takeReagentFinish && cuvetteMoveFinish && lastNeedTest1(
//                    cuvettePos - 3
//                )
//            ) {
//                //如果是最后一次比色皿检测完成，但是未清洗完搅拌针时，直接进入下一次检测步骤，不等搅拌针清洗完成了
//                Timber.d("已经检测最后一个了,进行下一个步骤，检测第二次2")
//                stepTest(TestState.Test2)
//            }
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
        //切换到TestState.Test2时，当前下位机的比色皿位置cuvettePos == 0，则不需要计算需要回退多少格，直接往前移动
        if (state == TestState.Test2) {
            cuvettePos = getNextStepCuvetteStartPos()
            moveCuvetteTest()
        } else {
            //当切换状态时，当前下位机的比色皿位置 cuvettePos != 0，则需要计算需要回退多少格才回到可使用的第一个比色皿的位置
            //直接移动到可使用的第一个比色皿的位置
            //如果 nextStartPos > -1 说明这一排比色皿有跳过的 ，那么就直接移动到可使用的第一个比色皿的位置
            val nextStartPos = getNextStepCuvetteStartPos()
            val moveStep =
                if (nextStartPos > -1) -(cuvetteStates.size - (nextStartPos + 2)) else -cuvettePos
            Timber.d("cuvettePos= $cuvettePos nextStartPos=$nextStartPos moveStep=$moveStep")
            moveCuvetteTest(moveStep)
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
        if (pos > cuvetteStates.size) {
            return true;
        }
        for (i in pos + 1 until cuvetteStates.size) {
            if (cuvetteStates[i] == state) {
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
//        nextDripReagent()
    }


    /**
     * 判断这排是否已经检测完全完成
     */
    private fun cuvetteTestFinish(): Boolean {
        return cuvettePos == 9 || cuvetteStates.lastIndexOf(CuvetteState.None) > cuvettePos
    }

    /**
     * 判断是否已经搅拌完
     */
    private fun cuvetteStirFinish(): Boolean {
        return cuvetteStates.last() == CuvetteState.Stir
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
        if (cuvettePos >= cuvetteStates.size) return false
        return cuvetteStates[cuvettePos] == CuvetteState.DripReagent
    }

    /**
     * 判断当前位置的比色皿是否需要检测第一次
     */
    private fun cuvetteNeedTest1(cuvettePos: Int): Boolean {
        if (cuvettePos >= cuvetteStates.size) return false
        return cuvetteStates[cuvettePos] == CuvetteState.Stir
    }

    /**
     * 判断当前位置的比色皿是否需要加试剂
     */
    private fun cuvetteNeedDripReagent(cuvettePos: Int): Boolean {
        if (cuvettePos >= cuvetteStates.size) return false
        return cuvetteStates[cuvettePos] == CuvetteState.DripSample
    }


    /**
     * 接收到加样
     */
    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 加样 reply=$reply cuvettePos=$cuvettePos shitTubePos=$shitTubePos")

        updateCuvetteState(cuvettePos, CuvetteState.DripSample)
        dripSampleFinish = true
        createResultModel(scanResults[shitTubePos - 1])
        samplingProbeCleaning()

        nextStepDripReagent()
    }

    /**
     * 判断是否是这排最后一个比色皿了
     * @param pos Int
     * @return Boolean
     */
    private fun lastCuvettePos(pos: Int): Boolean {
        return cuvetteStates.lastIndex == pos
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
     * 判断是否是这排最后一个采便管了
     * @param pos Int
     * @return Boolean
     */
    private fun lastShitTubePos(pos: Int): Boolean {
        return shitTubeMax == pos
    }

    /**
     * 判断是否是最后一排采便管了
     * @param pos Int
     * @return Boolean
     */
    private fun lastShitTubeShelf(pos: Int): Boolean {
        return lastShitTubeShelfPos == pos
    }

    /**
     * 判断是否需要进行加试剂步骤了。
     * 如果已经加完一排比色皿或者采便管已经完全加完样了，就去进行加试剂步骤，
     * 否则则去下一个采便管和比色皿处取样加样
     */

    private fun nextStepDripReagent() {
        Timber.d("piercedFinish=$piercedFinish scanFinish=$scanFinish samplingFinish=$samplingFinish dripSampleFinish=$dripSampleFinish cuvettePos=$cuvettePos shitTubePos=$shitTubePos")
        //|| shitTubePos == 0
        if ((piercedFinish && (scanFinish || lastShitTubePos(shitTubePos)) && (shitTubeNeedSampling(
                shitTubePos - 1
            ) && samplingFinish && dripSampleFinish) || (!shitTubeNeedSampling(
                shitTubePos - 1
            ))) || shitTubePos == 0
        ) {
            if (lastCuvettePos(cuvettePos)) {//这排最后一个比色皿，需要去下一个步骤，加试剂
                stepDripReagent()
            } else if (lastShitTubePos(shitTubePos)) {//这排最后一个采便管
                if (lastShitTubeShelf(shitTubeShelfPos)) {//最后一排
                    //已经加完了最后一个采便管了，加样结束，去下一个步骤，加试剂
                    Timber.d("采便管加样完成！")
                    if (shelfNeedDripReagent()) {
                        stepDripReagent()
                    } else {
                        Timber.d("不需要加试剂,检测结束！")
                        testFinishAction()
                    }
                } else {
                    //这排采便管已经取完样了，移动到下一排接着取样
                    moveNextShitTubeAndCuvette()
                }
            } else {
                //比色皿和采便管都还有，继续
                moveNextShitTubeAndCuvette()
            }
        }
    }

    /**
     * 判断这排比色皿是否需要加样
     * @return Boolean
     */
    private fun shelfNeedDripReagent(): Boolean {
        return cuvetteStates.any { it == CuvetteState.DripSample }
    }

    /**
     * 去下一个步骤 加试剂
     */
    private fun stepDripReagent() {
        Timber.d(" ———————— stepDripReagent testState=$testState ————————————————————————————————————————————————————————————————————————————————————————————————=")
        testState = TestState.DripReagent
        cuvettePos = getNextStepCuvetteStartPos()
        moveCuvetteDripReagent()
        takeReagent()
    }

    /**
     * 判断比色皿在取样时是否需要移动。
     * 因为比色皿和采便管不一定是同步移动的，可能上一次移动采便管时已经移动了比色皿，但这个采便管不存在或扫码失败，下一次移动采便管时就不需要再次移动比色皿了
     * @param cuvettePos Int
     * @return Boolean
     */
    private fun cuvetteNeedMove(cuvettePos: Int): Boolean {
        return cuvettePos >= 0 && (cuvetteStates[cuvettePos] == CuvetteState.DripSample || cuvetteStates[cuvettePos] == CuvetteState.Skip)
    }


    /**
     * 接收到取样
     */
    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 取样 reply=$reply cuvettePos=$cuvettePos shitTubePos=$shitTubePos cuvetteShelfPos=$cuvetteShelfPos shitTubeShelfPos=$shitTubeShelfPos")
        samplingFinish = true
        updateShitTubeState(shitTubePos - 1, ShitTubeState.Sampling)

        goDripSample()
    }

    /**
     * 判断是否采便管不足，是否需要提示
     */
    private fun shitTubeDeficiencyShowHint() {
        if (shitTubePos == shitTubeMax && shitTubeShelfPos == lastShitTubeShelfPos) {
            Timber.d("shitTubeDeficiencyShowHint 采便管不足")
            shitTubeShelfPos = -1
            showTestShitTubeDeficiencyDialog()
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
            cuvettePos = getFirstCuvetteStartPos();
            cuvetteStates = initCuvetteStates()

            moveCuvetteDripSample()
        }
    }

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
        return testState == TestState.TestFinish && shitTubeShelfMoveFinish && cuvetteShelfMoveFinish
    }


    /**
     * 接收到移动采便管架
     */
    override fun readDataMoveShitTubeShelfModel(reply: ReplyModel<MoveShitTubeShelfModel>) {
        Timber.d("接收到移动采便管架 reply=$reply shitTubeShelfPos=$shitTubeShelfPos $testState")
        if (testState == TestState.TestFinish) {
            shitTubeShelfMoveFinish = true
            if (isTestFinish()) {
                showFinishDialog()
                openAllDoor()
            }
        }
        if (!runningTest()) return
        if (!machineStateNormal()) return
        shitTubeShelfMoveFinish = true

        if (testState != TestState.TestFinish) {
            shitTubePos = -1;
            shitTubesStates = initShitTubeStates()
            moveShitTube()
        }
    }

    private fun openAllDoor() {
        openShitTubeDoor()
        openCuvetteDoor()
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
     * 移动到下一个位置 采便管和比色皿
     */
    private fun moveNextShitTubeAndCuvette() {
        //如果一排比色皿已经加样结束了
        if (shitTubePos < shitTubeMax) {
            //如果不是最后一个
            moveShitTube()
        } else {
            //如果比色皿还没全部取完样就该换下一排采便管去取样了
            moveShitTubeShelfNext()
        }
        //如果需要移动
        if (cuvetteNeedMove(cuvettePos)) {
            moveCuvetteDripSample()
        }
    }


    /**
     * 移动到下一排,第一次的时候不能调用，
     * 因为在只有一排时调用，会直接显示采便管不足
     */
    private fun moveShitTubeShelfNext() {
        if (lastShitTubeShelfPos == shitTubeShelfPos) {
            //已经是最后一排了，结束检测
            Timber.d("采便管取样结束")
            showTestShitTubeDeficiencyDialog()
            return
        }

        val oldPos = shitTubeShelfPos
        for (i in shitTubeShelfPos + 1 until shitTubeShelfStates.size) {
            if (shitTubeShelfStates[i] == 1) {
                if (shitTubeShelfPos == oldPos) {
                    shitTubeShelfPos = i
                }
            }
        }

        moveShitTubeShelf(shitTubeShelfPos)
        Timber.d("moveShitTubeShelfNext shitTubeShelfPos=$shitTubeShelfPos oldPos=$oldPos")
    }

    /**
     * 显示采便管不足的对话框
     */
    private fun showTestShitTubeDeficiencyDialog() {
        shitTubeDeficiency = true
        moveShitTubeShelf(shitTubeShelfPos)
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
     * 接收到获取状态
     */
    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        if (!runningTest()) return
        if (!machineStateNormal()) return
        Timber.d("接收到 获取状态 reply=$reply")
        cuvetteShelfStates = reply.data.cuvetteShelfs
        shitTubeShelfStates = reply.data.shitTubeShelfs
        val r1Reagent = reply.data.r1Reagent
        val r2Reagent = reply.data.r2Reagent
        val cleanoutFluid = reply.data.cleanoutFluid

        //比色皿不足才获取的状态
        //如果还是没有比色皿，继续弹框，如果有就移动比色皿架，继续检测
        if (continueTestCuvetteState) {
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
            return
        }
        //采便管不足才获取的状态
        //如果还是没有采便管，继续弹框，如果有就移动比色皿架，继续检测
        if (continueTestShitTubeState) {
            shitTubeShelfPos = -1
            getInitShitTubeShelfPos()
            if (shitTubeShelfPos == -1) {
                Timber.d("没有采便管架")
                shitTubeDeficiency = true
                showTestShitTubeDeficiencyDialog()
                return
            }
            continueTestShitTubeState = false
            moveShitTubeShelf(shitTubeShelfPos)
            return
        }

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

        //开始检测
        moveShitTubeShelf(shitTubeShelfPos)
        moveCuvetteShelf(cuvetteShelfPos)
    }

    /**
     * 仪器是否正常
     * @return Boolean
     */
    private fun machineStateNormal(): Boolean {
        return machineArgState == MachineState.Normal
    }

    /**
     * 移动到下一排 第一次的时候不能调用，因为在只有一排时，会直接显示采便管不足
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
     * 是否是第一排比色皿
     */
    private fun isFirstCuvetteShelf(): Boolean {
        return cuvetteShelfPos == getFirstCuvetteShelf()
    }

    /**
     * 获取第一排比色皿架的下标
     * @return Int
     */
    private fun getFirstCuvetteShelf(): Int {
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
        shitTubeShelfPos = -1
        cuvetteShelfPos = -1
        moveCuvetteShelf(cuvetteShelfPos)
        moveShitTubeShelf(shitTubeShelfPos)
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
     * 采便管不足
     * 点击我已添加
     */
    fun dialogTestShitTubeDeficiencyConfirm() {
        Timber.d("dialogDripSampleShitTubeDeficiencyConfirm 点击我已添加")
        continueTestShitTubeState = true
        getState()
    }

    /**
     * 正常检测中
     * 采便管不足
     * 点击结束检测
     */
    fun dialogTestShitTubeDeficiencyCancel() {
        Timber.d("dialogDripSampleShitTubeDeficiencyCancel 点击结束检测")
        testFinishAction();
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
        testFinishAction();
    }

    /**
     * 获取采便管架，比色皿架状态，试剂，清洗液状态
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
     * 移动采便管架
     * @param pos Int
     */
    private fun moveShitTubeShelf(pos: Int) {
        Timber.d("发送 移动采便管架 pos=$pos ")
        shitTubeShelfMoveFinish = false
        SerialPortUtil.Instance.moveShitTubeShelf(pos + 1)
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
     * 移动采便管
     * @param step Int
     */
    private fun moveShitTube(step: Int = 1) {
        Timber.d("发送 移动采便管 step=$step shitTubePos=$shitTubePos")
        shitTubeMoveFinish = false
        shitTubePos += step;
        SerialPortUtil.Instance.moveShitTube(step > 0, step.absoluteValue)
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
