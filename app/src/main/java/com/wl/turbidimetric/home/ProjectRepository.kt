package com.wl.turbidimetric.home

import com.wl.turbidimetric.App
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ProjectRepository {
    val dao = App.instance!!.mainDao

    /**
     * 所有项目
     */
    fun getProjects(): Flow<List<ProjectModel>> {
        return dao.getProjectModels()
    }


    /**
     * 更新项目参数
     * @param project ProjectModel
     * @return Long
     */
    suspend fun updateProject(project: ProjectModel): Int {
        return dao.updateProjectModel(project)
    }

    /**
     * 添加项目参数
     * @param project ProjectModel
     */
    suspend fun addProject(project: ProjectModel): Long {
        return dao.insertProjectModel(project)
    }

    /**
     * 删除项目参数
     * @param project ProjectModel
     * @return Boolean
     */
    suspend fun removeProject(project: ProjectModel): Boolean {
        return dao.removeProjectModel(project) > 0
    }

    /**
     * 查询是否有重复的项目
     */
    suspend fun queryRepeatProjectModel(projectName: String, projectCode: String): ProjectModel {
        return dao.queryRepeatProjectModel(projectName, projectCode)
    }

    /**
     * 获取项目 根据id
     */
    suspend fun getProjectModelForId(id: Long): ProjectModel {
        return dao.getProjectModelForId(id)
    }
}
