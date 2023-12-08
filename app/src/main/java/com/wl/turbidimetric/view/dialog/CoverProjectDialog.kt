package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.CurveModel
import java.util.*

class CoverProjectDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_cover_project) {
    var spnProject: Spinner? = null
    var selectProject: CurveModel? = null

    var projectAdapter: HomeProjectAdapter? = null
    val items: MutableList<CurveModel> = mutableListOf()

    fun projectStr(curveModel: CurveModel): String {
        return String.format(
            "序号:%9s  检测时间:%s\n%-1.3f  %-1.3f  %-1.3f  %-1.3f  ",
            curveModel.reagentNO,
            curveModel.createTime,
            curveModel.f0,
            curveModel.f1,
            curveModel.f2,
            curveModel.f3
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
        spnProject?.let { spn ->
            if(!items.contains(spn.selectedItem)){
                spn.setSelection(0)
            }
        }
        spnProject?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectProject = items.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectProject = null
            }
        }
    }

    fun show(
        curveModels: MutableList<CurveModel>,
        onConfirm: ((CurveModel?, BasePopupView) -> Unit)? = null,
        onCancel: onClick
    ) {
        items.clear()
        items.addAll(curveModels)
//        projectAdapter.notifyDataSetChanged()

        this.confirmText = "确定"
        this.confirmClick = { onConfirm?.invoke(selectProject, it) }
        this.cancelText = "取消"
        this.cancelClick = onCancel
        if(isCreated){
            setContent()
        }
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
