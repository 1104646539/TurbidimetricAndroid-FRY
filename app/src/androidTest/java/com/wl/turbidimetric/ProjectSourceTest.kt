package com.wl.turbidimetric

import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.repository.if2.ProjectSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProjectSourceTest {

    private lateinit var dao: MainDao
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
    fun setup() {
        ServiceLocator.database = null
        ServiceLocator.projectDataSource = null
        dao = ServiceLocator.getDb(ApplicationProvider.getApplicationContext(), true).mainDao()
        projectSource =
            ServiceLocator.provideProjectSource(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun getProjects_returnTrue() = runBlocking {
        val list = projectSource.getProjects().first()
        assertThat(list.isEmpty()).isEqualTo(true)
    }

    @Test
    fun addProjects_returnTrue() = runBlocking {
        projectSource.addProject(project1)
        val list = projectSource.getProjects().first()
        assertThat(list.first().projectName).isEqualTo("项目1")
        assertThat(list.first().projectCode).isEqualTo("FOB")
    }

    @Test
    fun removeProjects2_returnTrue() = runBlocking {
        projectSource.addProject(project2)
        val list = projectSource.getProjects().first()
        assertThat(list.isEmpty()).isEqualTo(false)
        projectSource.removeProject(project2)
        val list2 = projectSource.getProjects().first()
        assertThat(list2.isEmpty()).isEqualTo(true)
    }

    @Test
    fun updateProjects2_returnTrue() = runBlocking {
        projectSource.addProject(project2)
        projectSource.updateProject(
            project2.copy(
                projectName = "项目55"
            )
        )
        val list = projectSource.getProjects().first()
        assertThat(list.first().projectName).isEqualTo("项目55")
    }

    @Test
    fun queryRepeatProjectModel_returnTrue() = runBlocking {
        projectSource.addProject(project2)
        val p = projectSource.queryRepeatProjectModel(
            "项目2", "FOB2"
        )
        assertThat(p?.projectName).isEqualTo("项目2")
    }

    @Test
    fun queryRepeatProjectModel_returnFalse() = runBlocking {
        projectSource.addProject(project2)
        val p = projectSource.queryRepeatProjectModel(
            "项目1", "FOB"
        )
        assertThat(p == null).isEqualTo(true)
    }

    @Test
    fun getProjectModelForId_returnFalse() = runBlocking {
        val p = projectSource.getProjectModelForId(
            1
        )
        assertThat(p == null).isEqualTo(true)
    }
    @Test
    fun getProjectModelForId_returnTrue() = runBlocking {
        projectSource.addProject(project1)
        val p = projectSource.getProjectModelForId(
            1
        )
        assertThat(p == null).isEqualTo(false)
        assertThat(p?.projectName).isEqualTo("项目1")
        assertThat(p?.projectId).isEqualTo(1L)
    }

    @After
    fun finish() {
        ServiceLocator.resetDataSource()
    }

}
