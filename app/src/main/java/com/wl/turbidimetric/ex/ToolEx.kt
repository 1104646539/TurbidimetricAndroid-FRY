package com.wl.turbidimetric.ex

import android.app.Activity
import android.content.Intent
import android.widget.EditText
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.MachineState
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.util.CRC
import com.wl.turbidimetric.util.CurveFitter
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 浓度梯度
 */
val nds = doubleArrayOf(
    0.0,
    50.0,
    200.0,
    500.0,
    1000.0,
//    0.0, 1000.0, 500.0, 200.0, 50.0
)

/**
 * CRC16 验证
 * 输入需要CRC的数组，返回CRC结果
 */
fun CRC16(ba: UByteArray): UByteArray {
    return CRC.CRC16(ba)
}

/**
 * 验证CRC
 * 输入包含CRC的数组，返回CRC结果
 */
fun VerifyCrc(ba: UByteArray): Boolean {

    val data = ba.copyOfRange(0, ba.size - 2)
    val crc = ba.copyOfRange(ba.size - 2, ba.size)
    val re = CRC16(data)
    return crc.contentEquals(re)
}

/**
 * 返回ubyte的每一位的值
 * uByte 要取的值
 * pos 要取的位值，从右开始，最小为0
 * 返回值：0 or 1
 */
fun getStep(uByte: UByte, pos: Int): Int {
    if (pos > 7 || pos < 0) throw IndexOutOfBoundsException("错误 pos下标错误 pos=$pos")
    val temp: Int = 1 shl (pos)
    return if (uByte.toInt() and temp == temp) 1 else 0
}

/**
 * 合并整个数组的值，数组开头为高位，结尾为低位
 * uByteArray 要合并的数组
 */
fun merge(uByteArray: UByteArray): Int {
    var sum = 0
    for (i in uByteArray.indices) {
        sum += (uByteArray[i].toInt() shl 8 * (uByteArray.size - 1 - i))
    }
    return sum
}

/**
 * UByteArray
 * 输出为Hex字符串
 */
fun UByteArray.print(): String {
    val sb = StringBuffer()
    this.forEach {
        sb.append(String.format("%02X", it))
        sb.append(" ")
    }
    return sb.toString()
}

private val hexArray = "0123456789ABCDEF".toCharArray()
fun UByteArray.toHex(): String {
    if (this.isNullOrEmpty()) return ""
    val hexChars = CharArray(this.size * 3 - 1)
    for (j in this.indices) {
        val v = (this[j] and 0xFF.toUByte()).toInt()

        hexChars[j * 3] = hexArray[v ushr 4]
        hexChars[j * 3 + 1] = hexArray[v and 0x0F]
        if (j * 3 + 2 <= hexChars.size - 1) {
            hexChars[j * 3 + 2] = ','
        }
    }
    return String(hexChars)
}

inline fun <reified T : Activity> Activity.startActivity() {
    startActivity(Intent(this, T::class.java))
}

/**
 * 计算一排的吸光度差异
 */
fun calcAbsorbanceDifferences(
    resultTest1: ArrayList<BigDecimal>,
    resultTest2: ArrayList<BigDecimal>,
    resultTest3: ArrayList<BigDecimal>,
    resultTest4: ArrayList<BigDecimal>
): ArrayList<BigDecimal> {
    var absorbances = arrayListOf<BigDecimal>()
    for (i in resultTest1.indices) {
        absorbances.add(
            calcAbsorbanceDifference(resultTest1[i], resultTest4[i])
        )
    }
    return absorbances
}

/**
 * 计算单个吸光度差异
 * @param resultTest1 BigDecimal
 * @param resultTest4 BigDecimal
 * @return BigDecimal
 */
fun calcAbsorbanceDifference(resultTest1: BigDecimal, resultTest4: BigDecimal): BigDecimal {
    return resultTest4 - resultTest1
}

/**
 * 计算吸光度
 */
fun calcAbsorbance(
    resultTest: BigDecimal
): BigDecimal {
    return log10(
        65535.toBigDecimal().divide(resultTest, 5, RoundingMode.HALF_UP).toDouble()
    ).toBigDecimal()
        .setScale(5, RoundingMode.HALF_UP)
//    return log10(BigDecimal(65535).divide(BigDecimal(resultTest),5,BigDecimal.ROUND_HALF_UP))
}

/**
 * 计算4参数
 * @return CurveFitter
 */
fun matchingArg(absorbances: List<Double>): CurveFitter {
    val xs = absorbances.toDoubleArray().copyOfRange(0, nds.size)
    val curveFitter = CurveFitter(xs, nds)
    curveFitter.doFitCon()
    return curveFitter
}

///**
// * 根据四参数，吸光度，计算浓度
// * @param absorbance Double
// * @param project 四参数
// */
//fun calcCon(absorbance: BigDecimal, project: ProjectModel): BigDecimal {
//    val a1 = project.a1;
//    val a2 = project.a2;
//    val x0 = project.x0;
//    val p = project.p;
////    var con: Double = x0 * ((a2 - a1) / (a2 - absorbance) - 1).pow(1 / p)
////
////    return con.scale(2)
//
//    var dividend1 = BigDecimal(a2)
//        .subtract(absorbance).setScale(10, BigDecimal.ROUND_HALF_UP)
//    var dividend2 = p
//    if (dividend1.compareTo(BigDecimal(0)) == 0 || dividend2 == 0.0) {
//        println("newCalcBigD dividend1==0||dividend2==0 dividend1=$dividend1 dividend2=$dividend2")
//        return BigDecimal(0)
//    }
//    val temp21 = BigDecimal(a2).subtract(BigDecimal(a1)).divide(
//        dividend1, 10, BigDecimal.ROUND_HALF_UP
//    ).subtract(BigDecimal(1))
//
//    val temp22 = temp21.toDouble().pow((1 / dividend2)).scale(10).toBigDecimal()
//    val con = (x0.toBigDecimal() * temp22)
//    return con
//}
/**
 * 根据三次曲线方程参数，吸光度，计算浓度
 * @param absorbance Double
 * @param project 四参数
 */
fun calcCon(absorbance: BigDecimal, project: ProjectModel): BigDecimal {
    val f0 = project.f0;
    val f1 = project.f1;
    val f2 = project.f2;
    val f3 = project.f3;
//    var con: Double = x0 * ((a2 - a1) / (a2 - absorbance) - 1).pow(1 / p)

//    return con.scale(2)
    val con = CurveFitter.f(
        2,
        doubleArrayOf(f0, f1, f2, f3),
        absorbance.multiply(10000.toBigDecimal()).toDouble()
    )

    return con.toBigDecimal().setScale(0, RoundingMode.HALF_UP)
}

/**
 * 判断String是否是数字
 * @receiver String
 * @return Boolean
 */
fun String.isNum(): Boolean {
    return toDoubleOrNull()?.let {
        true
    } ?: false
}

/**
 * 把EditText的光标移动到最后
 * @receiver EditText
 */
fun EditText.selectionLast() {
    if (this != null && this.text != null && this.text.toString().isNotEmpty()) {
        setSelection(this.text.toString().length)
    }
}

//fun DoorAllOpen(): Boolean {
//    return (SystemGlobal.cuvetteDoorIsOpen.value == true) && (SystemGlobal.sampleDoorIsOpen.value == true)
//}

/**
 * 计算标准方差
 * @param numArray DoubleArray
 * @return Double
 */
fun calculateSD(numArray: DoubleArray): Double {
    var sum = numArray.sum()
    var standardDeviation = 0.0

    val mean = sum / numArray.size

    for (num in numArray) {
        standardDeviation += (num - mean).pow(2.0)
    }

    return sqrt(standardDeviation / (numArray.size - 1))
}

/**
 * 计算平均值
 * @param numArray DoubleArray
 * @return Double
 */
fun calculateMean(numArray: DoubleArray): Double {
    var sum = numArray.sum()
    val mean = sum / numArray.size
    return mean
}

/**
 * 判断是否是在运行中
 * @return Boolean
 */
fun isTestRunning(): Boolean {
    return TestState.None != SystemGlobal.testState && TestState.TestFinish != SystemGlobal.testState
}

/**
 * 是否是自动模式
 * @return Boolean
 */
fun isAuto(machineTestModel: MachineTestModel = MachineTestModel.valueOf(LocalData.CurMachineTestModel)): Boolean {
    return machineTestModel == MachineTestModel.Auto
}

/**
 * 仪器是否可正常运行
 * @see MachineState
 * @return Boolean
 */
fun machineStateNormal(): Boolean {
    return SystemGlobal.machineArgState == MachineState.Normal
}
