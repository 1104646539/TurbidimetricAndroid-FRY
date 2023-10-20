package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.media.Image
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.ProjectModel
import com.wl.wllib.LogToFile.i
import com.wl.wllib.toLongTimeStr
import java.util.*

class CoverProjectDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_cover_project) {
    var spnProject: Spinner? = null
    var selectProject: ProjectModel? = null

    var projectAdapter: HomeProjectAdapter? = null
    val items: MutableList<ProjectModel> = mutableListOf()

    fun projectStr(projectModel: ProjectModel): String {
        return String.format(
            "序号:%9s  检测时间:%s\n%-1.3f  %-1.3f  %-1.3f  %-1.3f  ",
            projectModel.reagentNO,
            projectModel.createTime,
            projectModel.f0,
            projectModel.f1,
            projectModel.f2,
            projectModel.f3
        )
    }

    override fun setContent() {
        super.setContent()
        if (projectAdapter == null) {
            projectAdapter = HomeProjectAdapter(context, items)
            spnProject?.adapter = projectAdapter
        } else {
            projectAdapter?.notifyDataSetChanged()
        }

        spnProject?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectProject = items?.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectProject = null
            }
        }
    }

    fun show(
        projectModels: MutableList<ProjectModel>,
        onConfirm: ((ProjectModel?, BasePopupView) -> Unit)? = null,
        onCancel: onClick
    ) {
        items.clear()
        items.addAll(projectModels)
//        projectAdapter.notifyDataSetChanged()

        this.confirmText = "确定"
        this.confirmClick = { onConfirm?.invoke(selectProject, it) }
        this.cancelText = "取消"
        this.cancelClick = onCancel
        super.show()
    }

    override fun initDialogView() {
        spnProject = findViewById(R.id.spn_project)
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
