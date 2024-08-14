package com.wl.turbidimetric.util

import com.wl.turbidimetric.model.CuvetteDoorModel
import com.wl.turbidimetric.model.DripReagentModel
import com.wl.turbidimetric.model.DripSampleModel
import com.wl.turbidimetric.model.GetMachineStateModel
import com.wl.turbidimetric.model.GetStateModel
import com.wl.turbidimetric.model.GetVersionModel
import com.wl.turbidimetric.model.McuUpdateModel
import com.wl.turbidimetric.model.MotorModel
import com.wl.turbidimetric.model.MoveCuvetteDripReagentModel
import com.wl.turbidimetric.model.MoveCuvetteDripSampleModel
import com.wl.turbidimetric.model.MoveCuvetteShelfModel
import com.wl.turbidimetric.model.MoveCuvetteTestModel
import com.wl.turbidimetric.model.MoveSampleModel
import com.wl.turbidimetric.model.MoveSampleShelfModel
import com.wl.turbidimetric.model.OverloadParamsModel
import com.wl.turbidimetric.model.PiercedModel
import com.wl.turbidimetric.model.ReplyModel
import com.wl.turbidimetric.model.SampleDoorModel
import com.wl.turbidimetric.model.SamplingModel
import com.wl.turbidimetric.model.SamplingProbeCleaningModel
import com.wl.turbidimetric.model.SqueezingModel
import com.wl.turbidimetric.model.StirModel
import com.wl.turbidimetric.model.StirProbeCleaningModel
import com.wl.turbidimetric.model.TakeReagentModel
import com.wl.turbidimetric.model.TempModel
import com.wl.turbidimetric.model.TestModel

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
    fun readDataOverloadParamsModel(reply: ReplyModel<OverloadParamsModel>)
}

fun interface McuUpdateCallBack {
    fun readDataMcuUpdate(reply: ReplyModel<McuUpdateModel>)
}

interface OriginalDataCall {
    fun readDataOriginalData(ready: UByteArray)
    fun sendOriginalData(ready: UByteArray)
}
