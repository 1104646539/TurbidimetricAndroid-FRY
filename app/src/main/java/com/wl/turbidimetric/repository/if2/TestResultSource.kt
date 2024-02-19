package com.wl.turbidimetric.repository.if2

import androidx.paging.PagingData
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import kotlinx.coroutines.flow.Flow

interface TestResultSource {

    fun listenerTestResult(condition: ConditionModel): Flow<PagingData<TestResultAndCurveModel>>

    suspend fun addTestResult(testResultModel: TestResultModel): Long

    suspend fun countTestResultAndCurveModels(condition: ConditionModel): Long

    suspend fun addTestResults(testResultModel: List<TestResultModel>)

    suspend fun getTestResultAndCurveModelById(id: Long): TestResultAndCurveModel

    suspend fun getTestResultModelById(id: Long): TestResultModel

    suspend fun updateTestResult(testResultModel: TestResultModel): Int


    suspend fun removeTestResult(testResults: List<TestResultModel>)

    suspend fun getAllTestResult(condition: ConditionModel): List<TestResultAndCurveModel>

}
