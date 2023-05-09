package com.wl.turbidimetric.datastore

import com.tencent.mmkv.MMKV

object LocalDataGlobal {
    val datas: MMKV by lazy {
        MMKV.defaultMMKV()
    }


    init {

    }

    enum class Key {
        /**
         * 编号
         */
        DetectionNum("DetectionNum"),

        /**
         * 第一次打开软件
         */
        FirstOpen("FirstOpen"),

        /**
         * 取R1量 缓冲液
         */
        TakeReagentR1("TakeReagentR1"),

        /**
         * 取R2量 乳胶试剂
         */
        TakeReagentR2("TakeReagentR2"),

        /**
         * 取样本量
         */
        SamplingVolume("SamplingVolume"),

        /**
         * 取缓冲液量
         */
        TakeBufferVolume("TakeBufferVolume"),

        /**
         * 取标准品量
         */
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
        val DetectionNum = "1"
    }


}
