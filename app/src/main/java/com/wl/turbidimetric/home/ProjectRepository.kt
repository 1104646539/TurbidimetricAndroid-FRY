package com.wl.turbidimetric.home

import com.wl.turbidimetric.App
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel

class ProjectRepository {
    val dao = App.instance!!.mainDao

    /**
     * 所有项目
     */
//    val allDatas =
////        DBManager.ProjectBox.query().orderDesc(ProjectModel_.projectId).build()
//        dao.getProjectModels()


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
}
