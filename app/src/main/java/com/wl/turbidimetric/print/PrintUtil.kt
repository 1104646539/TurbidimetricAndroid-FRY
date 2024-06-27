package com.wl.turbidimetric.print

import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.util.FitterType
import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import com.wl.wllib.toTimeStr
import java.nio.charset.Charset
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 热敏打印
 * @property serialPort BaseSerialPortUtil
 * @constructor
 */
class PrintUtil(private val serialPort: BaseSerialPort) {
    companion object {
        private val GET_STATE = byteArrayOf(0x1C, 0x76)
        private val PAGER_OUT: Byte = 0x55
        private val PAGER_FULL: Byte = 0x04
    }

    var scope: CoroutineScope? = null

    private val byteArray = ByteArray(4)
    var onPrintListener: OnPrintListener? = null

    private fun readData() {
        scope?.launch(Dispatchers.IO) {
            while (true) {
                val ret = serialPort.read(byteArray, byteArray.size)
                if (ret == 1) {
                    parse(byteArray[0])
                }
            }
        }
    }

    fun open(scope: CoroutineScope) {
        this.scope = scope
        serialPort.openSerial(WQSerialGlobal.COM3, 9600, 8)
        readData()
    }

    fun close() {
        scope?.cancel()
        serialPort?.close()
    }

    private fun parse(byte: Byte) {
        if (byte == PAGER_FULL) {
            printByte?.let {
                send(it)
            }
        } else if (byte == PAGER_OUT) {
            scope?.launch(Dispatchers.Main) {
                onPrintListener?.onPrinterPagerOut()
            }
        }
    }


    fun test() {
        send(getPrintByte("test\n\n\n\n"))
    }

    private fun getPrintByte(msg: String): ByteArray {
        return msg.toByteArray(Charset.forName("GB2312"))
    }

    fun printTest(results: List<TestResultAndCurveModel?>, onPrintListener: OnPrintListener?) {
        results.forEach {
            it?.let {
                val msg = getTestResultMsg(it)
                sendAndCheckState(getPrintByte(msg), onPrintListener);
            }
        }
    }

    private fun getTestResultMsg(result: TestResultAndCurveModel): String {

        val sb = StringBuilder()

        sb.append("\n")
        sb.append("\n")
        sb.append("粪便隐血定量检测报告".fixMin(25))
        sb.append("\n")
        sb.append("\n")

        sb.append("编号:${result.result.detectionNum}\n")
        sb.append("条码:${result.result.sampleBarcode}\n")
        sb.append("姓名:${result.result.name}\n")
        sb.append("性别:${result.result.gender}\n")
        sb.append("年龄:${result.result.age}\n")
//        sb.append("吸光度:${result.absorbances?.setScale(5, RoundingMode.HALF_UP) ?: ""}\n")
        sb.append("浓度:${result.result.concentration} ${result.curve?.projectUnit ?: ""}\n")
        sb.append("检测结果:${result.result.testResult}\n")
        sb.append("检测时间:${result.result.testTime.toTimeStr()}\n")

        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")

        return sb.toString()
    }

    private fun send(byte: ByteArray) {
        serialPort.write(byte)
    }

    private var printByte: ByteArray? = null
    private fun sendAndCheckState(byte: ByteArray, onPrintListener: OnPrintListener?) {
        i("sendAndCheckState")
        this.printByte = byte
        this.onPrintListener = onPrintListener
        sendGetState()
    }

    /**
     * 获取打印机状态
     */
    private fun sendGetState() {
        send(GET_STATE)
    }

    fun printMatchingQuality(
        absorbancys: List<Int>,
        nds: DoubleArray,
        yzs: List<Int>,
        params: MutableList<Double>,
        createTime: String,
        projectName: String,
        reagentNo: String,
        matchingNum: Int,
        onPrintListener: OnPrintListener?
    ) {
        val msg = getMatchingQualityMsg(
            absorbancys.toMutableList(),
            nds,
            yzs.toMutableList(),
            params,
            createTime,
            projectName,
            reagentNo,
            matchingNum
        )
        sendAndCheckState(getPrintByte(msg), onPrintListener)
    }

    /**
     * 获取拟合和质控的打印文字
     * @param absorbancys MutableList<Double>
     * @param nds DoubleArray
     * @param yzs MutableList<Double>
     * @param params DoubleArray
     * @param quality Boolean
     * @return String
     */
    private fun getMatchingQualityMsg(
        absorbancys: MutableList<Int>,
        nds: DoubleArray,
        yzs: MutableList<Int>,
        params: MutableList<Double>,
        createTime: String,
        projectName: String,
        reagentNo: String,
        matchingNum: Int
    ): String {

        val sb = StringBuilder()
        sb.append("\n\n")
        sb.append("序号:$reagentNo\n")
        sb.append("项目名:$projectName\n")
        sb.append("质控时间:$createTime\n")
        repeat(5) {
            val nd = "${nds[it]}ng/mL".fix(15)
            val abs = "${absorbancys[it].toInt()}".fix(5)
            sb.append("${it + 1} $nd $abs")
            sb.append("\n")
            val yz = "(${yzs[it]}ng/mL)".fix(17)
            sb.append(" $yz ")
            sb.append("\n")
        }

        sb.append("\n\n")

        if (yzs.size > matchingNum && absorbancys.size > matchingNum) {
            sb.append("${yzs[matchingNum]}ng/mL")
            sb.append("\n")
            sb.append("${absorbancys[matchingNum]}")
            sb.append("\n\n")
            sb.append("${yzs[matchingNum + 1]}ng/mL")
            sb.append("\n")
            sb.append("${absorbancys[matchingNum + 1]}")
            sb.append("\n\n")
        }

        sb.append("F(0)=${params[0].scale(10)}")
        sb.append("\n")
        sb.append("F(1)=${params[1].scale(10)}")
        sb.append("\n")
        sb.append("F(2)=${params[2].scale(10)}")
        sb.append("\n")
        sb.append("F(3)=${params[3].scale(10)}")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")

        return sb.toString()
    }


    private fun String.fix(length: Int): String {
        return if (this.length > length) {
            this
        } else {
            val sb = StringBuilder()
            repeat(length - this.length) {
                sb.append(" ")
            }
            sb.append(this)
            sb.toString()
        }
    }

    private fun String.fixMin(length: Int): String {
        return if (this.length > length) {
            this
        } else {
            val sb = StringBuilder()
            repeat((length - this.length) / 2) {
                sb.append(" ")
            }
            sb.append(this)
            sb.toString()
        }
    }

    interface OnPrintListener {
        fun onPrinterPagerOut()
    }
}

