package com.wl.turbidimetric.util

import com.wl.weiqianwllib.gpio.GPIOUtil
import kotlinx.coroutines.CoroutineScope

class DoorHelper {
    private val gpioUtil = GPIOUtil()
    fun SampleDoorIsClose(): Boolean {
        return gpioUtil.GetGpio(0) == 1
    }

    fun CuvetteDoorIsClose(): Boolean {
        return gpioUtil.GetGpio(2) == 1
    }
}
