package com.wl.turbidimetric.repository.if2

import com.wl.turbidimetric.model.ProjectModel
import kotlinx.coroutines.flow.Flow

interface ProjectSource {
    /**
     * 所有项目
     */
    fun getProjects() :Flow<List<ProjectModel>>

    /**
     * 更新项目参数
     * @param project ProjectModel
     * @return Long
     */
    suspend fun updateProject(project: ProjectModel): Int

    /**
     * 添加项目参数
     * @param project ProjectModel
     */
    suspend fun addProject(project: ProjectModel): Long

    /**
     * 删除项目参数
     * @param project ProjectModel
     * @return Boolean
     */
    suspend fun removeProject(project: ProjectModel): Boolean

    /**
     * 查询是否有重复的项目
     */
    suspend fun queryRepeatProjectModel(projectName: String, projectCode: String): ProjectModel?

    /**
     * 获取项目 根据id
     */
    suspend fun getProjectModelForId(id: Long): ProjectModel?
}
