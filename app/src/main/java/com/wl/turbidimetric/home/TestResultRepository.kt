package com.wl.turbidimetric.home

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.wl.turbidimetric.App
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import kotlinx.coroutines.Dispatchers

class TestResultRepository {
    val dao = App.instance!!.mainDao

    fun datas(condition: ConditionModel) = Pager(
        PagingConfig(pageSize = 50),
    ) {
        dao.listenerTestResultAndCurveModels(condition.name,condition.qrcode,condition.conMin,condition.conMax,condition.testTimeMin,condition.testTimeMax,condition.results.toList(),condition.results.size)
    }.flow

    fun addTestResult(testResultModel: TestResultModel): Long {
        return dao.insertTestResultModel(testResultModel)
    }
    fun countTestResultAndCurveModels(condition: ConditionModel):Long{
        return dao.countTestResultAndCurveModels(condition.name,condition.qrcode,condition.conMin,condition.conMax,condition.testTimeMin,condition.testTimeMax,condition.results.toList(),condition.results.size)
    }
    fun addTestResults(testResultModel: List<TestResultModel>) {
        dao.insertTestResultModels(*(testResultModel.toTypedArray()))
    }
    fun getTestResultAndCurveModelById(id:Long):TestResultAndCurveModel? {
        return dao.getTestResultAndCurveModelById(id)
    }
    fun getTestResultModelById(id:Long):TestResultModel? {
        return dao.getTestResultModelById(id)
    }
    fun updateTestResult(testResultModel: TestResultModel): Int {
        return dao.updateTestResultModel(testResultModel)
    }



    fun removeTestResult(testResults: List<TestResultModel>) {
        dao.removeTestResultModel(*(testResults.toTypedArray()))
    }

    fun getAllTestResult(condition: ConditionModel): List<TestResultAndCurveModel> {
        return dao.getTestResultAndCurveModels()
    }
}
