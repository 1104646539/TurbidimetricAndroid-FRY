package com.wl.turbidimetric.util

import android.content.Context
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.wllib.toTimeStr
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.math.RoundingMode
import java.util.*

object ExportExcelHelper {

    val titles = mutableListOf(
        "姓名",
        "性别",
        "年龄",
        "项目",
        "条码",
        "编号",
        "浓度",
        "检测结果",
        "反应度",
        "检测时间",
    )
    val titlesDebug = mutableListOf(
        "姓名",
        "性别",
        "年龄",
        "项目",
        "条码",
        "编号",
        "浓度",
        "检测结果",
        "反应度",
        "原始1",
        "原始2",
        "原始3",
        "原始4",
        "值1",
        "值2",
        "值3",
        "值4",
        "检测时间",
    )

    @JvmStatic
    fun export(
        debug: Boolean,
        context: Context,
        datas: List<TestResultAndCurveModel>,
        onSuccess: (String) -> Unit,
        onFailed: (String) -> Unit
    ) {

        if (!StorageUtil.isExist()) {
            onFailed("没有插入U盘")
            return
        }

        if (datas.isNullOrEmpty()) {
            onFailed("没有数据")
            return
        }
        val fileName = getFileName()

        val newfile = StorageUtil.createFile("数据表/$fileName", context, false)
        var excelOutputStream: OutputStream? = null
        try {
            excelOutputStream =
                StorageUtil.getOutputStream(newfile!!, context)
            if (excelOutputStream == null) {
                onFailed("无法写入")
                return
            }
            //输出
            val isSuccess =
                ExcelUtils.writeObjListToExcel(
                    if (debug) titlesDebug else titles,
                    getExportArray(debug, datas),
                    excelOutputStream
                )
            if (isSuccess) {
                onSuccess("数据表/$fileName")
            } else {
                onFailed("无法写入")
            }
        } catch (e: FileNotFoundException) {
            onFailed("无法写入")
        } catch (e: IOException) {
            onFailed("无法写入")
        } finally {
            try {
                excelOutputStream?.close()
            } catch (e: IOException) {
                onFailed("无法写入")
            }
        }
    }

    /**
     * 导出的全文件名
     * @return String
     */
    private fun getFileName(): String {
        return "${Date().toTimeStr()}.xls"
    }

    /**
     * 数据转换成导出的格式
     * @param datas List<TestResultModel>
     * @return List<List<String>>
     */
    private fun getExportArray(
        debug: Boolean,
        datas: List<TestResultAndCurveModel>
    ): List<List<String>> {
        return datas.map { it ->
            mutableListOf<String>().apply {
                add("${it.result.name}")
                add("${it.result.gender}")
                add("${it.result.age}")
                add("${it.curve?.projectName ?: "-"}")
                add("${it.result.sampleBarcode}")
                add("${it.result.detectionNum}")
                add("${it.result.concentration}")
                add("${it.result.testResult}")
//                add("${it.result.absorbances.setScale(5, RoundingMode.HALF_UP)}")
                add(it.result.absorbances.toInt().toString())
                if (debug) {
                    add("${it.result.testOriginalValue1}")
                    add("${it.result.testOriginalValue2}")
                    add("${it.result.testOriginalValue3}")
                    add("${it.result.testOriginalValue4}")
                    add(it.result.testValue1.toInt().toString())
                    add(it.result.testValue2.toInt().toString())
                    add(it.result.testValue3.toInt().toString())
                    add(it.result.testValue4.toInt().toString())
                }
                add("${it.result.testTime.toTimeStr()}")

            }
        }
    }
}
