package com.wl.turbidimetric.repository.if2

import androidx.paging.PagingData
import com.wl.turbidimetric.log.LogLevel
import com.wl.turbidimetric.log.LogModel
import kotlinx.coroutines.flow.Flow

interface LogListDataSource {
    fun listenerLogList(logCondition: LogCondition): Flow<PagingData<LogModel>>

    fun addLog(model: LogModel): Long

    fun queryLog(limit: Int, offset: Int): List<LogModel>
    fun countLogList(logCondition: LogCondition): Long
}

data class LogCondition(val levels: List<Int>, val startTime: Long)
