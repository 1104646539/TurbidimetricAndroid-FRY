package com.wl.turbidimetric.datastore

import com.wl.turbidimetric.ex.*

object LocalData {
    /**
     * 检测编号
     */
    var DetectionNum by StringDataStoreProperty(LocalDataGlobal.Default.DetectionNum)

    /**
     * 取试剂量R1 缓冲液
     */
    var TakeReagentR1 by IntDataStoreProperty(LocalDataGlobal.Default.TakeReagentR1)

    /**
     * 取试剂量R2 乳胶试剂
     */
    var TakeReagentR2 by IntDataStoreProperty(LocalDataGlobal.Default.TakeReagentR2)

    /**
     * 取样量
     */
    var SamplingVolume by IntDataStoreProperty(LocalDataGlobal.Default.SamplingVolume)

    /**
     * 最后一次进入的版本号
     */
    var CurrentVersion by IntDataStoreProperty(0)

    /**
     * 搅拌时长
     */
    var StirDuration by IntDataStoreProperty(LocalDataGlobal.Default.StirDuration)

    /**
     * 搅拌针清洗时长
     */
    var StirProbeCleaningDuration by IntDataStoreProperty(LocalDataGlobal.Default.StirProbeCleaningDuration)

    /**
     * 取样针清洗时长
     */
    var SamplingProbeCleaningDuration by IntDataStoreProperty(LocalDataGlobal.Default.SamplingProbeCleaningDuration)

    /**
     * 当前检测模式 自动 手动
     */
    var CurMachineTestModel by StringDataStoreProperty(LocalDataGlobal.Default.MachineTestModelDefault.name)

    /**
     * 自动模式下，是否使用样本传感器
     */
    var SampleExist by BooleanDataStoreProperty(LocalDataGlobal.Default.SampleExist)

    /**
     * 自动模式下，是否使用扫码器
     */
    var ScanCode by BooleanDataStoreProperty(LocalDataGlobal.Default.ScanCode)

    /**
     * 检测时，选择的检测项目的ID
     */
    var SelectProjectID by LongDataStoreProperty(LocalDataGlobal.Default.SelectProjectID)

    /**
     * 调试模式
     */
    var DebugMode by BooleanDataStoreProperty(LocalDataGlobal.Default.DebugMode)

    /**
     * 检测第一次的等待时间
     */
    var Test1DelayTime by LongDataStoreProperty(LocalDataGlobal.Default.test1DelayTime)
    /**
     * 检测第二次的等待时间
     */
    var Test2DelayTime by LongDataStoreProperty(LocalDataGlobal.Default.test2DelayTime)

    /**
     * 检测第三次的等待时间
     */
    var Test3DelayTime by LongDataStoreProperty(LocalDataGlobal.Default.test3DelayTime)

    /**
     * 检测第四次的等待时间
     */
    var Test4DelayTime by LongDataStoreProperty(LocalDataGlobal.Default.test4DelayTime)

    /**
     * 循环测试
     */
    var LooperTest by BooleanDataStoreProperty(LocalDataGlobal.Default.looperTest)
    /**
     * 报告医院名
     */
    var HospitalName by StringDataStoreProperty(LocalDataGlobal.Default.hospitalName)

    /**
     * 是否自动打印小票
     */
    var AutoPrintReceipt by BooleanDataStoreProperty(LocalDataGlobal.Default.autoPrintReceipt)
    /**
     * 是否自动打印A4报告
     */
    var AutoPrintReport by BooleanDataStoreProperty(LocalDataGlobal.Default.autoPrintReport)

    /**
     * 生成报告文件的规则
     * true 条码
     * false 编号
     */
    var ReportFileNameBarcode by BooleanDataStoreProperty(LocalDataGlobal.Default.reportFileNameBarcode)


    /**
     * 编号自增并保存
     * @param num String
     * @return String
     */
    fun getDetectionNumInc(num: String = DetectionNum): String {
        if (num.isNullOrEmpty()) return "1"
        val newNum = java.lang.String.format("%0" + num.length + "d", num.toLong() + 1)
        DetectionNum = newNum
        return num
    }

}

