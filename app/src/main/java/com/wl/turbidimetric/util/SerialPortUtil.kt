package com.wl.turbidimetric.util

import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.test.TestSerialPort
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * 串口操作类
 */
class SerialPortUtil(val serialPort: BaseSerialPortUtil = BaseSerialPortUtil("Com1", 115200)) {
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

    fun callback(ready: UByteArray, call: (Callback2) -> Unit) {
        callback.forEach {
            call.invoke(it)
        }
    }

    /**
     * 收到的消息分发
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun dispatchData(ready: UByteArray) = runBlocking {
        if (ready[1].toInt() != 0) {
            callback(ready) {
                it.readDataStateFailed(ready[0],ready[1])
            }
            return@runBlocking
        }

        when (ready[0]) {
            SerialGlobal.CMD_GetMachineState -> {
                callback(ready) {
                    it.readDataGetMachineStateModel(transitionGetMachineStateModel(ready))
                }
                callback.forEach {
                    it.readDataGetMachineStateModel(transitionGetMachineStateModel(ready))
                }
            }
            SerialGlobal.CMD_GetState -> {
                callback.forEach {
                    it.readDataGetStateModel(transitionGetStateModel(ready))
                }
            }
            SerialGlobal.CMD_MoveShitTubeShelf -> {
                callback.forEach {
                    it.readDataMoveShitTubeShelfModel(transitionMoveShitTubeShelfModel(ready))
                }
            }
            SerialGlobal.CMD_MoveCuvetteShelf -> {
                callback.forEach {
                    it.readDataMoveCuvetteShelfModel(transitionMoveCuvetteShelfModel(ready))
                }
            }
            SerialGlobal.CMD_MoveShitTube -> {
                callback.forEach {
                    it.readDataMoveShitTubeModel(transitionMoveShitTubeModel(ready))
                }
            }
            SerialGlobal.CMD_MoveCuvetteDripSample -> {
                callback.forEach {
                    it.readDataMoveCuvetteDripSampleModel(
                        transitionMoveCuvetteDripSampleModel(
                            ready
                        )
                    )
                }
            }
            SerialGlobal.CMD_MoveCuvetteDripReagent -> {
                callback.forEach {
                    it.readDataMoveCuvetteDripReagentModel(
                        transitionMoveCuvetteDripReagentModel(
                            ready
                        )
                    )
                }
            }
            SerialGlobal.CMD_MoveCuvetteTest -> {
                callback.forEach {
                    it.readDataMoveCuvetteTestModel(transitionMoveCuvetteTestModel(ready))
                }
            }
            SerialGlobal.CMD_Sampling -> {
                callback.forEach {
                    it.readDataSamplingModel(transitionSamplingModel(ready))
                }
            }
            SerialGlobal.CMD_SamplingProbeCleaning -> {
                callback.forEach {
                    it.readDataSamplingProbeCleaningModelModel(
                        transitionSamplingProbeCleaningModel(
                            ready
                        )
                    )
                }
            }
            SerialGlobal.CMD_DripSample -> {
                callback.forEach {
                    it.readDataDripSampleModel(transitionDripSampleModel(ready))
                }
            }
            SerialGlobal.CMD_DripReagent -> {
                callback.forEach {
                    it.readDataDripReagentModel(transitionDripReagentModel(ready))
                }
            }
            SerialGlobal.CMD_TakeReagent -> {
                callback.forEach {
                    it.readDataTakeReagentModel(transitionTakeReagentModel(ready))
                }
            }
            SerialGlobal.CMD_Stir -> {
                callback.forEach {
                    it.readDataStirModel(transitionStirModel(ready))
                }
            }
            SerialGlobal.CMD_StirProbeCleaning -> {
                callback.forEach {
                    it.readDataStirProbeCleaningModel(transitionStirProbeCleaningModel(ready))
                }
            }
            SerialGlobal.CMD_Test -> {
                callback.forEach {
                    it.readDataTestModel(transitionTestModel(ready))
                }
            }
            SerialGlobal.CMD_CuvetteDoor -> {
                callback.forEach {
                    it.readDataCuvetteDoorModel(transitionCuvetteDoorModel(ready))
                }
            }
            SerialGlobal.CMD_ShitTubeDoor -> {
                callback.forEach {
                    it.readDataShitTubeDoorModel(transitionShitTubeDoorModel(ready))
                }
            }
            SerialGlobal.CMD_Pierced -> {
                callback.forEach {
                    it.readDataPiercedModel(transitionPiercedModel(ready))
                }
            }
            SerialGlobal.CMD_GetVersion -> {
                callback.forEach {
                    it.readDataGetVersionModel(transitionGetVersionModel(ready))
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
//        thread {
//            while (true) {
//                Thread.sleep(100)
//                val count = serialPort.read(byteArray)
//                val re = byteArray.copyOf(count).toUByteArray()
//                data.addAll(re)
//
//                i@ for (i in data.indices) {
//                    if (data.size >= hCount + allCount && data[i] == header[0]) {
//                        var k = i;
//                        var count = 0
//                        j@ for (element in header) {
//                            if (data[k] == element) {
//                                count++
//                                if (hCount == count) {
////                                        println("date2 ${Date().time}")
////                                        println("匹配了")
//
//                                    //找到了前缀
//                                    val temp: UByteArray =
//                                        data.toUByteArray().copyOfRange(0, k + allCount + 1)
//                                    val ready =
//                                        temp.copyOfRange(temp.size - allCount, temp.size)
//                                    if (temp.size < data.size) {
//                                        val remaining = data.toUByteArray()
//                                            .copyOfRange(k + allCount + 1, data.size)
//                                        data.clear()
//                                        data.addAll(remaining)
//                                    } else {
//                                        data.clear()
//                                    }
//
//                                    println(
//                                        "带前缀的 temp=${temp.toHex()} data=${
//                                            data.toUByteArray().toHex()
//                                        } ready=${ready.toHex()}"
//                                    )
//                                    parse(ready)
//                                    break@i
//                                }
//                            } else {
//                                println("不对了")
//                                continue@i
//                            }
//                            k++
//                        }
//                    } else {
//                        if (!data.isNullOrEmpty()) {
//                            println(
//                                "data.size ${
//                                    data.toUByteArray().toHex()
//                                }"
//                            )
//                        }
//
//                        break@i
//                    }
//                }
//            }
//        }
        GlobalScope.launch {
            launch {
                while (true) {
                    delay(100)
                    val count = serialPort.read(byteArray)
                    val re = byteArray.copyOf(count).toUByteArray()
                    data.addAll(re)

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
    fun sampling(volume: Int = LocalData.getSamplingVolume(), squeezing: Boolean = true) {
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
                        data3 = (2000 shr 8).toUByte(),
                        data4 = 2000.toUByte()
                    )
                )
            }
        } else {
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_SamplingProbeCleaning,
                    data3 = (2000 shr 8).toUByte(),
                    data4 = 2000.toUByte()
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
        volume: Int = LocalData.getSamplingVolume()
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
                data1 = (LocalData.getTakeReagentR2() shr 8).toUByte(),
                data2 = LocalData.getTakeReagentR2().toUByte(),
                data3 = (LocalData.getTakeReagentR1() shr 8).toUByte(),
                data4 = LocalData.getTakeReagentR1().toUByte()
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
                data1 = (LocalData.getTakeReagentR2() shr 8).toUByte(),
                data2 = LocalData.getTakeReagentR2().toUByte(),
                data3 = (LocalData.getTakeReagentR1() shr 8).toUByte(),
                data4 = LocalData.getTakeReagentR1().toUByte()
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
                data3 = (500 shr 8).toUByte(),
                data4 = 500.toUByte()
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
                        data3 = (2000 shr 8).toUByte(),
                        data4 = 2000.toUByte()
                    )
                )
            }
        } else {
            writeAsync(
                createCmd(
                    SerialGlobal.CMD_StirProbeCleaning,
                    data3 = (2000 shr 8).toUByte(),
                    data4 = 2000.toUByte()
                )
            )
        }
    }

    /**
     * 检测
     */
    fun test() {
//        Timber.d("发送 检测")
        writeAsync(createCmd(SerialGlobal.CMD_Test))
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
    fun getGetCuvetteDoorState() {
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
    fun readDataStateFailed(cmd: UByte, state: UByte)
}
