package com.wl.turbidimetric.datamanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.repository.DefaultProjectDataSource
import com.wl.turbidimetric.repository.DefaultTestResultDataSource
import com.wl.turbidimetric.repository.if2.ProjectSource
import com.wl.turbidimetric.repository.if2.TestResultSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class DataManagerViewModel(
    private val appViewModel: AppViewModel,
    private val projectRepository: ProjectSource,
    private val testResultRepository: TestResultSource
) : BaseViewModel() {

    private val _dialogUiState = MutableSharedFlow<DataManagerUiState>()
    val dialogUiState = _dialogUiState.asSharedFlow()

    /**
     * 筛选条件
     */
    private val _conditionModel = MutableStateFlow(ConditionModel())
    val conditionModel: StateFlow<ConditionModel> = _conditionModel.asStateFlow()

    /**
     * 结果数量
     */
    private val _resultSize = MutableStateFlow(0L)
    val resultSize: StateFlow<Long> = _resultSize.asStateFlow()

    fun item(condition: ConditionModel): Flow<PagingData<TestResultAndCurveModel>> {
        viewModelScope.launch {
            _resultSize.value = testResultRepository.countTestResultAndCurveModels(condition)
        }
        return testResultRepository.listenerTestResult(condition).cachedIn(viewModelScope)
    }

    suspend fun update(testResult: TestResultModel): Int {
        return testResultRepository.updateTestResult(testResult)
    }

    suspend fun update(model: TestResultAndCurveModel): Int {
        return testResultRepository.updateTestResult(model.result)
    }

    suspend fun getTestResultAndCurveModelById(id: Long): TestResultAndCurveModel {
        return testResultRepository.getTestResultAndCurveModelById(id)
    }

    suspend fun add(testResult: TestResultModel): Long {
        return testResultRepository.addTestResult(testResult)
    }

    suspend fun add(testResult: List<TestResultModel>) {
        testResultRepository.addTestResults(testResult)
    }

    suspend fun remove(testResults: List<TestResultModel>) {
        testResultRepository.removeTestResult(testResults)
    }

    suspend fun clickDeleteDialogConfirm(results: List<TestResultModel>) {
        remove(results)
    }

    suspend fun getFilterAll(condition: ConditionModel): List<TestResultAndCurveModel> {
        return testResultRepository.getAllTestResult(condition)
    }

    suspend fun conditionChange(conditionModel: ConditionModel) {
        _conditionModel.value = conditionModel
        _resultSize.value = testResultRepository.countTestResultAndCurveModels(conditionModel)
    }

    /***
     * 显示详情
     */
    suspend fun showDetails(id: Long) {
        val result = getTestResultAndCurveModelById(id)
        _dialogUiState.emit(DataManagerUiState.ResultDetailsDialog(result))
    }

    fun deleteResult() {
        viewModelScope.launch {
            _dialogUiState.emit(DataManagerUiState.DeleteDialog)
        }
    }

}

class DataManagerViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
    private val projectRepository: ProjectSource = ServiceLocator.provideProjectSource(App.instance!!),
    private val testResultRepository: TestResultSource = ServiceLocator.provideTestResultSource(App.instance!!)
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DataManagerViewModel::class.java)) {
            return DataManagerViewModel(appViewModel, projectRepository, testResultRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

