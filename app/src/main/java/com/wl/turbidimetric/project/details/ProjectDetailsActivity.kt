package com.wl.turbidimetric.project.details

import android.view.View.OnClickListener
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.databinding.ActivityProjectDetailsBinding
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.project.list.ProjectListAdapter
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProjectDetailsActivity :
    BaseActivity<ProjectDetailsViewModel, ActivityProjectDetailsBinding>() {
    override val vd: ActivityProjectDetailsBinding by ActivityDataBindingDelegate(R.layout.activity_project_details)
    override val vm: ProjectDetailsViewModel by viewModels { ProjectDetailsViewModelFactory() }
    val adapter by lazy { ProjectListAdapter(mutableListOf()) }
    var id = 0L
    var project: ProjectModel? = null

    val hiltDialog by lazy { HiltDialog(this) }
    override fun init() {
        initData()

        initView()
        listener()
    }

    private fun initData() {
        vd.nav.setTitle("项目详情")
        id = intent.getLongExtra(ID, 0)
        if (id > 0) {
            lifecycleScope.launchWhenCreated {
                project = vm.getProjectModelForId(id)

                if (project == null) {
                    toast("没有找到这个项目")
                } else {
                    vm.updateCurProject(project)
                }

            }
            vd.nav.setRight1("保存",onRight)
        } else {
            vd.nav.setRight1("添加",onRight)
        }

    }

    private fun listener() {
        listenerEvent()
        listenerView()
    }

    private fun listenerView() {
        lifecycleScope.launchWhenCreated {
            vm.project.observe(this@ProjectDetailsActivity) {
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
            vm.dialogState.collectLatest { state->
                when (state.dialogState) {
                    ProjectDetailsDialogState.NONE -> {

                    }

                    ProjectDetailsDialogState.FAILED -> {
                        hiltDialog.showPop(this@ProjectDetailsActivity) {
                            it.showDialog(state.dialogMsg, confirmText = "确定", confirmClick = {
                                it.dismiss()
                            })
                        }
                    }

                    ProjectDetailsDialogState.SUCCESS -> {
                        hiltDialog.showPop(this@ProjectDetailsActivity) {
                            it.showDialog(state.dialogMsg, confirmText = "确定", confirmClick = {
                                it.dismiss()
                                finishAfterTransition()
                            })
                        }
                    }
                }
            }
        }
    }
    private var onRight:OnClickListener = OnClickListener {
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


        vd.nav.setOnBack{
            finishAfterTransition()
        }
    }

    private fun initView() {

    }

    companion object {
        val ID = "id"
    }
}
