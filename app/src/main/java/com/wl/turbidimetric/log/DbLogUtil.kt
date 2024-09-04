package com.wl.turbidimetric.log

import com.wl.turbidimetric.dao.LogDao
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.model.TestType
import com.wl.wllib.LogToFile
import java.util.Date
import kotlin.math.log

object DbLogUtil {
    const val path = "logdb"
    private var logDao: LogDao? = null
    fun init(logDao: LogDao) {
        this.logDao = logDao
    }

    @JvmStatic
    fun warring(testType: TestType, content: String) {
        saveToDb(testType, LogLevel.WARRING, content, Date().time)
    }

    @JvmStatic
    fun err(testType: TestType, content: String) {
        saveToDb(testType, LogLevel.ERROR, content, Date().time)
    }

    private fun saveToDb(testType: TestType, level: LogLevel, content: String, time: Long) {
        logDao?.let { dao ->
            val tag = if (testType == TestType.Test) {
                "样本分析"
            } else if (testType == TestType.MatchingArgs) {
                "拟合质控"
            } else {
                "其他"
            }
            dao.insertLog(
                LogModel(
                    level = level,
                    tag = tag,
                    content = content,
                    time = time
                )
            )
        }
    }
}
