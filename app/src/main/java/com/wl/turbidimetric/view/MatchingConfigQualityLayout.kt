package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import com.wl.turbidimetric.R
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.util.FitterType

class MatchingConfigQualityLayout : FrameLayout {
    private var root: View? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, -1)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        initView()
    }

    init {
        root = LayoutInflater.from(context)
            .inflate(R.layout.layout_matching_config_quality, this, true)
    }

    var curves = mutableListOf<CurveModel>()
    var projectNames = mutableListOf<String>()
    var selectCurve: CurveModel? = null

    var selectFitterType: FitterType = FitterType.Three
    var spnProject: Spinner? = null

    var qualityLow1: Int = 0
    var qualityLow2: Int = 0
    var qualityHigh1: Int = 0
    var qualityHigh2: Int = 0

    var etQualityLow1: EditText? = null
    var etQualityLow2: EditText? = null
    var etQualityHigh1: EditText? = null
    var etQualityHigh2: EditText? = null
    var cdvDetails: CurveDetailsView? = null
    private var spnProjectAdapter: SpnSampleAdapter? = null

    private fun initView() {
        etQualityLow1 = findViewById(R.id.et_quality_low_1)
        etQualityLow2 = findViewById(R.id.et_quality_low_2)
        etQualityHigh1 = findViewById(R.id.et_quality_high_1)
        etQualityHigh2 = findViewById(R.id.et_quality_high_2)
        spnProject = findViewById(R.id.spn_fitter_project)
        cdvDetails = findViewById(R.id.cdv_details)

        spnProjectAdapter = SpnSampleAdapter(rootView.context, projectNames)
        spnProject?.adapter = spnProjectAdapter
    }


    fun setContent() {
        val selectProjectIndex = curves.indexOf(selectCurve)
        if (selectProjectIndex < 0 && curves.isNotEmpty()) {//没有已选择的但有项目就默认选择第一个
            selectCurve = curves[0]
            this.spnProject?.let {
                it.post {
                    it.setSelection(0)
                    spnProjectAdapter?.notifyDataSetChanged()
                }
            }
        } else {
            if(curves.isNotEmpty()){
                selectCurve = curves[selectProjectIndex]
            }
            this.spnProject?.let {
                it.post {
                    it.setSelection(selectProjectIndex)
                    spnProjectAdapter?.notifyDataSetChanged()
                }
            }
        }
        etQualityLow1?.setText(qualityLow1.toString())
        etQualityLow2?.setText(qualityLow2.toString())
        etQualityHigh1?.setText(qualityHigh1.toString())
        etQualityHigh2?.setText(qualityHigh2.toString())

        spnProject?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectCurve = curves[position]
                cdvDetails?.update(selectCurve)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectCurve = null
            }
        }
        cdvDetails?.update(selectCurve)

        cdvDetails?.setChartHeight(260)
        cdvDetails?.updateTextSize(22f,22f)
    }


    fun updateContent(
        selectCurve: CurveModel?,
        curves: List<CurveModel>,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
    ) {

        this.selectCurve = selectCurve
        this.qualityLow1 = qualityLow1
        this.qualityLow2 = qualityLow2
        this.qualityHigh1 = qualityHigh1
        this.qualityHigh2 = qualityHigh2
        this.curves.clear()
        this.curves.addAll(curves)
        this.projectNames.clear()
        this.projectNames.addAll(this.curves.map { "曲线序号:${it.reagentNO}" })

        if (selectCurve == null && curves.isNotEmpty()) {
            this.selectCurve = curves.first()
        }

        setContent()
    }

    fun getCurInput() {
        qualityLow1 = etQualityLow1?.text.toString().toInt()
        qualityLow2 = etQualityLow2?.text.toString().toInt()
        qualityHigh1 = etQualityHigh1?.text.toString().toInt()
        qualityHigh2 = etQualityHigh2?.text.toString().toInt()
    }


}
