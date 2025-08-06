package com.wl.weiqianwllib.gpio

import weiqian.hardware.HardwareControl

class GPIOUtil {
    private var m_hardware: HardwareControl? = HardwareControl()
    fun open() {
//        m_hardware = HardwareControl()
    }

    fun GetType(pos: Int): Int {
        return HardwareControl.GpioGetType(pos)
    }

    fun SetGpio(pos: Int, state: Int): Int {
        return HardwareControl.GpioSetOutput(pos, state)
    }

    fun GetGpio(pos: Int): Int {
        return HardwareControl.GpioGetInput(pos)
    }
}
