package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.view.dialog.isShow
import java.math.RoundingMode

class MatchingFinishLayout : FrameLayout {
    private var root: View? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    private var tvReagentNo: TextView? = null
    private var tvProjectName: TextView? = null
    private var tvFitterType: TextView? = null
    private var tvTarget: TextView? = null
    private var tvAbs: TextView? = null
    private var tvYs: TextView? = null
    private var tvQuality: TextView? = null
    private var tvParams: TextView? = null
    private var tvTime: TextView? = null
    private var tvFitGoodness: TextView? = null

    init {
        root = LayoutInflater.from(context).inflate(R.layout.layout_matching_finish, this, true)
    }

    private var reagnetNo: String? = null
    private var abss: MutableList<MutableList<Double>> = mutableListOf()
    private var targets: MutableList<Double> = mutableListOf()
    private var means: MutableList<Double> = mutableListOf()
    private var selectFitterType: FitterType? = null
    private var curProject: CurveModel? = null
    private var isQuality: Boolean = false
    private var gradsNum: Int = 5

    fun showDialog(
        reagnetNo: String,
        gradsNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?,
        isQuality: Boolean,
    ) {
        initView()

        this.gradsNum = gradsNum
        this.reagnetNo = reagnetNo
        this.curProject = curProject
        this.abss.clear()
        this.abss.addAll(abss)
        this.targets.clear()
        this.targets.addAll(targets)
        this.means.clear()
        this.means.addAll(means)
        this.selectFitterType = selectFitterType
        this.isQuality = isQuality

        setContent()

    }

    private fun setContent() {
        if (tvReagentNo == null) return

        tvReagentNo?.text = "曲线序号:$reagnetNo"
        tvProjectName?.text = "拟合项目:${curProject?.projectName ?: ""}"
        tvFitterType?.text = "拟合方程:${selectFitterType?.showName ?: ""}"

        tvTarget?.text = "目标值:${targets?.subList(0, gradsNum)?.joinToString(",")}"
        tvAbs?.text = "反应值:${means?.subList(0, gradsNum)?.joinToString(",")}"
        tvYs?.text = "验算值:${curProject?.yzs?.joinToString(",")}"
        tvQuality?.visibility = isQuality.isShow()
        if (isQuality) {
            tvQuality?.text = "质控L:${means?.getOrElse(gradsNum, { 0 })}\n质控H:${
                means?.getOrElse(gradsNum + 1,
                    { 0 })
            }"
        }
        var paramsText = "参数1:${curProject?.f0?.scale(8)}\n参数2:${curProject?.f1?.scale(8)}"
        if (selectFitterType != FitterType.Linear) {
            paramsText += "\n参数3:${curProject?.f2?.scale(8)}"
        }
        if (selectFitterType == FitterType.Four) {
            paramsText += "\n参数4:${curProject?.f3?.scale(8)}"
        }
        tvParams?.text = paramsText

        tvFitGoodness?.text =
            "拟合度:${curProject?.fitGoodness?.toBigDecimal()?.setScale(6, RoundingMode.DOWN)}"
        tvTime?.text = "检测时间:${curProject?.createTime}"
    }

    private fun initView() {
        if (tvReagentNo != null) return
        tvReagentNo = findViewById(R.id.tv_reagent_no)
        tvProjectName = findViewById(R.id.tv_project_name)
        tvFitterType = findViewById(R.id.tv_fitter_type)
        tvTarget = findViewById(R.id.tv_target)
        tvAbs = findViewById(R.id.tv_abs)
        tvYs = findViewById(R.id.tv_ys)
        tvQuality = findViewById(R.id.tv_quality)
        tvParams = findViewById(R.id.tv_params)
        tvTime = findViewById(R.id.tv_time)
        tvFitGoodness = findViewById(R.id.tv_fitGoodness)
    }

}
