package com.wl.turbidimetric.view

import android.content.Context
import android.view.WindowManager
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.datamanager.SelectQueryAdapter
import com.wl.turbidimetric.ex.PD
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.model.ConditionModel
import kotlinx.coroutines.flow.flow

/**
 * 数据管理 筛选条件的对话框
 * @property spnResult Spinner
 * @property etName EditText
 * @property etQRCode EditText
 * @property etConMin EditText
 * @property etConMax EditText
 * @property resultAdapter SpinnerAdapter
 * @constructor
 */
class ConditionDialog(val context: Context) : BaseDialog(context) {
    private val spnResults: Spinner
    private val etName: EditText
    private val etQRCode: EditText
    private val etConMin: EditText
    private val etConMax: EditText
    private val tvResults: TextView
    private val resultAdapter: SelectQueryAdapter
    private val results = getResource().getStringArray(R.array.results)

    init {
        addView(R.layout.dialog_condition)
        dialogUtil.setWidthHeight(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        etName = getView(R.id.et_name)
        etQRCode = getView(R.id.et_qrcode)
        etConMin = getView(R.id.et_con_min)
        etConMax = getView(R.id.et_con_max)
        spnResults = getView(R.id.spn_results)
        tvResults = getView(R.id.tv_spn_results)

        resultAdapter = SelectQueryAdapter(context, results)
        spnResults.adapter = resultAdapter

        resultAdapter.onItemSelectChange = { index, _ ->
            tvResults.text = resultAdapter.getSelectText()
        }
    }

    fun show(
        onConfirm: (ConditionModel) -> Unit?,
        onCancel: onClick?,
        isCancelable: Boolean
    ) {
//        if (conditionModel.name.isNotEmpty()) {
//            etName.setText(conditionModel.name)
//        }
//        if (conditionModel.qrcode.isNotEmpty()) {
//            etQRCode.setText(conditionModel.qrcode)
//        }
//        if (conditionModel.conMin.isNotEmpty()) {
//            etConMin.setText(conditionModel.conMin)
//        }
//        if (conditionModel.conMax.isNotEmpty()) {
//            etConMax.setText(conditionModel.conMax)
//        }
//        if (conditionModel.results.isNotEmpty()) {
////            val index = results.indexOfFirst { it == conditionModel.conMax }
////            spnResults.setSelection(results.indexOf(conditionModel.results))
//        }


        super.show2(
            "确定",
            { onConfirm.invoke(getCondition()) },
            "清空",
            {
                clearCondition()
//                resultAdapter.clearSelectedInfo()
            },
            "取消",
            onCancel,
            isCancelable
        )
    }

    /**
     * 清空所有条件
     */
    private fun clearCondition() {
        etName.setText("")
        etQRCode.setText("")
        etConMin.setText("")
        etConMax.setText("")
        tvResults.setText("请选择")
        resultAdapter.clearSelectedInfo()

    }

    private fun getCondition(): ConditionModel {
        return ConditionModel(
            name = etName.text.toString().isNullOrEmpty().PD(etName.text.toString(), ""),
            qrcode = etQRCode.text.toString().isNullOrEmpty().PD(etQRCode.text.toString(), ""),
            conMin = etConMin.text.toString().toIntOrNull() ?: 0,
            conMax = etConMax.text.toString().toIntOrNull() ?: 0,
            results = if (resultAdapter.getSelectItemsValue() == null) arrayOf() else resultAdapter.getSelectItemsValue()!!
        )
    }



}
