package com.wl.turbidimetric.datamanager

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.ProjectModel
import io.objectbox.Box
import io.objectbox.android.ObjectBoxDataSource
import kotlinx.coroutines.Dispatchers

class DataManagerRepository(
    private val testResultBox: Box<TestResultModel>,
    private val projectBox: Box<ProjectModel>
) {

    fun datas() = Pager(
        PagingConfig(pageSize = 30),
        pagingSourceFactory = ObjectBoxDataSource.Factory(
            testResultBox.query().build()
        )
            .asPagingSourceFactory(Dispatchers.IO)
    ).flow

    fun updateDetectionResult(testResult: TestResultModel): Long {
        return testResultBox.put(testResult)
    }

    fun updateProject(project: ProjectModel): Long {
        return projectBox.put(project)
    }

    fun removeProject(testResult: TestResultModel): Boolean {
        return testResultBox.remove(testResult)
    }
}
