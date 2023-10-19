package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.WindowManager
import android.widget.EditText
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.isNum
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast

/**
 * 参数设置对话框
 * @property context Context
 * @property etTakeR1 EditText
 * @property etTakeR2 EditText
 * @property etSampling EditText
 * @property etSamplingProbeCleaningTime EditText
 * @property etStirProbeCleaningTime EditText
 * @constructor
 */
class ParamsDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_params_settings) {
    var etTakeR1: EditText? = null
    var etTakeR2: EditText? = null
    var etSampling: EditText? = null
    var etSamplingProbeCleaningTime: EditText? = null
    var etStirProbeCleaningTime: EditText? = null

    var takeR1: Int? = null
    var takeR2: Int? = null
    var sampling: Int? = null
    var samplingProbeCleaningTime: Int? = null
    var stirProbeCleaningTime: Int? = null
    fun showDialog(
        takeR1: Int,
        takeR2: Int,
        sampling: Int,
        samplingProbeCleaningTime: Int,
        stirProbeCleaningTime: Int,
        onConfirm: ((Int, Int, Int, Int, Int, BasePopupView) -> Unit)?,
        onCancel: onClick?
    ) {
        this.takeR1 = takeR1
        this.takeR2 = takeR2
        this.sampling = sampling
        this.samplingProbeCleaningTime = samplingProbeCleaningTime
        this.stirProbeCleaningTime = stirProbeCleaningTime
        this.confirmText = "确定"
        this.confirmClick = {
            confirm(onConfirm)
        }
        this.cancelText = "取消"
        this.cancelClick = onCancel
        super.show()

    }

    fun confirm(onConfirm: ((Int, Int, Int, Int, Int, BasePopupView) -> Unit)?) {
        val takeR1 = etTakeR1?.text.toString()
        val takeR2 = etTakeR2?.text.toString()
        val sampling = etSampling?.text.toString()
        val samplingProbeCleaningTime = etSamplingProbeCleaningTime?.text.toString()
        val stirProbeCleaningTime = etStirProbeCleaningTime?.text.toString()

        if (takeR1.trim().isNullOrEmpty() || takeR2.trim().isNullOrEmpty() || sampling.trim()
                .isNullOrEmpty() || samplingProbeCleaningTime.trim()
                .isNullOrEmpty() || stirProbeCleaningTime.trim()
                .isNullOrEmpty()
        ) {
            toast("请输入数字")
            return
        }
        if (!takeR1.isNum() || !takeR2.isNum() || !sampling.isNum()) {
            toast("请输入数字")
            return
        }
        onConfirm?.invoke(
            takeR1.toIntOrNull() ?: 0,
            takeR2.toIntOrNull() ?: 0,
            sampling.toIntOrNull() ?: 0,
            samplingProbeCleaningTime.toIntOrNull() ?: 0,
            stirProbeCleaningTime.toIntOrNull() ?: 0, this
        )
    }

    override fun initDialogView() {
        etTakeR1 = findViewById(R.id.et_take_r1)
        etTakeR2 = findViewById(R.id.et_take_r2)
        etSampling = findViewById(R.id.et_sampling)
        etSamplingProbeCleaningTime = findViewById(R.id.et_sampling_probe_cleaning_time)
        etStirProbeCleaningTime = findViewById(R.id.et_stir_probe_cleaning_time)
    }

    override fun setContent() {
        super.setContent()
        etTakeR1?.setText(takeR1.toString())
        etTakeR2?.setText(takeR2.toString())
        etSampling?.setText(sampling.toString())
        etSamplingProbeCleaningTime?.setText(samplingProbeCleaningTime.toString())
        etStirProbeCleaningTime?.setText(stirProbeCleaningTime.toString())

        etTakeR1?.selectionLast()
        etTakeR2?.selectionLast()
        etSampling?.selectionLast()
        etSamplingProbeCleaningTime?.selectionLast()
        etStirProbeCleaningTime?.selectionLast()
    }
}
