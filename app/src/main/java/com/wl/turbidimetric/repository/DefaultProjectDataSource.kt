package com.wl.turbidimetric.repository

import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.repository.if2.ProjectSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class DefaultProjectDataSource constructor(
    private val dao: MainDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProjectSource {
    /**
     * 所有项目
     */
    override fun getProjects(): Flow<List<ProjectModel>> {
        return dao.getProjectModels()
    }


    /**
     * 更新项目参数
     * @param project ProjectModel
     * @return Long
     */
    override suspend fun updateProject(project: ProjectModel): Int {
        return dao.updateProjectModel(project)
    }

    /**
     * 添加项目参数
     * @param project ProjectModel
     */
    override suspend fun addProject(project: ProjectModel): Long {
        return dao.insertProjectModel(project)
    }

    /**
     * 删除项目参数
     * @param project ProjectModel
     * @return Boolean
     */
    override suspend fun removeProject(project: ProjectModel): Boolean {
        return dao.removeProjectModel(project) > 0
    }

    /**
     * 查询是否有重复的项目
     */
    override suspend fun queryRepeatProjectModel(
        projectName: String,
        projectCode: String
    ): ProjectModel {
        return dao.queryRepeatProjectModel(projectName, projectCode)
    }

    /**
     * 获取项目 根据id
     */
    override suspend fun getProjectModelForId(id: Long): ProjectModel? {
        return dao.getProjectModelForId(id)
    }

}
