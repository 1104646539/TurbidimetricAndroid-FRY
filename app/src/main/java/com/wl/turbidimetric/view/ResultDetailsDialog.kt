package com.wl.turbidimetric.view

import android.content.Context
import android.widget.Button
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.TestResultModel
import com.wl.wllib.toTimeStr

/**
 * 编辑检测结果的对话框
 * @property context Context
 * @property tvID TextView
 * @property tvDetectionNum TextView
 * @property tvTestTime TextView
 * @property etName TextView
 * @property etGender TextView
 * @property etAbs TextView
 * @property etCon TextView
 * @property etResult TextView
 * @constructor
 */
class ResultDetailsDialog(val context: Context) : BaseDialog(context) {
    private val tvID: TextView
    private val tvDetectionNum: TextView
    private val tvTestTime: TextView
    private val etName: TextView
    private val etGender: TextView
    private val etAbs: TextView
    private val etCon: TextView
    private val etResult: TextView


    init {
        addView(R.layout.dialog_result_details)
        tvID = getView(R.id.tvID) as TextView
        tvDetectionNum = getView(R.id.tvDetectionNum) as TextView
        tvTestTime = getView(R.id.tvTestTime) as TextView
        etName = getView(R.id.etName) as TextView
        etGender = getView(R.id.etGender) as TextView
        etAbs = getView(R.id.etAbs) as TextView
        etCon = getView(R.id.etCon) as TextView
        etResult = getView(R.id.etResult) as TextView
    }

    open fun show(
        result: TestResultModel,
        onConfirm: (result: TestResultModel) -> Boolean,
    ) {
        super.show("确定", {}, "取消", { dismiss() }, true)
        tvID.text = result.id.toString()
        tvDetectionNum.text = result.detectionNum
        tvTestTime.text = result.testTime.toTimeStr()

        etName.text = result.name
        etGender.text = result.gender
        etAbs.text = result.absorbances.toString()
        etCon.text = result.concentration.toString()
        etResult.text = result.testResult


        btnConfirm.setOnClickListener {
            result.name = etName.text.toString()
            result.gender = etGender.text.toString()
            result.absorbances = (etAbs.text.toString().toDoubleOrNull() ?: 0.0).toBigDecimal()
            result.concentration = etCon.text.toString().toIntOrNull() ?: 0
            result.testResult = etResult.text.toString()
            dismiss().takeIf {
                onConfirm.invoke(result)
            }
        }
    }
}
