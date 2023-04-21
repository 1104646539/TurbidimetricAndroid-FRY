package com.wl.turbidimetric.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.wl.turbidimetric.App
import com.wl.turbidimetric.ex.dataStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object LocalDataGlobal {
    var data: Preferences

    init {
        data = runBlocking {
            App.instance!!.dataStore.data.first()
        }
        runBlocking {
            App.instance!!.dataStore.data.collectLatest {
                data = it
            }
        }
    }

    enum class Key {
        //第一次打开软件
        FirstOpen("FirstOpen"),

        //取R1量 缓冲液
        TakeReagentR1("TakeReagentR1"),

        //取R2量 乳胶试剂
        TakeReagentR2("TakeReagentR2"),

        //取样本量
        SamplingVolume("SamplingVolume"),

        //取缓冲液量
        TakeBufferVolume("TakeBufferVolume"),

        //取标准品量
        TakeStandardVolume("TakeStandardVolume");

        var key: String = ""

        constructor(key: String) {
            this.key = key
        }

    }

    object Default {
        val TakeReagentR1 = 300
        val TakeReagentR2 = 60
        val SamplingVolume = 8
        val FirstOpen = true
    }


}
