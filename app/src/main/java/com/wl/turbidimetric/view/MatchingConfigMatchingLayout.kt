package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Switch
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.view.MatchingConfigLayout.MatchingType
import com.wl.turbidimetric.view.dialog.isShow

class MatchingConfigMatchingLayout : FrameLayout {
    private var root: View? = null

    constructor(context: Context) : this(context,null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet,-1)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ){
        initView()
    }

    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX)
    }

    override fun getTranslationX(): Float {
        return super.getTranslationX()
    }

    init {
        root = LayoutInflater.from(context)
            .inflate(R.layout.layout_matching_config_matching, this, true)

    }

    private fun listenerView() {

    }

    var projects = mutableListOf<ProjectModel>()
    var projectNames = mutableListOf<String>()

    var fitterTypes = mutableListOf<FitterType>()
    var fitterTypeNames = mutableListOf<String>()

    var reagentNoStr: String = ""
    var quality: Boolean = false
    var autoAttenuation = false
    var gradsNum = 5
    var selectProject: ProjectModel? = null
    var selectFitterType: FitterType = FitterType.Three
    val defaultCon5 = arrayListOf(0.0, 50.0, 200.0, 500.0, 1000.0)
    val defaultCon6 = arrayListOf(0.0, 25.0, 50.0, 200.0, 500.0, 1000.0)
    var cons = mutableListOf<Double>()


    var spnProject: Spinner? = null
    var spnFitterType: Spinner? = null
    var rbAuto: RadioButton? = null
    var rbManual: RadioButton? = null
    var rbGrad5: RadioButton? = null
    var rbGrad6: RadioButton? = null
    var swQuality: Switch? = null
    var etReagentNo: EditText? = null

    var etTargetCon1: EditText? = null
    var etTargetCon2: EditText? = null
    var etTargetCon3: EditText? = null
    var etTargetCon4: EditText? = null
    var etTargetCon5: EditText? = null
    var etTargetCon6: EditText? = null

    var etQualityLow1: EditText? = null
    var etQualityLow2: EditText? = null
    var etQualityHigh1: EditText? = null
    var etQualityHigh2: EditText? = null

    private var spnProjectAdapter: SpnSampleAdapter? = null
    private var spnFitterTypeAdapter: SpnSampleAdapter? = null

    var qualityLow1: Int = 0
    var qualityLow2: Int = 0
    var qualityHigh1: Int = 0
    var qualityHigh2: Int = 0

    //5个梯度可以自动稀释或人工稀释
    //6个梯度只能人工稀释
    //人工稀释时才可以手动输入目标浓度，自动稀释则是按照固定的浓度比例去稀释
    fun initView() {
        if (swQuality != null) return

        swQuality = findViewById(R.id.sw_quality)
        etReagentNo = findViewById(R.id.et_reagent_no)
        spnProject = findViewById(R.id.spn_project)
        spnFitterType = findViewById(R.id.spn_fitter_type)
        rbAuto = findViewById(R.id.rb_auto)
        rbManual = findViewById(R.id.rb_manual)
        rbGrad5 = findViewById(R.id.rb_grad_5)
        rbGrad6 = findViewById(R.id.rb_grad_6)
        etTargetCon1 = findViewById(R.id.et_target_con_1)
        etTargetCon2 = findViewById(R.id.et_target_con_2)
        etTargetCon3 = findViewById(R.id.et_target_con_3)
        etTargetCon4 = findViewById(R.id.et_target_con_4)
        etTargetCon5 = findViewById(R.id.et_target_con_5)
        etTargetCon6 = findViewById(R.id.et_target_con_6)

        etQualityLow1 = findViewById(R.id.et_quality_low_1)
        etQualityLow2 = findViewById(R.id.et_quality_low_2)
        etQualityHigh1 = findViewById(R.id.et_quality_high_1)
        etQualityHigh2 = findViewById(R.id.et_quality_high_2)

        spnProjectAdapter = SpnSampleAdapter(rootView.context, projectNames)
        spnProject?.adapter = spnProjectAdapter

        fitterTypes.addAll(FitterType.values())
        fitterTypeNames.addAll(fitterTypes.map { it.showName })
        spnFitterTypeAdapter = SpnSampleAdapter(rootView.context, fitterTypeNames)
        spnFitterType?.adapter = spnFitterTypeAdapter

    }


    fun updateContent(
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
        reagentNo: String = "",
        quality: Boolean = false,
        projects: List<ProjectModel>,
        autoAttenuation: Boolean,
        gradsNum: Int = 5,
        selectProject: ProjectModel? = null,
        selectFitterType: FitterType = FitterType.Three,
        targetCons: List<Double> = mutableListOf()
    ) {
        this.projects.clear()
        this.projectNames.clear()

        this.qualityLow1 = qualityLow1
        this.qualityLow2 = qualityLow2
        this.qualityHigh1 = qualityHigh1
        this.qualityHigh2 = qualityHigh2
        this.reagentNoStr = reagentNo
        this.quality = quality
        this.projects.clear()
        this.projects.addAll(projects)
        this.projectNames.clear()
        this.projectNames.addAll(this.projects.map { it.projectName })
        this.autoAttenuation = autoAttenuation
        this.gradsNum = gradsNum
        this.selectProject = selectProject
        this.selectFitterType = selectFitterType
        this.cons.clear()
        this.cons.addAll(targetCons)

        if (selectProject == null && projects.isNotEmpty()) {
            this.selectProject = projects.first()
        }

        setContent()
    }


    fun getCurInput() {
        getCons()

        reagentNoStr = etReagentNo?.text.toString()
        qualityLow1 = etQualityLow1?.text.toString().toInt()
        qualityLow2 = etQualityLow2?.text.toString().toInt()
        qualityHigh1 = etQualityHigh1?.text.toString().toInt()
        qualityHigh2 = etQualityHigh2?.text.toString().toInt()
    }

    private fun getCons(): String {
        val tempCons = mutableListOf<Double>()
        val con1 = etTargetCon1?.text.toString().toDoubleOrNull() ?: 0.0
        val con2 = etTargetCon2?.text.toString().toDoubleOrNull() ?: 0.0
        val con3 = etTargetCon3?.text.toString().toDoubleOrNull() ?: 0.0
        val con4 = etTargetCon4?.text.toString().toDoubleOrNull() ?: 0.0
        val con5 = etTargetCon5?.text.toString().toDoubleOrNull() ?: 0.0

        tempCons.add(con1.scale(8))
        tempCons.add(con2.scale(8))
        tempCons.add(con3.scale(8))
        tempCons.add(con4.scale(8))
        tempCons.add(con5.scale(8))
        if (gradsNum > 5) {
            val con6 = etTargetCon6?.text.toString().toDoubleOrNull() ?: 0.0
            tempCons.add(con6.scale(8))
        }
        this.cons = tempCons
        return ""
    }

    fun setContent() {
        val selectProjectIndex = projects.indexOf(selectProject)
        if (selectProjectIndex < 0 && projects.isNotEmpty()) {//没有已选择的但有项目就默认选择第一个
            selectProject = projects[0]
            this.spnProject?.let {
                it.post {
                    it.setSelection(0)
                    spnProjectAdapter?.notifyDataSetChanged()
                }
            }
        } else {
            selectProject = projects[selectProjectIndex]
            this.spnProject?.let {
                it.post {
                    it.setSelection(selectProjectIndex)
                    spnProjectAdapter?.notifyDataSetChanged()
                }
            }
        }

        val selectFitterIndex = fitterTypes.indexOf(selectFitterType)
        this.spnFitterType?.setSelection(selectFitterIndex)

        etReagentNo?.setText("${reagentNoStr ?: ""}")

        swQuality?.isChecked = quality

        rbAuto?.isChecked = autoAttenuation
        rbManual?.isChecked = !autoAttenuation

        rbGrad5?.isChecked = gradsNum == 5
        rbGrad6?.isChecked = gradsNum == 6

        changeCon()
        etTargetCon6?.visibility = (gradsNum > 5).isShow()

        changeQualityState(quality)
        swQuality?.setOnCheckedChangeListener { buttonView, isChecked ->
            quality = isChecked
            changeQualityState(quality)
        }

        rbAuto?.setOnCheckedChangeListener { buttonView, isChecked ->
            autoAttenuation = isChecked

            etTargetCon1?.isEnabled = !autoAttenuation
            etTargetCon2?.isEnabled = !autoAttenuation
            etTargetCon3?.isEnabled = !autoAttenuation
            etTargetCon4?.isEnabled = !autoAttenuation
            etTargetCon5?.isEnabled = !autoAttenuation

            etTargetCon6?.isEnabled = !autoAttenuation && (gradsNum > 5)

        }

        etQualityLow1?.setText(qualityLow1.toString())
        etQualityLow2?.setText(qualityLow2.toString())
        etQualityHigh1?.setText(qualityHigh1.toString())
        etQualityHigh2?.setText(qualityHigh2.toString())

        rbGrad5?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                gradsNum = 5
                //5可以选择自动或人工稀释
                rbAuto?.isEnabled = true
                rbManual?.isEnabled = true

                etTargetCon6?.visibility = false.isShow()

                changeCon()
            }
        }

        rbGrad6?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {//大于5只能人工稀释
                gradsNum = 6

                rbAuto?.isEnabled = false
                rbManual?.isEnabled = true

                rbManual?.isChecked = true
                etTargetCon6?.visibility = true.isShow()
                changeCon()
            }
        }

        spnProject?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectProject = projects[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectProject = null
            }
        }
        spnFitterType?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectFitterType = fitterTypes[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectFitterType = FitterType.Three
            }
        }
    }

    private fun changeQualityState(quality: Boolean) {
        etQualityLow1?.isEnabled = quality
        etQualityLow2?.isEnabled = quality
        etQualityHigh1?.isEnabled = quality
        etQualityHigh2?.isEnabled = quality
    }

    private fun changeCon() {
        if (gradsNum == 5) {
            cons.clear()
            cons.addAll(defaultCon5)
        } else if (gradsNum == 6) {
            cons.clear()
            cons.addAll(defaultCon6)
        }

        etTargetCon1?.setText(cons[0].toString())
        etTargetCon2?.setText(cons[1].toString())
        etTargetCon3?.setText(cons[2].toString())
        etTargetCon4?.setText(cons[3].toString())
        etTargetCon5?.setText(cons[4].toString())
        if (cons.size > 5) {
            etTargetCon6?.setText(cons[5].toString())
        }
    }

}
