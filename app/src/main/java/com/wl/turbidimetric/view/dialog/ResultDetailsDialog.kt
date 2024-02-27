package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.matchingargs.MatchingConfigSampleAdapter
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
    var etDetectionNum: EditText? = null
    var tvTestTime: TextView? = null
    var etName: EditText? = null
    var spnGender: Spinner? = null
    var etAbs: EditText? = null
    var etCon: EditText? = null
    var etResult: EditText? = null
    var etSampleBarcode: EditText? = null


    var result: TestResultAndCurveModel? = null
    open fun showDialog(
        result: TestResultAndCurveModel,
        isDebug:Boolean,
        onConfirm: (result: TestResultAndCurveModel) -> Boolean,
    ) {
        this.confirmText = "确定"
        this.confirmClick = {
            result.result.name = etName?.text.toString()
            result.result.gender = spnGender?.selectedItem as String
            result.result.absorbances =
                (etAbs?.text.toString().toDoubleOrNull() ?: 0.0).toBigDecimal()
            result.result.concentration = etCon?.text.toString().toIntOrNull() ?: 0
            result.result.testResult = etResult?.text.toString()
            result.result.sampleBarcode = etSampleBarcode?.text.toString()
            dismiss().takeIf {
                onConfirm.invoke(result)
            }
        }
        this.cancelText = "取消"
        this.cancelClick = { dismiss() }

        this.result = result

        if (isCreated) {
            setContent()
        }
        etDetectionNum?.isEnabled = isDebug
        etAbs?.isEnabled = isDebug
        etCon?.isEnabled = isDebug
        etResult?.isEnabled = false
        super.show()
    }

    val sexs = resources.getStringArray(R.array.sexs)
    override fun initDialogView() {
        tvID = findViewById(R.id.tv_id)
        etDetectionNum = findViewById(R.id.et_detectionNum)
        tvTestTime = findViewById(R.id.tv_test_time)
        etName = findViewById(R.id.et_name)
        spnGender = findViewById(R.id.spn_gender)
        etAbs = findViewById(R.id.et_abs)
        etCon = findViewById(R.id.et_con)
        etResult = findViewById(R.id.et_result)
        etSampleBarcode = findViewById(R.id.et_sample_barcode)


        spnGender?.adapter = MatchingConfigSampleAdapter(rootView.context, sexs.toMutableList())
    }

    override fun setContent() {
        super.setContent()
        tvID?.text = result?.result?.resultId.toString()
        etDetectionNum?.setText(result?.result?.detectionNum)
        tvTestTime?.text = result?.result?.testTime?.toTimeStr()

        etName?.setText(result?.result?.name)
        etAbs?.setText(result?.result?.absorbances?.toInt().toString())
        etCon?.setText(result?.result?.concentration.toString())
        etResult?.setText(result?.result?.testResult)
        etSampleBarcode?.setText(result?.result?.sampleBarcode)

        sexs.indexOf(result?.result?.gender).let { index ->
            if (index > -1 && index in sexs.indices) {
                spnGender?.setSelection(index)
            } else {
                spnGender?.setSelection(0)//没有设置性别默认为- 未知
            }
        }
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
