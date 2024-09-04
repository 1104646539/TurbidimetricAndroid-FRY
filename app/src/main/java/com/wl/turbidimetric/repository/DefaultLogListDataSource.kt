package com.wl.turbidimetric.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wl.turbidimetric.dao.LogDao
import com.wl.turbidimetric.log.LogModel
import com.wl.turbidimetric.repository.if2.LogCondition
import com.wl.turbidimetric.repository.if2.LogListDataSource
import kotlinx.coroutines.flow.Flow

class DefaultLogListDataSource(val logDao: LogDao) : LogListDataSource {
    override fun listenerLogList(condition: LogCondition): Flow<PagingData<LogModel>> {
        return Pager(
            PagingConfig(pageSize = 50),
        ) {
            logDao.listenerLog(condition.levels, condition.startTime)
        }.flow
    }

    override fun addLog(model: LogModel): Long {
        return logDao.insertLog(model)
    }

    override fun queryLog(limit: Int, offset: Int): List<LogModel> {
        return logDao.queryLog(limit, offset)
    }

    override fun countLogList(condition: LogCondition): Long {
        return logDao.countLogList(condition.levels, condition.startTime)
    }
}
