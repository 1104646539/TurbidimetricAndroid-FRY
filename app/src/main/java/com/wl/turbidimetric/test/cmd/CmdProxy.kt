package com.wl.turbidimetric.test.cmd

import com.wl.turbidimetric.model.*

class CmdProxy() : CmdIF() {
    var cmd: CmdIF? = null

    override fun exec() {
        cmd?.exec()
    }

    override fun finish() {
        cmd?.finish()
    }

    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
    }

    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {

    }

    override fun readDataMoveSampleShelfModel(reply: ReplyModel<MoveSampleShelfModel>) {
        cmd?.finish()
    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {

    }

    override fun readDataMoveSampleModel(reply: ReplyModel<MoveSampleModel>) {

    }

    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {

    }

    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {

    }

    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {

    }

    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {

    }

    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {

    }

    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {

    }

    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {

    }

    override fun readDataStirModel(reply: ReplyModel<StirModel>) {

    }

    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {

    }

    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {

    }

    override fun readDataTestModel(reply: ReplyModel<TestModel>) {

    }

    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {

    }

    override fun readDataSampleDoorModel(reply: ReplyModel<SampleDoorModel>) {

    }

    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {

    }

    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {

    }

    override fun readDataStateFailed(cmd: UByte, state: UByte) {

    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {


    }

}
