package com.wl.turbidimetric.model

data class ReplyModel<T>(val what: UByte, val state: Int, val data: T)

//自检
data class GetMachineStateModel(val errorInfo: MutableList<ErrorInfo> = mutableListOf());
//错误信息
data class ErrorInfo(val errorMsg: String, val motorMsg: String)

//获取状态
data class GetStateModel(
    val shitTubeShelfs: IntArray = IntArray(4),
    val cuvetteShelfs: IntArray = IntArray(4),
    val r1Reagent: Boolean = true,
    val r2Reagent: Boolean = true,
    val cleanoutFluid: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetStateModel) return false

        if (!shitTubeShelfs.contentEquals(other.shitTubeShelfs)) return false
        if (!cuvetteShelfs.contentEquals(other.cuvetteShelfs)) return false
        if (r1Reagent != other.r1Reagent) return false
        if (r2Reagent != other.r2Reagent) return false
        if (cleanoutFluid != other.cleanoutFluid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shitTubeShelfs.contentHashCode()
        result = 31 * result + cuvetteShelfs.contentHashCode()
        result = 31 * result + r1Reagent.hashCode()
        result = 31 * result + r2Reagent.hashCode()
        result = 31 * result + cleanoutFluid.hashCode()
        return result
    }
}

//移动采便管架
data class MoveShitTubeShelfModel(val v: Int = 0)

//移动比色皿架
data class MoveCuvetteShelfModel(val v: Int = 0)

//移动采便管
data class MoveShitTubeModel(val exist: Boolean = false)

//移动比色皿 加样位
data class MoveCuvetteDripSampleModel(val v: Int = 0)

//移动比色皿 搅拌位，加试剂位
data class MoveCuvetteDripReagentModel(val v: Int = 0)

//移动比色皿 检测位
data class MoveCuvetteTestModel(val v: Int = 0)

//取样
data class SamplingModel(val v: Int = 0)

//取样针清洗
data class SamplingProbeCleaningModel(val v: Int = 0)

//搅拌针清洗
data class StirProbeCleaningModel(val v: Int = 0)

//加样
data class DripSampleModel(val v: Int = 0)

//加试剂
data class DripReagentModel(val v: Int = 0)

//取试剂
data class TakeReagentModel(val v: Int = 0)

//搅拌
data class StirModel(val v: Int = 0)

//获取版本号
data class GetVersionModel(val version: String = "")

//刺破
data class PiercedModel(val v: Int = 0)

//检测
data class TestModel(val value: Int = 0)

//比色皿舱门
data class CuvetteDoorModel(val isOpen: Boolean = false)

//采便管舱门
data class ShitTubeDoorModel(val isOpen: Boolean = false)

