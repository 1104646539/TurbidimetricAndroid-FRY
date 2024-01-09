package com.wl.turbidimetric.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import kotlinx.coroutines.flow.Flow

@Dao
interface MainDao {
    //and (case when (array_length(:results) != 0) then (testResult in (:results)) else 1 = 1 end)
    @Transaction
    @Query("select * from TestResultModel where (case when length(:name) > 0 then name = :name else 1 = 1 end) and (case when length(:qrcode) > 0 then sampleBarcode = :qrcode else 1 = 1 end) and (case when (:conMin != 0) then concentration >= :conMin else 1 = 1 end) and (case when (:conMax != 0) then concentration <= :conMax else 1 = 1 end) and (case when (:testTimeMin != 0) then testTime >= :testTimeMin else 1 = 1 end) and (case when (:testTimeMax != 0) then testTime <= :testTimeMax else 1 = 1 end) and (case when (:resultsSize > 0) then (testResult in (:results)) else 1 = 1 end) order by resultId desc")
    fun listenerTestResultAndCurveModels(
        name: String,
        qrcode: String,
        conMin: Int,
        conMax: Int,
        testTimeMin: Long,
        testTimeMax: Long,
        results: List<String>,
        resultsSize: Int
    ): PagingSource<Int, TestResultAndCurveModel>

    @Query("select count(resultId) from TestResultModel where (case when length(:name) > 0 then name = :name else 1 = 1 end) and (case when length(:qrcode) > 0 then sampleBarcode = :qrcode else 1 = 1 end) and (case when (:conMin != 0) then concentration >= :conMin else 1 = 1 end) and (case when (:conMax != 0) then concentration <= :conMax else 1 = 1 end) and (case when (:testTimeMin != 0) then testTime >= :testTimeMin else 1 = 1 end) and (case when (:testTimeMax != 0) then testTime <= :testTimeMax else 1 = 1 end) and (case when (:resultsSize > 0) then (testResult in (:results)) else 1 = 1 end) ")
    suspend fun countTestResultAndCurveModels(
        name: String,
        qrcode: String,
        conMin: Int,
        conMax: Int,
        testTimeMin: Long,
        testTimeMax: Long,
        results: List<String>,
        resultsSize: Int
    ): Long

    @Transaction
    @Query("select * from TestResultModel where (case when length(:name) > 0 then name = :name else 1 = 1 end) and (case when length(:qrcode) > 0 then sampleBarcode = :qrcode else 1 = 1 end) and (case when (:conMin != 0) then concentration >= :conMin else 1 = 1 end) and (case when (:conMax != 0) then concentration <= :conMax else 1 = 1 end) and (case when (:testTimeMin != 0) then testTime >= :testTimeMin else 1 = 1 end) and (case when (:testTimeMax != 0) then testTime <= :testTimeMax else 1 = 1 end) and (case when (:resultsSize > 0) then (testResult in (:results)) else 1 = 1 end) order by resultId desc")
    suspend fun getTestResultAndCurveModels(
        name: String,
        qrcode: String,
        conMin: Int,
        conMax: Int,
        testTimeMin: Long,
        testTimeMax: Long,
        results: List<String>,
        resultsSize: Int
    ): List<TestResultAndCurveModel>

    @Transaction
    @Query("select * from TestResultModel where resultId = :id")
    suspend fun getTestResultAndCurveModelById(id: Long): TestResultAndCurveModel

    @Transaction
    @Query("select * from TestResultModel where resultId = :id")
    suspend fun getTestResultModelById(id: Long): TestResultModel

    @Query("select * from CurveModel where isValid = :valid order by createTime desc limit 10")
    suspend fun getCurveModels(valid: Boolean = true): List<CurveModel>

    @Query("select * from CurveModel where isValid = :valid order by createTime desc limit 10")
    fun listenerCurveModels(valid: Boolean = true): Flow<List<CurveModel>>

    @Query("select * from ProjectModel")
    fun getProjectModels(): Flow<List<ProjectModel>>

    @Query("select * from ProjectModel where projectId = :id")
    fun getProjectModelForId(id: Long): ProjectModel

    @Query("select * from Projectmodel where projectName = :projectName or projectCode = :projectCode")
    suspend fun queryRepeatProjectModel(projectName: String, projectCode: String): ProjectModel

    @Insert
    suspend fun insertTestResultModel(model: TestResultModel): Long

    @Insert
    suspend fun insertTestResultModels(vararg model: TestResultModel)

    @Insert
    suspend fun insertProjectModel(model: ProjectModel): Long

    @Insert
    suspend fun insertCurveModel(model: CurveModel): Long

    @Update
    suspend fun updateTestResultModel(model: TestResultModel): Int

    @Update
    suspend fun updateCurveModel(model: CurveModel): Int

    @Update
    suspend fun updateProjectModel(model: ProjectModel): Int

    @Delete
    suspend fun removeProjectModel(model: ProjectModel): Int

    @Delete
    suspend fun removeCurveModel(model: CurveModel): Int

    @Delete
    suspend fun removeTestResultModel(vararg model: TestResultModel)


}
