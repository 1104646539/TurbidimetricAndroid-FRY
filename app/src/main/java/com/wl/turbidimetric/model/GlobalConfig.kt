package com.wl.turbidimetric.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wl.turbidimetric.datastore.LocalDataGlobal

@Entity
data class GlobalConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /**
     * 检测编号
     */
    val DetectionNum: String = LocalDataGlobal.Default.DetectionNum,
    /**
     * 取试剂量R1 缓冲液
     */
    val TakeReagentR1: Int = LocalDataGlobal.Default.TakeReagentR1,
    /**
     * 取试剂量R2 乳胶试剂
     */
    val TakeReagentR2: Int = LocalDataGlobal.Default.TakeReagentR2,
    /**
     * 取样量
     */
    val SamplingVolume: Int = LocalDataGlobal.Default.SamplingVolume,
    /**
     * 最后一次进入的版本号
     */
    val CurrentVersion: Int = LocalDataGlobal.Default.CurrentVersion,
    /**
     * 搅拌时长
     */
    val StirDuration: Int = LocalDataGlobal.Default.StirDuration,
    /**
     * 搅拌针清洗时长
     */
    val StirProbeCleaningDuration: Int = LocalDataGlobal.Default.StirProbeCleaningDuration,
    /**
     * 取样针清洗时长
     */
    val SamplingProbeCleaningDuration: Int = LocalDataGlobal.Default.SamplingProbeCleaningDuration,
    /**
     * 当前检测模式 自动 手动
     */
    val CurMachineTestModel: String = LocalDataGlobal.Default.MachineTestModelDefault.name,
    /**
     * 自动模式下，是否使用样本传感器
     */
    val SampleExist: Boolean = LocalDataGlobal.Default.SampleExist,

    /**
     * 自动模式下，是否使用扫码器
     */
    val ScanCode: Boolean = LocalDataGlobal.Default.ScanCode,
    /**
     * 检测时，选择的检测项目的ID
     */
    val SelectProjectID: Long = LocalDataGlobal.Default.SelectProjectID,
    /**
     * 调试模式
     */
    val DebugMode: Boolean = LocalDataGlobal.Default.DebugMode,
    /**
     * 检测第一次的等待时间
     */
    val Test1DelayTime: Long = LocalDataGlobal.Default.Test1DelayTime,
    /**
     * 检测第二次的等待时间
     */
    val Test2DelayTime: Long = LocalDataGlobal.Default.Test2DelayTime,
    /**
     * 检测第三次的等待时间
     */
    val Test3DelayTime: Long = LocalDataGlobal.Default.Test3DelayTime,
    /**
     * 检测第四次的等待时间
     */
    val Test4DelayTime: Long = LocalDataGlobal.Default.Test4DelayTime,
    /**
     * 反应时间
     * 第二次减去第一次
     */
    val ReactionTime: Long = LocalDataGlobal.Default.ReactionTime,
    /**
     * 循环测试
     */
    val LooperTest: Boolean = LocalDataGlobal.Default.LooperTest,
    /**
     * 报告医院名
     */
    val HospitalName: String = LocalDataGlobal.Default.HospitalName,
    /**
     * 是否自动打印小票
     */
    val AutoPrintReceipt: Boolean = LocalDataGlobal.Default.AutoPrintReceipt,
    /**
     * 是否自动打印A4报告
     */
    val AutoPrintReport: Boolean = LocalDataGlobal.Default.AutoPrintReport,
    /**
     * 生成报告文件的规则
     * true 条码
     * false 编号
     */
    val ReportFileNameBarcode: Boolean = LocalDataGlobal.Default.ReportFileNameBarcode,
    /**
     * 打印报告的间隔时间
     */
    val ReportIntervalTime: Int = LocalDataGlobal.Default.ReportIntervalTime,
    /**
     * 可以检测的温度的上限
     */
    val TempUpLimit: Int = LocalDataGlobal.Default.TempUpLimit,
    /**
     * 可以检测的温度的下限
     */
    val TempLowLimit: Int = LocalDataGlobal.Default.TempLowLimit,
)
