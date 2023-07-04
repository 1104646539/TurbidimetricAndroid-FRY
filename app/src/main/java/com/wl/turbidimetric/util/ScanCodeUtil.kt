package com.wl.turbidimetric.util

import com.wl.turbidimetric.ex.toBigLongString
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.Date

/**
 * 扫描条形码类
 * 劳易测 CR100
 */
class ScanCodeUtil private constructor(
    private val serialPort: BaseSerialPortUtil = BaseSerialPortUtil(
//        "/dev/ttyS3",
        "Com3",
        9600
    )
) {
    companion object {
        val Instance: ScanCodeUtil = ScanCodeUtil()
    }

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

    /**
     * 扫码返回的内容是
     * 0x02,内容..., 0x0D, 0x0A
     * 关闭扫码返回的内容是固定的
     * 0x02, 0x3F, 0x0D 0x0A
     */
    fun open() {
        serialPort.open()
        GlobalScope.launch {
            while (true) {
                delay(100)
                val size = serialPort.read(temp)
                if (size > 0) {
                    val temp = temp.copyOf(size).toList()
                    Timber.d("temp=$temp")
                    if (isScan && temp.size > 3) {
                        if (temp.first() == 2.toByte() && temp[temp.lastIndex - 1] == 13.toByte() && temp.last() == 10.toByte()) {
                            val result = temp.subList(1, temp.lastIndex - 1)
                            Timber.d("result=$result")
                            canScanJob?.cancelAndJoin()
                            withContext(Dispatchers.Main) {
                                if (isScan) {
                                    isScan = false
                                    onScanResult?.scanSuccess(result.toString())
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
        Timber.d("开始扫码:${Date().toBigLongString()}")
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
        Timber.d("结束扫码:${Date().toBigLongString()}")
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
