package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.MachineTestModel

/**
 * 检测模式选择对话框
 * @property context Context
 * @constructor
 */
class MachineTestModelDialog(val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_machine_test_model) {
    var rbAuto: RadioButton? = null
    var rbManual: RadioButton? = null
    var rbManualSampling: RadioButton? = null
    var cbSample: CheckBox? = null
    var cbScanCode: CheckBox? = null
    var llAuto: View? = null

    var machineTestModel: MachineTestModel? = null
    var sampleExist: Boolean? = null
    var scanCode: Boolean? = null
    fun showDialog(
        machineTestModel: MachineTestModel,
        sampleExist: Boolean,
        scanCode: Boolean,
        onConfirm: ((MachineTestModel, Boolean, Boolean, BasePopupView?) -> Unit),
        onCancel: onClick
    ) {
        this.machineTestModel = machineTestModel
        this.sampleExist = sampleExist
        this.scanCode = scanCode
        this.confirmText = "确定"
        this.confirmClick = { confirm(onConfirm) }
        this.cancelText = "取消"
        this.cancelClick = onCancel
        if (isCreated) {
            setContent()
        }
        super.show()
    }

    private fun confirm(onConfirm: (MachineTestModel, Boolean, Boolean, BasePopupView?) -> Unit) {
        val machineTestModel = if (rbAuto?.isChecked == true) {
            MachineTestModel.Auto
        } else if (rbManual?.isChecked == true) {
            MachineTestModel.Manual
        } else {
            MachineTestModel.ManualSampling
        }
        onConfirm.invoke(
            machineTestModel,
            cbSample?.isChecked == true,
            cbScanCode?.isChecked == true,
            this
        )
    }

    override fun initDialogView() {
        rbAuto = findViewById(R.id.rb_auto)
        rbManual = findViewById(R.id.rb_manual)
        rbManualSampling = findViewById(R.id.rb_manual_sampling)
        cbSample = findViewById(R.id.cb_sample)
        cbScanCode = findViewById(R.id.cb_scan_code)
        llAuto = findViewById(R.id.ll_auto)
    }

    override fun setContent() {
        super.setContent()

        if (machineTestModel == MachineTestModel.Auto) {
            rbAuto?.isChecked = true
        } else if (machineTestModel == MachineTestModel.Manual) {
            rbManual?.isChecked = true
        } else {
            rbManualSampling?.isChecked = true
        }

        rbAuto?.setOnCheckedChangeListener { buttonView, isChecked ->
            llAuto?.visibility = isChecked.isShow()
        }
        llAuto?.visibility = rbAuto?.isChecked.isShow()
        cbSample?.isChecked = sampleExist == true
        cbScanCode?.isChecked = scanCode == true
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
