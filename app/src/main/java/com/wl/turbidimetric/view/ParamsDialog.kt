package com.wl.turbidimetric.view

import android.content.Context
import android.view.WindowManager
import android.widget.EditText
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
class ParamsDialog(val context: Context) : BaseDialog(context) {
    val etTakeR1: EditText
    val etTakeR2: EditText
    val etSampling: EditText
    val etSamplingProbeCleaningTime: EditText
    val etStirProbeCleaningTime: EditText

    init {
        addView(R.layout.dialog_params_settings)
        dialogUtil.setWidthHeight(1000, WindowManager.LayoutParams.WRAP_CONTENT)

        etTakeR1 = getView(R.id.et_take_r1)
        etTakeR2 = getView(R.id.et_take_r2)
        etSampling = getView(R.id.et_sampling)
        etSamplingProbeCleaningTime = getView(R.id.et_sampling_probe_cleaning_time)
        etStirProbeCleaningTime = getView(R.id.et_stir_probe_cleaning_time)
    }

    fun show(
        takeR1: Int,
        takeR2: Int,
        sampling: Int,
        samplingProbeCleaningTime: Int,
        stirProbeCleaningTime: Int,
        onConfirm: ((Int, Int, Int, Int, Int, BaseDialog) -> Unit)?,
        onCancel: onClick?
    ) {
        super.show("确定", {
            val takeR1 = etTakeR1.text.toString()
            val takeR2 = etTakeR2.text.toString()
            val sampling = etSampling.text.toString()
            val samplingProbeCleaningTime = etSamplingProbeCleaningTime.text.toString()
            val stirProbeCleaningTime = etStirProbeCleaningTime.text.toString()

            if (takeR1.trim().isNullOrEmpty() || takeR2.trim().isNullOrEmpty() || sampling.trim()
                    .isNullOrEmpty() || samplingProbeCleaningTime.trim()
                    .isNullOrEmpty() || stirProbeCleaningTime.trim()
                    .isNullOrEmpty()
            ) {
                toast("请输入数字")
                return@show
            }
            if (!takeR1.isNum() || !takeR2.isNum() || !sampling.isNum()) {
                toast("请输入数字")
                return@show
            }
            onConfirm?.invoke(
                takeR1.toIntOrNull() ?: 0,
                takeR2.toIntOrNull() ?: 0,
                sampling.toIntOrNull() ?: 0,
                samplingProbeCleaningTime.toIntOrNull() ?: 0,
                stirProbeCleaningTime.toIntOrNull() ?: 0, this
            )

        }, "取消", onCancel, false)
        etTakeR1.setText(takeR1.toString())
        etTakeR2.setText(takeR2.toString())
        etSampling.setText(sampling.toString())
        etSamplingProbeCleaningTime.setText(samplingProbeCleaningTime.toString())
        etStirProbeCleaningTime.setText(stirProbeCleaningTime.toString())

        etTakeR1.selectionLast()
        etTakeR2.selectionLast()
        etSampling.selectionLast()
        etSamplingProbeCleaningTime.selectionLast()
        etStirProbeCleaningTime.selectionLast()
    }
}
