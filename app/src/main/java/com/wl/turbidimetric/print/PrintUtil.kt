package com.wl.turbidimetric.print

import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.print.PrintUtil.serialPort
import com.wl.turbidimetric.util.FitterType
import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import com.wl.wllib.toTimeStr
import java.nio.charset.Charset
import com.wl.wllib.LogToFile.i

/**
 * 热敏打印
 * @property serialPort BaseSerialPortUtil
 * @constructor
 */
object PrintUtil {
    private val serialPort: BaseSerialPort = BaseSerialPort()

    init {
        open()
    }

    private fun open() {
        serialPort.openSerial(WQSerialGlobal.COM2, 9600, 8)
    }


    fun test() {
        send("test\n\n\n\n")
    }

    fun printTest(results: List<TestResultAndCurveModel?>) {
        results.forEach {
            it?.let {
                val msg = getTestResultMsg(it)
                send(msg)
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

        return sb.toString()
    }

    fun send(msg: String) {
        i("$msg")
        serialPort.write(msg.toByteArray(Charset.forName("GB2312")))
    }

    fun printMatchingQuality(
        absorbancys: List<Int>,
        nds: DoubleArray,
        yzs: List<Int>,
        params: MutableList<Double>,
        createTime: String,
        projectName: String,
        reagentNo: String,
        matchingNum: Int
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
        send(msg)
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
}
