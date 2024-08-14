package com.wl.turbidimetric.util

import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.mcuupdate.UpdateResult
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
    fun readDataMotor(reply: ReplyModel<MotorModel>)
}

fun interface McuUpdateCallBack {
    fun readDataMcuUpdate(reply: ReplyModel<McuUpdateModel>)
}

interface OriginalDataCall {
    fun readDataOriginalData(ready: UByteArray)
    fun sendOriginalData(ready: UByteArray)
}
