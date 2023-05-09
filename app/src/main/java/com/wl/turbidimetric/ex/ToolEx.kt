package com.wl.turbidimetric.ex

import android.app.Activity
import android.content.Intent
import android.widget.EditText
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.CRC
import com.wl.turbidimetric.util.CurveFitter
import java.math.BigDecimal
import kotlin.math.log10
import kotlin.math.pow

/**
 * 浓度梯度
 */
val nds = doubleArrayOf(
//    0.0,
//    50.0,
//    200.0,
//    500.0,
//    1000.0,
    0.0, 1000.0, 500.0, 200.0, 50.0
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
 * 计算吸光度
 */
fun calcAbsorbances(
    resultTest1: ArrayList<Double>,
    resultTest2: ArrayList<Double>,
    resultTest3: ArrayList<Double>,
    resultTest4: ArrayList<Double>
): ArrayList<Double> {
    var absorbances = arrayListOf<Double>()
    for (i in resultTest1.indices) {
        absorbances.add(
            resultTest4[i] - resultTest1[i]
        )
    }
    return absorbances
}

/**
 * 计算吸光度
 */
fun calcAbsorbance(
    resultTest: Double
): Double {
    return log10((65535.toDouble() / resultTest))
}

/**
 * 计算4参数
 * @return CurveFitter
 */
fun matchingArg(absorbances: ArrayList<Double>): CurveFitter {
    val curveFitter = CurveFitter(nds, absorbances.toDoubleArray())
    curveFitter.doFitCon()
    return curveFitter;
//    val res = curveFitter.params

//    for (i in res.indices) {
//        println(res[i])
//    }
//    println("------")
//    println(curveFitter.fitGoodness)
//    println(curveFitter.resultString)
}

/**
 * 根据四参数，吸光度，计算浓度
 * @param absorbance Double
 * @param project 四参数
 */
fun calcCon(absorbance: Double, project: ProjectModel): Double {
    val a1 = project.a1;
    val a2 = project.a2;
    val x0 = project.x0;
    val p = project.p;
    var con: Double = x0 * ((a2 - a1) / (a2 - absorbance) - 1).pow(1 / p)

    return con.scale(2)
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
