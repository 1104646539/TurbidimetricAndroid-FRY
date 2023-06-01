package com.wl.turbidimetric.util

import com.wl.turbidimetric.ex.getStep
import com.wl.turbidimetric.ex.merge
import com.wl.turbidimetric.global.SerialGlobal
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.*

/**
 * 自检的错误信息
 */
val Motors = listOf(
    "取样泵",
    "采便管架X",
    "采便管架Y",
    "比色皿架X",
    "比色皿架Y",
    "试剂针X",
    "试剂针Y",
    "样本针X",
    "样本针Z",
    "试剂泵",
    "搅拌",
    "扶正",
    "转盘",
    "请移除样本架和比色皿",
    "舱门未关",
    "缓冲液不足",
    "清洗液不足",
)


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
            errorInfo.add(ErrorInfo("错误号:$num", "电机:${Motors[(i)]}"))
        }
    }
    val removeSampleAndCuvette = (states shr 26) and 1
    val door = (states shr 27) and 1
    val r1 = (states shr 28) and 1
    val cleanoutFluid = (states shr 29) and 1

    if (removeSampleAndCuvette == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${Motors[(13)]}"))
    if (door == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${Motors[(14)]}"))
    if (r1 == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${Motors[15]}"))
    if (cleanoutFluid == 1) errorInfo.add(ErrorInfo("错误号:1", "错误信息:${Motors[(16)]}"))

    return ReplyModel(
        SerialGlobal.CMD_GetMachineState,
        data[1].toInt(),
        GetMachineStateModel(errorInfo)
    )
}

/**
 * 解析成 获取比色皿架|采便管架|试剂状态|r2量
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionGetStateModel(data: UByteArray): ReplyModel<GetStateModel> {
    return ReplyModel(
        SerialGlobal.CMD_GetState,
        data[1].toInt(),
        GetStateModel(
            shitTubeShelfs = intArrayOf(
                getStep(data[4], 4),
                getStep(data[4], 5),
                getStep(data[4], 6),
                getStep(data[4], 7)
            ),
            cuvetteShelfs = intArrayOf(
                getStep(data[4], 0),
                getStep(data[4], 1),
                getStep(data[4], 2),
                getStep(data[4], 3)
            ),
            r1Reagent = getStep(data[5], 0) == 1,
            r2Reagent = getStep(data[5], 1) == 1,
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
        SerialGlobal.CMD_MoveCuvetteShelf,
        data[1].toInt(),
        MoveCuvetteShelfModel()
    )
}

/**
 * 解析成 移动采便管架
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveShitTubeShelfModel(data: UByteArray): ReplyModel<MoveShitTubeShelfModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveShitTubeShelf,
        data[1].toInt(),
        MoveShitTubeShelfModel()
    )
}

/**
 * 解析成 移动采便管
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveShitTubeModel(data: UByteArray): ReplyModel<MoveShitTubeModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveShitTube,
        data[1].toInt(),
        MoveShitTubeModel(getStep(data[5], 0) == 1)
    )
}

/**
 * 解析成 解析成 移动比色皿 加样位
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveCuvetteDripSampleModel(data: UByteArray): ReplyModel<MoveCuvetteDripSampleModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveCuvetteDripSample,
        data[1].toInt(),
        MoveCuvetteDripSampleModel()
    )
}

/**
 * 解析成 移动比色皿 加试剂|搅拌位
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveCuvetteDripReagentModel(data: UByteArray): ReplyModel<MoveCuvetteDripReagentModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveCuvetteDripReagent,
        data[1].toInt(),
        MoveCuvetteDripReagentModel()
    )
}

/**
 * 解析成 移动比色皿 检测位
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionMoveCuvetteTestModel(data: UByteArray): ReplyModel<MoveCuvetteTestModel> {
    return ReplyModel(
        SerialGlobal.CMD_MoveCuvetteTest,
        data[1].toInt(),
        MoveCuvetteTestModel()
    )
}

/**
 * 解析成 取样
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionSamplingModel(data: UByteArray): ReplyModel<SamplingModel> {
    return ReplyModel(
        SerialGlobal.CMD_Sampling,
        data[1].toInt(),
        SamplingModel()
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
        data[1].toInt(),
        SamplingProbeCleaningModel()
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
        data[1].toInt(),
        StirProbeCleaningModel()
    )
}

/**
 * 解析成 加样
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionDripSampleModel(data: UByteArray): ReplyModel<DripSampleModel> {
    return ReplyModel(
        SerialGlobal.CMD_DripSample,
        data[1].toInt(),
        DripSampleModel()
    )
}

/**
 * 解析成 搅拌
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionStirModel(data: UByteArray): ReplyModel<StirModel> {
    return ReplyModel(
        SerialGlobal.CMD_Stir,
        data[1].toInt(),
        StirModel()
    )
}

/**
 * 解析成 加试剂
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionDripReagentModel(data: UByteArray): ReplyModel<DripReagentModel> {
    return ReplyModel(
        SerialGlobal.CMD_DripReagent,
        data[1].toInt(),
        DripReagentModel()
    )
}

/**
 * 解析成 加试剂
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionTakeReagentModel(data: UByteArray): ReplyModel<TakeReagentModel> {
    return ReplyModel(
        SerialGlobal.CMD_TakeReagent,
        data[1].toInt(),
        TakeReagentModel()
    )
}

/**
 * 解析成 检测
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionTestModel(data: UByteArray): ReplyModel<TestModel> {
    return ReplyModel(
        SerialGlobal.CMD_Test,
        data[1].toInt(),
        TestModel(value = merge(data.copyOfRange(2, 6)))
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
        data[1].toInt(),
        CuvetteDoorModel(isOpen = getStep(data[5], 0) == 1)
    )
}

/**
 * 解析成 设置|获取采便管舱门状态
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionShitTubeDoorModel(data: UByteArray): ReplyModel<ShitTubeDoorModel> {
    return ReplyModel(
        SerialGlobal.CMD_ShitTubeDoor,
        data[1].toInt(),
        ShitTubeDoorModel(isOpen = getStep(data[5], 0) == 1)
    )
}

/**
 * 解析成 刺破
 * @param data UByteArray
 * @return ReplyModel<Any>
 */
fun transitionPiercedModel(data: UByteArray): ReplyModel<PiercedModel> {
    return ReplyModel(
        SerialGlobal.CMD_Pierced,
        data[1].toInt(),
        PiercedModel()
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
        data[1].toInt(),
        GetVersionModel("${data[2].toInt()}.${data[3].toInt()}.${data[4].toInt()}.${data[5].toInt()}.")
    )
}

/**
 * 解析成 获取温度
 * @param data UByteArray
 */
fun transitionTempModel(data: UByteArray): ReplyModel<TempModel> {
    return ReplyModel(
        SerialGlobal.CMD_GetSetTemp,
        data[1].toInt(),
        TempModel(
            reactionTemp = merge(ubyteArrayOf(data[2], data[3])),
            r1Temp = merge(ubyteArrayOf(data[4], data[5]))
        )
    )
}
