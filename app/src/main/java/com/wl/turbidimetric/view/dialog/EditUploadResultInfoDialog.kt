package com.wl.turbidimetric.view.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.github.gzuliyujiang.wheelpicker.DatimePicker
import com.github.gzuliyujiang.wheelpicker.annotation.DateMode
import com.github.gzuliyujiang.wheelpicker.annotation.TimeMode
import com.github.gzuliyujiang.wheelpicker.entity.DateEntity
import com.github.gzuliyujiang.wheelpicker.entity.DatimeEntity
import com.github.gzuliyujiang.wheelpicker.entity.TimeEntity
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.wllib.DateUtil
import com.wl.wllib.toDate
import com.wl.wllib.toLongTimeStr
import com.wl.wllib.toTimeStr
import java.util.Calendar
import java.util.Date

/**
 * 上传发送编辑测试结果对话框
 * @property context Context
 * @property selectProject ProjectModel? 选择的标曲
 * @property items MutableList<ProjectModel> 所有标曲
 * @constructor
 */
class EditUploadResultInfoDialog(val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_upload_edit_result_info) {
    var spnProject: Spinner? = null
    var spnResult: Spinner? = null
    var spnSex: Spinner? = null

    var etName: EditText? = null
    var etAge: EditText? = null
    var etDetectionNum: EditText? = null
    var etBarcode: EditText? = null
    var etAbs: EditText? = null
    var etCon: EditText? = null
    var etDeliveryDoctor: EditText? = null
    var etDeliveryDepartment: EditText? = null

    var tvTestTime: TextView? = null
    var tvDeliveryTime: TextView? = null

    var projects: List<ProjectModel> = mutableListOf()
    var projectIndex = -1
    var projectsAdapter: SpnSampleAdapter? = null
    fun showDialog(
        projects: List<ProjectModel>,
        onConfirm: ((name: String, age: String, detectionNum: String, barcode: String, abs: String, con: String, deliveryDoctor: String, deliveryDepartment: String, deliveryTime: String, testTime: String, project: ProjectModel, sex: String, result: String, BasePopupView) -> Unit)? = null,
        onCancel: onClick,
    ) {
        this.projects = projects

        this.confirmText = "确定"
        this.confirmClick = {
            onConfirm?.invoke(
                etName?.text?.toString() ?: "",
                etAge?.text?.toString() ?: "",
                etDetectionNum?.text?.toString() ?: "",
                etBarcode?.text?.toString() ?: "",
                etAbs?.text?.toString() ?: "",
                etCon?.text?.toString() ?: "",
                etDeliveryDoctor?.text?.toString() ?: "",
                etDeliveryDepartment?.text?.toString() ?: "",
                tvDeliveryTime?.text?.toString() ?: "",
                tvTestTime?.text?.toString() ?: "",
                projects.getOrNull(projectIndex) ?: projects.first(),
                spnSex?.selectedItem.toString() ?: "男",
                spnResult?.selectedItem.toString() ?: "阴性", it
            )
        }
        this.cancelText = "取消"
        this.cancelClick = { onCancel.invoke(it) }


//        if (isCreated) {
//            setContent()
//        }
        super.show()
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
            onDateSet(calendar.time.toTimeStr(DateUtil.Time4Format), calendar.time.time)
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

        tvTestTime?.setOnClickListener {
            val calendar =
                if (tvTestTime?.text.isNullOrEmpty()) Calendar.getInstance() else Calendar.getInstance()
                    .apply {
                        time = tvTestTime?.text.toString().toDate(DateUtil.Time4Format)
                    }
            showSelectTimeDialog(calendar) { longStr, time ->
                tvTestTime?.text = longStr
            }
        }
        tvDeliveryTime?.setOnClickListener {
            val calendar =
                if (tvDeliveryTime?.text.isNullOrEmpty()) Calendar.getInstance() else Calendar.getInstance()
                    .apply {
                        time = tvDeliveryTime?.text.toString().toDate(DateUtil.Time4Format)
                    }
            showSelectTimeDialog(calendar) { longStr, time ->
                tvDeliveryTime?.text = longStr
            }
        }
        spnProject?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                projectIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        etName?.setText("张三")
        etAge?.setText("45")
        etDetectionNum?.setText("6")
        etBarcode?.setText("ABCDEF")
        etAbs?.setText("122")
        etCon?.setText("60")
        etDeliveryDoctor?.setText("张医生")
        etDeliveryDepartment?.setText("体检科")

        spnSex?.setSelection(0)
        spnProject?.setSelection(0)
        spnResult?.setSelection(0)

        tvTestTime?.setText(Date().time.toTimeStr())
        tvDeliveryTime?.setText(Date().time.toTimeStr())
    }


    override fun initDialogView() {
        spnProject = findViewById(R.id.spn_project)
        spnResult = findViewById(R.id.spn_result)
        spnSex = findViewById(R.id.spn_sex)

        etName = findViewById(R.id.et_name)
        etAge = findViewById(R.id.et_age)
        etDetectionNum = findViewById(R.id.et_sn)
        etBarcode = findViewById(R.id.et_bc)
        etAbs = findViewById(R.id.et_abs)
        etCon = findViewById(R.id.et_con)
        etDeliveryDoctor = findViewById(R.id.et_delivery_doctor)
        etDeliveryDepartment = findViewById(R.id.et_delivery_department)

        tvTestTime = findViewById(R.id.tv_test_time)
        tvDeliveryTime = findViewById(R.id.tv_delivery_time)

        val projectNames = projects.map { it.projectName }.toMutableList()
        projectsAdapter = SpnSampleAdapter(context, projectNames)
        spnProject?.adapter = projectsAdapter

        val sexs = context.resources.getStringArray(R.array.sexs)
        spnSex?.adapter = SpnSampleAdapter(context, sexs.toMutableList())

        val results = context.resources.getStringArray(R.array.results)
        spnResult?.adapter = SpnSampleAdapter(context, results.toMutableList())

    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
