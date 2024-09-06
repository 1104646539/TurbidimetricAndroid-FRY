package com.wl.turbidimetric.repository.if2

interface LocalDataSource {
    fun getDetectionNum(): String
    fun getTakeReagentR1(): Int
    fun getTakeReagentR2(): Int
    fun getSamplingVolume(): Int
    fun getCurrentVersion(): Int
    fun getStirDuration(): Int
    fun getStirProbeCleaningDuration(): Int
    fun getSamplingProbeCleaningDuration(): Int
    fun getCurMachineTestModel(): String
    fun getSampleExist(): Boolean
    fun getScanCode(): Boolean
    fun getSelectProjectID(): Long
    fun getDebugMode(): Boolean
    fun getTest1DelayTime(): Long
    fun getTest2DelayTime(): Long
    fun getTest3DelayTime(): Long
    fun getTest4DelayTime(): Long
    fun getReactionTime(): Long
    fun getDetectionNumInc(num: String = getDetectionNum()): String
    fun getAutoPrintReceipt(): Boolean
    fun getHospitalName(): String
    fun getLooperTest(): Boolean
    fun getAutoPrintReport(): Boolean
    fun getReportFileNameBarcode(): Boolean
    fun getReportIntervalTime(): Int
    fun getTempUpLimit(): Int
    fun getTempLowLimit(): Int

    fun setLooperTest(looperTest: Boolean)
    fun setHospitalName(name: String)
    fun setAutoPrintReceipt(auto: Boolean)
    fun setDetectionNum(num: String)
    fun setTakeReagentR1(value: Int)
    fun setTakeReagentR2(value: Int)
    fun setSamplingVolume(value: Int)
    fun setCurrentVersion(value: Int)
    fun setStirDuration(value: Int)
    fun setStirProbeCleaningDuration(value: Int)
    fun setSamplingProbeCleaningDuration(value: Int)
    fun setCurMachineTestModel(value: String)
    fun setSampleExist(value: Boolean)
    fun setScanCode(value: Boolean)
    fun setSelectProjectID(value: Long)
    fun setDebugMode(value: Boolean)
    fun setTest1DelayTime(value: Long)
    fun setTest2DelayTime(value: Long)
    fun setTest3DelayTime(value: Long)
    fun setTest4DelayTime(value: Long)
    fun setReactionTime(value: Long)
    fun setDetectionNumInc(num: String)
    fun setAutoPrintReport(value: Boolean)
    fun setReportFileNameBarcode(value: Boolean)
    fun setReportIntervalTime(value: Int)
    fun setTempUpLimit(temp: Int)
    fun setTempLowLimit(temp: Int)

    fun resetGlobal()
}
