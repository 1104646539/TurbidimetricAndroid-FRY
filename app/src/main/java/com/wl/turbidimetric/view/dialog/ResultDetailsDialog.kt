package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.TestResultAndCurveModel
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
class ResultDetailsDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_result_details) {
    var tvID: TextView? = null
    var tvDetectionNum: TextView? = null
    var tvTestTime: TextView? = null
    var etName: TextView? = null
    var etGender: TextView? = null
    var etAbs: TextView? = null
    var etCon: TextView? = null
    var etResult: TextView? = null
    var etSampleBarcode: TextView? = null


    var result: TestResultAndCurveModel? = null
    open fun showDialog(
        result: TestResultAndCurveModel,
        onConfirm: (result: TestResultAndCurveModel) -> Boolean,
    ) {
        this.confirmText = "确定"
        this.confirmClick = {
            result?.result?.name = etName?.text.toString()
            result?.result?.gender = etGender?.text.toString()
            result?.result?.absorbances = (etAbs?.text.toString().toDoubleOrNull() ?: 0.0).toBigDecimal()
            result?.result?.concentration = etCon?.text.toString().toIntOrNull() ?: 0
            result?.result?.testResult = etResult?.text.toString()
            result?.result?.sampleBarcode = etSampleBarcode?.text.toString()
            dismiss().takeIf {
                onConfirm.invoke(result)
            }
        }
        this.cancelText = "取消"
        this.cancelClick = { dismiss() }

        this.result = result

        if(isCreated){
            setContent()
        }
        super.show()
    }

    override fun initDialogView() {
        tvID = findViewById(R.id.tvID)
        tvDetectionNum = findViewById(R.id.tvDetectionNum)
        tvTestTime = findViewById(R.id.tvTestTime)
        etName = findViewById(R.id.etName)
        etGender = findViewById(R.id.etGender)
        etAbs = findViewById(R.id.etAbs)
        etCon = findViewById(R.id.etCon)
        etResult = findViewById(R.id.etResult)
        etSampleBarcode = findViewById(R.id.etSampleBarcode)
    }

    override fun setContent() {
        super.setContent()
        tvID?.text = result?.result?.resultId.toString()
        tvDetectionNum?.text = result?.result?.detectionNum
        tvTestTime?.text = result?.result?.testTime?.toTimeStr()

        etName?.text = result?.result?.name
        etGender?.text = result?.result?.gender
        etAbs?.text = result?.result?.absorbances.toString()
        etCon?.text = result?.result?.concentration.toString()
        etResult?.text = result?.result?.testResult
        etSampleBarcode?.text = result?.result?.sampleBarcode
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
