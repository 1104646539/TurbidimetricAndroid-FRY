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
    var matchingNum = 5
    var selectProject: ProjectModel? = null
    var selectFitterType: FitterType = FitterType.Three
    val defaultCon5 = arrayListOf(0, 50, 200, 500, 1000)
    val defaultCon6 = arrayListOf(0, 25, 50, 200, 500, 1000)
    var cons = mutableListOf<Int>()


    var spnProject: Spinner? = null
    var spnFitterType: Spinner? = null
    var rbAuto: RadioButton? = null
    var rbManual: RadioButton? = null
    var rbGrad5: RadioButton? = null
    var rbGrad6: RadioButton? = null

    var etTargetCon1: EditText? = null
    var etTargetCon2: EditText? = null
    var etTargetCon3: EditText? = null
    var etTargetCon4: EditText? = null
    var etTargetCon5: EditText? = null
    var etTargetCon6: EditText? = null

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
        etTargetCon1 = findViewById(R.id.et_target_con_1)
        etTargetCon2 = findViewById(R.id.et_target_con_2)
        etTargetCon3 = findViewById(R.id.et_target_con_3)
        etTargetCon4 = findViewById(R.id.et_target_con_4)
        etTargetCon5 = findViewById(R.id.et_target_con_5)
        etTargetCon6 = findViewById(R.id.et_target_con_6)

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
        matchingNum: Int = 5,
        selectProject: ProjectModel? = null,
        selectFitterType: FitterType = FitterType.Three,
        cons: List<Int> = mutableListOf(),
        onConfirmClick: (matchingNum: Int,autoAttenuation: Boolean,selectProject: ProjectModel?,selectFitterType: FitterType,cons: List<Int>) -> Unit,
        onCancelClick: onClick
    ) {
        this.projects.clear()
        this.projects.addAll(projects)
        this.projectNames.clear()
        this.projectNames.addAll(this.projects.map { it.projectName })
        this.autoAttenuation = autoAttenuation
        this.matchingNum = matchingNum
        this.selectProject = selectProject
        this.selectFitterType = selectFitterType
        this.cons.clear()
        this.cons.addAll(cons)

        this.confirmText = "确定"
        this.confirmClick = {
            getCons()
            onConfirmClick?.invoke(
                this@MatchingConfigDialog.matchingNum,
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
        val tempCons = mutableListOf<Int>()
        val con1 = etTargetCon1?.text.toString().toIntOrNull() ?: 0
        val con2 = etTargetCon2?.text.toString().toIntOrNull() ?: 0
        val con3 = etTargetCon3?.text.toString().toIntOrNull() ?: 0
        val con4 = etTargetCon4?.text.toString().toIntOrNull() ?: 0
        val con5 = etTargetCon5?.text.toString().toIntOrNull() ?: 0

        tempCons.add(con1)
        tempCons.add(con2)
        tempCons.add(con3)
        tempCons.add(con4)
        tempCons.add(con5)
        if (matchingNum > 5) {
            val con6 = etTargetCon6?.text.toString().toIntOrNull() ?: 0
            tempCons.add(con6)
        }
        this.cons = tempCons
        return ""
    }

    override fun setContent() {
        super.setContent()

        val selectProjectIndex = projects.indexOf(selectProject)
        this.spnProject?.setSelection(selectProjectIndex)

        val selectFitterIndex = fitterTypes?.indexOf(selectFitterType)
        this.spnFitterType?.setSelection(selectFitterIndex)


        rbAuto?.isChecked = autoAttenuation

        rbGrad5?.isChecked = matchingNum == 5

        changeCon()
        etTargetCon6?.visibility = (matchingNum != 5).isShow()
        rbAuto?.setOnCheckedChangeListener { buttonView, isChecked ->
            autoAttenuation = isChecked

            etTargetCon1?.isEnabled = !autoAttenuation
            etTargetCon2?.isEnabled = !autoAttenuation
            etTargetCon3?.isEnabled = !autoAttenuation
            etTargetCon4?.isEnabled = !autoAttenuation
            etTargetCon5?.isEnabled = !autoAttenuation

            etTargetCon6?.isEnabled = !autoAttenuation && rbGrad6?.isChecked == true

        }

        rbGrad5?.setOnCheckedChangeListener { buttonView, isChecked ->
            matchingNum = if (isChecked) 5 else 6

            if (matchingNum == 5) {//5可以选择自动或人工稀释
                rbAuto?.isEnabled = true
                rbManual?.isEnabled = true

                etTargetCon6?.visibility = false.isShow()

            } else {//6只能人工稀释
                rbAuto?.isEnabled = false
                rbManual?.isEnabled = true

                rbManual?.isChecked = true
                etTargetCon6?.visibility = true.isShow()

            }
            changeCon()
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
        if (matchingNum == 5) {
            cons = defaultCon5
        } else {
            cons = defaultCon6
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

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
