package com.wl.turbidimetric.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wl.turbidimetric.App
import com.wl.turbidimetric.ex.dataStore
import com.wl.turbidimetric.model.MachineTestModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object LocalDataGlobal {

    val datas by lazy {
        App.instance!!.dataStore
    }
    var cache: Preferences? = null


    init {
        GlobalScope.launch {
            cache = datas.data.first()
            datas.data.collectLatest {
                cache = it
            }
        }
    }

    object Default {
        val TakeReagentR1 = 300
        val TakeReagentR2 = 60
        val SamplingVolume = 25
        val FirstOpen = true
        val DetectionNum = "1"
        val StirDuration = 1000
        val StirProbeCleaningDuration = 1000
        val SamplingProbeCleaningDuration = 2000
        val MachineTestModelDefault = MachineTestModel.Auto
        val SampleExist = true
        val ScanCode = false
        val SelectProjectID = 0L
        val DebugMode = false
        val test1DelayTime = 30 * 1000L
        val test2DelayTime = 220 * 1000L
        val test3DelayTime = 0 * 1000L
        val test4DelayTime = 0 * 1000L
        val testIntervalTime = 10 * 1000L
    }


}
