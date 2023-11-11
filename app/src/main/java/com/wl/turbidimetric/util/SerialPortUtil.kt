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
import java.util.concurrent.LinkedBlockingQueue

/**
 * 串口操作类
 */
object SerialPortUtil {
    val serialPort: BaseSerialPort = BaseSerialPort()

    //  lateinit var callback: Callback<ReplyModel<Any>>
    val callback: MutableList<Callback2> = mutableListOf()
    var data: MutableList<UByte> = mutableListOf<UByte>()
    val header = arrayOf<UByte>(0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u)
    val hCount = header.size
    val allCount = 8;
    var byteArray = ByteArray(100)
    val responseCommand1: UByte = SerialGlobal.CMD_Response;
    val sendQueue: BlockingQueue<UByteArray> = LinkedBlockingQueue()

    private val sendMap = mutableMapOf<UByte, Int>()

    init {
        open()
    }

    fun open() {
        if (SystemGlobal.isCodeDebug) {
            TestSerialPort.callback = this::dispatchData
        } else {
            serialPort.openSerial(WQSerialGlobal.COM1, 9600, 8)
            openRead()
            openWrite()
        }
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
        val cmd = ready[0]
        val state = ready[1]
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
                it.readDataStateFailed(cmd, state)
            }
            return@runBlocking
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
    private fun openWrite() {
        GlobalScope.launch {
            launch {
                while (true) {
                    Thread.sleep(100)
                    val take = sendQueue.take()
//                    println("take=$take")
                    if (take != null) {
                        write(take)
                    }
                }
            }
        }
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
                            var k = i;
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
        c("writeAsync ${data.toHex()}")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch(Dispatchers.IO) {
                TestSerialPort.testReply(data)
            }
        } else {
            sendQueue.add(data)
        }
    }


    private fun write(data: UByteArray) {
        c("write ${data.toHex()}")
        val d = data.toByteArray()
        serialPort.write(d)
        val a1 = data[0].toInt()
        val a2 = 0xffu.toInt()
        if (a1 != a2) {
            sendMap[data[0]] = 1
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
    fun samplingProbeCleaning() {
//        c("发送 取样针清洗")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
                delay(300)
                writeAsync(
                    createCmd(
                        SerialGlobal.CMD_SamplingProbeCleaning,
                        data3 = (LocalData.SamplingProbeCleaningDuration shr 8).toUByte(),
                        data4 = LocalData.SamplingProbeCleaningDuration.toUByte()
                    )
                )
            }
        } else {
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_SamplingProbeCleaning,
                    data3 = (LocalData.SamplingProbeCleaningDuration shr 8).toUByte(),
                    data4 = LocalData.SamplingProbeCleaningDuration.toUByte()
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
    fun dripReagent() {
//        c("发送 加试剂 ${LocalData.getTakeReagentR2()} ${LocalData.getTakeReagentR1()}")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_DripReagent,
                data1 = (LocalData.TakeReagentR2 shr 8).toUByte(),
                data2 = LocalData.TakeReagentR2.toUByte(),
                data3 = (LocalData.TakeReagentR1 shr 8).toUByte(),
                data4 = LocalData.TakeReagentR1.toUByte()
            )
        )
    }

    /**
     * 取试剂
     */
    fun takeReagent() {
//        c("发送 取试剂")
        GlobalScope.launch {
            if (SystemGlobal.isCodeDebug) {
                delay(1000)
            }
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_TakeReagent,
                    data1 = (LocalData.TakeReagentR2 shr 8).toUByte(),
                    data2 = LocalData.TakeReagentR2.toUByte(),
                    data3 = (LocalData.TakeReagentR1 shr 8).toUByte(),
                    data4 = LocalData.TakeReagentR1.toUByte()
                )
            )
        }
    }

    /**
     * 搅拌
     */
    fun stir() {
//        c("发送 搅拌")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_Stir,
                data3 = (LocalData.StirDuration shr 8).toUByte(),
                data4 = LocalData.StirDuration.toUByte()
            )
        )
    }

    /**
     * 搅拌针清洗
     */
    fun stirProbeCleaning() {
//        c("发送 搅拌针清洗")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
                delay(300)
                writeAsync(
                    createCmd(
                        SerialGlobal.CMD_StirProbeCleaning,
                        data3 = (LocalData.StirProbeCleaningDuration shr 8).toUByte(),
                        data4 = LocalData.StirProbeCleaningDuration.toUByte()
                    )
                )
            }
        } else {
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_StirProbeCleaning,
                    data3 = (LocalData.StirProbeCleaningDuration shr 8).toUByte(),
                    data4 = LocalData.StirProbeCleaningDuration.toUByte()
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
    public fun setTemp(reactionTemp: Int = 0, r1Temp: Int = 0) {
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
    public fun shutdown() {
        writeAsync(
            createCmd(SerialGlobal.CMD_Shutdown)
        )
    }

    /**
     * 挤压
     * @param enable 是否挤压 比色杯不挤压
     */
    public fun squeezing(enable: Boolean = true) {
        writeAsync(
            createCmd(SerialGlobal.CMD_Squeezing, data4 = if (enable) 0x1u else 0x0u)
        )
    }

    /**
     * 获取当前温度
     */
    public fun getTemp() {
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
    fun readDataStateFailed(cmd: UByte, state: UByte)
    fun readDataSqueezing(reply: ReplyModel<SqueezingModel>)
}
