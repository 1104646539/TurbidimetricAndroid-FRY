package com.wl.turbidimetric.home

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.model.TestResultModel
import io.objectbox.android.ObjectBoxDataSource
import io.objectbox.query.Query
import kotlinx.coroutines.Dispatchers

class TestResultRepository() {

    fun datas(condition: Query<TestResultModel>?) = Pager(
        PagingConfig(pageSize = 500),
        pagingSourceFactory = ObjectBoxDataSource.Factory(
            condition
        )
            .asPagingSourceFactory(Dispatchers.IO)
    ).flow

    fun addTestResult(testResultModel: TestResultModel): Long {
        return DBManager.TestResultBox.put(testResultModel)
    }

    fun updateTestResult(testResultModel: TestResultModel): Long {
        return DBManager.TestResultBox.put(testResultModel)
    }

    fun removeTestResult(testResults: List<TestResultModel>) {
         DBManager.TestResultBox.remove(testResults)
    }

}
