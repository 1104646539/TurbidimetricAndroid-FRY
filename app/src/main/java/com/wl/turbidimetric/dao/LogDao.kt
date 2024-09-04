package com.wl.turbidimetric.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.wl.turbidimetric.log.LogLevel
import com.wl.turbidimetric.log.LogModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    fun insertLog(logModel: LogModel): Long

    @Query("delete from LogModel")
    fun deleteAll(): Int

    @Query("select * from LogModel limit :limit offset :offset")
    fun queryLog(limit: Int, offset: Int): List<LogModel>

    @Query("select * from LogModel where time >= :startTime and level in (:levels) order by id desc")
    fun listenerLog(levels: List<Int>, startTime: Long): PagingSource<Int, LogModel>

    @Query("select count(id) from LogModel where time >= :startTime and level in (:levels)")
    fun countLogList(levels: List<Int>, startTime: Long): Long
}
