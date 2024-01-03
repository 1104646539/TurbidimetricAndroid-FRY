package com.wl.turbidimetric.project.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.home.DialogState
import com.wl.turbidimetric.home.ProjectRepository
import com.wl.turbidimetric.model.ProjectModel
import com.wl.wwanandroid.base.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow


class ProjectDetailsViewModel(private val projectRepository: ProjectRepository) :
    BaseViewModel() {
    val dialogState =
        MutableSharedFlow<ProjectDetailsDialogUiState>()//需要改，现在有问题重复修改信息成功后不弹出对话框，因为stateFlow不能重发一样的信息？代测

    val project = MutableLiveData<ProjectModel?>()

    fun updateCurProject(projectModel: ProjectModel?) {
//        if (projectModel != null && project.value != null) {
//            projectModel.projectId = project.value!!.projectId
        project.value = projectModel
//        }
    }

    fun getProjects(): Flow<List<ProjectModel>> {
        return projectRepository.getProjects()
    }

    suspend fun updateProject(projectModel: ProjectModel): Int {
        return projectRepository.updateProject(projectModel)
    }

    suspend fun addProject(projectModel: ProjectModel): Long {
        return projectRepository.addProject(projectModel)
    }

    suspend fun queryRepeatProjectModel(projectName: String, projectCode: String): ProjectModel {
        return projectRepository.queryRepeatProjectModel(projectName, projectCode)
    }

    suspend fun getProjectModelForId(id: Long): ProjectModel {
        return projectRepository.getProjectModelForId(id)
    }

    suspend fun add(projectModel: ProjectModel) {
        val pro = projectRepository.queryRepeatProjectModel(
            projectModel.projectName,
            projectModel.projectCode
        )
        val ver = verifyProject(projectModel)
        if (ver.isNotEmpty()) {
            //验证不过
            dialogState.emit(
                ProjectDetailsDialogUiState(
                    ProjectDetailsDialogState.FAILED,
                    ver
                )
            )
            return
        }
        if (pro == null) {
            val id = addProject(projectModel)
            if (id > 0) {
                dialogState.emit(
                    ProjectDetailsDialogUiState(
                        ProjectDetailsDialogState.SUCCESS,
                        "添加成功"
                    )
                )
            } else {
                dialogState.emit(
                    ProjectDetailsDialogUiState(
                        ProjectDetailsDialogState.SUCCESS,
                        "添加失败"
                    )
                )
            }
        } else {
            dialogState.emit(
                ProjectDetailsDialogUiState(
                    ProjectDetailsDialogState.FAILED,
                    "项目名与项目代码不能与其他项目相同"
                )
            )
        }
    }

    private fun verifyProject(projectModel: ProjectModel?): String {
        if (projectModel == null) {
            return "项目为空"
        }
        if (projectModel.projectName.isNullOrEmpty()) {
            return "项目名不能为空"
        }
        if (projectModel.projectCode.isNullOrEmpty()) {
            return "项目代号不能为空"
        }
        if (projectModel.projectLjz <= 0) {
            return "临界值必须大于0"
        }
        if (projectModel.projectUnit.isNullOrEmpty()) {
            return "项目单位不能为空"
        }
        return ""
    }

    suspend fun update(projectModel: ProjectModel) {
        val pro = projectRepository.queryRepeatProjectModel(
            projectModel.projectName,
            projectModel.projectCode
        )
        val ver = verifyProject(projectModel)
        if (ver.isNotEmpty()) {
            //验证不过
            dialogState.emit(
                ProjectDetailsDialogUiState(
                    ProjectDetailsDialogState.FAILED,
                    ver
                )
            )
            return
        }
        //如果更新的信息不和其他项目一样
        if (pro == null || pro.projectId == projectModel.projectId) {
            val id = updateProject(projectModel)
            if (id > 0) {
                dialogState.emit(
                    ProjectDetailsDialogUiState(
                        ProjectDetailsDialogState.SUCCESS,
                        "更新成功"
                    )
                )
            } else {
                dialogState.emit(
                    ProjectDetailsDialogUiState(
                        ProjectDetailsDialogState.SUCCESS,
                        "更新失败"
                    )
                )
            }
        } else {
            dialogState.emit(
                ProjectDetailsDialogUiState(
                    ProjectDetailsDialogState.FAILED,
                    "项目名与项目代码不能与其他项目相同"
                )
            )
        }
    }

}

class ProjectDetailsViewModelFactory(
    private val projectRepository: ProjectRepository = ProjectRepository(),
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(ProjectDetailsViewModel::class.java)) {
            return ProjectDetailsViewModel(projectRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ProjectDetailsDialogUiState(
    val dialogState: ProjectDetailsDialogState,
    val dialogMsg: String
)

enum class ProjectDetailsDialogState {
    NONE,//无
    FAILED,//更新/添加 失败
    SUCCESS, //更新/添加 成功
}
