package com.wl.turbidimetric.datamanager.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.wl.turbidimetric.datamanager.DataManagerRepository
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.model.TestResultModel
import com.wl.wwanandroid.base.BaseViewModel
import io.objectbox.android.ObjectBoxDataSource
import kotlinx.coroutines.Dispatchers


class DataManagerViewModel(private val repository: DataManagerRepository) : BaseViewModel() {

    public val items =
        repository.datas().cachedIn(viewModelScope)

    var row = "0"


    fun update(testResult: TestResultModel): Long {
        repository.updateProject(testResult.project.target)
        return repository.updateDetectionResult(testResult)
    }

    fun add(testResult: TestResultModel): Long {
        repository.updateProject(testResult.project.target)
        return repository.updateDetectionResult(testResult)
    }

    fun remove(testResult: TestResultModel): Boolean {
        return repository.removeProject(testResult)
    }
}

class DataManagerViewModelFactory(private val repository: DataManagerRepository) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DataManagerViewModel::class.java)) {
            return DataManagerViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

