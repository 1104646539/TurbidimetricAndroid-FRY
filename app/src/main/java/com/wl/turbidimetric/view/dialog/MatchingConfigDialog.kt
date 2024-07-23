package com.wl.turbidimetric.view.dialog


import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.lxj.xpopup.util.KeyboardUtils
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.util.ViewWrapper
import com.wl.turbidimetric.view.MatchingConfigLayout
import com.wl.turbidimetric.view.MatchingFinishLayout
import com.wl.turbidimetric.view.MatchingStateLayout


class MatchingConfigDialog(val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_matching_config) {


    private var rlRoot: ViewGroup? = null
    private var mcl: MatchingConfigLayout? = null
    private var msl: MatchingStateLayout? = null
    private var mfl: MatchingFinishLayout? = null
    private var rootVW: ViewWrapper? = null
    private var tvTitle: TextView? = null
    override fun initDialogView() {
        rlRoot = findViewById(R.id.rl_root)
        mcl = findViewById(R.id.mcl)
        msl = findViewById(R.id.msl)
        mfl = findViewById(R.id.mfl)
        tvTitle = findViewById(R.id.tv_title)
        rootVW = ViewWrapper(rlRoot!!)


    }

    val baseWidth = 960
    private var showStep: ShowStep = ShowStep.Step1

    private enum class ShowStep {
        Step1, Step2, Step3
    }

    var stepData1: StepData1? = null
    var stepData2: StepData2? = null
    var stepData3: StepData3? = null

    data class StepData2(
        val gradsNum: Int,
        val abss: MutableList<MutableList<Double>>,
        val targets: List<Double>,
        val means: List<Double>,
        val selectFitterType: FitterType,
        val curProject: CurveModel?,
        val isQuality: Boolean,
        val onConfirm: onClick,
        val onConfirm2: onClick,
        val onFitterTypeChange: (selectFitterType: FitterType) -> Unit,
        val debug: Boolean = false,
        val onClickDebug: onClick
    )

    data class StepData3(
        val reagnetNo: String,
        val gradsNum: Int,
        val abss: MutableList<MutableList<Double>>,
        val targets: List<Double>,
        val means: List<Double>,
        val selectFitterType: FitterType,
        val curProject: CurveModel?,
        val isQuality: Boolean,
        val onConfirm: onClick,
        val onConfirm2: onClick,
        val onCancelClick: onClick,
    )

    data class StepData1(
        val reagentNo: String = "",
        val quality: Boolean = false,
        val projects: List<ProjectModel>,
        val autoAttenuation: Boolean,
        val gradsNum: Int = 5,
        val selectProject: ProjectModel? = null,
        val selectFitterType: FitterType = FitterType.Three,
        val targetCons: List<Double> = mutableListOf(),
        val onConfirmClick: (reagentNo: String, quality: Boolean, gradsNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Double>) -> Unit,
        val onCancelClick: onClick
    )

    fun showDialogStep3(
        reagnetNo: String,
        gradsNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?,
        isQuality: Boolean,
        onConfirm: onClick,
        onConfirm2: onClick,
        onCancelClick: onClick,
    ) {
        this.showStep = ShowStep.Step3
        tvTitle?.text = "Step3 拟合结果"

        stepData3 = StepData3(
            reagnetNo,
            gradsNum,
            abss,
            targets,
            means,
            selectFitterType,
            curProject,
            isQuality,
            onConfirm,
            onConfirm2,
            onCancelClick
        )

        showDialog("继续检测", onConfirm, "保存结果", onConfirm2, "不保存结果", onCancelClick)
        changeUi()
        noTestUI()
    }

    fun showDialogStep1(
        reagentNo: String = "",
        quality: Boolean = false,
        projects: List<ProjectModel>,
        autoAttenuation: Boolean,
        gradsNum: Int = 5,
        selectProject: ProjectModel? = null,
        selectFitterType: FitterType = FitterType.Three,
        targetCons: List<Double> = mutableListOf(),
        onConfirmClick: (reagentNo: String, quality: Boolean, gradsNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Double>) -> Unit,
        onCancelClick: onClick
    ) {
        this.showStep = ShowStep.Step1

        tvTitle?.text = "Step1 设置拟合参数"
        stepData1 = StepData1(
            reagentNo,
            quality,
            projects,
            autoAttenuation,
            gradsNum,
            selectProject,
            selectFitterType,
            targetCons,
            onConfirmClick,
            onCancelClick
        )
        showDialog("下一步", {
            mcl?.getCurInput()
            onConfirmClick.invoke(
                mcl!!.reagentNoStr,
                mcl!!.quality,
                mcl!!.gradsNum,
                mcl!!.autoAttenuation,
                mcl!!.selectProject,
                mcl!!.selectFitterType,
                mcl!!.cons,
            )
        }, "", {}, "取消", onCancelClick)

        changeUi()
        noTestUI()
    }

    fun testingUI() {
        this.btnConfirm?.isEnabled = false
        this.btnConfirm2?.isEnabled = false
    }

    fun noTestUI() {
        this.btnConfirm?.isEnabled = true
        this.btnConfirm2?.isEnabled = true
    }


    private fun changeUi() {
        if (mcl == null) return
        if (showStep == ShowStep.Step2) {
            msl?.visibility = View.VISIBLE
            mcl?.visibility = View.GONE
            mfl?.visibility = View.GONE
        } else if (showStep == ShowStep.Step1) {
            msl?.visibility = View.GONE
            mcl?.visibility = View.VISIBLE
            mfl?.visibility = View.GONE
        } else {
            mcl?.visibility = View.GONE
            msl?.visibility = View.GONE
            mfl?.visibility = View.VISIBLE
        }
    }

    private fun getStep2Width(): Int {
        if (stepData2 != null) {
            var w = baseWidth + (stepData2!!.gradsNum - 5) * 85
            if (stepData2!!.isQuality) {
                w += 85 * 2
            }
            return w
        }
        return baseWidth
    }


    fun showDialogStep2(
        gradsNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?,
        isQuality: Boolean,
        onConfirm: onClick,
        onConfirm2: onClick,
        onFitterTypeChange: (selectFitterType: FitterType) -> Unit,
        debug: Boolean = false,
        onClickDebug: onClick
    ) {
        this.showStep = ShowStep.Step2
        tvTitle?.text = "Step2 拟合曲线"
        stepData2 = StepData2(
            gradsNum,
            abss,
            targets,
            means,
            selectFitterType,
            curProject,
            isQuality,
            onConfirm,
            onConfirm2,
            onFitterTypeChange,
            debug,
            onClickDebug
        )
        showDialog("添加检测", onConfirm, "拟合结束", onConfirm2, "调试框", onClickDebug)

        noTestUI()
    }

    fun showDialog(
        onConfirmText: String,
        onConfirm: onClick,
        onConfirmText2: String,
        onConfirm2: onClick,
        onCancelText: String,
        onCancelClick: onClick
    ) {
        this.confirmText = onConfirmText
        this.confirmClick = onConfirm
        this.confirmText2 = onConfirmText2
        this.confirmClick2 = onConfirm2

        this.cancelText = onCancelText
        this.cancelClick = onCancelClick

        if (isCreated) {
            setContent()
        }
        //非调试模式不显示调试框
        if (showStep == ShowStep.Step2) {
            if (stepData2?.debug != true) {
                this.cancelText = ""
            }
            KeyboardUtils.hideSoftInput(this)
        }
        super.show()
    }


    override fun setContent() {
        super.setContent()

        if (showStep == ShowStep.Step1) {
            stepData1?.let { data ->
                mcl?.showDialog(
                    data.reagentNo,
                    data.quality,
                    data.projects!!,
                    data.autoAttenuation!!,
                    data.gradsNum,
                    data.selectProject,
                    data.selectFitterType,
                    data.targetCons,
                )
            }
        } else if (showStep == ShowStep.Step2) {
            stepData2?.let { data ->
                msl?.showDialog(
                    data.gradsNum,
                    data.abss,
                    data.targets,
                    data.means,
                    data.selectFitterType,
                    data.curProject,
                    data.isQuality,
                    data.onFitterTypeChange
                )
            }
        } else {
            stepData3?.let { data ->
                mfl?.showDialog(
                    data.reagnetNo,
                    data.gradsNum,
                    data.abss,
                    data.targets,
                    data.means,
                    data.selectFitterType,
                    data.curProject,
                    data.isQuality
                )
            }
        }
        changeUi()
    }


    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
