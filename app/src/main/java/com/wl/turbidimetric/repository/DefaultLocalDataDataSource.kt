package com.wl.turbidimetric.repository

import com.wl.turbidimetric.dao.GlobalDao
import com.wl.turbidimetric.model.GlobalConfig
import com.wl.turbidimetric.repository.if2.LocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultLocalDataDataSource(
    private val globalDao: GlobalDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalDataSource {
    companion object {
        val backupsId: Long = 1L
        val defaultId: Long = 2L
    }

    fun global(): GlobalConfig {
        return globalDao.getGlobalConfig(defaultId)
    }

    fun backupsGlobal(): GlobalConfig {
        return globalDao.getGlobalConfig(backupsId)
    }

    /**
     * 恢复默认参数
     */
    override fun resetGlobal() {
        update {
            backupsGlobal()
        }
    }

    override fun getDetectionNum(): String {
        return global().DetectionNum
    }

    override fun getTakeReagentR1(): Int {
        return global().TakeReagentR1
    }

    override fun getTakeReagentR2(): Int {
        return global().TakeReagentR2
    }

    override fun getSamplingVolume(): Int {
        return global().SamplingVolume
    }

    override fun getCurrentVersion(): Int {
        return global().CurrentVersion
    }

    override fun getStirDuration(): Int {
        return global().StirDuration
    }

    override fun getStirProbeCleaningDuration(): Int {
        return global().StirProbeCleaningDuration
    }

    override fun getSamplingProbeCleaningDuration(): Int {
        return global().SamplingProbeCleaningDuration
    }

    override fun getCurMachineTestModel(): String {
        return global().CurMachineTestModel
    }

    override fun getSampleExist(): Boolean {
        return global().SampleExist
    }

    override fun getScanCode(): Boolean {
        return global().ScanCode
    }

    override fun getSelectProjectID(): Long {
        return global().SelectProjectID
    }

    override fun getDebugMode(): Boolean {
        return global().DebugMode
    }

    override fun getTest1DelayTime(): Long {
        return global().Test1DelayTime
    }

    override fun getTest2DelayTime(): Long {
        return global().Test2DelayTime
    }

    override fun getTest3DelayTime(): Long {
        return global().Test3DelayTime
    }

    override fun getTest4DelayTime(): Long {
        return global().Test4DelayTime
    }

    override fun getReactionTime(): Long {
        return global().ReactionTime
    }

    /**
     * 编号自增并保存
     * @param num String
     * @return String
     */
    override fun getDetectionNumInc(num: String): String {
        if (num.isNullOrEmpty()) return "1"
        val newNum = java.lang.String.format("%0" + num.length + "d", num.toLong() + 1)
        setDetectionNum(newNum)
        return num
    }

    override fun getAutoPrintReceipt(): Boolean {
        return global().AutoPrintReceipt
    }

    override fun getHospitalName(): String {
        return global().HospitalName
    }

    override fun getLooperTest(): Boolean {
        return global().LooperTest
    }

    override fun getAutoPrintReport(): Boolean {
        return global().AutoPrintReport
    }

    override fun getReportFileNameBarcode(): Boolean {
        return global().ReportFileNameBarcode
    }

    override fun getReportIntervalTime(): Int {
        return global().ReportIntervalTime
    }

    override fun getTempUpLimit(): Int {
        return global().TempUpLimit
    }

    override fun getTempLowLimit(): Int {
        return global().TempLowLimit
    }

    override fun setLooperTest(looperTest: Boolean) {
        update {
            it.copy(LooperTest = looperTest)
        }
    }

    fun update(mod: (old: GlobalConfig) -> GlobalConfig) {
        val oldM = global()
        val newM = mod.invoke(oldM)
        globalDao.updateGlobalConfig(newM)
    }

    override fun setHospitalName(name: String) {
        update {
            it.copy(HospitalName = name)
        }
    }

    override fun setAutoPrintReceipt(auto: Boolean) {
        update {
            it.copy(AutoPrintReceipt = auto)
        }
    }

    override fun setDetectionNum(num: String) {
        update {
            it.copy(DetectionNum = num)
        }
    }

    override fun setTakeReagentR1(value: Int) {
        update {
            it.copy(TakeReagentR1 = value)
        }
    }

    override fun setTakeReagentR2(value: Int) {
        update {
            it.copy(TakeReagentR2 = value)
        }
    }

    override fun setSamplingVolume(value: Int) {
        update {
            it.copy(SamplingVolume = value)
        }
    }

    override fun setCurrentVersion(value: Int) {
        update {
            it.copy(CurrentVersion = value)
        }
    }

    override fun setStirDuration(value: Int) {
        update {
            it.copy(StirDuration = value)
        }
    }

    override fun setStirProbeCleaningDuration(value: Int) {
        update {
            it.copy(StirProbeCleaningDuration = value)
        }
    }

    override fun setSamplingProbeCleaningDuration(value: Int) {
        update {
            it.copy(SamplingProbeCleaningDuration = value)
        }
    }

    override fun setCurMachineTestModel(value: String) {
        update {
            it.copy(CurMachineTestModel = value)
        }
    }

    override fun setSampleExist(value: Boolean) {
        update {
            it.copy(SampleExist = value)
        }
    }

    override fun setScanCode(value: Boolean) {
        update {
            it.copy(ScanCode = value)
        }
    }

    override fun setSelectProjectID(value: Long) {
        update {
            it.copy(SelectProjectID = value)
        }
    }

    override fun setDebugMode(value: Boolean) {
        update {
            it.copy(DebugMode = value)
        }
    }

    override fun setTest1DelayTime(value: Long) {
        update {
            it.copy(Test1DelayTime = value)
        }
    }

    override fun setTest2DelayTime(value: Long) {
        update {
            it.copy(Test2DelayTime = value)
        }
    }

    override fun setTest3DelayTime(value: Long) {
        update {
            it.copy(Test3DelayTime = value)
        }
    }

    override fun setTest4DelayTime(value: Long) {
        update {
            it.copy(Test4DelayTime = value)
        }
    }

    override fun setReactionTime(value: Long) {
       update {
           it.copy(ReactionTime = value)
       }
    }

    override fun setDetectionNumInc(num: String) {
        update {
            it.copy(DetectionNum = num)
        }
    }

    override fun setAutoPrintReport(value: Boolean) {
        update {
            it.copy(AutoPrintReport = value)
        }
    }

    override fun setReportFileNameBarcode(value: Boolean) {
        update {
            it.copy(ReportFileNameBarcode = value)
        }
    }

    override fun setReportIntervalTime(value: Int) {
        update {
            it.copy(ReportIntervalTime = value)
        }
    }

    override fun setTempUpLimit(temp: Int) {
        update {
            it.copy(TempUpLimit = temp)
        }
    }

    override fun setTempLowLimit(temp: Int) {
        update {
            it.copy(TempLowLimit = temp)
        }
    }
}
