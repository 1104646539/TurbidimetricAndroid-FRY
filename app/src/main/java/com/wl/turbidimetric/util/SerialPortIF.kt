package com.wl.turbidimetric.util


import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.mcuupdate.UpdateResult
import com.wl.turbidimetric.model.*
import com.wl.wllib.CRC.CRC16
import kotlinx.coroutines.*
import java.util.*

/**
 * 串口操作类
 */
interface SerialPortIF {

    fun setMcuUpdateCallBack(mcuUpdateCallBack: McuUpdateCallBack)
    fun setOnResult(onResult: ((UpdateResult) -> Unit)?)
    fun addCallback(call: Callback2)
    fun removeCallback(call: Callback2)
    fun addOriginalCallback(call: OriginalDataCall)
    fun removeOriginalCallback(call: OriginalDataCall)

    fun updateWrite(data: UByteArray)

    fun allowRunning()

    /**
     * 打开
     */
    fun open(scope: CoroutineScope)

    /**
     * 关闭
     */
    fun close()

    /**
     * 检测状态发生改变
     * @param testState TestState
     */
    fun testStateChange(testState: TestState)

    /**
     * 获取状态
     */
    fun getState()

    /**
     * 自检
     */
    fun getMachineState()

    /**
     * 移动样本架
     * @param pos Int 移动位置，绝对的
     */
    fun moveSampleShelf(pos: Int)


    /**
     *
     * 移动比色皿架
     * @param pos Int 移动位置，绝对的
     */
    fun moveCuvetteShelf(pos: Int)

    /**
     *
     * 移动样本
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveSample(forward: Boolean = true, pos: Int)


    /**
     * 移动比色皿 加样位
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveCuvetteDripSample(forward: Boolean = true, pos: Int)


    /**
     *
     * 移动比色皿 搅拌位，加试剂位
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveCuvetteDripReagent(forward: Boolean = true, pos: Int)

    /**
     *
     * 移动比色皿 检测位
     * @param forward Boolean 是否向前
     * @param pos Int 移动多少个位置，相对的
     */
    fun moveCuvetteTest(forward: Boolean = true, pos: Int)

    /**
     * 取样
     * @param volume 取样量
     * @param squeezing 是否挤压
     */
    fun sampling(volume: Int, sampleType: SampleType)
    /**
     * 杀死所有进程
     */
    fun killAll()

    /**
     * 取样针清洗
     */
    fun samplingProbeCleaning(samplingProbeCleaningDuration: Int)

    /**
     * 加样
     * @param autoBlending Boolean 是否自动混匀
     * @param inplace Boolean 是否原地加样
     * @param volume Int 加样量
     */
    fun dripSample(
        autoBlending: Boolean = false,
        inplace: Boolean = false,
        volume: Int
    )

    /**
     * 加试剂
     */
    fun dripReagent(
        r1Volume: Int,
        r2Volume: Int
    )

    /**
     * 取试剂
     */
    fun takeReagent(
        r1Volume: Int,
        r2Volume: Int
    )

    /**
     * 搅拌
     */
    fun stir(stirDuration: Int)

    /**
     * 搅拌针清洗
     */
    fun stirProbeCleaning(stirProbeCleaningDuration: Int)

    /**
     * 检测
     */
    fun test()

    /**
     * 获取样本舱门状态
     */
    fun getSampleDoorState()

    /**
     * 开启样本舱门
     */
    fun openSampleDoor()


    /**
     * 获取比色皿舱门状态
     */
    fun getCuvetteDoorState()

    /**
     * 获取比色皿舱门状态
     */
    fun openCuvetteDoor()

    /**
     * 刺破
     */
    fun pierced(sampleType: SampleType)

    /**
     * 获取版本号
     */
    fun getVersion()

    /**
     * 获取比色皿舱门状态|开比色皿舱门
     * @param open Boolean
     */
    fun setGetCuvetteDoor(open: Boolean = false)

    /**
     * 设置温度
     */
    fun setTemp(reactionTemp: Int = 0, r1Temp: Int = 0)

    /**
     * 关机
     */
    fun shutdown()

    /**
     * 挤压
     * @param enable 是否挤压 比色杯不挤压
     */
    fun squeezing(enable: Boolean = true)

    /**
     * 获取当前温度
     */
    fun getTemp()

    /**
     * 升级mcu
     */
    fun mcuUpdate(fileSize: Long)


    /**
     * 控制电机
     */
    fun motor(motorNum: Int, direction: Int, params: Int)

    /**
     * 重载参数
     */
    fun overloadParams()

    /**
     * 填充R1
     */
    fun fullR1()

    /**
     * 是否停止运行
     */
    fun stopRunning(stop: Boolean)
    /**
     * 创建一个完整的命令
     */
    private fun createCmd(
        cmd: UByte,
        data1: UByte = 0x00u,
        data2: UByte = 0x00u,
        data3: UByte = 0x00u,
        data4: UByte = 0x00u
    ): UByteArray {
        var t = ubyteArrayOf(cmd, data1, data2, data3, data4)
        val crc = CRC16(t)
        t = t.plus(crc[0])
        t = t.plus(crc[1])
        return t
    }

}
