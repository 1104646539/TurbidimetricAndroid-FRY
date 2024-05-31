package com.wl.turbidimetric.util

import android.content.Context
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.wllib.DateUtil
import com.wl.wllib.LogToFile
import com.wl.wllib.toTimeStr
import jxl.write.Label
import jxl.write.WritableSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ExportReportHelper {
    const val defaultReportSavePath = "sdcard/检测报告"
    const val defaultStorageCatalogue = "免疫比浊检测报告"
    val root = File(defaultReportSavePath)

    @JvmStatic
    fun getSaveFileName(model: TestResultAndCurveModel, barcode: Boolean): String {
        var fileName = ""
        val time = model.result.testTime.toTimeStr(DateUtil.Date1Format)
        if (barcode) {
            fileName += time + " " + model.result.sampleBarcode + ".pdf"
        } else {
            fileName += time + " " + model.result.detectionNum + ".pdf"
        }
        return fileName
    }

    @JvmStatic
    fun exportReport(
        context: Context,
        datas: List<TestResultAndCurveModel>,
        hospitalName: String,
        scope: CoroutineScope,
        barcode: Boolean,
        onSuccess: (count: Int, successCount: Int, failedCount: Int) -> Unit,
        onFailed: (err: String) -> Unit,
    ) {
        createReport(
            context,
            datas,
            hospitalName,
            scope,
            barcode,
            onSuccess,
            onFailed,
            true
        )
    }

    @JvmStatic
    fun createReport(
        context: Context,
        datas: List<TestResultAndCurveModel>,
        hospitalName: String,
        scope: CoroutineScope,
        barcode: Boolean,
        onSuccess: (count: Int, successCount: Int, failedCount: Int) -> Unit,
        onFailed: (err: String) -> Unit,
        export: Boolean
    ) {
        if (export) {
            if (!StorageUtil.isExist()) {
                onFailed("没有插入U盘")
                return
            }
        }

        if (datas.isNullOrEmpty()) {
            onFailed("没有数据")
            return
        }
        var count = datas.size
        var successCount = 0
        var failedCount = 0


        if (!root.exists()) {
            root.mkdirs()
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                datas.forEach {
                    val ret = create(it, hospitalName, barcode)
                    if (ret.isNullOrEmpty()) {
                        failedCount++
                    } else {
                        if (export) {
                            val localFile = File(ret)
                            StorageUtil.copyStorageToUpan(
                                context,
                                localFile,
                                "$defaultStorageCatalogue/${localFile.name}",
                                {
                                    LogToFile.i("");
                                    successCount++
                                },
                                { err ->
                                    LogToFile.i("$err");
                                    failedCount++
                                })
                        } else {
                            successCount++
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onSuccess.invoke(count, successCount, failedCount)
            }
        }

    }

    @JvmStatic
    fun create(result: TestResultAndCurveModel, hospitalName: String, barcode: Boolean): String? {
        var fileName = getSaveFileName(result, barcode)
        val localFile = File(root, "$fileName")
        if (localFile.isFile && localFile.exists()) {
            localFile.delete()
        }
        return PdfCreateUtil.create(result, localFile, hospitalName)
    }

}
