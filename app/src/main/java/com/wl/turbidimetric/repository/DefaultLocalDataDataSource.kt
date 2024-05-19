package com.wl.turbidimetric.repository

import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.repository.if2.LocalDataSource

class DefaultLocalDataDataSource:LocalDataSource{
    override fun getDetectionNum(): String {
        return LocalData.DetectionNum
    }

    override fun getTakeReagentR1(): Int {
        return LocalData.TakeReagentR1
    }

    override fun getTakeReagentR2(): Int {
        return LocalData.TakeReagentR2
    }

    override fun getSamplingVolume(): Int {
        return LocalData.SamplingVolume
    }

    override fun getCurrentVersion(): Int {
        return LocalData.CurrentVersion
    }

    override fun getStirDuration(): Int {
        return LocalData.StirDuration
    }

    override fun getStirProbeCleaningDuration(): Int {
        return LocalData.StirProbeCleaningDuration
    }

    override fun getSamplingProbeCleaningDuration(): Int {
        return LocalData.SamplingProbeCleaningDuration
    }

    override fun getCurMachineTestModel(): String {
        return LocalData.CurMachineTestModel
    }

    override fun getSampleExist(): Boolean {
        return LocalData.SampleExist
    }

    override fun getScanCode(): Boolean {
        return LocalData.ScanCode
    }

    override fun getSelectProjectID(): Long {
        return LocalData.SelectProjectID
    }

    override fun getDebugMode(): Boolean {
        return LocalData.DebugMode
    }

    override fun getTest1DelayTime(): Long {
        return LocalData.Test1DelayTime
    }

    override fun getTest2DelayTime(): Long {
        return LocalData.Test2DelayTime
    }

    override fun getTest3DelayTime(): Long {
        return LocalData.Test3DelayTime
    }

    override fun getTest4DelayTime(): Long {
        return LocalData.Test4DelayTime
    }

    override fun getDetectionNumInc(num: String): String {
        return LocalData.getDetectionNumInc(getDetectionNum())
    }

    override fun getAutoPrintReceipt(): Boolean {
        return LocalData.AutoPrintReceipt
    }

    override fun getHospitalName(): String {
        return LocalData.HospitalName
    }

    override fun getLooperTest(): Boolean {
        return LocalData.LooperTest
    }

    override fun setLooperTest(looperTest: Boolean) {
        LocalData.LooperTest = looperTest
    }

    override fun setHospitalName(name: String) {
        LocalData.HospitalName = name
    }

    override fun setAutoPrintReceipt(auto: Boolean) {
        LocalData.AutoPrintReceipt = auto
    }

    override fun setDetectionNum(num: String) {
        LocalData.DetectionNum = num
    }

    override fun setTakeReagentR1(value: Int) {
        LocalData.TakeReagentR1 = value
    }

    override fun setTakeReagentR2(value: Int) {
        LocalData.TakeReagentR2 = value
    }

    override fun setSamplingVolume(value: Int) {
        LocalData.SamplingVolume = value
    }

    override fun setCurrentVersion(value: Int) {
        LocalData.CurrentVersion = value
    }

    override fun setStirDuration(value: Int) {
        LocalData.StirDuration = value
    }

    override fun setStirProbeCleaningDuration(value: Int) {
        LocalData.StirProbeCleaningDuration = value
    }

    override fun setSamplingProbeCleaningDuration(value: Int) {
        LocalData.SamplingProbeCleaningDuration = value
    }

    override fun setCurMachineTestModel(value: String) {
        LocalData.CurMachineTestModel = value
    }

    override fun setSampleExist(value: Boolean) {
        LocalData.SampleExist = value
    }

    override fun setScanCode(value: Boolean) {
        LocalData.ScanCode = value
    }

    override fun setSelectProjectID(value: Long) {
        LocalData.SelectProjectID = value
    }

    override fun setDebugMode(value: Boolean) {
        LocalData.DebugMode = value
    }

    override fun setTest1DelayTime(value: Long) {
        LocalData.Test1DelayTime = value
    }

    override fun setTest2DelayTime(value: Long) {
        LocalData.Test2DelayTime = value
    }

    override fun setTest3DelayTime(value: Long) {
        LocalData.Test3DelayTime = value
    }

    override fun setTest4DelayTime(value: Long) {
        LocalData.Test4DelayTime = value
    }

    override fun setDetectionNumInc(num: String) {
        LocalData.DetectionNum = num
    }

}
