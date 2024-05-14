package com.wl.turbidimetric.repository

import androidx.test.core.app.ApplicationProvider
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.repository.if2.ProjectSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ProjectSourceTest {

    private lateinit var projectSource: ProjectSource

    private val project1 = ProjectModel().apply {
        projectId = 1
        projectName = "项目1"
        projectCode = "FOB"
    }
    private val project2 = ProjectModel().apply {
        projectId = 2
        projectName = "项目2"
        projectCode = "FOB2"
    }
    private val projects: MutableList<ProjectModel> = mutableListOf(project1)

    @Before
    fun start() {
        projectSource = FakeProjectSource(projects)
    }


    @ExperimentalCoroutinesApi
    @Test
    fun getProjects() = runTest {
        var retFlow = projectSource.getProjects()
        var tempProjects: List<ProjectModel>? = listOf()
        retFlow.collect {
            tempProjects = it
        }
        advanceUntilIdle()

        assert(tempProjects?.size == 1)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun addProject() = runTest {
        projectSource.addProject(project2)

        var retFlow = projectSource.getProjects()
        var tempProjects: List<ProjectModel>? = listOf()
        retFlow.collect {
            tempProjects = it
        }
        advanceUntilIdle()

        assert(tempProjects?.size == 2)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun removeProject() = runTest {
        projectSource.removeProject(project1)

        var retFlow = projectSource.getProjects()
        var tempProjects: List<ProjectModel>? = listOf()
        retFlow.collect {
            tempProjects = it
        }
        advanceUntilIdle()

        assert(tempProjects?.isEmpty() == true)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun queryRepeatProjectModel_true() = runTest {
        val project = projectSource.queryRepeatProjectModel("项目1", "FOB")

        assert(project == project1)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun queryRepeatProjectModel_false() = runTest {
        val project = projectSource.queryRepeatProjectModel("项目2", "FOB")

        assert(project != project1)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getProjectForId() = runTest {
        val project = projectSource.getProjectModelForId(1)

        assert(project == project1)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun updateProject() = runTest {
        projectSource.updateProject(project1.copy(projectName = "项目3"))
        val p = projectSource.getProjectModelForId(project1.projectId)
        assert(p?.projectName == "项目3")
    }

    @After
    fun finish() {
    }
}
