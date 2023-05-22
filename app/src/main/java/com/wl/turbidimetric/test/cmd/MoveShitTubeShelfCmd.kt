package com.wl.turbidimetric.test.cmd

import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.SerialPortUtil

class MoveShitTubeShelfCmd(private val pos: Int) : CmdIF() {
    override fun exec() {
        super.exec()
        SerialPortUtil.Instance.moveShitTubeShelf(pos)
    }

    override fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        super.readDataGetMachineStateModel(reply)
    }

    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        super.readDataGetStateModel(reply)
    }

    override fun readDataMoveShitTubeShelfModel(reply: ReplyModel<MoveShitTubeShelfModel>) {
        super.readDataMoveShitTubeShelfModel(reply)
    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        super.readDataMoveCuvetteShelfModel(reply)
    }

    override fun readDataMoveShitTubeModel(reply: ReplyModel<MoveShitTubeModel>) {
        super.readDataMoveShitTubeModel(reply)
    }

    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {
        super.readDataMoveCuvetteDripSampleModel(reply)
    }

    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        super.readDataMoveCuvetteDripReagentModel(reply)
    }

    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        super.readDataMoveCuvetteTestModel(reply)
    }

    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        super.readDataSamplingModel(reply)
    }

    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        super.readDataTakeReagentModel(reply)
    }

    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        super.readDataDripSampleModel(reply)
    }

    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        super.readDataDripReagentModel(reply)
    }

    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        super.readDataStirModel(reply)
    }

    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        super.readDataStirProbeCleaningModel(reply)
    }

    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {
        super.readDataSamplingProbeCleaningModelModel(reply)
    }

    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        super.readDataTestModel(reply)
    }

    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
        super.readDataCuvetteDoorModel(reply)
    }

    override fun readDataShitTubeDoorModel(reply: ReplyModel<ShitTubeDoorModel>) {
        super.readDataShitTubeDoorModel(reply)
    }

    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {
        super.readDataPiercedModel(reply)
    }

    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        super.readDataGetVersionModel(reply)
    }

    override fun readDataStateFailed(cmd: UByte, state: UByte) {
        super.readDataStateFailed(cmd, state)
    }
}
