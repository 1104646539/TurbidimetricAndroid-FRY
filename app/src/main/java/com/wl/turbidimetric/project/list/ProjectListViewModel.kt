package com.wl.turbidimetric.project.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.base.BaseViewModel
import kotlinx.coroutines.flow.Flow

class ProjectListViewModel(private val projectRepository: ProjectRepository) :
    BaseViewModel() {
    fun getProjects(): Flow<List<ProjectModel>> {
        return projectRepository.getProjects()
    }
}

class ProjectListViewModelFactory(
    private val projectRepository: ProjectRepository = ProjectRepository(),
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(ProjectListViewModel::class.java)) {
            return ProjectListViewModel(projectRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
