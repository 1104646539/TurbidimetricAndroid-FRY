package com.wl.turbidimetric.util

import com.wl.turbidimetric.global.SystemGlobal
import com.wl.weiqianwllib.gpio.GPIOUtil
import kotlinx.coroutines.CoroutineScope

class DoorHelper {
    private val gpioUtil = GPIOUtil()
    fun SampleDoorIsClose(): Boolean {
//        if (SystemGlobal.isCodeDebug) return true
        return gpioUtil.GetGpio(0) == 1
    }

    fun CuvetteDoorIsClose(): Boolean {
//        if (SystemGlobal.isCodeDebug) return true
        return gpioUtil.GetGpio(2) == 1
    }
}
