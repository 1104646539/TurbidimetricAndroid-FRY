package com.wl.turbidimetric.util

import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.test.TestSerialPort
import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import com.wl.wllib.CRC.CRC16
import com.wl.wllib.CRC.VerifyCrc16
import com.wl.wllib.LogToFile.c
import com.wl.wllib.LogToFile.e
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * 串口操作类
 */
object SerialPortUtil {
    private val serialPort: BaseSerialPort = BaseSerialPort()

    val callback: MutableList<Callback2> = mutableListOf()
    val originalCallback: MutableList<OriginalDataCall> = mutableListOf()
    private var data: MutableList<UByte> = mutableListOf<UByte>()
    private val header = arrayOf<UByte>(0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u)
    private val hCount = header.size
    private const val allCount = 8
    var byteArray = ByteArray(100)
    private val responseCommand1: UByte = SerialGlobal.CMD_Response

    /**
     * 等待发送的命令队列
     */
    private val sendQueue: BlockingQueue<UByteArray> = LinkedBlockingQueue()

    /**
     * 已经超时 等待重发的命令
     */
    private val retryQueue: BlockingQueue<UByteArray> = LinkedBlockingQueue()

    /**
     * 超时重发的job
     */
    private val retryJobQueue: ConcurrentHashMap<UByte, Job> = ConcurrentHashMap()

    /**
     * 防止同样的命令发一次但收到多次结果（下位机没收到响应会重发），多收的只发响应码不处理
     */
    private val sendMap = mutableMapOf<UByte, Int>()

    /**
     * 超时重发时间
     */
    private val timeout = 30000L

    init {
        open()
    }

    fun open() {
        if (SystemGlobal.isCodeDebug) {
            TestSerialPort.callback = this::dispatchData
        } else {
            serialPort.openSerial(WQSerialGlobal.COM1, 9600, 8)
            openRead()
        }
        openWrite()
        openRetry()
    }


    private fun parse(ready: UByteArray) {
        c("parse ready=${ready.toHex()}")
        val ver = VerifyCrc16(ready)
        if (!ver) {
            c("验证不过 parse ready=${ready.toHex()}")
            println("验证不过")
            return
        }
        //验证通过，返回响应码，分发消息
//        println("验证过了")

        response(ready)
        dispatchData(ready)
    }

    /**
     * 取消这条命令的超时重发
     */
    private fun removeRetry(cmd: UByte) {
        val ret = retryJobQueue.remove(cmd)
        runBlocking {
            launch {
                ret?.cancelAndJoin()
            }
        }
    }

    private fun dispatchOriginalData(ready: UByteArray) {
        originalCallback.forEach {
            it.readDataOriginalData(ready)
        }
    }

    fun callback(call: (Callback2) -> Unit) {
        callback.forEach {
            call.invoke(it)
        }
    }

    /**
     * 收到的消息分发
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun dispatchData(ready: UByteArray) = runBlocking {
        dispatchOriginalData(ready)
        val cmd = ready[0]
        val state = ready[1]
        removeRetry(cmd)
        //重发的信息就不处理了
        if (!SystemGlobal.isCodeDebug) {
            if (cmd.toInt() != 0xffu.toInt()) {
                if (sendMap[cmd] != null && sendMap[cmd]!! > 0) {
//                    c("dispatchData 正常的 $ready")
                    sendMap[cmd] = 0
                } else {
                    c("dispatchData 重发的 $ready")
                    return@runBlocking
                }
            }
        }
        //状态错误的(不为0)
        if (state.toInt() != 0) {
            callback {
                if (!it.stateSuccess(cmd.toInt(), state.toInt())) {
                    return@callback
                }
            }
        }

        when (cmd) {
            SerialGlobal.CMD_GetMachineState -> {
                callback {
                    it.readDataGetMachineStateModel(transitionGetMachineStateModel(ready))
                }
            }

            SerialGlobal.CMD_GetState -> {
                callback {
                    it.readDataGetStateModel(transitionGetStateModel(ready))
                }
            }

            SerialGlobal.CMD_MoveSampleShelf -> {
                callback {
                    it.readDataMoveSampleShelfModel(transitionMoveSampleShelfModel(ready))
                }
            }

            SerialGlobal.CMD_MoveCuvetteShelf -> {
                callback {
                    it.readDataMoveCuvetteShelfModel(transitionMoveCuvetteShelfModel(ready))
                }
            }

            SerialGlobal.CMD_MoveSample -> {
                callback {
                    it.readDataMoveSampleModel(transitionMoveSampleModel(ready))
                }
            }

            SerialGlobal.CMD_MoveCuvetteDripSample -> {
                callback {
                    it.readDataMoveCuvetteDripSampleModel(
                        transitionMoveCuvetteDripSampleModel(
                            ready
                        )
                    )
                }
            }

            SerialGlobal.CMD_MoveCuvetteDripReagent -> {
                callback {
                    it.readDataMoveCuvetteDripReagentModel(
                        transitionMoveCuvetteDripReagentModel(
                            ready
                        )
                    )
                }
            }

            SerialGlobal.CMD_MoveCuvetteTest -> {
                callback {
                    it.readDataMoveCuvetteTestModel(transitionMoveCuvetteTestModel(ready))
                }
            }

            SerialGlobal.CMD_Sampling -> {
                callback {
                    it.readDataSamplingModel(transitionSamplingModel(ready))
                }
            }

            SerialGlobal.CMD_SamplingProbeCleaning -> {
                callback {
                    it.readDataSamplingProbeCleaningModelModel(
                        transitionSamplingProbeCleaningModel(
                            ready
                        )
                    )
                }
            }

            SerialGlobal.CMD_DripSample -> {
                callback {
                    it.readDataDripSampleModel(transitionDripSampleModel(ready))
                }
            }

            SerialGlobal.CMD_DripReagent -> {
                callback {
                    it.readDataDripReagentModel(transitionDripReagentModel(ready))
                }
            }

            SerialGlobal.CMD_TakeReagent -> {
                callback {
                    it.readDataTakeReagentModel(transitionTakeReagentModel(ready))
                }
            }

            SerialGlobal.CMD_Stir -> {
                callback {
                    it.readDataStirModel(transitionStirModel(ready))
                }
            }

            SerialGlobal.CMD_StirProbeCleaning -> {
                callback {
                    it.readDataStirProbeCleaningModel(transitionStirProbeCleaningModel(ready))
                }
            }

            SerialGlobal.CMD_Test -> {
                callback {
                    it.readDataTestModel(transitionTestModel(ready))
                }
            }

            SerialGlobal.CMD_CuvetteDoor -> {
                callback {
                    it.readDataCuvetteDoorModel(transitionCuvetteDoorModel(ready))
                }
            }

            SerialGlobal.CMD_SampleDoor -> {
                callback {
                    it.readDataSampleDoorModel(transitionSampleDoorModel(ready))
                }
            }

            SerialGlobal.CMD_Pierced -> {
                callback {
                    it.readDataPiercedModel(transitionPiercedModel(ready))
                }
            }

            SerialGlobal.CMD_GetVersion -> {
                callback {
                    it.readDataGetVersionModel(transitionGetVersionModel(ready))
                }
            }

            SerialGlobal.CMD_GetSetTemp -> {
                callback {
                    it.readDataTempModel(transitionTempModel(ready))
                }
            }

            SerialGlobal.CMD_Squeezing -> {
                callback {
                    it.readDataSqueezing(transitionSqueezingModel(ready))
                }
            }

            else -> {}
        }
    }


    /**
     * 响应码
     */
    private fun response(ready: UByteArray) {
        val response = getResponse(ready)
        writeAsync(response)
    }

    /**
     * 获取响应码
     */
    private fun getResponse(ready: UByteArray): UByteArray {
        val response = ubyteArrayOf(
            responseCommand1,
            0x00u,
            0x00u,
            0x00u,
            ready[0],
        )
        val re = CRC16(response)
        val merge = response + re
        return merge
    }

    /**
     * 写
     *
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun openWrite() {
        GlobalScope.launch {
            launch {
                while (true) {
                    Thread.sleep(50)
                    val take = sendQueue.take()
//                    println("take=$take")
                    if (take != null && isNeedRetry(take)) {
                        val job = launch {
                            delay(timeout)
                            addRetry(take)
                        }
                        retryJobQueue[take[0]] = job
                    }
                    write(take)
                }
            }
        }
    }

    /**
     * 该是否需要重发
     * 自检、获取温度不需要重发
     */
    private fun isNeedRetry(take: UByteArray): Boolean {
        return !(take[0] == SerialGlobal.CMD_GetMachineState || take[0] == SerialGlobal.CMD_GetSetTemp)
    }

    /**
     * 重发
     *
     */
    private fun openRetry() {
        GlobalScope.launch {
            launch {
                while (true) {
                    Thread.sleep(50)
                    val take = retryQueue.take()

                    if (take != null) {
                        c("重发了 $take")
                        writeAsync(take)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun addRetry(cmd: UByteArray) {
        retryQueue.add(cmd)
    }

    /**
     * 读
     *
     */
    private fun openRead() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    delay(50)
                    val count = serialPort.read(byteArray, byteArray.size)
                    if (count > 0) {
                        val re = byteArray.copyOf(count).toUByteArray()
                        data.addAll(re)
//                        c("每次接收的re=${re.toHex()}")
                    }
                    if (data.size < hCount + allCount) {
                        continue
                    }
                    i@ for (i in data.indices) {
                        if (data.size >= hCount + allCount && data[i] == header[0]) {
                            var k = i
                            var count = 0
                            j@ for (element in header) {
                                if (data[k] == element) {
                                    count++
                                    if (hCount == count) {
                                        //找到了前缀
                                        val temp: UByteArray =
                                            data.toUByteArray().copyOfRange(0, k + allCount + 1)
                                        val ready =
                                            temp.copyOfRange(temp.size - allCount, temp.size)
                                        if (temp.size < data.size) {
                                            val remaining = data.toUByteArray()
                                                .copyOfRange(k + allCount + 1, data.size)
                                            data.clear()
                                            data.addAll(remaining)
                                        } else {
                                            data.clear()
                                        }

//                                        println(
//                                            "带前缀的 temp=${temp.toHex()} data=${
//                                                data.toUByteArray().toHex()
//                                            } ready=${ready.toHex()}"
//                                        )
                                        parse(ready)
                                        break@i
                                    }
                                } else {
                                    e("不对了")
                                    continue@i
                                }
                                k++
                            }
                        } else {
                            if (!data.isNullOrEmpty()) {
                                c(
                                    "data.size ${
                                        data.toUByteArray().toHex()
                                    }"
                                )
                            }

                            break@i
                        }
                    }
                }
            }
        }
    }

    private fun writeAsync(data: UByteArray) {
//        c("writeAsync ${data.toHex()}")
        originalCallback.forEach {
            it.sendOriginalData(data)
        }

        if (isAllowRunning(data)) {
            sendQueue.add(data)
        }
    }

    /**
     * 是否运行发送命令
     * 当仪器已经发生电机、传感器、非法参数等错误时，不允许再次发送命令，防止仪器损坏
     */
    private fun isAllowRunning(data: UByteArray): Boolean {
        if (TestState.RunningError == SystemGlobal.testState) {
            if (data[0] != SerialGlobal.CMD_GetSetTemp) {
                return false
            }
        }
        return true
    }


    private fun write(data: UByteArray) {
        c("write ${data.toHex()}")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch(Dispatchers.IO) {
                TestSerialPort.testReply(data)
            }
        } else {
            val d = data.toByteArray()
            serialPort.write(d)
            val a1 = data[0].toInt()
            val a2 = 0xffu.toInt()
            if (a1 != a2) {
                sendMap[data[0]] = 1
            }
        }
    }

    /**
     * 获取状态
     */
    fun getState() {
//        c("发送 获取状态")
        writeAsync(createCmd(SerialGlobal.CMD_GetState))
    }

    /**
     * 自检
     */
    fun getMachineState() {
//        c("发送 自检")
        writeAsync(createCmd(SerialGlobal.CMD_GetMachineState))
    }

    /**
     * 移动样本架
     * @param pos Int 移动位置，绝对的
     */
    fun moveSampleShelf(pos: Int) {
//        c("发送 移动样本架")
        writeAsync(createCmd(SerialGlobal.CMD_MoveSampleShelf, data4 = pos.toUByte()))
    }


    /**
     *
     * 移动比色皿架
     * @param pos Int 移动位置，绝对的
     */
    fun moveCuvetteShelf(pos: Int) {
//        c("发送 移动比色皿架")

        writeAsync(createCmd(SerialGlobal.CMD_MoveCuvetteShelf, data4 = pos.toUByte()))
    }

    /**
     *
     * 移动样本
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveSample(forward: Boolean = true, pos: Int) {
//        c("发送 移动样本")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_MoveSample,
                data3 = (if (forward) 0 else 1).toUByte(),
                data4 = pos.toUByte()
            )
        )
    }


    /**
     * 移动比色皿 加样位
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveCuvetteDripSample(forward: Boolean = true, pos: Int) {
//        c("发送 移动比色皿 加样位")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_MoveCuvetteDripSample,
                data3 = (if (forward) 0 else 1).toUByte(),
                data4 = pos.toUByte()
            )
        )
    }


    /**
     *
     * 移动比色皿 搅拌位，加试剂位
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveCuvetteDripReagent(forward: Boolean = true, pos: Int) {
//        c("发送 移动比色皿 搅拌位，加试剂位")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_MoveCuvetteDripReagent,
                data3 = (if (forward) 0 else 1).toUByte(),
                data4 = pos.toUByte()
            )
        )
    }


    /**
     *
     * 移动比色皿 检测位
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveCuvetteTest(forward: Boolean = true, pos: Int) {
//        c("发送 移动比色皿 检测位")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_MoveCuvetteTest,
                data3 = (if (forward) 0 else 1).toUByte(),
                data4 = pos.toUByte()
            )
        )
    }

    /**
     * 取样
     * @param volume 取样量
     * @param squeezing 是否挤压
     */
    fun sampling(volume: Int = LocalData.SamplingVolume, sampleType: SampleType) {
//        c("发送 取样")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_Sampling,
                data2 = (if (sampleType.isSample()) 0 else 1).toUByte(),
                data3 = (volume shr 8).toUByte(),
                data4 = volume.toUByte()
            )
        )
    }

    /**
     * 取样针清洗
     */
    fun samplingProbeCleaning(samplingProbeCleaningDuration: Int = LocalData.SamplingProbeCleaningDuration) {
//        c("发送 取样针清洗")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
                delay(300)
                writeAsync(
                    createCmd(
                        SerialGlobal.CMD_SamplingProbeCleaning,
                        data3 = (samplingProbeCleaningDuration shr 8).toUByte(),
                        data4 = samplingProbeCleaningDuration.toUByte()
                    )
                )
            }
        } else {
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_SamplingProbeCleaning,
                    data3 = (samplingProbeCleaningDuration shr 8).toUByte(),
                    data4 = samplingProbeCleaningDuration.toUByte()
                )
            )
        }
    }

    /**
     * 加样
     * @param autoBlending Boolean 是否自动混匀
     * @param inplace Boolean 是否原地加样
     * @param volume Int 加样量
     */
    fun dripSample(
        autoBlending: Boolean = false,
        inplace: Boolean = false,
        volume: Int = LocalData.SamplingVolume
    ) {
//        c("发送 加样")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_DripSample,
                data1 = (if (autoBlending) 1 else 0).toUByte(),
                data2 = (if (inplace) 1 else 0).toUByte(),
                data3 = (volume shr 8).toUByte(),
                data4 = volume.toUByte()
            )
        )
    }

    /**
     * 加试剂
     */
    fun dripReagent(
        r1Volume: Int = LocalData.TakeReagentR1,
        r2Volume: Int = LocalData.TakeReagentR2
    ) {
//        c("发送 加试剂 ${LocalData.getTakeReagentR2()} ${LocalData.getTakeReagentR1()}")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_DripReagent,
                data1 = (r2Volume shr 8).toUByte(),
                data2 = r2Volume.toUByte(),
                data3 = (r1Volume shr 8).toUByte(),
                data4 = r1Volume.toUByte()
            )
        )
    }

    /**
     * 取试剂
     */
    fun takeReagent(
        r1Volume: Int = LocalData.TakeReagentR1,
        r2Volume: Int = LocalData.TakeReagentR2
    ) {
//        c("发送 取试剂")
        GlobalScope.launch {
            if (SystemGlobal.isCodeDebug) {
                delay(1000)
            }
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_TakeReagent,
                    data1 = (r2Volume shr 8).toUByte(),
                    data2 = r2Volume.toUByte(),
                    data3 = (r1Volume shr 8).toUByte(),
                    data4 = r1Volume.toUByte()
                )
            )
        }
    }

    /**
     * 搅拌
     */
    fun stir(stirDuration: Int = LocalData.StirDuration) {
//        c("发送 搅拌")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_Stir,
                data3 = (stirDuration shr 8).toUByte(),
                data4 = stirDuration.toUByte()
            )
        )
    }

    /**
     * 搅拌针清洗
     */
    fun stirProbeCleaning(stirProbeCleaningDuration: Int = LocalData.StirProbeCleaningDuration) {
//        c("发送 搅拌针清洗")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
                delay(300)
                writeAsync(
                    createCmd(
                        SerialGlobal.CMD_StirProbeCleaning,
                        data3 = (stirProbeCleaningDuration shr 8).toUByte(),
                        data4 = stirProbeCleaningDuration.toUByte()
                    )
                )
            }
        } else {
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_StirProbeCleaning,
                    data3 = (stirProbeCleaningDuration shr 8).toUByte(),
                    data4 = stirProbeCleaningDuration.toUByte()
                )
            )
        }
    }

    /**
     * 检测
     */
    fun test() {
//        c("发送 检测")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
//                delay(10000)
                writeAsync(createCmd(SerialGlobal.CMD_Test))
            }
        } else {
            writeAsync(createCmd(SerialGlobal.CMD_Test))
        }
    }

    /**
     * 获取样本舱门状态
     */
    fun getSampleDoorState() {
//        c("发送 获取样本舱门状态")
        setGetSampleDoor(false)
    }

    /**
     * 开启样本舱门
     */
    fun openSampleDoor() {
//        c("发送 开启样本舱门")
        setGetSampleDoor(true)
    }

    /**
     * 获取样本舱门状态|开样本舱门
     * @param open Boolean
     */
    private fun setGetSampleDoor(open: Boolean = false) {
        writeAsync(
            createCmd(
                SerialGlobal.CMD_SampleDoor, data4 = if (open) 0x1u else 0x0u
            )
        )
    }

    /**
     * 获取比色皿舱门状态
     */
    fun getCuvetteDoorState() {
//        c("发送 获取比色皿舱门状态")
        setGetCuvetteDoor(false)
    }

    /**
     * 获取比色皿舱门状态
     */
    fun openCuvetteDoor() {
//        c("发送 获取比色皿舱门状态")
        setGetCuvetteDoor(true)
    }

    /**
     * 刺破
     */
    fun pierced(sampleType: SampleType) {
        writeAsync(
            createCmd(
                SerialGlobal.CMD_Pierced,
                data4 = if (sampleType.isSample()) 0x1u else 0x0u
            )
        )
    }

    /**
     * 获取版本号
     */
    fun getVersion() {
        writeAsync(createCmd(SerialGlobal.CMD_GetVersion))
    }

    /**
     * 获取比色皿舱门状态|开比色皿舱门
     * @param open Boolean
     */
    private fun setGetCuvetteDoor(open: Boolean = false) {
        writeAsync(
            createCmd(
                SerialGlobal.CMD_CuvetteDoor, data4 = if (open) 0x1u else 0x0u
            )
        )
    }

    /**
     * 设置温度
     */
    fun setTemp(reactionTemp: Int = 0, r1Temp: Int = 0) {
        writeAsync(
            createCmd(
                SerialGlobal.CMD_GetSetTemp,
                (reactionTemp shr 8).toUByte(),
                (reactionTemp).toUByte(),
                (r1Temp shr 8).toUByte(),
                (r1Temp).toUByte()
            )
        )
    }

    /**
     * 关机
     */
    fun shutdown() {
        writeAsync(
            createCmd(SerialGlobal.CMD_Shutdown)
        )
    }

    /**
     * 挤压
     * @param enable 是否挤压 比色杯不挤压
     */
    fun squeezing(enable: Boolean = true) {
        writeAsync(
            createCmd(SerialGlobal.CMD_Squeezing, data4 = if (enable) 0x1u else 0x0u)
        )
    }

    /**
     * 获取当前温度
     */
    fun getTemp() {
        setTemp(0, 0)
    }

    /**
     * 创建一个完整的命令
     */
    private fun createCmd(
        cmd: UByte,
        data1: UByte = 0x00u,
        data2: UByte = 0x00u,
        data3: UByte = 0x00u,
        data4: UByte = 0x00u
    ): UByteArray {
        var t = ubyteArrayOf(cmd, data1, data2, data3, data4)
        val crc = CRC16(t)
        t = t.plus(crc[0])
        t = t.plus(crc[1])
        return t
    }
}

interface Callback2 {
    fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>)
    fun readDataGetStateModel(reply: ReplyModel<GetStateModel>)
    fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>)
    fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>)
    fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>)
    fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>)
    fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>)
    fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>)
    fun readDataSamplingModel(reply: ReplyModel<SamplingModel>)
    fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>)
    fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>)
    fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>)
    fun readDataStirModel(reply: ReplyModel<StirModel>)
    fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>)
    fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>)
    fun readDataTestModel(reply: ReplyModel<TestModel>)
    fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>)
    fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>)
    fun readDataPiercedModel(reply: ReplyModel<PiercedModel>)
    fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>)
    fun readDataTempModel(reply: ReplyModel<TempModel>)
    fun stateSuccess(cmd: Int, state: Int): Boolean
    fun readDataSqueezing(reply: ReplyModel<SqueezingModel>)
}

interface OriginalDataCall {
    fun readDataOriginalData(ready: UByteArray)
    fun sendOriginalData(ready: UByteArray)
}
