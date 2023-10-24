package com.wl.turbidimetric.util

import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import com.wl.wllib.toLongTimeStr
import kotlinx.coroutines.*
import java.util.*
import com.wl.wllib.LogToFile.i
/**
 * 扫描条形码类
 * 劳易测 CR100
 */
object ScanCodeUtil {
    private val serialPort: BaseSerialPort = BaseSerialPort()
    /**
     * 开始扫码的命令
     */
    private val startScan = byteArrayOf(0x02, 0x2B, 0x0D, 0x0A)

    /**
     * 停止扫码的命令
     */
    private val stopScan = byteArrayOf(0x02, 0x2D, 0x0D, 0x0A)

    /**
     * 是否正在扫码
     */
    private var isScan = false

    /**
     * 一次获取的扫码数据 缓冲区
     */
    private var temp = ByteArray(100)

    /**
     * 扫码结果回调
     */
    var onScanResult: OnScanResult? = null

    /**
     * 超时停止时间
     */
    private val timeout: Long = 3000

    /**
     * 停止扫码的任务
     */
    private var canScanJob: Job? = null
    init {
        open()
    }
    /**
     * 扫码返回的内容是
     * 0x02,内容..., 0x0D, 0x0A
     * 关闭扫码返回的内容是固定的
     * 0x02, 0x3F, 0x0D 0x0A
     */
    fun open() {
        serialPort.openSerial(WQSerialGlobal.COM3, 9600, 8)

        GlobalScope.launch {
            while (true) {
                delay(100)
                val size = serialPort.read(temp)
                if (size > 0) {
                    val temp = temp.copyOf(size).toList()
                    i("temp=$temp")
                    if (isScan && temp.size > 3) {
                        if (temp.first() == 2.toByte() && temp[temp.lastIndex - 1] == 13.toByte() && temp.last() == 10.toByte()) {
                            val result = temp.subList(1, temp.lastIndex - 1)
                            i("result=$result")
                            canScanJob?.cancelAndJoin()
                            withContext(Dispatchers.Main) {
                                if (isScan) {
                                    isScan = false
                                    onScanResult?.scanSuccess(String(result.toByteArray()))
//                                    onScanResult?.scanSuccess(result.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * 开始扫码
     */
    suspend fun startScan() {
        i("开始扫码:${Date().toLongTimeStr()}")
        serialPort.write(startScan)
        isScan = true
        withContext(Dispatchers.IO) {
            canScanJob = launch {
                delay(timeout)
                stopScan()
            }
        }
    }

    /**
     * 结束扫码
     */
    private suspend fun stopScan() {
        i("结束扫码:${Date().toLongTimeStr()}")
        serialPort.write(stopScan)
        withContext(Dispatchers.Main) {
            if (isScan) {
                isScan = false
                onScanResult?.scanFailed()
            }
        }
    }
}

interface OnScanResult {
    fun scanSuccess(str: String)
    fun scanFailed()
}
