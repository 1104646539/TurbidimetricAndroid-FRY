package com.wl.turbidimetric.view

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.widget.*
import com.github.gzuliyujiang.wheelpicker.DatimePicker
import com.github.gzuliyujiang.wheelpicker.annotation.DateMode
import com.github.gzuliyujiang.wheelpicker.annotation.TimeMode
import com.github.gzuliyujiang.wheelpicker.entity.DateEntity
import com.github.gzuliyujiang.wheelpicker.entity.DatimeEntity
import com.github.gzuliyujiang.wheelpicker.entity.TimeEntity
import com.wl.turbidimetric.R
import com.wl.turbidimetric.datamanager.SelectQueryAdapter
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.model.ConditionModel
import java.util.*

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
    private val tvTestTimeMin: TextView
    private val tvTestTimeMax: TextView
    private val resultAdapter: SelectQueryAdapter
    private val results = getResource().getStringArray(R.array.results)

    var testTimeMin = ""
    var testTimeMax = ""

    init {
        addView(R.layout.dialog_condition)
        dialogUtil.setWidthHeight(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT
        )
        etName = getView(R.id.et_name)
        etQRCode = getView(R.id.et_qrcode)
        etConMin = getView(R.id.et_con_min)
        etConMax = getView(R.id.et_con_max)
        spnResults = getView(R.id.spn_results)
        tvResults = getView(R.id.tv_spn_results)
        tvTestTimeMin = getView(R.id.tv_testtime_min)
        tvTestTimeMax = getView(R.id.tv_testtime_max)

        resultAdapter = SelectQueryAdapter(context, results)
        spnResults.adapter = resultAdapter

        resultAdapter.onItemSelectChange = { index, _ ->
            tvResults.text = resultAdapter.getSelectText()
        }

        tvTestTimeMin.setOnClickListener {
            val calendar =
                if (tvTestTimeMin.text.isNullOrEmpty()) Calendar.getInstance() else Calendar.getInstance()
                    .apply {
                        time = tvTestTimeMin.text.toString().longStrToDate()
                    }
            showSelectTimeDialog(calendar) { longStr, time ->
                tvTestTimeMin.text = longStr
                testTimeMin = longStr
            }
        }
        tvTestTimeMax.setOnClickListener {
            val calendar =
                if (tvTestTimeMax.text.isNullOrEmpty()) Calendar.getInstance() else Calendar.getInstance()
                    .apply {
                        time = tvTestTimeMax.text.toString().longStrToDate()
                    }
            showSelectTimeDialog(calendar) { longStr, time ->
                tvTestTimeMax.text = longStr
                testTimeMax = longStr
            }
        }
    }

    /**
     * 时间选择器
     * @param calendar Calendar
     * @param onDateSet Function2<[@kotlin.ParameterName] String, [@kotlin.ParameterName] Long, Unit>
     */
    private fun showSelectTimeDialog(
        calendar: Calendar = Calendar.getInstance(),
        onDateSet: (longStr: String, time: Long) -> Unit
    ) {
        val start = DatimeEntity().apply {
            date = DateEntity.target(Date(1262315415000L))
            time = TimeEntity.target(Date(1262315415000L))
        }
        val picker = DatimePicker(context as Activity)
        val wheelLayout = picker.wheelLayout
        picker.setOnDatimePickedListener { year, month, day, hour, minute, second ->
            calendar.set(year, month - 1, day)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, second)
            onDateSet(calendar.time.toLongString(), calendar.time.time)
//            var text = "$year-$month-$day $hour:$minute:$second"
//            text += if (wheelLayout.timeWheelLayout.isAnteMeridiem) " 上午" else " 下午"
//            toast(text)
        }
        wheelLayout.setDateMode(DateMode.YEAR_MONTH_DAY)
        wheelLayout.setTimeMode(TimeMode.HOUR_24_NO_SECOND)
        wheelLayout.setRange(start, DatimeEntity.yearOnFuture(50), DatimeEntity().apply {
            date = DateEntity.target(calendar)
            time = TimeEntity.target(calendar)
        })
        wheelLayout.setDateLabel("年", "月", "日")
        wheelLayout.setTimeLabel("时", "分", "秒")
        wheelLayout.setSelectedTextColor(getResource().getColor(R.color.themePositiveColor))
        wheelLayout.setSelectedTextSize(26f)
        wheelLayout.setSelectedTextBold(true)
        picker.show()
    }


    fun show(
        onConfirm: (ConditionModel) -> Unit?, onCancel: onClick?, isCancelable: Boolean
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


        super.show2("确定", {
            if (verify()) {
                onConfirm.invoke(getCondition())
            }
        }, "清空", {
            clearCondition()
//                resultAdapter.clearSelectedInfo()
        }, "取消", onCancel, isCancelable
        )
    }

    /**
     *
     * @return Boolean
     */
    private fun verify(): Boolean {
        if (testTimeMin.isNotEmpty() && testTimeMax.isNotEmpty()) {
            if (testTimeMin.strToLong() > testTimeMax.strToLong()) {
                toast("检测时间最大值不能小于最小值")
                return false
            }
        }
        return true
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
        tvTestTimeMin.setText("")
        tvTestTimeMax.setText("")
        testTimeMin = ""
        testTimeMax = ""
    }

    private fun getCondition(): ConditionModel {
        val name = etName.text.toString().ifEmpty { "" }
        val qrcode = etQRCode.text.toString().ifEmpty { "" }
        val testTimeMax = if (testTimeMax.isEmpty()) 0 else testTimeMax.strToLong()
        val testTimeMin = if (testTimeMin.isEmpty()) 0 else testTimeMin.strToLong()

        return ConditionModel(
            name = name,
            qrcode = qrcode,
            conMin = etConMin.text.toString().toIntOrNull() ?: 0,
            conMax = etConMax.text.toString().toIntOrNull() ?: 0,
            results = if (resultAdapter.getSelectItemsValue() == null) arrayOf() else resultAdapter.getSelectItemsValue()!!,
            testTimeMin = testTimeMin,
            testTimeMax = testTimeMax,
        )
    }


}
