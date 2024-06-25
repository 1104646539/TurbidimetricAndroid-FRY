package com.wl.turbidimetric.ex

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.view.View
import android.widget.EditText
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.data.Entry
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.StorageState
import com.wl.turbidimetric.main.MainActivity
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.SampleType
import com.wl.turbidimetric.util.Fitter
import com.wl.turbidimetric.util.FitterFactory
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.util.FourFun
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 浓度梯度
 */
//val nds = doubleArrayOf(
//    0.0,
//    50.0,
//    200.0,
//    500.0,
//    1000.0,
////    0.0, 1000.0, 500.0, 200.0, 50.0
//)

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
            calcAbsorbanceDifference(resultTest1[i], resultTest2[i])
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
 * 根据浓度值和临界值计算阴阳性
 * @param con Int
 * @param ljz Int
 * @return String
 */
fun calcShowTestResult(con: Int, ljz: Int): String {
    return if (con >= ljz) {
        getResource().getString(R.string.result_positive)
    } else {
        getResource().getString(R.string.result_negative)
    }
}

/**
 * 计算吸光度
 */
fun calcAbsorbance(
    resultTest: BigDecimal
): BigDecimal {
    if (resultTest.toInt() <= 0) {
        return 0.toBigDecimal()
    }
    return log10(
        65535.toBigDecimal().divide(resultTest, 5, RoundingMode.HALF_UP).toDouble()
    ).toBigDecimal().multiply(10000.toBigDecimal()).setScale(0, RoundingMode.HALF_UP)
}

/**
 * 拟合
 */
fun matchingArg(fitterType: FitterType, absorbances: List<Double>, targets: DoubleArray): Fitter {
    val fitter = FitterFactory.create(fitterType)
    fitter.calcParams(absorbances.toDoubleArray(), targets)
    return fitter
}

/**
 * 根据曲线方程参数，反应度度，计算浓度
 * @param absorbance Double
 * @param project 四参数
 */
fun calcCon(absorbance: BigDecimal, project: CurveModel): Int {
    val fitter = FitterFactory.create(FitterType.toValue(project.fitterType))
    var con = fitter.ratCalcCon(
        doubleArrayOf(project.f0, project.f1, project.f2, project.f3), absorbance.toDouble()
    )

    //浓度不能小于0
    if (con.compareTo(0.0) <= 0) {
        con = 0.0
    }
    i("type=${FitterType.toValue(project.fitterType)} absorbance=${absorbance.toDouble()}")
    return con.toInt()
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
 * 是否是自动模式
 * @return Boolean
 */
fun isAuto(machineTestModel: MachineTestModel): Boolean {
    return machineTestModel == MachineTestModel.Auto
}

/**
 * 是否是手动加样模式
 * @return Boolean
 */
fun isManualSampling(machineTestModel: MachineTestModel): Boolean {
    return machineTestModel == MachineTestModel.ManualSampling
}

/**
 * 是否是手动模式
 * @return Boolean
 */
fun isManual(machineTestModel: MachineTestModel): Boolean {
    return machineTestModel == MachineTestModel.Manual
}


/**
 * 获取打开桌面的intent
 * @return Intent
 */
fun getLauncher(): Intent {
    val showIntent = Intent()
    val cn = ComponentName("com.android.launcher3", "com.android.launcher3.Launcher")
    showIntent.component = cn
    return showIntent
}

/**
 * 获取上位机版本
 *
 * @return
 */
fun getPackageInfo(context: Context): PackageInfo? {
    var packageInfo: PackageInfo? = null
    try {
        packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return packageInfo
}

inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}

/**
 * 是样本
 * @receiver SampleType
 * @return Boolean
 */
fun SampleType.isSample(): Boolean {
    return this == SampleType.SAMPLE
}

/**
 * 不存在
 * @receiver SampleType
 * @return Boolean
 */
fun SampleType.isNonexistent(): Boolean {
    return this == SampleType.NONEXISTENT
}

/**
 * 是比色杯
 * @receiver SampleType
 * @return Boolean
 */
fun SampleType.isCuvette(): Boolean {
    return this == SampleType.CUVETTE
}

/**
 * 保存字符串到文件
 * @receiver File
 * @param str String
 * @param cover Boolean
 * @return Boolean
 */
fun File.saveFile(str: String, cover: Boolean = true): Boolean {
    if (isDirectory) {
        return false
    }
    if (!exists()) {
        if (!createNewFile()) {
            return false
        }
    } else if (!cover) {//文件已存在，但不覆盖（cover=false）直接返回false
        return false
    }

    FileOutputStream(this).apply {
        write(str.toByteArray())
        flush()
        close()
    }
    return true
}

/**
 * 到文件读取字符串
 * @receiver File
 * @param str String
 * @param cover Boolean
 * @return Boolean
 */
fun File.getContent(): String? {
    if (isDirectory) {
        return null
    }
    if (!exists()) {
        return null
    } else {//文件存在

    }
    return readText()
}

/**
 * 从项目参数新建曲线
 */
fun CurveModel.copyForProject(project: ProjectModel): CurveModel {
    return this.apply {
        projectName = project.projectName
        projectCode = project.projectCode
        projectLjz = project.projectLjz
        projectUnit = project.projectUnit
    }
}

fun <T> getIndexOrNullDefault(
    item: List<T>, index: Int, defaultText: String
): String {
    return if (item.size > index) {
        item[index].toString()
    } else {
        defaultText
    }
}

/**
 * 根据参数和拟合类型，返回公式
 */
fun getEquation(
    fitterType: FitterType, params: MutableList<Double>
): String {
    return when (fitterType) {
        FitterType.Three -> {
            "Y=${(params.getOrNull(0) ?: 0.0).scale(8)}+${(params.getOrNull(1) ?: 0.0).scale(8)}x+${
                (params.getOrNull(2) ?: 0.0).scale(
                    8
                )
            }x²+${(params.getOrNull(3) ?: 0.0).scale(8)}x³"
        }

        FitterType.Linear -> {
            "y=${(params.getOrNull(1) ?: 0.0).scale(8)}+${(params.getOrNull(0) ?: 0.0).scale(8)}x"
        }

        FitterType.Four -> {
            "y=(((${(params.getOrNull(3) ?: 0.0).scale(8)}-${(params.getOrNull(0) ?: 0.0).scale(8)})/(${
                (params.getOrNull(
                    3
                ) ?: 0.0).scale(8)
            }-x)-1)^(1/${(params.getOrNull(1) ?: 0.0).scale(8)}))*${
                (params.getOrNull(2) ?: 0.0).scale(
                    8
                )
            }"
        }
    }
}

/**
 * 根据参数和拟合类型，返回拟合度
 */
fun getFitGoodness(fitterType: FitterType, fitGoodness: Double): String {
    return when (fitterType) {
        FitterType.Three -> {
            "R²=${fitGoodness.toBigDecimal().setScale(6, RoundingMode.DOWN)}"
        }

        FitterType.Linear -> {
            "R²=${fitGoodness.toBigDecimal().setScale(6, RoundingMode.DOWN)}"
        }

        FitterType.Four -> {
            "R²=${fitGoodness.toBigDecimal().setScale(6, RoundingMode.DOWN)}"
        }
    }
}

///**
// * 获取不同拟合类型的图表数据
// */
//fun getChartEntry(curve: CurveModel): List<Entry> {
//    val values = mutableListOf<Entry>()
//    curve.reactionValues.forEachIndexed { i, it ->
//        if (i < curve.gradsNum) {
//            val reaction = it.toFloat()
//            val target = curve.targets[i].toFloat()
//            //三种拟合方式的x,y轴不一样
//            val x = if (FitterType.toValue(curve.fitterType) == FitterType.Three) {
//                reaction
//            } else {
//                target
//            }
//            val y = if (FitterType.toValue(curve.fitterType) == FitterType.Three) {
//                target
//            } else {
//                reaction
//            }
//            values.add(Entry(x, y))
//        }
//    }
//    return values
//}
/**
 * 获取不同拟合类型的图表数据
 * 根据曲线参数动态计算每个点【50个】计算的结果
 * 反应值为y轴 浓度为x轴
 * 三次多项式：
 * 四参数：直接通过浓度计算反应值
 * 线性：直接通过浓度计算反应值
 */
fun getChartEntry(curve: CurveModel): List<Entry> {
    val values = mutableListOf<Entry>()
    val num = 50
    var y = 0
    var x = 0
    var needContinue = true
    repeat(num) {
        if (!needContinue) return@repeat
        if (FitterType.toValue(curve.fitterType) == FitterType.Three) {
            y += 2000 / num
            x = calcCon(y.toBigDecimal(), curve)
            if (x > 1000) {
                needContinue = false
            }
        } else {
            x += 1000 / num
            val fitter = FitterFactory.create(FitterType.toValue(curve.fitterType))
            y = fitter.conCalcRat(
                doubleArrayOf(curve.f0, curve.f1, curve.f2, curve.f3),
                x.toDouble()
            ).toInt()
        }
        if (x >= 0 && y >= 0) {
            values.add(Entry(x.toFloat(), y.toFloat()))
        }
    }
    return values
}

/**
 * 获取全局唯一的ViewModel示例
 */
fun <T : ViewModel> getAppViewModel(classVm: Class<T>): T {
    return App.AppViewModelStoreOwner.getAppViewModel(classVm)
}

/**
 * @param during 防抖时间间隔
 * @param combine 一个接口中的多个回调方法是否共用防抖时间
 */
fun <T> T.throttle(during: Long = 2000L, combine: Boolean = true): T {
    return Proxy.newProxyInstance(this!!::class.java.classLoader, this!!::class.java.interfaces,
        object : InvocationHandler {

            private val map = HashMap<Method?, Long>()

            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                val current = System.currentTimeMillis()
                return if (current - (map[if (combine) null else method] ?: 0) > during) {
                    map[if (combine) null else method] = current
                    method.invoke(this@throttle, *args.orEmpty())
                } else {
                    resolveDefaultReturnValue(method)
                }

            }

        }
    ) as T
}

private fun resolveDefaultReturnValue(method: Method): Any? {
    return when (method.returnType.name.toLowerCase(Locale.US)) {
        Void::class.java.simpleName.toLowerCase(Locale.US) -> null
        else -> throw IllegalArgumentException("无法正确对返回值不为空的回调进行节流")
    }
}

//fun Activity.transitionTo(i: Intent?) {
//    val pairs: Array<Pair<View, String>> =
//        arrayOf()
//    val transitionActivityOptions =
//        ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)
//    this.startActivity(i, transitionActivityOptions.toBundle())
//}

/**
 * 如果一个Double不是有效的值，例如Nan或无穷，则返回默认的值
 */
fun Double.isNotValid(defaultValue: Double = 0.0): Double {
    return this.let {
        if (it.isNaN() || it.isInfinite()) {
            defaultValue
        } else {
            it
        }
    }
}

/**
 * 两种u盘格式互相转换，其实对一对一的
 * @receiver com.wl.weiqianwllib.upan.StorageState
 * @return com.wl.turbidimetric.app.StorageState
 */
fun com.wl.weiqianwllib.upan.StorageState.toState(): com.wl.turbidimetric.app.StorageState {
    return when (this) {
        com.wl.weiqianwllib.upan.StorageState.NONE -> com.wl.turbidimetric.app.StorageState.None
        com.wl.weiqianwllib.upan.StorageState.INSERTED -> com.wl.turbidimetric.app.StorageState.Inserted
        com.wl.weiqianwllib.upan.StorageState.EXIST -> com.wl.turbidimetric.app.StorageState.Exist
        com.wl.weiqianwllib.upan.StorageState.UNAUTHORIZED -> com.wl.turbidimetric.app.StorageState.Unauthorized
    }
}

/**
 * 两种u盘格式互相转换，其实对一对一的
 * @receiver com.wl.turbidimetric.app.StorageState
 * @return com.wl.weiqianwllib.upan.StorageState
 */
fun com.wl.turbidimetric.app.StorageState.toState(): com.wl.weiqianwllib.upan.StorageState {
    return when (this) {
        com.wl.turbidimetric.app.StorageState.None -> com.wl.weiqianwllib.upan.StorageState.NONE
        com.wl.turbidimetric.app.StorageState.Inserted -> com.wl.weiqianwllib.upan.StorageState.INSERTED
        com.wl.turbidimetric.app.StorageState.Exist -> com.wl.weiqianwllib.upan.StorageState.EXIST
        com.wl.turbidimetric.app.StorageState.Unauthorized -> com.wl.weiqianwllib.upan.StorageState.UNAUTHORIZED
    }
}

/**
 * 获取一个View的动画参数 x,y和width
 * @receiver View
 * @return MainActivity.PrintAnimParams
 */
fun View.getPrintParamsAnim(): MainActivity.PrintAnimParams {
    val xy = IntArray(2)
    getLocationInWindow(xy)
    return MainActivity.PrintAnimParams(xy[0],xy[1],width)

}
