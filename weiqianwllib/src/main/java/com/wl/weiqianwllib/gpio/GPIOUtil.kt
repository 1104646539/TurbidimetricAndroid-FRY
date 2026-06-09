package com.wl.weiqianwllib.gpio

import weiqian.hardware.HardwareControl

class GPIOUtil {
    private var m_hardware: HardwareControl? = null

    fun open() {
        try {
            m_hardware = HardwareControl()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun GetType(pos: Int): Int {
        return try {
            HardwareControl.GpioGetType(pos)
        } catch (e: Throwable) {
            0
        }
    }

    fun SetGpio(pos: Int, state: Int): Int {
        return try {
            HardwareControl.GpioSetOutput(pos, state)
        } catch (e: Throwable) {
            0
        }
    }

    fun GetGpio(pos: Int): Int {
        return try {
            HardwareControl.GpioGetInput(pos)
        } catch (e: Throwable) {
            0
        }
    }
}
