package com.wl.turbidimetric.project.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.repository.DefaultProjectDataSource
import com.wl.turbidimetric.repository.if2.ProjectSource
import kotlinx.coroutines.flow.Flow

class ProjectListViewModel(private val projectRepository: ProjectSource) :
    BaseViewModel() {
    fun getProjects(): Flow<List<ProjectModel>> {
        return projectRepository.getProjects()
    }
}

class ProjectListViewModelFactory(
    private val projectRepository: ProjectSource = DefaultProjectDataSource(App.instance!!.mainDao!!),
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(ProjectListViewModel::class.java)) {
            return ProjectListViewModel(projectRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
