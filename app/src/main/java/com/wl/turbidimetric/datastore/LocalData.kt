package com.wl.turbidimetric.datastore

import com.wl.turbidimetric.ex.getData
import com.wl.turbidimetric.ex.putData

object LocalData {

    fun getTakeReagentR1(): Int {
        return LocalDataGlobal.Key.TakeReagentR1.getData(LocalDataGlobal.Default.TakeReagentR1)
    }

    fun setTakeReagentR1(value: Int) {
        LocalDataGlobal.Key.TakeReagentR1.putData(value)
    }

    fun getTakeReagentR2(): Int {
        return LocalDataGlobal.Key.TakeReagentR2.getData(LocalDataGlobal.Default.TakeReagentR2)
    }

    fun setTakeReagentR2(value: Int) {
        LocalDataGlobal.Key.TakeReagentR2.putData(value)
    }

    fun getSamplingVolume(): Int {
        return LocalDataGlobal.Key.SamplingVolume.getData(LocalDataGlobal.Default.SamplingVolume)
    }

    fun setSamplingVolume(value: Int) {
        LocalDataGlobal.Key.SamplingVolume.putData(value)
    }

    fun getFirstOpen(): Boolean {
        return LocalDataGlobal.Key.FirstOpen.getData(LocalDataGlobal.Default.FirstOpen)
    }

    fun setFirstOpen(value: Boolean) {
        LocalDataGlobal.Key.FirstOpen.putData(value)
    }


}

