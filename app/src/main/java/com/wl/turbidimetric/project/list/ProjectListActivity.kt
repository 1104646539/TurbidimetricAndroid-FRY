package com.wl.turbidimetric.project.list

//import com.wl.turbidimetric.ex.transitionTo
import android.content.Intent
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.databinding.ActivityProjectListBinding
import com.wl.turbidimetric.project.details.ProjectDetailsActivity
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest

/**
 * 项目列表
 */
class ProjectListActivity : BaseActivity<ProjectListViewModel, ActivityProjectListBinding>() {
    override val vd: ActivityProjectListBinding by ActivityDataBindingDelegate(R.layout.activity_project_list)
    override val vm: ProjectListViewModel by viewModels { ProjectListViewModelFactory() }
    val adapter by lazy { ProjectListAdapter(mutableListOf()) }
    override fun init() {
        initView()
        listener()
    }

    private fun listener() {
        lifecycleScope.launchWhenCreated {
            vm.getProjects().collectLatest {
                adapter.submit(it)
            }
        }
//        vd.nav.setTitle("项目列表")
//        vd.nav.setOnBack {
//            finish()
//        }
        vd.btnAddProject.setOnClickListener {
            startActivity(Intent(this, ProjectDetailsActivity::class.java).apply {
                putExtra(
                    ProjectDetailsActivity.ID,
                    0
                )
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }
    private fun initView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        vd.rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        vd.rv.adapter = adapter

        adapter.onItemClick = {
            startActivity(Intent(this, ProjectDetailsActivity::class.java).apply {
                putExtra(
                    ProjectDetailsActivity.ID,
                    it.projectId
                )
            })
        }
    }

}
