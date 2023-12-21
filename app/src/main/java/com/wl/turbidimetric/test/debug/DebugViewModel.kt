package com.wl.turbidimetric.test.debug

import com.wl.turbidimetric.ex.toHex
import com.wl.turbidimetric.util.OriginalDataCall
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wllib.LogToFile
import com.wl.wwanandroid.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DebugViewModel : BaseViewModel(), OriginalDataCall {
    val originalDataMsg = MutableStateFlow("")
    fun listener() {
        SerialPortUtil.originalCallback.add(this)
        LogToFile.i("SerialPortUtil.callback listener")
    }

    fun clearListener() {
        SerialPortUtil.originalCallback.remove(this)
        LogToFile.i("SerialPortUtil.callback onCleared")
    }

    override fun readDataOriginalData(ready: UByteArray) {
        originalDataMsg.value = "接收："+ready.toHex()+"\n"
    }

    override fun sendOriginalData(ready: UByteArray) {
        originalDataMsg.value = "发送："+ready.toHex()+"\n"
    }
}
