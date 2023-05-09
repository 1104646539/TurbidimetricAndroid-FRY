package com.wl.turbidimetric.datamanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.home.TestResultRepository
import com.wl.turbidimetric.model.TestResultModel
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.query.Query
import kotlinx.coroutines.flow.Flow


class DataManagerViewModel(
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val testResultRepository: TestResultRepository = TestResultRepository()
) : BaseViewModel() {

    public fun item(condition: Query<TestResultModel>?): Flow<PagingData<TestResultModel>> {
        return testResultRepository.datas(condition).cachedIn(viewModelScope)
    }

    fun update(testResult: TestResultModel): Long {
        return testResultRepository.updateTestResult(testResult)
    }

    fun add(testResult: TestResultModel): Long {
        return testResultRepository.addTestResult(testResult)
    }

    fun remove(testResult: TestResultModel): Boolean {
        return testResultRepository.removeTestResult(testResult)
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

