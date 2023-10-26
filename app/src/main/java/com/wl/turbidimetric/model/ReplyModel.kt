package com.wl.turbidimetric.model

data class ReplyModel<T>(val what: UByte, val state: Int, val data: T)

/**
 * 自检
 * @property errorInfo MutableList<ErrorInfo> 错误信息
 * @constructor
 */
data class GetMachineStateModel(val errorInfo: MutableList<ErrorInfo> = mutableListOf());
/**
 * 错误信息
 * @property errorMsg String
 * @property motorMsg String
 * @constructor
 */
data class ErrorInfo(val errorMsg: String, val motorMsg: String)

/**
 * 获取状态
 * @property sampleShelfs IntArray 样本架状态
 * @property cuvetteShelfs IntArray 比色皿架状态
 * @property r1Reagent Boolean r1状态
 * @property r2Reagent Boolean r2状态
 * @property cleanoutFluid Boolean 清洗液状态
 * @property r2Volume Int r2量
 * @constructor
 */
data class GetStateModel(
    val sampleShelfs: IntArray = IntArray(4),
    val cuvetteShelfs: IntArray = IntArray(4),
    val r1Reagent: Boolean = true,
    val r2Reagent: Boolean = true,
    val cleanoutFluid: Boolean = true,
    val r2Volume: Int = 0
)

/**
 * 移动样本架
 * @property v Int
 * @constructor
 */
data class MoveSampleShelfModel(val v: Int = 0)

/**
 * 移动比色皿架
 * @property v Int
 * @constructor
 */
data class MoveCuvetteShelfModel(val v: Int = 0)

/**
 * 移动样本
 * @property SampleType
 * @constructor
 */
data class MoveSampleModel(val type: SampleType = SampleType.NONEXISTENT)

/**
 * 移动比色皿 加样位
 * @property v Int
 * @constructor
 */
data class MoveCuvetteDripSampleModel(val v: Int = 0)

/**
 * 移动比色皿 搅拌位，加试剂位
 * @property v Int
 * @constructor
 */
data class MoveCuvetteDripReagentModel(val v: Int = 0)

/**
 * 移动比色皿 检测位
 * @property v Int
 * @constructor
 */
data class MoveCuvetteTestModel(val v: Int = 0)

/**
 * 取样
 * @property v Int
 * @constructor
 */
data class SamplingModel(val v: Int = 0)

/**
 * 取样针清洗
 * @property v Int
 * @constructor
 */
data class SamplingProbeCleaningModel(val cleanoutFluid: Boolean = true)

/**
 * 搅拌针清洗
 * @property v Int
 * @constructor
 */
data class StirProbeCleaningModel(val cleanoutFluid: Boolean = true)

/**
 * 加样
 * @property v Int
 * @constructor
 */
data class DripSampleModel(val v: Int = 0)

/**
 * 加试剂
 * @property v Int
 * @constructor
 */
data class DripReagentModel(val v: Int = 0)

/**
 * 取试剂
 * @property v Int
 * @constructor
 */
data class TakeReagentModel(val r1Reagent: Boolean = true, val r2Volume: Int = 0)

/**
 * 搅拌
 * @property v Int
 * @constructor
 */
data class StirModel(val v: Int = 0)

/**
 * 获取版本号
 * @property version String 版本号 x.x.x.x
 * @constructor
 */
data class GetVersionModel(val version: String = "")

/**
 * 刺破
 * @property v Int
 * @constructor
 */
data class PiercedModel(val v: Int = 0)

/**
 * 检测
 * @property value Int 检测值
 * @constructor
 */
data class TestModel(val value: Int = 0)

/**
 * 比色皿舱门
 * @property isOpen Boolean 是否开启
 * @constructor
 */
data class CuvetteDoorModel(val isOpen: Boolean = false)

/**
 * 样本舱门
 * @property isOpen Boolean 是否开启
 * @constructor
 */
data class SampleDoorModel(val isOpen: Boolean = false)


/**
 * 获取设置温度
 * @property reactionTemp Int 反应槽温度
 * @property r1Temp Int r1试剂温度
 * @constructor
 */
data class TempModel(val reactionTemp: Int = 0, val r1Temp: Int = 0)


/**
 * 挤压
 * @property v Int
 * @constructor
 */
data class SqueezingModel(val v: Int = 0)

/**
 * 检测到的采便管的类型
 */
enum class SampleType(val state: String) {
    NONEXISTENT("不存在"), SAMPLE("样本管"), CUVETTE("比色杯")
}
