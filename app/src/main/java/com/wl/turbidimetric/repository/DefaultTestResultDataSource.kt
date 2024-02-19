package com.wl.turbidimetric.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.repository.if2.TestResultSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class DefaultTestResultDataSource constructor(
    private val dao: MainDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TestResultSource {

    override fun listenerTestResult(condition: ConditionModel): Flow<PagingData<TestResultAndCurveModel>> {
        return Pager(
            PagingConfig(pageSize = 50),
        ) {
            dao.listenerTestResultAndCurveModels(
                condition.name,
                condition.qrcode,
                condition.conMin,
                condition.conMax,
                condition.testTimeMin,
                condition.testTimeMax,
                condition.results.toList(),
                condition.results.size
            )
        }.flow
    }


    override suspend fun addTestResult(testResultModel: TestResultModel): Long {
        return dao.insertTestResultModel(testResultModel)
    }

    override suspend fun countTestResultAndCurveModels(condition: ConditionModel): Long {
        return dao.countTestResultAndCurveModels(
            condition.name,
            condition.qrcode,
            condition.conMin,
            condition.conMax,
            condition.testTimeMin,
            condition.testTimeMax,
            condition.results.toList(),
            condition.results.size
        )
    }

    override suspend fun addTestResults(testResultModel: List<TestResultModel>) {
        dao.insertTestResultModels(*(testResultModel.toTypedArray()))
    }

    override suspend fun getTestResultAndCurveModelById(id: Long): TestResultAndCurveModel {
        return dao.getTestResultAndCurveModelById(id)
    }

    override suspend fun getTestResultModelById(id: Long): TestResultModel {
        return dao.getTestResultModelById(id)
    }

    override suspend fun updateTestResult(testResultModel: TestResultModel): Int {
        return dao.updateTestResultModel(testResultModel)
    }


    override suspend fun removeTestResult(testResults: List<TestResultModel>) {
        dao.removeTestResultModel(*(testResults.toTypedArray()))
    }

    override suspend fun getAllTestResult(condition: ConditionModel): List<TestResultAndCurveModel> {
        return dao.getTestResultAndCurveModels(
            condition.name,
            condition.qrcode,
            condition.conMin,
            condition.conMax,
            condition.testTimeMin,
            condition.testTimeMax,
            condition.results.toList(),
            condition.results.size
        )
    }
}
