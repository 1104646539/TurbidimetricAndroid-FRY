package com.wl.turbidimetric.print

import com.wl.turbidimetric.ex.longToStr
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.util.BaseSerialPortUtil
import com.wl.turbidimetric.util.CurveFitter
import timber.log.Timber
import java.math.RoundingMode
import java.nio.charset.Charset
import kotlin.math.abs

/**
 * 热敏打印
 * @property serialPort BaseSerialPortUtil
 * @constructor
 */
object PrintUtil {
    private val serialPort: BaseSerialPortUtil = BaseSerialPortUtil("COM2", 9600)

    init {
        serialPort.open()
    }


    fun test() {
        send("test\n\n\n\n")
    }

    fun printTest(results: List<TestResultModel?>) {
        results.forEach {
            it?.let {
                val msg = getTestResultMsg(it)
                send(msg)
            }
        }
    }

    private fun getTestResultMsg(result: TestResultModel): String {

        val sb = StringBuilder()

        sb.append("\n")
        sb.append("\n")
        sb.append("粪便隐血定量检测报告".fix(17))
        sb.append("\n")

        sb.append("编号:${result.detectionNum ?: ""}\n")
        sb.append("条码:${result.sampleQRCode ?: ""}\n")
        sb.append("姓名:${result.name ?: ""}\n")
        sb.append("性别:${result.gender ?: ""}\n")
        sb.append("年龄:${result.age ?: ""}\n")
//        sb.append("吸光度:${result.absorbances?.setScale(5, RoundingMode.HALF_UP) ?: ""}\n")
        sb.append("血红蛋白浓度值:${result.concentration ?: ""} ${result.project.target?.projectUnit ?: ""}\n")
        sb.append("检测结论:${result.testResult ?: ""}\n")
        sb.append("检测日期:${result.testTime.longToStr() ?: ""}\n")

        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")
        sb.append("\n")

        return sb.toString()
    }

    fun send(msg: String) {
        Timber.d("$msg")
        serialPort.write(msg.toByteArray(Charset.forName("GB2312")))
    }

    fun printMatchingQuality(
        absorbancys: List<Double>,
        nds: DoubleArray,
        yzs: List<Double>,
        params: MutableList<Double>,
        quality: Boolean
    ) {
        val msg = getMatchingQualityMsg(
            absorbancys.toMutableList(),
            nds,
            yzs.toMutableList(),
            params,
            quality
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
        absorbancys: MutableList<Double>,
        nds: DoubleArray,
        yzs: MutableList<Double>,
        params: MutableList<Double>,
        quality: Boolean
    ): String {

//        absorbancys[1] = absorbancys[4].apply {
//            absorbancys[4] = absorbancys[1]
//        }
//        absorbancys[2] = absorbancys[3].apply {
//            absorbancys[3] = absorbancys[2]
//        }
//        yzs[1] = yzs[4].apply {
//            yzs[4] = yzs[1]
//        }
//        yzs[2] = yzs[3].apply {
//            yzs[3] = yzs[2]
//        }
//
//        nds.sort()

        val sb = StringBuilder()
        sb.append("\n\n")
        sb.append("\n\n")
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

        if (quality) {
            sb.append("${yzs[5]}ng/mL")
            sb.append("\n")
            sb.append("${absorbancys[5]}")
            sb.append("\n\n")
            sb.append("${yzs[6]}ng/mL")
            sb.append("\n")
            sb.append("${absorbancys[6]}")
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
