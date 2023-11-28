package com.wl.turbidimetric.util

import android.content.Context
import com.wl.turbidimetric.model.TestResultModel
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.wllib.toTimeStr
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.util.*

object ExportExcelHelper {

    val titles = mutableListOf(
        "姓名",
        "性别",
        "年龄",
        "编号",
        "浓度",
        "检测结果",
        "吸光度",
        "原始1",
        "原始2",
        "原始3",
        "原始4",
        "值1",
        "值2",
        "值3",
        "值4",
        "检测时间",
        "条码"
    )

    @JvmStatic
    fun export(
        context: Context,
        datas: List<TestResultModel>,
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
                    titles,
                    getExportArray(datas),
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
        return "${Date().toTimeStr()}.xls";
    } /**
     * 数据转换成导出的格式
     * @param datas List<TestResultModel>
     * @return List<List<String>>
     */
    private fun getExportArray(datas: List<TestResultModel>): List<List<String>> {
        return datas.map { it ->
            mutableListOf<String>().apply {
                add("${it.name}")
                add("${it.gender}")
                add("${it.age}")
                add("${it.detectionNum}")
                add("${it.concentration}")
                add("${it.testResult}")
                add("${it.absorbances}")
                add("${it.testOriginalValue1}")
                add("${it.testOriginalValue2}")
                add("${it.testOriginalValue3}")
                add("${it.testOriginalValue4}")
                add("${it.testValue1}")
                add("${it.testValue2}")
                add("${it.testValue3}")
                add("${it.testValue4}")
                add("${it.testTime.toTimeStr()}")
                add("${it.sampleBarcode}")
            }
        }
    }
}
