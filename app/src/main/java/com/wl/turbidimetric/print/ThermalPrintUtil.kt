package com.wl.turbidimetric.print

import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.report.PrintHelper.PrintReport
import com.wl.turbidimetric.util.WorkQueue
import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import com.wl.wllib.toTimeStr
import java.nio.charset.Charset
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 热敏打印
 * @property serialPort BaseSerialPortUtil
 * @constructor
 */
class ThermalPrintUtil(private val serialPort: BaseSerialPort) {
    companion object {
        /**
         * 获取打印机状态的命令
         */
        private val GET_STATE = byteArrayOf(0x1C, 0x76)

        /**
         * 打印机状态 缺纸
         */
        private const val PAGER_OUT: Byte = 0x55

        /**
         * 打印机状态 有纸
         */
        private const val PAGER_FULL: Byte = 0x04

        /**
         * 发送打印信息后的间隔时间
         */
        private const val INTERVAL_TIME = 300;

        /**
         * 发送获取打印机状态的超时时长
         */
        private const val OVERTIME = 1500L;
    }

    private lateinit var queue: WorkQueue<TestResultAndCurveModel>

    private var scope: CoroutineScope? = null

    private val byteArray = ByteArray(4)
    private var onPrintListener: OnPrintListener? = null

    private fun readData() {
        scope?.launch(Dispatchers.IO) {
            while (true) {
                val ret = serialPort.read(byteArray, byteArray.size)
                if (ret == 1) {
                    overtimeTask?.cancel()
                    parse(byteArray[0])
                    queue.finishedWork()
                }
            }
        }
    }


    fun open(scope: CoroutineScope) {
        this.scope = scope
        serialPort.openSerial(WQSerialGlobal.COM2, 9600, 8)
        queue = WorkQueue((INTERVAL_TIME).toLong(), scope)
        readData()
        printTask()
    }

    private var overtimeTask: Job? = null
    private fun printTask() {
        queue.onWorkStart = { t ->
            queue.working()
            sendAndCheckState(getPrintByte(getTestResultMsg(t)), onPrintListener)
            overtimeTask = scope?.launch(Dispatchers.IO) {
                //获取检测状态超时,报错并取消所有打印任务
                delay(OVERTIME)
                i("打印机超时")
                scope?.launch(Dispatchers.Main) {
                    onPrintListener?.onPrinterOvertime()
                }
                queue.clear()
                queue.finishedWork()
            }
        }
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
        this.onPrintListener = onPrintListener
        results.forEach {
            it?.let {
                queue.addWork(it)
            }
        }
    }

    private fun getTestResultMsg(result: TestResultAndCurveModel): String {

        val sb = StringBuilder()

        sb.append("\n")
        sb.append("\n")
        sb.append("粪便隐血定量检测报告".fixMinAfter(25))
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
        i("sendAndCheckState\n${String(byte, Charset.forName("GB2312"))}")
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
        repeat(matchingNum) {
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

    private fun String.fixMin(length: Int, after: Boolean): String {
        return if (this.length > length) {
            this
        } else {
            val sb = StringBuilder()
            if (after) {
                sb.append(this)
            }
            repeat(length - this.length) {
                sb.append(" ")
            }
            if (!after) {
                sb.append(this)
            }
            sb.toString()
        }
    }

    private fun String.fixMinBefore(length: Int): String {
        return this.fixMin(length, false)
    }

    private fun String.fixMinAfter(length: Int): String {
        return this.fixMin(length, true)
    }

    /**
     * 打印质控结果
     */
    fun printQualityResult(
        createTime: String,
        projectName: String,
        reagentNo: String,
        params: MutableList<Double>,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
        qualityLowCon: Int,
        qualityHighCon: Int,
        qualityLowAbs: Double,
        qualityHighAbs: Double,
        result: String
    ) {
        val msg = getQualityResultMsg(
            createTime,
            projectName,
            reagentNo,
            params,
            qualityLow1,
            qualityLow2,
            qualityHigh1,
            qualityHigh2,
            qualityLowCon,
            qualityHighCon,
            qualityLowAbs,
            qualityHighAbs,
            result
        )
        sendAndCheckState(getPrintByte(msg), onPrintListener)
    }

    private fun getQualityResultMsg(
        createTime: String,
        projectName: String,
        reagentNo: String,
        params: MutableList<Double>,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
        qualityLowCon: Int,
        qualityHighCon: Int,
        qualityLowAbs: Double,
        qualityHighAbs: Double,
        result:String,
    ): String {
        val sb = StringBuilder()
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("序号:$reagentNo\n")
        sb.append("项目名:$projectName\n")
        sb.append("质控时间:$createTime\n")

        sb.append(
            "$qualityLow1-$qualityLow2".fixMinAfter(10) + "$qualityLowCon".fixMinAfter(8) + "$qualityLowAbs".fixMinAfter(
                8
            ) + "\n"
        )
        sb.append(
            "$qualityHigh1-$qualityHigh2".fixMinAfter(10) + "$qualityHighCon".fixMinAfter(8) + "$qualityHighAbs".fixMinAfter(
                8
            ) + "\n"
        )

        sb.append("F(0)=${params[0].scale(10)}")
        sb.append("\n")
        sb.append("F(1)=${params[1].scale(10)}")
        sb.append("\n")
        sb.append("F(2)=${params[2].scale(10)}")
        sb.append("\n")
        sb.append("F(3)=${params[3].scale(10)}")
        sb.append("\n")
        sb.append("结论:$result")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")

        return sb.toString()
    }


    interface OnPrintListener {
        /**
         * 打印机缺纸
         */
        fun onPrinterPagerOut()

        /**
         * 打印机超时未响应
         */
        fun onPrinterOvertime()
    }
}

