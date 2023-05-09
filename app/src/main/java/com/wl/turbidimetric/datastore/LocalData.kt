package com.wl.turbidimetric.datastore

import com.wl.turbidimetric.ex.get
import com.wl.turbidimetric.ex.put

object LocalData {


    fun getTakeReagentR1(): Int {

        return LocalDataGlobal.Key.TakeReagentR1.get(LocalDataGlobal.Default.TakeReagentR1)
    }

    fun setTakeReagentR1(value: Int) {
        LocalDataGlobal.Key.TakeReagentR1.put(value)
    }

    fun getTakeReagentR2(): Int {
        return LocalDataGlobal.Key.TakeReagentR2.get(LocalDataGlobal.Default.TakeReagentR2)
    }

    fun setTakeReagentR2(value: Int) {
        LocalDataGlobal.Key.TakeReagentR2.put(value)
    }

    fun getSamplingVolume(): Int {
        return LocalDataGlobal.Key.SamplingVolume.get(LocalDataGlobal.Default.SamplingVolume)
    }

    fun setSamplingVolume(value: Int) {
        LocalDataGlobal.Key.SamplingVolume.put(value)
    }

    fun getFirstOpen(): Boolean {
        return LocalDataGlobal.Key.FirstOpen.get(LocalDataGlobal.Default.FirstOpen)
    }

    fun setFirstOpen(value: Boolean) {
        LocalDataGlobal.Key.FirstOpen.put(value)
    }

    fun getDetectionNum(): String {
        return LocalDataGlobal.Key.DetectionNum.get(LocalDataGlobal.Default.DetectionNum)
    }

    fun putDetectionNum(num: String) {
        LocalDataGlobal.Key.DetectionNum.put(num)
    }

    /**
     * 编号自增并保存
     * @param num String
     * @return String
     */
    fun getDetectionNumInc(num: String = getDetectionNum()): String {
        if (num.isNullOrEmpty()) return "1";
        val newNum = java.lang.String.format("%0" + num.length + "d", num.toLong() + 1)
        putDetectionNum(newNum)
        return num
    }

}

