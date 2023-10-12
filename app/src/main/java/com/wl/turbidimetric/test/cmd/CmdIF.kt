package com.wl.turbidimetric.test.cmd

import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wllib.LogToFile.i
abstract class CmdIF : Callback2 {
    open fun exec() {
        SerialPortUtil.callback.add(this)
    }
    open fun finish() {
        SerialPortUtil.callback.remove(this)
    }


    override  fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        i("readDataGetMachineStateModel reply=$reply")
    }

    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        i("readDataGetStateModel reply=$reply")
    }

    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        i("readDataMoveSampleShelfModel reply=$reply")
    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        i("readDataMoveCuvetteShelfModel reply=$reply")
    }

    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {
        i("readDataMoveSampleModel reply=$reply")
    }

    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {
        i("readDataMoveCuvetteDripSampleModel reply=$reply")
    }

    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        i("readDataMoveCuvetteDripReagentModel reply=$reply")
    }

    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        i("readDataMoveCuvetteTestModel reply=$reply")
    }

    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        i("readDataSamplingModel reply=$reply")
    }

    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        i("readDataTakeReagentModel reply=$reply")
    }

    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        i("readDataDripSampleModel reply=$reply")
    }

    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        i("readDataDripReagentModel reply=$reply")
    }

    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        i("readDataStirModel reply=$reply")
    }

    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        i("readDataStirProbeCleaningModel reply=$reply")
    }

    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {
        i("readDataSamplingProbeCleaningModelModel reply=$reply")
    }

    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        i("readDataTestModel reply=$reply")
    }

    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
        i("readDataCuvetteDoorModel reply=$reply")
    }

    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {
        i("readDataSampleDoorModel reply=$reply")
    }

    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {
        i("readDataPiercedModel reply=$reply")
    }

    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        i("readDataGetVersionModel reply=$reply")
    }

    override fun readDataStateFailed(cmd: UByte, state: UByte) {
        i("readDataStateFailed cmd=$cmd state=$state")
    }
}
