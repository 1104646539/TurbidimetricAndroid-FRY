package com.wl.turbidimetric.util

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.wl.turbidimetric.App
import com.wl.turbidimetric.ex.toLongString
import com.wl.turbidimetric.model.TestResultModel
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * 导出到Excel包装类
 * 具体实现在
 * @see ExcelUtils
 */
object ExportExcelUtil {
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
        "检测时间"
    )

    suspend fun printExcel(
        datas: List<TestResultModel>,
        onSuccess: (String) -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (!StorageUtil.isExist()) {
            onFailed("导出失败,未识别到U盘")
            return
        }
        val context: Context = App.instance!!
        val fileName = getFileName()
        val rootFile = File(StorageUtil.curPath)
        var documentFile = DocumentsUtils.getDocumentFile(rootFile, true, context)

        if (documentFile != null) {
            //创建文件夹
            documentFile = StorageUtil.createDocumentDir(documentFile, "数据表")
            //创建文件
            val newfile = StorageUtil.createDocumentFile(documentFile, "xls", fileName)
            var excelOutputStream: OutputStream? = null
            try {
                excelOutputStream =
                    StorageUtil.getOutputStream(newfile!!, context)
                //输出
                val isSuccess =
                    ExcelUtils.writeObjListToExcel(
                        titles,
                        getExportArray(datas),
                        excelOutputStream
                    )
                if (isSuccess) {
                    onSuccess(fileName)
                } else {
                    onFailed("导出失败")
                }
            } catch (e: FileNotFoundException) {
                onFailed("导出失败")
            } catch (e: IOException) {
                onFailed("导出失败")
            } finally {
                try {
                    excelOutputStream?.close()
                } catch (e: IOException) {
                    onFailed("导出失败")
                }
            }
        } else {
            onFailed("导出失败")
        }

//        val fileName = StorageUtil.curPath + "/" + getFileName()
//        val isSuccess =
//            ExcelUtils.writeObjListToExcel(
//                mutableListOf(""),
//                getExportArray(datas),
//                FileOutputStream(fileName)
//            )
//
//        if (isSuccess) {
//            onSuccess(fileName)
//        } else {
//            onFailed("导出失败")
//        }
    }

    /**
     * 导出的全文件名
     * @return String
     */
    private fun getFileName(): String {
        return "数据表${Date().toLongString()}.xls";
    }

    /**
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
                add("${it.testTime}")
            }
        }
    }
}
