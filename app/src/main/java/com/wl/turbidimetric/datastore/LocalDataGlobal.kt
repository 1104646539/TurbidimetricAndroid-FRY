package com.wl.turbidimetric.datastore

import androidx.datastore.preferences.core.Preferences
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.ex.dataStore
import com.wl.turbidimetric.model.MachineTestModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
        val SamplingVolume = 25.0
        val CurrentVersion = 0
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
        val Test1DelayTime = 30 * 1000L
        val Test2DelayTime = 220 * 1000L
        val Test3DelayTime = 0 * 1000L
        val Test4DelayTime = 0 * 1000L
        val ReactionTime = Test2DelayTime - Test1DelayTime
        val LooperTest = false
        val HospitalName = ""
        val DetectionDoctor = ""
        val AutoPrintReceipt = false
        val AutoPrintReport = false
        val ReportFileNameBarcode = false
        val ReportIntervalTime = 35
        val TempUpLimit = 400
        val TempLowLimit = 360
    }


}
