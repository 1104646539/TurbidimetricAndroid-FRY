package com.wl.turbidimetric.project.details

import android.os.Bundle
import android.view.View.AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
import android.view.View.OnClickListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.ActivityProjectDetailsBinding
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.project.details.ProjectDetailsActivity.Companion.ID
import com.wl.turbidimetric.project.list.ProjectListAdapter
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class ProjectDetailsFragment :
    BaseFragment<ProjectDetailsViewModel, ActivityProjectDetailsBinding>(R.layout.activity_project_details) {
    override val vm: ProjectDetailsViewModel by viewModels { ProjectDetailsViewModelFactory() }
    val adapter by lazy { ProjectListAdapter(mutableListOf()) }
    var id = 0L
    var project: ProjectModel? = null
    val hiltDialog by lazy { HiltDialog(requireContext()) }

    override fun initViewModel() {
    }

    override fun init(savedInstanceState: Bundle?) {
        initData()
        initView()
        listener()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        i("onHiddenChanged hidden=$hidden")
        if (!hidden) {
            initData()
            initView()
        }
    }

    var isChange = false;
    override fun onResume() {
        super.onResume()
        i("onResume id=$id")
//        if (isChange)
//        initView()
    }

    private fun initView() {
        if (id > 0) {
            lifecycleScope.launchWhenCreated {
                project = vm.getProjectModelForId(id)

                if (project == null) {
                    toast("没有找到这个项目")
                } else {
                    vm.updateCurProject(project)
                }

            }
            vd.btnSave.tv.text = "保存"
        } else {
            vd.btnSave.tv.text = "添加"
            vd.etProjectName.setText("")
            vd.etProjectCode.setText("")
            vd.etProjectUnit.setText("")
            vd.etProjectLjz.setText("")
        }
    }

    override fun onStart() {
        super.onStart()
        i("onStart")
    }

    private fun initData() {
        isChange = true
//        vd.nav.setTitle("项目详情")
        id = arguments?.getLong(ID, 0) ?: 0
//        id = intent.getLongExtra(ID, 0)
    }

    private fun listener() {
        listenerEvent()
        listenerView()
    }

    private fun listenerView() {
        lifecycleScope.launchWhenCreated {
            vm.project.observe(this@ProjectDetailsFragment) {
                if (it != null) {
                    vd.etProjectName.setText(project?.projectName ?: "")
                    vd.etProjectCode.setText(project?.projectCode ?: "")
                    vd.etProjectUnit.setText(project?.projectUnit ?: "")
                    vd.etProjectLjz.setText(project?.projectLjz?.toString() ?: "")

                    vd.etProjectName.selectionLast()
                    vd.etProjectCode.selectionLast()
                    vd.etProjectUnit.selectionLast()
                    vd.etProjectLjz.selectionLast()
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            vm.dialogState.collectLatest { state ->
                when (state.dialogState) {
                    ProjectDetailsDialogState.NONE -> {

                    }

                    ProjectDetailsDialogState.FAILED -> {
                        hiltDialog.showPop(requireContext()) {
                            it.showDialog(state.dialogMsg, confirmText = "确定", confirmClick = {
                                it.dismiss()
                            })
                        }
                    }

                    ProjectDetailsDialogState.SUCCESS -> {
                        hiltDialog.showPop(requireContext()) {
                            it.showDialog(state.dialogMsg, confirmText = "确定", confirmClick = {
                                it.dismiss()
                                EventBus.getDefault()
                                    .post(EventMsg<String>(EventGlobal.WHAT_PROJECT_DETAILS_FINISH))

//                                finishAfterTransition()
                            })
                        }
                    }
                }
            }
        }
    }

    private var onRight: OnClickListener = OnClickListener {
        if (id <= 0) {//新增
            lifecycleScope.launch {
                vm.add(ProjectModel().apply {
                    projectName = vd.etProjectName.text.toString()
                    projectCode = vd.etProjectCode.text.toString()
                    projectUnit = vd.etProjectUnit.text.toString()
                    projectLjz = vd.etProjectLjz.text.toString().toIntOrNull() ?: 0
                })
            }
        } else {//修改
            lifecycleScope.launch {
                vm.project.value?.let {
                    vm.update(it.apply {
                        projectName = vd.etProjectName.text.toString()
                        projectCode = vd.etProjectCode.text.toString()
                        projectUnit = vd.etProjectUnit.text.toString()
                        projectLjz = vd.etProjectLjz.text.toString().toIntOrNull() ?: 0
                    })
                }

            }
        }
    }

    private fun listenerEvent() {
        vd.llBack.setOnClickListener {
//            finishAfterTransition()
            EventBus.getDefault().post(EventMsg<String>(EventGlobal.WHAT_PROJECT_DETAILS_FINISH))
        }
    }

    companion object {
        val ID = "id"
    }
}
