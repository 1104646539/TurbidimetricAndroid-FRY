package com.wl.turbidimetric.util

import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.test.TestSerialPort
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * 串口操作类
 */
class SerialPortUtil(val serialPort: BaseSerialPortUtil = BaseSerialPortUtil("Com1", 9600)) {
    //class SerialPortUtil(val serialPort: BaseSerialPortUtil = BaseSerialPortUtil("/dev/ttyS1", 9600)) {
    companion object {
        val Instance: SerialPortUtil = SerialPortUtil()
    }

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

    fun open() {
        if (SystemGlobal.isCodeDebug) {
            TestSerialPort.callback = this::dispatchData
        } else {
            serialPort.open()
            openRead()
            openWrite()
        }
    }


    private fun parse(ready: UByteArray) {
        Timber.d("parse ready=${ready.toHex()}")
        val ver = VerifyCrc(ready)
        if (!ver) {
            Timber.d("验证不过 parse ready=${ready.toHex()}")
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
//                    Timber.d("dispatchData 正常的 $ready")
                    sendMap[cmd] = 0
                } else {
                    Timber.d("dispatchData 重发的 $ready")
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
            SerialGlobal.CMD_MoveShitTubeShelf -> {
                callback {
                    it.readDataMoveShitTubeShelfModel(transitionMoveShitTubeShelfModel(ready))
                }
            }
            SerialGlobal.CMD_MoveCuvetteShelf -> {
                callback {
                    it.readDataMoveCuvetteShelfModel(transitionMoveCuvetteShelfModel(ready))
                }
            }
            SerialGlobal.CMD_MoveShitTube -> {
                callback {
                    it.readDataMoveShitTubeModel(transitionMoveShitTubeModel(ready))
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
            SerialGlobal.CMD_ShitTubeDoor -> {
                callback {
                    it.readDataShitTubeDoorModel(transitionShitTubeDoorModel(ready))
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
                    val count = serialPort.read(byteArray)
                    val re = byteArray.copyOf(count).toUByteArray()
                    data.addAll(re)

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
//                                        println("date2 ${Date().time}")
//                                        println("匹配了")

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
                                            println(
                                                "remaining=${remaining} k=$k"
                                            )
                                        } else {
                                            data.clear()
                                        }

                                        println(
                                            "带前缀的 temp=${temp.toHex()} data=${
                                                data.toUByteArray().toHex()
                                            } ready=${ready.toHex()}"
                                        )
                                        parse(ready)
                                        break@i
                                    }
                                } else {
                                    println("不对了")
                                    continue@i
                                }
                                k++
                            }
                        } else {
                            if (!data.isNullOrEmpty()) {
                                println(
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
        Timber.d("writeAsync ${data.toHex()}")
        if (SystemGlobal.isCodeDebug) {
            TestSerialPort.testReply(data)
        } else {
            sendQueue.add(data)
        }
    }


    private fun write(data: UByteArray) {
        Timber.d("write ${data.toHex()}")
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
//        Timber.d("发送 获取状态")
        writeAsync(createCmd(SerialGlobal.CMD_GetState))
    }

    /**
     * 自检
     */
    fun getMachineState() {
//        Timber.d("发送 自检")
        writeAsync(createCmd(SerialGlobal.CMD_GetMachineState))
    }

    /**
     * 移动采便管架
     * @param pos Int 移动位置，绝对的
     */
    fun moveShitTubeShelf(pos: Int) {
//        Timber.d("发送 移动采便管架")
        writeAsync(createCmd(SerialGlobal.CMD_MoveShitTubeShelf, data4 = pos.toUByte()))
    }


    /**
     *
     * 移动比色皿架
     * @param pos Int 移动位置，绝对的
     */
    fun moveCuvetteShelf(pos: Int) {
//        Timber.d("发送 移动比色皿架")

        writeAsync(createCmd(SerialGlobal.CMD_MoveCuvetteShelf, data4 = pos.toUByte()))
    }

    /**
     *
     * 移动采便管
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveShitTube(forward: Boolean = true, pos: Int) {
//        Timber.d("发送 移动采便管")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_MoveShitTube,
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
//        Timber.d("发送 移动比色皿 加样位")
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
//        Timber.d("发送 移动比色皿 搅拌位，加试剂位")
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
//        Timber.d("发送 移动比色皿 检测位")
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
    fun sampling(volume: Int = LocalData.SamplingVolume, squeezing: Boolean = true) {
//        Timber.d("发送 取样")
        writeAsync(
            createCmd(
                SerialGlobal.CMD_Sampling,
                data2 = (if (squeezing) 1 else 0).toUByte(),
                data3 = (volume shr 8).toUByte(),
                data4 = volume.toUByte()
            )
        )
    }

    /**
     * 取样针清洗
     */
    fun samplingProbeCleaning() {
//        Timber.d("发送 取样针清洗")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
                delay(1000)
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
//        Timber.d("发送 加样")
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
//        Timber.d("发送 加试剂 ${LocalData.getTakeReagentR2()} ${LocalData.getTakeReagentR1()}")
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
//        Timber.d("发送 取试剂")
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

    /**
     * 搅拌
     */
    fun stir() {
//        Timber.d("发送 搅拌")
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
//        Timber.d("发送 搅拌针清洗")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
                delay(1000)
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
//        Timber.d("发送 检测")
        if (SystemGlobal.isCodeDebug) {
            GlobalScope.launch {
//                delay(5000)
                writeAsync(createCmd(SerialGlobal.CMD_Test))
            }
        } else {
            writeAsync(createCmd(SerialGlobal.CMD_Test))
        }
    }

    /**
     * 获取采便管舱门状态
     */
    fun getShitTubeDoorState() {
//        Timber.d("发送 获取采便管舱门状态")
        setGetShitTubeDoor(false)
    }

    /**
     * 开启采便管舱门
     */
    fun openShitTubeDoor() {
//        Timber.d("发送 开启采便管舱门")
        setGetShitTubeDoor(true)
    }

    /**
     * 获取采便管舱门状态|开采便管舱门
     * @param open Boolean
     */
    private fun setGetShitTubeDoor(open: Boolean = false) {
        writeAsync(
            createCmd(
                SerialGlobal.CMD_ShitTubeDoor, data4 = if (open) 0x1u else 0x0u
            )
        )
    }

    /**
     * 获取比色皿舱门状态
     */
    fun getCuvetteDoorState() {
//        Timber.d("发送 获取比色皿舱门状态")
        setGetCuvetteDoor(false)
    }

    /**
     * 获取比色皿舱门状态
     */
    fun openCuvetteDoor() {
//        Timber.d("发送 获取比色皿舱门状态")
        setGetCuvetteDoor(true)
    }

    /**
     * 刺破
     */
    fun pierced() {
        writeAsync(createCmd(SerialGlobal.CMD_Pierced))
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
    fun readDataMoveShitTubeShelfModel(reply: ReplyModel<MoveShitTubeShelfModel>)
    fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>)
    fun readDataMoveShitTubeModel(reply: ReplyModel<MoveShitTubeModel>)
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
    fun readDataShitTubeDoorModel(reply: ReplyModel<ShitTubeDoorModel>)
    fun readDataPiercedModel(reply: ReplyModel<PiercedModel>)
    fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>)
    fun readDataTempModel(reply: ReplyModel<TempModel>)
    fun readDataStateFailed(cmd: UByte, state: UByte)
}
