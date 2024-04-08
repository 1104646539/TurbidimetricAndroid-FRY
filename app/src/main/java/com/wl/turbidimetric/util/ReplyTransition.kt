package com.wl.turbidimetric.util

import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.ex.getStep
import com.wl.turbidimetric.ex.merge
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.*

/**
 *
 * 自检的错误信息
 */
val MOTORS: Array<String> = getResource().getStringArray(R.array.getMachineStateErrors)

/**
 * 错误号的具体信息
 * 错误0代表无错误 错误1代表不能复位  错误2表示不能向前移动 错误3表示管架X轴错误
 */
val ERRORNUM = getResource().getStringArray(R.array.errorNum)
/**
 *一次合格的回复格式如下。一共14位，其中6位前缀，1位功能码，1位状态码，4位数据位，2位CRC校验位
 * 前缀	        功能   状态码    数据位		校验
 * 6*8bit(0x00)	8bit	8bit	 4*8bit		2*8bit
 */

/**
 * 解析成 自检
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionGetMachineStateModel(data: UByteArray): ReplyModel<GetMachineStateModel> {
    val errorInfo = mutableListOf<ErrorInfo>()
    val states = merge(data.copyOfRange(2, 6))
    for (i in 0..12) {
        val num = (states shr (i * 2)) and 3
//        println("states=$states num=$num i =$i ")
        if (num > 0) {
            errorInfo.add(ErrorInfo("错误号:$num", "错误信息:${MOTORS[i]}"))
        }
    }
    //这几个是只有一种错误的，固定错误号为1
    val removeSampleAndCuvette = (states shr 26) and 1
    val door = (states shr 27) and 1
    val r1 = (states shr 28) and 1
    val cleanoutFluid = (states shr 29) and 1
    val squeezing = (states shr 30) and 1

    if (removeSampleAndCuvette == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${MOTORS[(13)]}"))
    if (door == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${MOTORS[(14)]}"))
    if (r1 == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${MOTORS[15]}"))
    if (cleanoutFluid == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${MOTORS[(16)]}"))
    if (squeezing == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${MOTORS[(17)]}"))

    return ReplyModel(
        SerialGlobal.CMD_GetMachineState, convertReplyState(data[1].toInt()), GetMachineStateModel(errorInfo)
    )
}

/**
 * 解析成 获取比色皿架|样本架|试剂状态|r2量
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionGetStateModel(data: UByteArray): ReplyModel<GetStateModel> {
    return ReplyModel(
        SerialGlobal.CMD_GetState, convertReplyState(data[1].toInt()), GetStateModel(
            sampleShelfs = intArrayOf(
                getStep(data[4], 4), getStep(data[4], 5), getStep(data[4], 6), getStep(data[4], 7)
            ),
            cuvetteShelfs = intArrayOf(
                getStep(data[4], 0), getStep(data[4], 1), getStep(data[4], 2), getStep(data[4], 3)
            ),
            r1Reagent = getStep(data[5], 1) == 1,
            r2Reagent = getStep(data[5], 0) == 1,
            cleanoutFluid = getStep(data[5], 2) == 1,
            r2Volume = data[3].toInt()
        )
    )
}

/**
 * 解析成 比色皿架
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveCuvetteShelfModel(data: UByteArray): ReplyModel<MoveCuvetteShelfModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveCuvetteShelf, convertReplyState(data[1].toInt()), MoveCuvetteShelfModel()
    )
}

/**
 * 解析成 移动样本架
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveSampleShelfModel(data: UByteArray): ReplyModel<MoveSampleShelfModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveSampleShelf, convertReplyState(data[1].toInt()), MoveSampleShelfModel()
    )
}

/**
 * 解析成 移动样本
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveSampleModel(data: UByteArray): ReplyModel<MoveSampleModel> {
    val type = data[5].toInt().takeIf { it < 3 } ?: 0
    return ReplyModel(
        SerialGlobal.CMD_MoveSample, convertReplyState(data[1].toInt()), MoveSampleModel(SampleType.values()[type])
    )
}

/**
 * 解析成 解析成 移动比色皿 加样位
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveCuvetteDripSampleModel(data: UByteArray): ReplyModel<MoveCuvetteDripSampleModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveCuvetteDripSample, convertReplyState(data[1].toInt()), MoveCuvetteDripSampleModel()
    )
}

/**
 * 解析成 移动比色皿 加试剂|搅拌位
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveCuvetteDripReagentModel(data: UByteArray): ReplyModel<MoveCuvetteDripReagentModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveCuvetteDripReagent, convertReplyState(data[1].toInt()), MoveCuvetteDripReagentModel()
    )
}

/**
 * 解析成 移动比色皿 检测位
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveCuvetteTestModel(data: UByteArray): ReplyModel<MoveCuvetteTestModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveCuvetteTest, convertReplyState(data[1].toInt()), MoveCuvetteTestModel()
    )
}

/**
 * 解析成 取样
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionSamplingModel(data: UByteArray): ReplyModel<SamplingModel> {
    return ReplyModel(
        SerialGlobal.CMD_Sampling, convertReplyState(data[1].toInt()), SamplingModel()
    )
}

/**
 * 解析成 取样针清洗
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionSamplingProbeCleaningModel(data: UByteArray): ReplyModel<SamplingProbeCleaningModel> {
    return ReplyModel(
        SerialGlobal.CMD_SamplingProbeCleaning,
        convertReplyState(data[1].toInt()),
        SamplingProbeCleaningModel(cleanoutFluid = getStep(data[5], 0) == 1)
    )
}

/**
 * 解析成 取样针清洗
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionStirProbeCleaningModel(data: UByteArray): ReplyModel<StirProbeCleaningModel> {
    return ReplyModel(
        SerialGlobal.CMD_StirProbeCleaning,
        convertReplyState(data[1].toInt()),
        StirProbeCleaningModel(cleanoutFluid = getStep(data[5], 0) == 1)
    )
}

/**
 * 解析成 加样
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionDripSampleModel(data: UByteArray): ReplyModel<DripSampleModel> {
    return ReplyModel(
        SerialGlobal.CMD_DripSample, convertReplyState(data[1].toInt()), DripSampleModel()
    )
}

/**
 * 解析成 搅拌
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionStirModel(data: UByteArray): ReplyModel<StirModel> {
    return ReplyModel(
        SerialGlobal.CMD_Stir, convertReplyState(data[1].toInt()), StirModel()
    )
}

/**
 * 解析成 加试剂
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionDripReagentModel(data: UByteArray): ReplyModel<DripReagentModel> {
    return ReplyModel(
        SerialGlobal.CMD_DripReagent, convertReplyState(data[1].toInt()), DripReagentModel()
    )
}

/**
 * 解析成 取试剂
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionTakeReagentModel(data: UByteArray): ReplyModel<TakeReagentModel> {
    return ReplyModel(
        SerialGlobal.CMD_TakeReagent,
        convertReplyState(data[1].toInt()),
        TakeReagentModel(r1Reagent = getStep(data[4], 0) == 1, r2Volume = data[5].toInt())
    )
}

/**
 * 解析成 检测
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionTestModel(data: UByteArray): ReplyModel<TestModel> {
    return ReplyModel(
        SerialGlobal.CMD_Test, convertReplyState(data[1].toInt()), TestModel(value = merge(data.copyOfRange(2, 6)))
    )
}

/**
 * 解析成 设置|获取比色皿舱门状态
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionCuvetteDoorModel(data: UByteArray): ReplyModel<CuvetteDoorModel> {
    return ReplyModel(
        SerialGlobal.CMD_CuvetteDoor,
        convertReplyState(data[1].toInt()),
        CuvetteDoorModel(isOpen = getStep(data[5], 0) == 1)
    )
}

/**
 * 解析成 设置|获取样本舱门状态
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionSampleDoorModel(data: UByteArray): ReplyModel<SampleDoorModel> {
    return ReplyModel(
        SerialGlobal.CMD_SampleDoor,
        convertReplyState(data[1].toInt()),
        SampleDoorModel(isOpen = getStep(data[5], 0) == 1)
    )
}

/**
 * 解析成 刺破
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionPiercedModel(data: UByteArray): ReplyModel<PiercedModel> {
    return ReplyModel(
        SerialGlobal.CMD_Pierced, convertReplyState(data[1].toInt()), PiercedModel()
    )
}

/**
 * 解析成 获取版本号
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionGetVersionModel(data: UByteArray): ReplyModel<GetVersionModel> {
    return ReplyModel(
        SerialGlobal.CMD_GetVersion,
        convertReplyState(data[1].toInt()),
        GetVersionModel("${data[2].toInt()}.${data[3].toInt()}.${data[4].toInt()}.${data[5].toInt()}")
    )
}

/**
 * 解析成 获取温度
 * @param data UByteArray
 */
fun transitionTempModel(data: UByteArray): ReplyModel<TempModel> {
    return ReplyModel(
        SerialGlobal.CMD_GetSetTemp, convertReplyState(data[1].toInt()), TempModel(
            reactionTemp = merge(ubyteArrayOf(data[2], data[3])),
            r1Temp = merge(ubyteArrayOf(data[4], data[5]))
        )
    )
}

/**
 * 解析成 挤压
 * @param data UByteArray
 * @return ReplyModel<SqueezingModel>
 */
fun transitionSqueezingModel(data: UByteArray): ReplyModel<SqueezingModel> {
    return ReplyModel(
        SerialGlobal.CMD_Squeezing, convertReplyState(data[1].toInt()), SqueezingModel(
            data[1].toInt(),
        )
    )
}
/**
 * 解析成 mcu升级
 * @param data UByteArray
 * @return ReplyModel<SqueezingModel>
 */
fun transitionMcuUpdateModel(data: UByteArray): ReplyModel<McuUpdateModel> {
    return ReplyModel(
        SerialGlobal.CMD_McuUpdate, convertReplyState(data[1].toInt()), McuUpdateModel(
            data[3].toInt() == 1,
        )
    )
}
/**
 * 电机控制
 * @param data UByteArray
 * @return ReplyModel<SqueezingModel>
 */
fun transitionMotorModel(data: UByteArray): ReplyModel<MotorModel> {
    return ReplyModel(
        SerialGlobal.CMD_Motor, convertReplyState(data[1].toInt()), MotorModel(
            data[3].toInt(),
        )
    )
}
