package com.wl.turbidimetric.home

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.ex.put
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.ProjectModel_
import io.objectbox.android.ObjectBoxDataSource
import kotlinx.coroutines.Dispatchers

class ProjectRepository() {
    /**
     * 所有项目
     */
    val allDatas =
        DBManager.ProjectBox.query().orderDesc(ProjectModel_.projectId).build()

    /**
     * 分页的项目
     */
    val paginDatas = Pager(
        PagingConfig(pageSize = 300),
        pagingSourceFactory = ObjectBoxDataSource.Factory(
            DBManager.ProjectBox.query().orderDesc(ProjectModel_.projectId).build()
        )
            .asPagingSourceFactory(Dispatchers.IO)
    )

    /**
     * 更新项目参数
     * @param project ProjectModel
     * @return Long
     */
    fun updateProject(project: ProjectModel): Long {
        return DBManager.ProjectBox.put(project)
    }

    /**
     * 添加项目参数
     * @param project ProjectModel
     */
    fun addProject(project: ProjectModel): Long {
        return DBManager.ProjectBox.put(project)
    }

    /**
     * 删除项目参数
     * @param project ProjectModel
     * @return Boolean
     */
    fun removeProject(project: ProjectModel): Boolean {
        return DBManager.ProjectBox.remove(project)
    }
}
