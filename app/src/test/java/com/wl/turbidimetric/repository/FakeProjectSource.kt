package com.wl.turbidimetric.repository

import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.repository.if2.ProjectSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeProjectSource(private val projects: MutableList<ProjectModel> = mutableListOf()) : ProjectSource {
    override fun getProjects(): Flow<List<ProjectModel>> {
        return flowOf(projects)
    }

    override suspend fun updateProject(project: ProjectModel): Int {
        val k = projects.firstOrNull { it.projectId == project.projectId }
            ?.let {
                it.projectCode = project.projectCode
                it.projectName = project.projectName
                it.projectLjz = project.projectLjz
                it.projectUnit = project.projectUnit
                it.createTime = project.createTime
            }
        if (k == null) return 0
        return 1
    }

    override suspend fun addProject(project: ProjectModel): Long {
        projects.add(project)
        return 1
    }

    override suspend fun removeProject(project: ProjectModel): Boolean {
        return projects.remove(project)
    }

    override suspend fun queryRepeatProjectModel(
        projectName: String,
        projectCode: String
    ): ProjectModel? {
        return projects.firstOrNull { it.projectName == projectName && it.projectCode == projectCode }
    }

    override suspend fun getProjectModelForId(id: Long): ProjectModel? {
        return projects.firstOrNull { it.projectId == id }
    }
}
