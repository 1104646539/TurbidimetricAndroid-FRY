package com.wl.turbidimetric.project.list

import android.content.Intent
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ActivityProjectListBinding
import com.wl.turbidimetric.project.details.ProjectDetailsActivity
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.base.BaseActivity
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
        lifecycleScope.launchWhenStarted {
            vm.getProjects().collectLatest {
                adapter.submit(it)
            }
        }

        vd.llBack.setOnClickListener {
            finish()
        }
        vd.tvAdd.setOnClickListener {
            startActivity(Intent(this, ProjectDetailsActivity::class.java).apply {
                putExtra(
                    ProjectDetailsActivity.ID,
                    0
                )
            })
        }
    }

    private fun initView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "测试页面"

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
