package com.wl.turbidimetric.view.dialog

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
import com.wl.wllib.longStrToDate
import com.wl.wllib.longStrToLong
import com.wl.wllib.toLongTimeStr
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
open class ConditionDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_condition) {
    var spnResults: Spinner? = null
    var etName: EditText? = null
    var etQRCode: EditText? = null
    var etConMin: EditText? = null
    var etConMax: EditText? = null
    var tvResults: TextView? = null
    var tvTestTimeMin: TextView? = null
    var tvTestTimeMax: TextView? = null
    var resultAdapter: SelectQueryAdapter? = null
    var results = getResource().getStringArray(R.array.results)

    var testTimeMin = ""
    var testTimeMax = ""

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
            onDateSet(calendar.time.toLongTimeStr(), calendar.time.time)
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

    override fun setContent() {
        super.setContent()

        if (resultAdapter == null) {
            resultAdapter = SelectQueryAdapter(context, results)
            spnResults?.adapter = resultAdapter
        }

        resultAdapter?.onItemSelectChange = { index, _ ->
            tvResults?.text = resultAdapter?.getSelectText()
        }

        tvTestTimeMin?.setOnClickListener {
            val calendar =
                if (tvTestTimeMin?.text.isNullOrEmpty()) Calendar.getInstance() else Calendar.getInstance()
                    .apply {
                        time = tvTestTimeMin?.text.toString().longStrToDate()
                    }
            showSelectTimeDialog(calendar) { longStr, time ->
                tvTestTimeMin?.text = longStr
                testTimeMin = longStr
            }
        }
        tvTestTimeMax?.setOnClickListener {
            val calendar =
                if (tvTestTimeMax?.text.isNullOrEmpty()) Calendar.getInstance() else Calendar.getInstance()
                    .apply {
                        time = tvTestTimeMax?.text.toString().longStrToDate()
                    }
            showSelectTimeDialog(calendar) { longStr, time ->
                tvTestTimeMax?.text = longStr
                testTimeMax = longStr
            }
        }

    }

    fun showDialog(
        onConfirm: (ConditionModel) -> Unit?, onCancel: onClick?
    ) {
        this.confirmText = "确定"
        this.confirmClick = {
            if (verify()) {
                onConfirm.invoke(getCondition())
            }
        }
        this.confirmText2 = "清空"
        this.confirmClick2 = {
            clearCondition()
        }
        this.cancelText = "取消"
        this.cancelClick = onCancel

        super.show()
    }

    /**
     *
     * @return Boolean
     */
    private fun verify(): Boolean {
        if (testTimeMin.isNotEmpty() && testTimeMax.isNotEmpty()) {
            if (testTimeMin.longStrToLong() > testTimeMax.longStrToLong()) {
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
        etName?.setText("")
        etQRCode?.setText("")
        etConMin?.setText("")
        etConMax?.setText("")
        tvResults?.setText("请选择")
        resultAdapter?.clearSelectedInfo()
        tvTestTimeMin?.setText("")
        tvTestTimeMax?.setText("")
        testTimeMin = ""
        testTimeMax = ""
    }

    private fun getCondition(): ConditionModel {
        val name = etName?.text.toString().ifEmpty { "" }
        val qrcode = etQRCode?.text.toString().ifEmpty { "" }
        val testTimeMax = if (testTimeMax.isEmpty()) 0 else testTimeMax.longStrToLong()
        val testTimeMin = if (testTimeMin.isEmpty()) 0 else testTimeMin.longStrToLong()

        return ConditionModel(
            name = name,
            qrcode = qrcode,
            conMin = etConMin?.text.toString().toIntOrNull() ?: 0,
            conMax = etConMax?.text.toString().toIntOrNull() ?: 0,
            results = if (resultAdapter?.getSelectItemsValue() == null) arrayOf() else resultAdapter?.getSelectItemsValue()!!,
            testTimeMin = testTimeMin,
            testTimeMax = testTimeMax,
        )
    }

    override fun initDialogView() {
        etName = findViewById(R.id.et_name)
        etQRCode = findViewById(R.id.et_qrcode)
        etConMin = findViewById(R.id.et_con_min)
        etConMax = findViewById(R.id.et_con_max)
        spnResults = findViewById(R.id.spn_results)
        tvResults = findViewById(R.id.tv_spn_results)
        tvTestTimeMin = findViewById(R.id.tv_testtime_min)
        tvTestTimeMax = findViewById(R.id.tv_testtime_max)


    }


}
