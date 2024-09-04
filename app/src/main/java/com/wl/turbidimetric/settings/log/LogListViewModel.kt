package com.wl.turbidimetric.settings.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.log.LogModel
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.LogCondition
import com.wl.turbidimetric.repository.if2.LogListDataSource
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.log

class LogListViewModel(val logListDataSource: LogListDataSource) : BaseViewModel() {

    /**
     * 筛选条件
     */
    private val _conditionModel = MutableStateFlow(
        LogCondition(
            levels = mutableListOf(0, 1),
            startTime = 0,
        )
    )
    val conditionModel: StateFlow<LogCondition> = _conditionModel.asStateFlow()

    fun listenerLogList(logCondition: LogCondition): Flow<PagingData<LogModel>> {
        getCount(logCondition)
        return logListDataSource.listenerLogList(logCondition).cachedIn(viewModelScope)
    }

    fun changeCondition(logCondition: LogCondition) {
        viewModelScope.launch {
            _conditionModel.emit(logCondition)
            getCount(logCondition)
        }
    }

    fun getCount(logCondition: LogCondition) {
//        val count = logListDataSource.countLogList(logCondition)
//        i("logCondition count=$count")
    }
}

class LogListViewModelFactory(
    private val logListDataSource: LogListDataSource = ServiceLocator.providerLogListDataSource(App.instance!!)
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogListViewModel::class.java)) {
            return LogListViewModel(logListDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
