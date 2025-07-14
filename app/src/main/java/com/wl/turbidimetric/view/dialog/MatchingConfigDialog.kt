package com.wl.turbidimetric.view.dialog


import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.lxj.xpopup.util.KeyboardUtils
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.util.ViewWrapper
import com.wl.turbidimetric.view.MatchingConfigLayout
import com.wl.turbidimetric.view.MatchingStateLayout


class MatchingConfigDialog(val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_matching_config) {


    private var rlRoot: ViewGroup? = null
    private var mcl: MatchingConfigLayout? = null
    private var msl: MatchingStateLayout? = null
    private var rootVW: ViewWrapper? = null
    override fun initDialogView() {
        rlRoot = findViewById(R.id.rl_root)
        mcl = findViewById(R.id.mcl)
        msl = findViewById(R.id.msl)
        rootVW = ViewWrapper(rlRoot!!)
    }

    private var showStep: ShowStep = ShowStep.Step1

    private enum class ShowStep {
        Step1, Step2
    }

    var stepData1: StepData1? = null
    var stepData2: StepData2? = null

    data class StepData2(
        val matchingType: MatchingConfigLayout.MatchingType,
        val qualityLow1: Int,
        val qualityLow2: Int,
        val qualityHigh1: Int,
        val qualityHigh2: Int,
        val gradsNum: Int,
        val abss: MutableList<Double>,
        val targets: List<Double>,
        val means: List<Double>,
        val selectFitterType: FitterType,
        val curProject: CurveModel?,
        val isQuality: Boolean,
        val onConfirm: onClick,
        val onSaveConfirm: onClick,
        val onPrintConfirm: (result: String) -> Unit,
        val onConfirm2: onClick,
        val onFitterTypeChange: (selectFitterType: FitterType) -> Unit,
        val debug: Boolean = false,
        val onClickDebug: onClick
    )


    data class StepData1(
        val curves: MutableList<CurveModel>,
        val qualityLow1: Int,
        val qualityLow2: Int,
        val qualityHigh1: Int,
        val qualityHigh2: Int,
        val reagentNo: String = "",
        val quality: Boolean = false,
        val projects: List<ProjectModel>,
        val autoAttenuation: Boolean,
        val gradsNum: Int = 6,
        val selectProject: ProjectModel? = null,
        val selectFitterType: FitterType = FitterType.Three,
        val targetCons: List<Double> = mutableListOf(),
        val onConfirmClick: (
            matchingType: MatchingConfigLayout.MatchingType, selectCurve: CurveModel?, qualityLow1: Int, qualityLow2: Int, qualityHigh1: Int, qualityHigh2: Int,
            reagentNo: String, quality: Boolean, gradsNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Double>
        ) -> Unit,
        val onCancelClick: onClick
    )


    fun showDialogStep1(
        curves: MutableList<CurveModel>,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
        reagentNo: String = "",
        quality: Boolean = false,
        projects: List<ProjectModel>,
        autoAttenuation: Boolean,
        gradsNum: Int = 6,
        selectProject: ProjectModel? = null,
        selectFitterType: FitterType = FitterType.Three,
        targetCons: List<Double> = mutableListOf(),
        onConfirmClick: (
            matchingType: MatchingConfigLayout.MatchingType, selectCurve: CurveModel?, qualityLow1: Int, qualityLow2: Int, qualityHigh1: Int, qualityHigh2: Int,
            reagentNo: String, quality: Boolean, gradsNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Double>
        ) -> Unit,
        onCancelClick: onClick
    ) {
        this.showStep = ShowStep.Step1

        stepData1 = StepData1(
            curves,
            qualityLow1,
            qualityLow2,
            qualityHigh1,
            qualityHigh2,
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
            mcl?.let { mcl ->
                mcl.getCurInput()
                onConfirmClick.invoke(
                    mcl.matchingType,
                    mcl.selectCurve,
                    mcl.qualityLow1,
                    mcl.qualityLow2,
                    mcl.qualityHigh1,
                    mcl.qualityHigh2,
                    mcl.reagentNo,
                    mcl.quality,
                    mcl.gradsNum,
                    mcl.autoAttenuation,
                    mcl.selectProject,
                    mcl.selectFitterType,
                    mcl.targetCons,
                )
            }
        }, "", {}, "取消", onCancelClick)

        changeUi()
        noTestUI()
    }

    fun testingUI() {
        this.btnConfirm?.isEnabled = false
        this.btnConfirm2?.isEnabled = false
        this.btnConfirm?.text = "正在检测"
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
        } else {
            msl?.visibility = View.GONE
            mcl?.visibility = View.VISIBLE
        }
    }

    fun showDialogStep2(
        isError: Boolean,
        matchingType: MatchingConfigLayout.MatchingType,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
        gradsNum: Int,
        abss: MutableList<Double>,
        targets: List<Double>,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?,
        isQuality: Boolean,
        onConfirm: onClick,
        onSaveConfirm: onClick,
        onPrintConfirm: (result: String) -> Unit,
        onNotSaveConfirm: onClick,
        onFitterTypeChange: (selectFitterType: FitterType) -> Unit,
        debug: Boolean = false,
        onClickDebug: onClick
    ) {
        this.showStep = ShowStep.Step2
        stepData2 = StepData2(
            matchingType,
            qualityLow1,
            qualityLow2,
            qualityHigh1,
            qualityHigh2,
            gradsNum,
            abss,
            targets,
            means,
            selectFitterType,
            curProject,
            isQuality,
            onConfirm,
            onSaveConfirm,
            onPrintConfirm,
            onNotSaveConfirm,
            onFitterTypeChange,
            debug,
            onClickDebug
        )
        val confirmBtnStr =
            if (matchingType == MatchingConfigLayout.MatchingType.Matching) {
                if (abss.isEmpty() || isError) {
                    "开始检测"
                } else "保存结果"
            } else {
                if (abss.isEmpty() || isError) {
                    "开始质控"
                } else "打印结果"
            }
        val confirmBtnClick =
            if (matchingType == MatchingConfigLayout.MatchingType.Matching) {
                if (abss.isEmpty()) {
                    onConfirm
                } else onSaveConfirm
            } else {
//                if (abss.isEmpty()) {
                onConfirm
//                }
            }
        val cancelText = if (debug) "调试框" else ""
        if (matchingType == MatchingConfigLayout.MatchingType.Quality && abss.isNotEmpty()) {
            //打印结果
            showDialog(
                confirmBtnStr,
                {
                    onPrintConfirm.invoke(msl?.result ?: "")
                },
                "结束",
                onNotSaveConfirm,
                cancelText,
                onClickDebug
            )
        } else {
            showDialog(
                confirmBtnStr,
                confirmBtnClick,
                "结束",
                onNotSaveConfirm,
                cancelText,
                onClickDebug
            )
        }
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
                mcl?.updateContent(
                    MatchingConfigLayout.MatchingType.Matching,
                    data.curves,
                    data.qualityLow1,
                    data.qualityLow2,
                    data.qualityHigh1,
                    data.qualityHigh2,
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
                    data.matchingType,
                    data.qualityLow1,
                    data.qualityLow2,
                    data.qualityHigh1,
                    data.qualityHigh2,
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
