package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fondesa.recyclerviewdivider.dividerBuilder
import com.github.mikephil.charting.charts.LineChart
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.calcCon
import com.wl.turbidimetric.matchingargs.MatchingStateAdapter
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.util.FitterType
import kotlin.math.absoluteValue

class MatchingStateLayout : FrameLayout {
    private var root: View? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, -1)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context, attributeSet, defStyleAttr
    ) {
        initView()
        listenerView()
    }

    init {
        root = LayoutInflater.from(context).inflate(R.layout.layout_matching_state, this, true)
    }

    var tvTitle: TextView? = null
    var rv: RecyclerView? = null
    var cdvDetails: CurveDetailsView? = null
    var selectFitterType: FitterType = FitterType.Three
    var tvEquation: TextView? = null
    var lcCurve: LineChart? = null
    var spnFitterType: Spinner? = null
    var abss: MutableList<Double> = mutableListOf()
    var gradsNum: Int = 5
    var targets: MutableList<Double> = mutableListOf()
    var means: MutableList<Double> = mutableListOf()
    var isQuality: Boolean = false
    private var spnFitterTypeAdapter: SpnSampleAdapter? = null
    private var stateAdapter: MatchingStateAdapter? = null
    var fitterTypes = mutableListOf<FitterType>()
    var fitterTypeNames = mutableListOf<String>()
    var curProject: CurveModel? = null
    var matchingType: MatchingConfigLayout.MatchingType = MatchingConfigLayout.MatchingType.Matching
    var qualityLow1: Int = 0
    var qualityLow2: Int = 0
    var qualityHigh1: Int = 0
    var qualityHigh2: Int = 0
    var result = ""
    private fun initView() {
        rv = findViewById(R.id.rv)
        cdvDetails = findViewById(R.id.cdv_details)
        tvTitle = findViewById(R.id.tv_title)
//        tvEquation = findViewById(R.id.tv_equation)
//        lcCurve = findViewById(R.id.lc_curve)
//        spnFitterType = findViewById(R.id.spn_fitter_type)
//        fitterTypes.addAll(FitterType.values())
//        fitterTypeNames.addAll(fitterTypes.map { it.showName })
//        spnFitterTypeAdapter = SpnSampleAdapter(rootView.context, fitterTypeNames)
//        spnFitterType?.adapter = spnFitterTypeAdapter
        calcData()
        stateAdapter = MatchingStateAdapter(data, result)
        rv?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rv?.adapter = stateAdapter
        rv?.addItemDecoration(context.dividerBuilder().apply {
            color(resources.getColor(R.color.black2))
            size(1)
        }.build())

        cdvDetails?.setChartHeight(300)

        spnFitterType?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                onFitterTypeChange?.invoke(fitterTypes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

    }

    /**
     * 判断结果是否合格，并返回描述结论
     * 测量浓度 < 目标浓度的±10%内为合格
     * 质控测量浓度 <= 输入的目标浓度范围内为合格
     * @return String
     */
    private fun getResult() {
        var resultType = ""
        var temp = if (abss.isNotEmpty()) {
            resultType =
                if (matchingType == MatchingConfigLayout.MatchingType.Matching) "拟合" else "质控"
            data.map { d ->
                val targets = d.targetCon.split("-").map { it.toDouble() }
                //合格范围
                var bl  = 0.1
                if(targets[0] in 0.1..50.1){//如果目标浓度是50
                    bl = 0.3
                }

                var low = if (targets.size > 1) targets[0] else targets[0] * (1 - bl)
                var high = if (targets.size > 1) targets[1] else targets[0] * (1 + bl)


                if (low > 0.001 && high > 0.001) {

                    d.testCon.toDouble() in low..high
                } else {
                    //如果目标值是0就不判断
                    true
                }
            }.indexOfFirst { !it }.let { index ->
                val fit = curProject?.fitGoodness ?: 0.0
                //没有不合格的
                if (index == -1 && fit >= 0.99) {
                    "合格"
                } else {
                    "不合格"
                }
            }
        } else {
            ""
        }
        result = resultType + temp
    }


    fun setContent() {
        val index = fitterTypes.indexOf(selectFitterType)
        if (index >= 0) {
            spnFitterType?.setSelection(index)
        }

        cdvDetails?.update(curProject)
        calcData();
        stateAdapter?.update(data, result)
        tvTitle?.text = if (matchingType == MatchingConfigLayout.MatchingType.Matching) {
            "Step2 拟合结果"
        } else {
            "Step2 质控结果"
        }
    }

    var resultCon = mutableListOf<Int>()
    var data = mutableListOf<MatchingStateAdapter.Data>()
    private fun calcData() {
        resultCon = mutableListOf<Int>()
        data = mutableListOf()
        curProject?.let { project ->
            abss.forEach { abs ->
                resultCon.add(calcCon(abs.toBigDecimal(), project))
            }
        }

        //梯度
        targets.forEachIndexed { index, targetCon ->
            data.add(
                MatchingStateAdapter.Data(
                    targetCon.toString(),
                    resultCon.getOrNull(index)?.toString() ?: "",
                    abss.getOrNull(index)?.toString() ?: ""
                )
            )
        }
        //质控
        if (isQuality) {
            curProject?.let { project ->
                abss.getOrNull(targets.size)?.let { lowAbs ->
                    resultCon.add(calcCon(lowAbs.toBigDecimal(), project))
                }
                abss.getOrNull(targets.size + 1)?.let { highAbs ->
                    resultCon.add(calcCon(highAbs.toBigDecimal(), project))
                }
            }
            data.add(
                MatchingStateAdapter.Data(
                    "$qualityLow1-$qualityLow2",
                    resultCon.getOrNull(targets.size)?.toString() ?: "",
                    abss.getOrNull(targets.size)?.toString() ?: ""
                )
            )
            data.add(
                MatchingStateAdapter.Data(
                    "$qualityHigh1-$qualityHigh2",
                    resultCon.getOrNull(targets.size + 1)?.toString() ?: "",
                    abss.getOrNull(targets.size + 1)?.toString() ?: ""
                )
            )
        }
        getResult()
    }


    var onFitterTypeChange: ((selectFitterType: FitterType) -> Unit)? = null


    fun showDialog(
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
        onFitterTypeChange: (selectFitterType: FitterType) -> Unit
    ) {

        this.means.clear()
        this.matchingType = matchingType
        this.qualityLow1 = qualityLow1
        this.qualityLow2 = qualityLow2
        this.qualityHigh1 = qualityHigh1
        this.qualityHigh2 = qualityHigh2
        this.curProject = curProject
        this.gradsNum = gradsNum
        this.abss.clear()
        this.abss.addAll(abss)
        this.targets.clear()
        this.targets.addAll(targets)
        this.means.clear()
        this.means.addAll(means)
        this.selectFitterType = selectFitterType
        this.onFitterTypeChange = onFitterTypeChange
        this.isQuality = isQuality

        setContent()
    }

    private fun listenerView() {

    }


}
