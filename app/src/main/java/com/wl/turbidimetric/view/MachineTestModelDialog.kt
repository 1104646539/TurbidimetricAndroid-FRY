package com.wl.turbidimetric.view

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Spinner
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.model.ProjectModel

/**
 * 检测模式选择对话框
 * @property context Context
 * @constructor
 */
class MachineTestModelDialog(val context: Context) : BaseDialog(context) {
    var rbAuto: RadioButton
    var rbManual: RadioButton
    var cbSample: CheckBox
    var cbScanCode: CheckBox
    var llAuto: View

    init {
        addView(R.layout.dialog_machine_test_model)
        dialogUtil.setWidthHeight(1000, WindowManager.LayoutParams.WRAP_CONTENT)
        rbAuto = getView(R.id.rb_auto)
        rbManual = getView(R.id.rb_manual)
        cbSample = getView(R.id.cb_sample)
        cbScanCode = getView(R.id.cb_scan_code)
        llAuto = getView(R.id.ll_auto)
    }

    fun show(
        machineTestModel: MachineTestModel,
        sampleExist: Boolean,
        scanCode: Boolean,
        onConfirm: ((MachineTestModel, Boolean, Boolean, BaseDialog?) -> Unit),
        onCancel: onClick
    ) {
        super.show(
            "确定",
            { confirm(onConfirm) },
            "取消",
            { onCancel.invoke(it) },
            false
        )
        if (machineTestModel == MachineTestModel.Auto) {
            rbAuto.isChecked = true
        } else {
            rbManual.isChecked = true
        }

        rbAuto.setOnCheckedChangeListener { buttonView, isChecked ->
            llAuto.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        llAuto.visibility = if (rbAuto.isChecked) View.VISIBLE else View.GONE
        cbSample.isChecked = sampleExist
        cbScanCode.isChecked = scanCode

    }

    private fun confirm(onConfirm: (MachineTestModel, Boolean, Boolean, BaseDialog?) -> Unit) {
        var machineTestModel = MachineTestModel.Auto
        if (rbAuto.isChecked) {
            machineTestModel = MachineTestModel.Auto
        } else {
            machineTestModel = MachineTestModel.Manual
        }
        onConfirm.invoke(machineTestModel, cbSample.isChecked, cbScanCode.isChecked, this)
    }
}
