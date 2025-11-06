package com.wl.turbidimetric.project.list

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.ActivityProjectListBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.project.details.ProjectDetailsActivity
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import org.greenrobot.eventbus.EventBus

class ProjectListFragment :
    BaseFragment<ProjectListViewModel, ActivityProjectListBinding>(R.layout.activity_project_list) {
    override val vm: ProjectListViewModel by viewModels { ProjectListViewModelFactory() }
    val adapter by lazy { ProjectListAdapter(mutableListOf()) }
    override fun initViewModel() {
    }

    override fun init(savedInstanceState: Bundle?) {
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
//        vd.nav.setBackTitle(imgHide = true, titleHide = false)
        vd.btnAddProject.setOnClickListener {
            EventBus.getDefault().post(EventMsg<Long>(EventGlobal.WHAT_PROJECT_LIST_TO_DETAILS))

//            startActivity(Intent(requireContext(), ProjectDetailsActivity::class.java).apply {
//                putExtra(
//                    ProjectDetailsActivity.ID,
//                    0
//                )
//            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }

    private fun initView() {

        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.adapter = adapter

        adapter.onItemClick = {
            EventBus.getDefault()
                .post(EventMsg<Long>(EventGlobal.WHAT_PROJECT_LIST_TO_DETAILS, it.projectId))

//            startActivity(Intent(requireContext(), ProjectDetailsActivity::class.java).apply {
//                putExtra(
//                    ProjectDetailsActivity.ID,
//                    it.projectId
//                )
//            })
        }
    }
}
