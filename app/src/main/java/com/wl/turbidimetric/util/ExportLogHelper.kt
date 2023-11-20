package com.wl.turbidimetric.util

import android.content.Context
import com.wl.turbidimetric.App
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.wllib.DateUtil
import com.wl.wllib.LogToFile
import com.wl.wllib.toLongTimeStr
import com.wl.wllib.toTimeStr
import java.io.File
import java.util.*

object ExportLogHelper {
    const val dirName = "全自动乳胶比浊日志"
    fun export(
        context: Context,
        onSuccess: (fileName1: String, fileName2: String) -> Unit,
        onFailed: (err: String) -> Unit
    ) {
        if (!StorageUtil.isExist()) {
            onFailed("没有插入U盘")
            return
        }
        val file1 = File(LogToFile.fileName1)
        val target1 = getLogFileTargetPath(file1.name)
        val file2 = File(LogToFile.fileName2)
        val target2 = getLogFileTargetPath(file2.name)
        StorageUtil.copyStorageToUpan(context, file1, target1, {
            StorageUtil.copyStorageToUpan(context, file2, target2, {
                onSuccess(target1, target2)
            }, { err ->
                onFailed(err)
            })
        }, { err ->
            onFailed(err)
        })
    }

    private fun getLogFileTargetPath(fileName: String): String {
        return "${dirName}/${Date().toTimeStr(DateUtil.Time2Format)}$fileName"
    }


}
