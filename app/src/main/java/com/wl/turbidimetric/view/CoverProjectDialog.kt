package com.wl.turbidimetric.view

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Spinner
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.ProjectModel

class CoverProjectDialog(val context: Context) : BaseDialog(context) {
    val spnProject: Spinner

    var selectProject: ProjectModel? = null
    lateinit var projectAdapter: HomeProjectAdapter
    val items: MutableList<ProjectModel> = mutableListOf()

    init {
        addView(R.layout.dialog_cover_project)
        spnProject = getView(R.id.spn_project) as Spinner
        projectAdapter = HomeProjectAdapter(context, items)
        spnProject.adapter = projectAdapter
        dialogUtil.setWidthHeight(1600, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    fun show(
        projectModels: MutableList<ProjectModel>,
        onConfirm: ((ProjectModel?, BaseDialog) -> Unit)? = null,
        onCancel: onClick
    ) {
        items.clear()
        items.addAll(projectModels)
        projectAdapter.notifyDataSetChanged()

        spnProject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectProject = projectModels?.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectProject = null
            }
        }
        super.show(
            "确定",
            { onConfirm?.invoke(selectProject, it) },
            "取消",
            { onCancel.invoke(it) },
            false
        )

    }
}
