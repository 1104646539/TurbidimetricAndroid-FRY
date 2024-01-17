package com.wl.turbidimetric.view.dialog


import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.RadioButton
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.matchingargs.MatchingConfigSampleAdapter
import com.wl.turbidimetric.model.CuvetteState
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.SampleState
import com.wl.turbidimetric.model.SampleType
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.util.FitterType
import com.wl.wllib.LogToFile.i

class MatchingConfigDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_matching_config) {
    var projects = mutableListOf<ProjectModel>()
    var projectNames = mutableListOf<String>()
    var fitterTypes = mutableListOf<FitterType>()
    var fitterTypeNames = mutableListOf<String>()

    var autoAttenuation = false
    var gradsNum = 5
    var selectProject: ProjectModel? = null
    var selectFitterType: FitterType = FitterType.Three
    val defaultCon5 = arrayListOf(0.0, 50.0, 200.0, 500.0, 1000.0)
    val defaultCon6 = arrayListOf(0.0, 25.0, 50.0, 200.0, 500.0, 1000.0)
    val defaultCon7 = arrayListOf(0.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)
    val defaultCon8 = arrayListOf(0.0, 15.625, 31.25, 62.5, 125.0, 250.0, 500.0, 1000.0)
    var cons = mutableListOf<Double>()


    var spnProject: Spinner? = null
    var spnFitterType: Spinner? = null
    var rbAuto: RadioButton? = null
    var rbManual: RadioButton? = null
    var rbGrad5: RadioButton? = null
    var rbGrad6: RadioButton? = null
    var rbGrad7: RadioButton? = null
    var rbGrad8: RadioButton? = null

    var etTargetCon1: EditText? = null
    var etTargetCon2: EditText? = null
    var etTargetCon3: EditText? = null
    var etTargetCon4: EditText? = null
    var etTargetCon5: EditText? = null
    var etTargetCon6: EditText? = null
    var etTargetCon7: EditText? = null
    var etTargetCon8: EditText? = null

    private var spnProjectAdapter: MatchingConfigSampleAdapter? = null
    private var spnFitterTypeAdapter: MatchingConfigSampleAdapter? = null

    //5个梯度可以自动稀释或人工稀释
    //6个梯度只能人工稀释
    //人工稀释时才可以手动输入目标浓度，自动稀释则是按照固定的浓度比例去稀释
    override fun initDialogView() {
        spnProject = findViewById(R.id.spn_project)
        spnFitterType = findViewById(R.id.spn_fitter_type)
        rbAuto = findViewById(R.id.rb_auto)
        rbManual = findViewById(R.id.rb_manual)
        rbGrad5 = findViewById(R.id.rb_grad_5)
        rbGrad6 = findViewById(R.id.rb_grad_6)
        rbGrad7 = findViewById(R.id.rb_grad_7)
        rbGrad8 = findViewById(R.id.rb_grad_8)
        etTargetCon1 = findViewById(R.id.et_target_con_1)
        etTargetCon2 = findViewById(R.id.et_target_con_2)
        etTargetCon3 = findViewById(R.id.et_target_con_3)
        etTargetCon4 = findViewById(R.id.et_target_con_4)
        etTargetCon5 = findViewById(R.id.et_target_con_5)
        etTargetCon6 = findViewById(R.id.et_target_con_6)
        etTargetCon7 = findViewById(R.id.et_target_con_7)
        etTargetCon8 = findViewById(R.id.et_target_con_8)

        spnProjectAdapter = MatchingConfigSampleAdapter(rootView.context, projectNames)
        spnProject?.adapter = spnProjectAdapter

        fitterTypes.addAll(FitterType.values())
        fitterTypeNames.addAll(fitterTypes.map { it.showName })
        spnFitterTypeAdapter = MatchingConfigSampleAdapter(rootView.context, fitterTypeNames)
        spnFitterType?.adapter = spnFitterTypeAdapter

    }


    fun showDialog(
        projects: List<ProjectModel>,
        autoAttenuation: Boolean,
        gradsNum: Int = 5,
        selectProject: ProjectModel? = null,
        selectFitterType: FitterType = FitterType.Three,
        targetCons: List<Double> = mutableListOf(),
        onConfirmClick: (gradsNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Double>) -> Unit,
        onCancelClick: onClick
    ) {
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

        this.confirmText = "确定"
        this.confirmClick = {
            getCons()
            onConfirmClick.invoke(
                this@MatchingConfigDialog.gradsNum,
                this@MatchingConfigDialog.autoAttenuation,
                this@MatchingConfigDialog.selectProject,
                this@MatchingConfigDialog.selectFitterType,
                this@MatchingConfigDialog.cons,
            )
        }

        this.cancelText = "取消"
        this.cancelClick = onCancelClick

        if (isCreated) {
            setContent()
        }

        super.show()
    }

    private fun getCons(): String {
        val tempCons = mutableListOf<Double>()
        val con1 = etTargetCon1?.text.toString().toDoubleOrNull() ?: 0.0
        val con2 = etTargetCon2?.text.toString().toDoubleOrNull() ?: 0.0
        val con3 = etTargetCon3?.text.toString().toDoubleOrNull() ?: 0.0
        val con4 = etTargetCon4?.text.toString().toDoubleOrNull() ?: 0.0
        val con5 = etTargetCon5?.text.toString().toDoubleOrNull() ?: 0.0

        tempCons.add(con1)
        tempCons.add(con2)
        tempCons.add(con3)
        tempCons.add(con4)
        tempCons.add(con5)
        if (gradsNum > 5) {
            val con6 = etTargetCon6?.text.toString().toDoubleOrNull() ?: 0.0
            tempCons.add(con6)
        }
        if (gradsNum > 6) {
            val con7 = etTargetCon7?.text.toString().toDoubleOrNull() ?: 0.0
            tempCons.add(con7)
        }
        if (gradsNum > 7) {
            val con8 = etTargetCon8?.text.toString().toDoubleOrNull() ?: 0.0
            tempCons.add(con8)
        }
        this.cons = tempCons
        return ""
    }

    override fun setContent() {
        super.setContent()

        val selectProjectIndex = projects.indexOf(selectProject)
        if (selectProjectIndex < 0 && projects.isNotEmpty()) {//没有已选择的但有项目就默认选择第一个
            selectProject = projects[0]
            this.spnProject?.setSelection(0)

        } else {
            selectProject = projects[selectProjectIndex]
            this.spnProject?.setSelection(selectProjectIndex)

        }


        val selectFitterIndex = fitterTypes.indexOf(selectFitterType)
        this.spnFitterType?.setSelection(selectFitterIndex)


        rbAuto?.isChecked = autoAttenuation

        rbGrad5?.isChecked = gradsNum == 5
        rbGrad6?.isChecked = gradsNum == 6
        rbGrad7?.isChecked = gradsNum == 7
        rbGrad8?.isChecked = gradsNum == 8

        changeCon()
        etTargetCon6?.visibility = (gradsNum > 5).isShow()
        etTargetCon7?.visibility = (gradsNum > 6).isShow()
        etTargetCon8?.visibility = (gradsNum > 7).isShow()
        rbAuto?.setOnCheckedChangeListener { buttonView, isChecked ->
            autoAttenuation = isChecked

            etTargetCon1?.isEnabled = !autoAttenuation
            etTargetCon2?.isEnabled = !autoAttenuation
            etTargetCon3?.isEnabled = !autoAttenuation
            etTargetCon4?.isEnabled = !autoAttenuation
            etTargetCon5?.isEnabled = !autoAttenuation

            etTargetCon6?.isEnabled = !autoAttenuation && (gradsNum > 5)
            etTargetCon7?.isEnabled = !autoAttenuation && (gradsNum > 6)
            etTargetCon8?.isEnabled = !autoAttenuation && (gradsNum > 7)

        }

        rbGrad5?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                gradsNum = 5
                //5可以选择自动或人工稀释
                rbAuto?.isEnabled = true
                rbManual?.isEnabled = true

                etTargetCon6?.visibility = false.isShow()
                etTargetCon7?.visibility = false.isShow()
                etTargetCon8?.visibility = false.isShow()

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
                etTargetCon7?.visibility = false.isShow()
                etTargetCon8?.visibility = false.isShow()
                changeCon()
            }
        }
        rbGrad7?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {//大于5只能人工稀释
                gradsNum = 7

                rbAuto?.isEnabled = false
                rbManual?.isEnabled = true

                rbManual?.isChecked = true
                etTargetCon6?.visibility = true.isShow()
                etTargetCon7?.visibility = true.isShow()
                etTargetCon8?.visibility = false.isShow()
                changeCon()
            }
        }
        rbGrad8?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {//大于5只能人工稀释
                gradsNum = 8

                rbAuto?.isEnabled = false
                rbManual?.isEnabled = true

                rbManual?.isChecked = true
                etTargetCon6?.visibility = true.isShow()
                etTargetCon7?.visibility = true.isShow()
                etTargetCon8?.visibility = true.isShow()
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

    private fun changeCon() {
        if (gradsNum == 5) {
            cons.clear()
            cons.addAll(defaultCon5)
        } else if (gradsNum == 6) {
            cons.clear()
            cons.addAll(defaultCon6)
        } else if (gradsNum == 7) {
            cons.clear()
            cons.addAll(defaultCon7)
        } else if (gradsNum == 8) {
            cons.clear()
            cons.addAll(defaultCon8)
        }

        etTargetCon1?.setText(cons[0].toString())
        etTargetCon2?.setText(cons[1].toString())
        etTargetCon3?.setText(cons[2].toString())
        etTargetCon4?.setText(cons[3].toString())
        etTargetCon5?.setText(cons[4].toString())
        if (cons.size > 5) {
            etTargetCon6?.setText(cons[5].toString())
        }
        if (cons.size > 6) {
            etTargetCon7?.setText(cons[6].toString())
        }
        if (cons.size > 7) {
            etTargetCon8?.setText(cons[7].toString())
        }


    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
