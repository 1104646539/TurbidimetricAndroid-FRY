package com.wl.turbidimetric.test.cmd

import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.Callback2
import com.wl.turbidimetric.util.SerialPortUtil
import timber.log.Timber

abstract class CmdIF : Callback2 {
    open fun exec() {
        SerialPortUtil.Instance.callback.add(this)
    }
    open fun finish() {
        SerialPortUtil.Instance.callback.remove(this)
    }


    override  fun readDataGetMachineStateModel(reply: ReplyModel<GetMachineStateModel>) {
        Timber.d("readDataGetMachineStateModel reply=$reply")
    }

    override fun readDataGetStateModel(reply: ReplyModel<GetStateModel>) {
        Timber.d("readDataGetStateModel reply=$reply")
    }

    override fun readDataMoveShitTubeShelfModel(reply: ReplyModel<MoveShitTubeShelfModel>) {
        Timber.d("readDataMoveShitTubeShelfModel reply=$reply")
    }

    override fun readDataMoveCuvetteShelfModel(reply: ReplyModel<MoveCuvetteShelfModel>) {
        Timber.d("readDataMoveCuvetteShelfModel reply=$reply")
    }

    override fun readDataMoveShitTubeModel(reply: ReplyModel<MoveShitTubeModel>) {
        Timber.d("readDataMoveShitTubeModel reply=$reply")
    }

    override fun readDataMoveCuvetteDripSampleModel(reply: ReplyModel<MoveCuvetteDripSampleModel>) {
        Timber.d("readDataMoveCuvetteDripSampleModel reply=$reply")
    }

    override fun readDataMoveCuvetteDripReagentModel(reply: ReplyModel<MoveCuvetteDripReagentModel>) {
        Timber.d("readDataMoveCuvetteDripReagentModel reply=$reply")
    }

    override fun readDataMoveCuvetteTestModel(reply: ReplyModel<MoveCuvetteTestModel>) {
        Timber.d("readDataMoveCuvetteTestModel reply=$reply")
    }

    override fun readDataSamplingModel(reply: ReplyModel<SamplingModel>) {
        Timber.d("readDataSamplingModel reply=$reply")
    }

    override fun readDataTakeReagentModel(reply: ReplyModel<TakeReagentModel>) {
        Timber.d("readDataTakeReagentModel reply=$reply")
    }

    override fun readDataDripSampleModel(reply: ReplyModel<DripSampleModel>) {
        Timber.d("readDataDripSampleModel reply=$reply")
    }

    override fun readDataDripReagentModel(reply: ReplyModel<DripReagentModel>) {
        Timber.d("readDataDripReagentModel reply=$reply")
    }

    override fun readDataStirModel(reply: ReplyModel<StirModel>) {
        Timber.d("readDataStirModel reply=$reply")
    }

    override fun readDataStirProbeCleaningModel(reply: ReplyModel<StirProbeCleaningModel>) {
        Timber.d("readDataStirProbeCleaningModel reply=$reply")
    }

    override fun readDataSamplingProbeCleaningModelModel(reply: ReplyModel<SamplingProbeCleaningModel>) {
        Timber.d("readDataSamplingProbeCleaningModelModel reply=$reply")
    }

    override fun readDataTestModel(reply: ReplyModel<TestModel>) {
        Timber.d("readDataTestModel reply=$reply")
    }

    override fun readDataCuvetteDoorModel(reply: ReplyModel<CuvetteDoorModel>) {
        Timber.d("readDataCuvetteDoorModel reply=$reply")
    }

    override fun readDataShitTubeDoorModel(reply: ReplyModel<ShitTubeDoorModel>) {
        Timber.d("readDataShitTubeDoorModel reply=$reply")
    }

    override fun readDataPiercedModel(reply: ReplyModel<PiercedModel>) {
        Timber.d("readDataPiercedModel reply=$reply")
    }

    override fun readDataGetVersionModel(reply: ReplyModel<GetVersionModel>) {
        Timber.d("readDataGetVersionModel reply=$reply")
    }

    override fun readDataStateFailed(cmd: UByte, state: UByte) {
        Timber.d("readDataStateFailed cmd=$cmd state=$state")
    }
}
