package com.wl.turbidimetric.global

object SerialGlobal {
    /**
     * 自检
     */
    const val CMD_GetMachineState: UByte = 0x01u

    /**
     * 获取样本架，比色皿架状态
     */
    const val CMD_GetState: UByte = 0x02u

    /**
     * 移动样本架
     */
    const val CMD_MoveSampleShelf: UByte = 0x03u

    /**
     * 移动比色皿架
     */
    const val CMD_MoveCuvetteShelf: UByte = 0x04u

    /**
     * 移动样本
     */
    const val CMD_MoveSample: UByte = 0x05u

    /**
     * 移动比色皿到加样位
     */
    const val CMD_MoveCuvetteDripSample: UByte = 0x06u

    /**
     * 移动比色皿到加试剂位
     */
    const val CMD_MoveCuvetteDripReagent: UByte = 0x07u

    /**
     * 移动比色皿到检测位
     */
    const val CMD_MoveCuvetteTest: UByte = 0x08u

    /**
     * 取样
     */
    const val CMD_Sampling: UByte = 0x09u

    /**
     * 加样
     */
    const val CMD_DripSample: UByte = 0x0au

    /**
     * 加试剂
     */
    const val CMD_DripReagent: UByte = 0x10u

    /**
     * 取试剂
     */
    const val CMD_TakeReagent: UByte = 0x0bu

    /**
     * 取样针清洗
     */
    const val CMD_SamplingProbeCleaning: UByte = 0x12u

    /**
     * 搅拌针清洗
     */
    const val CMD_StirProbeCleaning: UByte = 0x11u

    /**
     * 搅拌
     */
    const val CMD_Stir: UByte = 0x0cu

    /**
     * 检测
     */
    const val CMD_Test: UByte = 0x0du

    /**
     * 获取版本号
     */
    const val CMD_GetVersion: UByte = 0x13u

    /**
     * 刺破
     */
    const val CMD_Pierced: UByte = 0x14u

    /**
     * 开门|获取样本舱门
     */
    const val CMD_SampleDoor: UByte = 0x0eu

    /**
     * 开门|获取比色皿舱门
     */
    const val CMD_CuvetteDoor: UByte = 0x0fu

    /**
     * 获取设置温度
     */
    const val CMD_GetSetTemp:UByte = 0x15u
    /**
     * 关机
     */
    const val CMD_Shutdown:UByte = 0x16u
    /**
     * 挤压
     */
    const val CMD_Squeezing:UByte = 0x17u
    /**
     * 升级mcu
     */
    const val CMD_McuUpdate:UByte = 0x18u

    /**
     * 响应
     */
    const val CMD_Response: UByte = 0xFFu
}
