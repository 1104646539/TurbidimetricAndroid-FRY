package com.wl.turbidimetric.datamanager

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.home.TestResultRepository
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.wwanandroid.base.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class DataManagerViewModel(
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val testResultRepository: TestResultRepository = TestResultRepository()
) : BaseViewModel() {
    /**
     * 显示删除提示对话框
     */
    val showDeleteDialog = MutableLiveData(false)

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
        return testResultRepository.datas(condition).cachedIn(viewModelScope)
    }

    suspend fun update(testResult: TestResultModel): Int {
        return testResultRepository.updateTestResult(testResult)
    }

    suspend fun update(model: TestResultAndCurveModel): Int {
        return testResultRepository.updateTestResult(model.result)
    }

    suspend fun getTestResultAndCurveModelById(id: Long): TestResultAndCurveModel? {
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

//    open fun ConditionModel.buildQuery(): Query<TestResultModel> {
//        val condition: QueryBuilder<TestResultModel> = DBManager.TestResultBox.query().orderDesc(
//            TestResultModel_.id
//        )
//
//        if (name.isNotEmpty()) {
//            condition.contains(
//                TestResultModel_.name,
//                name,
//                QueryBuilder.StringOrder.CASE_INSENSITIVE
//            )
//        }
//        if (qrcode.isNotEmpty()) {
//            condition.contains(
//                TestResultModel_.sampleBarcode,
//                qrcode,
//                QueryBuilder.StringOrder.CASE_INSENSITIVE
//            )
//        }
//        if (conMin != 0) {
//            condition.greaterOrEqual(TestResultModel_.concentration, conMin.toLong())
//        }
//        if (conMax != 0) {
//            condition.lessOrEqual(TestResultModel_.concentration, conMax.toLong())
//        }
//        if (testTimeMin != 0L) {
//            condition.greaterOrEqual(TestResultModel_.testTime, testTimeMin)
//        }
//        if (testTimeMax != 0L) {
//            condition.lessOrEqual(TestResultModel_.testTime, testTimeMax)
//        }
//
//
//        if (results.isNotEmpty()) {
//            condition.`in`(
//                TestResultModel_.testResult,
//                results,
//                QueryBuilder.StringOrder.CASE_INSENSITIVE
//            )
//        }
//
//        return condition.build()
//    }
}

class DataManagerViewModelFactory(
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val testResultRepository: TestResultRepository = TestResultRepository()
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DataManagerViewModel::class.java)) {
            return DataManagerViewModel(projectRepository, testResultRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

