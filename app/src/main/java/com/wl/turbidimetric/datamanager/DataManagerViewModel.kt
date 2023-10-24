package com.wl.turbidimetric.datamanager

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.home.TestResultRepository
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.TestResultModel_
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.kotlin.toFlow
import io.objectbox.query.LazyList
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.flow.*


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

    public fun item(condition: ConditionModel): Flow<PagingData<TestResultModel>> {
        _resultSize.value = condition.buildQuery().count()
        return testResultRepository.datas(condition.buildQuery()).cachedIn(viewModelScope)
    }

    fun update(testResult: TestResultModel): Long {
        return testResultRepository.updateTestResult(testResult)
    }

    fun add(testResult: TestResultModel): Long {
        return testResultRepository.addTestResult(testResult)
    }

    fun remove(testResults: List<TestResultModel>) {
        testResultRepository.removeTestResult(testResults)
    }

    fun clickDeleteDialogConfirm(results: List<TestResultModel>) {
        remove(results)
    }

    fun getFilterAll(condition: ConditionModel): LazyList<TestResultModel> {
        return testResultRepository.getAllTestResult(condition.buildQuery())
    }

    fun conditionChange(conditionModel: ConditionModel) {
        _conditionModel.value = conditionModel
        _resultSize.value = conditionModel.buildQuery().count()
    }

    open fun ConditionModel.buildQuery(): Query<TestResultModel> {
        val condition: QueryBuilder<TestResultModel> = DBManager.TestResultBox.query().orderDesc(
            TestResultModel_.id
        )

        if (name.isNotEmpty()) {
            condition.contains(
                TestResultModel_.name,
                name,
                QueryBuilder.StringOrder.CASE_INSENSITIVE
            )
        }
        if (qrcode.isNotEmpty()) {
            condition.contains(
                TestResultModel_.sampleBarcode,
                qrcode,
                QueryBuilder.StringOrder.CASE_INSENSITIVE
            )
        }
        if (conMin != 0) {
            condition.greaterOrEqual(TestResultModel_.concentration, conMin.toLong())
        }
        if (conMax != 0) {
            condition.lessOrEqual(TestResultModel_.concentration, conMax.toLong())
        }
        if (testTimeMin != 0L) {
            condition.greaterOrEqual(TestResultModel_.testTime, testTimeMin)
        }
        if (testTimeMax != 0L) {
            condition.lessOrEqual(TestResultModel_.testTime, testTimeMax)
        }


        if (results.isNotEmpty()) {
            condition.`in`(
                TestResultModel_.testResult,
                results,
                QueryBuilder.StringOrder.CASE_INSENSITIVE
            )
        }

        return condition.build()
    }
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

