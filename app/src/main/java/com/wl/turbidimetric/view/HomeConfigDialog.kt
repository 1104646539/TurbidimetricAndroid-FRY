package com.wl.turbidimetric.view

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import com.wl.turbidimetric.R
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.isAuto
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.ProjectModel
import com.wl.wllib.LogToFile.i
/**
 * 首页设置检测参数对话框
 * @property context Context
 * @property selectProject ProjectModel? 选择的标曲
 * @property items MutableList<ProjectModel> 所有标曲
 * @constructor
 */
class HomeConfigDialog(val context: Context) : BaseDialog(context) {
    val spnProject: Spinner
    val etSkipNum: EditText
    val etDetectionNum: EditText
    val etSampleNum: EditText
    val llSampleNum: View

    var selectProject: ProjectModel? = null
    var projectAdapter: HomeProjectAdapter
    val items: MutableList<ProjectModel> = mutableListOf()

    init {
        addView(R.layout.dialog_home_config)
        spnProject = getView(R.id.spn_project) as Spinner
        etSkipNum = getView(R.id.et_skip) as EditText
        etDetectionNum = getView(R.id.et_start_num) as EditText
        etSampleNum = getView(R.id.et_sample_num) as EditText
        llSampleNum = getView(R.id.ll_sample_num)

        projectAdapter = HomeProjectAdapter(context, items)
        spnProject.adapter = projectAdapter
        dialogUtil.setWidthHeight(1400, WindowManager.LayoutParams.WRAP_CONTENT)
    }
    fun show(
        selectProjectEnable:Boolean,
        editDetectionNumEnable:Boolean,
        skipCuvetteEnable:Boolean,
        projectModels: MutableList<ProjectModel>,
        projectModel: ProjectModel?,
        skipNum: Int,
        detectionNum: String,
        sampleNum: Int,
        onConfirm: ((ProjectModel?, Int, String, Int, BaseDialog) -> Unit)? = null,
        onCancel: onClick,
    ) {

        super.show(
            "确定",
            { confirm(onConfirm) },
            "取消",
            { onCancel.invoke(it) },
            false
        )

        items.clear()
        items.addAll(projectModels)
        projectAdapter.notifyDataSetChanged()

        etSkipNum.setText(skipNum.toString())
        etDetectionNum.setText(detectionNum)
        etSampleNum.setText(sampleNum.toString())

        etSkipNum.selectionLast()
        etDetectionNum.selectionLast()
        etSampleNum.selectionLast()

        etSkipNum.isEnabled = skipCuvetteEnable
        spnProject.isEnabled = selectProjectEnable
        etDetectionNum.isEnabled = editDetectionNumEnable

        val selectedIndex = projectModels.indexOf(projectModel)
        spnProject.setSelection(selectedIndex)

        llSampleNum.visibility = if (isAuto()) View.GONE else View.VISIBLE

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


    }

    private fun confirm(onConfirm: ((ProjectModel?, Int, String, Int, BaseDialog) -> Unit)?) {
        if (selectProject == null) {
            toast("请选择标曲")
            return
        }
        var sampleNum: Int = (etSampleNum.text.trim().toString()).toIntOrNull() ?: 0
        var skipNum: Int = (etSkipNum.text.trim().toString()).toIntOrNull() ?: 0
        var detectionNum: String = etDetectionNum.text.trim().toString()

        if (!isAuto()) {
            if (sampleNum < 0 || sampleNum > 50) {
                sampleNum = 0
                toast("检测数量必须为1-50")
                return
            }
        }
        if (skipNum < 0 || skipNum > 9) {
            skipNum = 0
            toast("跳过比色皿必须为0-9")
            return
        }

        if (detectionNum.isNullOrEmpty()) {
            detectionNum = LocalData.DetectionNum
        }

        onConfirm?.invoke(selectProject, skipNum, detectionNum, sampleNum, this)
    }
}
