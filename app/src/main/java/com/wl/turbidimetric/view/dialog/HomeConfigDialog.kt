package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.isAuto
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.CurveModel

/**
 * 首页设置检测参数对话框
 * @property context Context
 * @property selectProject ProjectModel? 选择的标曲
 * @property items MutableList<ProjectModel> 所有标曲
 * @constructor
 */
class HomeConfigDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_home_config) {
    var spnProject: Spinner? = null
    var etSkipNum: EditText? = null
    var etDetectionNum: EditText? = null
    var etSampleNum: EditText? = null
    var llSampleNum: View? = null
    var swBanSampling: Switch? = null
    var etBanSamplingNum: EditText? = null
    var llBanSamplingNum: View? = null

    var selectProject: CurveModel? = null
    var projectAdapter: HomeProjectAdapter? = null
    val items: MutableList<CurveModel> = mutableListOf()

    var configViewEnable: Boolean? = null
    var curveModel: CurveModel?? = null
    var skipNum: Int? = null
    var detectionNum: String? = null
    var sampleNum: Int? = null
    var banSampling: Boolean? = null
    var banSamplingNum: Int? = null

    fun showDialog(
        configViewEnable: Boolean,
        curveModels: MutableList<CurveModel>,
        curveModel: CurveModel?,
        skipNum: Int,
        detectionNum: String,
        sampleNum: Int,
        banSampling: Boolean,
        banSamplingNum: Int,
        onConfirm: ((CurveModel?, Int, String, Int, Boolean, Int, BasePopupView) -> Unit)? = null,
        onCancel: onClick,
    ) {
        this.confirmText = "确定"
        this.confirmClick = { confirm(onConfirm) }
        this.cancelText = "取消"
        this.cancelClick = { onCancel.invoke(it) }

        this.configViewEnable = configViewEnable
        this.curveModel = curveModel
        this.skipNum = skipNum
        this.detectionNum = detectionNum
        this.sampleNum = sampleNum
        this.banSampling = banSampling
        this.banSamplingNum = banSamplingNum

        items.clear()
        items.addAll(curveModels)
//        projectAdapter?.notifyDataSetChanged()
        if (isCreated) {
            setContent()
        }
        super.show()
    }

    override fun setContent() {
        super.setContent()

        etSkipNum?.setText(skipNum.toString())
        etDetectionNum?.setText(detectionNum)
        etSampleNum?.setText(sampleNum.toString())
        etBanSamplingNum?.setText(banSamplingNum.toString())

        etSkipNum?.selectionLast()
        etDetectionNum?.selectionLast()
        etSampleNum?.selectionLast()
        etBanSamplingNum?.selectionLast()

        etSkipNum?.isEnabled = configViewEnable ?: false
        spnProject?.isEnabled = configViewEnable ?: false
        etDetectionNum?.isEnabled = configViewEnable ?: false

        swBanSampling?.isChecked = banSampling ?: false
        llBanSamplingNum?.visibility = banSampling.isShow()
        llBanSamplingNum?.isEnabled = configViewEnable ?: false
        swBanSampling?.isEnabled = configViewEnable ?: false

        val selectedIndex = items.indexOf(curveModel)
        spnProject?.setSelection(selectedIndex)

        if (items.size == 1) {//解决从没有项目到只有一个项目时，出现的不能选中的bug
            projectAdapter = HomeProjectAdapter(context, items)
            spnProject?.adapter = projectAdapter
        }

        llSampleNum?.visibility = if (isAuto()) View.GONE else View.VISIBLE

        spnProject?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectProject = items[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectProject = null
            }
        }
        swBanSampling?.setOnCheckedChangeListener { buttonView, isChecked ->
            banSampling = isChecked
            llBanSamplingNum?.visibility = banSampling.isShow()
        }
    }

    private fun confirm(onConfirm: ((CurveModel?, Int, String, Int, Boolean, Int,  BasePopupView) -> Unit)?) {
        if (selectProject == null) {
            toast("请选择标曲")
            return
        }
        var sampleNum: Int = (etSampleNum?.text?.trim().toString()).toIntOrNull() ?: 0
        var skipNum: Int = (etSkipNum?.text?.trim().toString()).toIntOrNull() ?: 0
        var detectionNum: String = etDetectionNum?.text?.trim().toString()
        var banSamplingNum: Int = etBanSamplingNum?.text?.trim().toString().toIntOrNull() ?: 0
        if (!isAuto()) {
            if (sampleNum < 0 || sampleNum > 50) {
                sampleNum = 0
                toast("检测数量必须为1-50")
                return
            }
        }
        if (skipNum < 0 || skipNum > 9) {
            skipNum = 0
            toast("跳过比色皿必须为0-9")
            return
        }
        if (banSamplingNum + skipNum > 10) {
            toast("禁止加样数量+跳过比色皿数量必须小于等于10")
            return
        }

        if (detectionNum.isNullOrEmpty()) {
            detectionNum = LocalData.DetectionNum
        }

        onConfirm?.invoke(
            selectProject,
            skipNum,
            detectionNum,
            sampleNum,
            banSampling ?: false,
            banSamplingNum,
            this
        )
    }

    override fun initDialogView() {
        spnProject = findViewById(R.id.spn_project)
        etSkipNum = findViewById(R.id.et_skip)
        etDetectionNum = findViewById(R.id.et_start_num)
        etSampleNum = findViewById(R.id.et_sample_num)
        llSampleNum = findViewById(R.id.ll_sample_num)
        swBanSampling = findViewById(R.id.sw_ban_sampling)
        etBanSamplingNum = findViewById(R.id.et_ban_sampling_num)
        llBanSamplingNum = findViewById(R.id.ll_ban_sampling)

        projectAdapter = HomeProjectAdapter(context, items)
        spnProject?.adapter = projectAdapter
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
