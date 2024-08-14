package com.wl.turbidimetric.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wl.turbidimetric.model.GlobalConfig

@Dao
interface GlobalDao {
    @Update
    fun updateGlobalConfig(model: GlobalConfig): Int

    @Insert
    fun insertGlobalConfig(model: GlobalConfig): Long

    @Query("select * from GlobalConfig where id = :id")
    fun getGlobalConfig(id: Long): GlobalConfig

    @Query("select * from GlobalConfig")
    fun getAllGlobalConfig(): List<GlobalConfig>
}
