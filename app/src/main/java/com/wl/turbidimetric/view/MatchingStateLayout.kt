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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getChartEntry
import com.wl.turbidimetric.ex.getEquation
import com.wl.turbidimetric.ex.getFitGoodness
import com.wl.turbidimetric.ex.getIndexOrNullDefault
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.matchingargs.MatchingStateAdapter
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.view.dialog.isShow
import com.wl.wllib.LogToFile

class MatchingStateLayout : FrameLayout {
    private var root: View? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    init {
        root = LayoutInflater.from(context).inflate(R.layout.layout_matching_state, this, true)
    }

    var vHeader: View? = null
    var vFooter: View? = null
    var tvHeaderTitle: TextView? = null
    var tvFooterTitle: TextView? = null
    var tvHeader1: TextView? = null
    var tvHeader2: TextView? = null
    var tvHeader3: TextView? = null
    var tvHeader4: TextView? = null
    var tvHeader5: TextView? = null
    var tvHeader6: TextView? = null
    var tvHeader7: TextView? = null
    var tvHeader8: TextView? = null
    var tvHeaderQualityL: TextView? = null
    var tvHeaderQualityH: TextView? = null
    var tvFooter1: TextView? = null
    var tvFooter2: TextView? = null
    var tvFooter3: TextView? = null
    var tvFooter4: TextView? = null
    var tvFooter5: TextView? = null
    var tvFooter6: TextView? = null
    var tvFooter7: TextView? = null
    var tvFooter8: TextView? = null
    var tvFooterQualityL: TextView? = null
    var tvFooterQualityH: TextView? = null
    var rv: RecyclerView? = null
    var selectFitterType: FitterType = FitterType.Three
    var tvEquation: TextView? = null
    var lcCurve: LineChart? = null
    var spnFitterType: Spinner? = null
    var abss: MutableList<MutableList<Double>> = mutableListOf()
    var gradsNum: Int = 5
    var targets: MutableList<Double> = mutableListOf()
    var means: MutableList<Double> = mutableListOf()
    var isQuality: Boolean = false
    private var spnFitterTypeAdapter: SpnSampleAdapter? = null
    private var stateAdapter: MatchingStateAdapter? = null
    private val bgGray = getResource().getColor(R.color.bg_gray)
    private val textColor = getResource().getColor(R.color.textColor)
    private val lineColor = getResource().getColor(R.color.themePositiveColor)
    var fitterTypes = mutableListOf<FitterType>()
    var fitterTypeNames = mutableListOf<String>()
    var curProject: CurveModel? = null
    private fun initView() {
        if (vHeader != null) return

        vHeader = findViewById(R.id.inc_header)
        vFooter = findViewById(R.id.inc_footer)
        rv = findViewById(R.id.rv)
        tvEquation = findViewById(R.id.tv_equation)
        lcCurve = findViewById(R.id.lc_curve)
        spnFitterType = findViewById(R.id.spn_fitter_type)
        vHeader?.let { it ->
            tvHeaderTitle = it.findViewById(R.id.tv_result_header)
            tvHeader1 = it.findViewById(R.id.tv_result_1)
            tvHeader2 = it.findViewById(R.id.tv_result_2)
            tvHeader3 = it.findViewById(R.id.tv_result_3)
            tvHeader4 = it.findViewById(R.id.tv_result_4)
            tvHeader5 = it.findViewById(R.id.tv_result_5)
            tvHeader6 = it.findViewById(R.id.tv_result_6)
            tvHeader7 = it.findViewById(R.id.tv_result_7)
            tvHeader8 = it.findViewById(R.id.tv_result_8)
            tvHeaderQualityL = it.findViewById(R.id.tv_quality_l)
            tvHeaderQualityH = it.findViewById(R.id.tv_quality_h)
        }
        vFooter?.let { it ->
            tvFooterTitle = it.findViewById(R.id.tv_result_header)
            tvFooter1 = it.findViewById(R.id.tv_result_1)
            tvFooter2 = it.findViewById(R.id.tv_result_2)
            tvFooter3 = it.findViewById(R.id.tv_result_3)
            tvFooter4 = it.findViewById(R.id.tv_result_4)
            tvFooter5 = it.findViewById(R.id.tv_result_5)
            tvFooter6 = it.findViewById(R.id.tv_result_6)
            tvFooter7 = it.findViewById(R.id.tv_result_7)
            tvFooter8 = it.findViewById(R.id.tv_result_8)
            tvFooterQualityL = it.findViewById(R.id.tv_quality_l)
            tvFooterQualityH = it.findViewById(R.id.tv_quality_h)
        }
        fitterTypes.addAll(FitterType.values())
        fitterTypeNames.addAll(fitterTypes.map { it.showName })
        spnFitterTypeAdapter = SpnSampleAdapter(rootView.context, fitterTypeNames)
        spnFitterType?.adapter = spnFitterTypeAdapter

        vHeader?.setBackgroundResource(R.drawable.bg_item)

        initCurveView()
        LogToFile.i("$tvHeaderTitle $tvFooterTitle")
    }

    private fun initCurveView() {
        lcCurve?.let { lcCurve ->
            val set1 = LineDataSet(arrayListOf(), null)
            set1.setDrawValues(false)//不绘制值在点上
            set1.setDrawIcons(false)//不绘制值icon在点上
            set1.color = lineColor
            set1.label = ""
            set1.enableDashedLine(10f, 0f, 0f)//线连成虚线
            set1.circleColors = listOf(lineColor)
            set1.circleRadius = 1f //点的半径
            set1.setDrawCircleHole(false)

            //数据集,一个数据集一条线
            val data = LineData(set1)

            //横向的轴
            val xAxis = lcCurve.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM //数字显示在下方
            xAxis.gridColor = bgGray
            xAxis.axisLineColor = bgGray
            xAxis.textColor = textColor

            //竖轴 L是左边的，R是右边的
            val yAxisL = lcCurve.axisLeft
            val yAxisR = lcCurve.axisRight
            yAxisL.gridColor = bgGray
            yAxisR.gridColor = bgGray

            yAxisL.zeroLineColor = bgGray
            yAxisR.zeroLineColor = bgGray

            yAxisL.axisLineColor = bgGray
            yAxisR.axisLineColor = bgGray

            yAxisL.textColor = textColor
            yAxisR.textColor = textColor

            yAxisL.setDrawZeroLine(false)
            yAxisR.setDrawZeroLine(false)
            //右边的轴不显示值
            yAxisR.setValueFormatter { value, axis -> "" }

            lcCurve.animateXY(1000, 1000)

            //数据集的文字不显示，不启用
            lcCurve.description.isEnabled = false
            //数据集的色块不显示，不启用
            lcCurve.legend.isEnabled = false
            //空数据显示文字
            lcCurve.setNoDataText("无数据")
            //设置数据，更新
            lcCurve.data = data
        }
    }

    fun setContent() {

        tvHeaderTitle?.text = "目标值"
        tvFooterTitle?.text = "平均值"

        setTarget()
        setMean()

        stateAdapter = MatchingStateAdapter(gradsNum, abss, isQuality)
        rv?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rv?.adapter = stateAdapter

        changeEquation()
        spnFitterType?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onFitterTypeChange?.invoke(fitterTypes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        val index = fitterTypes.indexOf(selectFitterType)
        if (index >= 0) {
            spnFitterType?.setSelection(index)
        }
    }


    private fun setMean() {
        tvFooter6?.visibility = (gradsNum > 5).isShow()
        tvFooter7?.visibility = (gradsNum > 6).isShow()
        tvFooter8?.visibility = (gradsNum > 7).isShow()
        tvFooter1?.text = getIndexOrNullDefault(means, 0, "-")
        tvFooter2?.text = getIndexOrNullDefault(means, 1, "-")
        tvFooter3?.text = getIndexOrNullDefault(means, 2, "-")
        tvFooter4?.text = getIndexOrNullDefault(means, 3, "-")
        tvFooter5?.text = getIndexOrNullDefault(means, 4, "-")
        tvFooter6?.text = getIndexOrNullDefault(means, 5, "-")
        tvFooter7?.text = getIndexOrNullDefault(means, 6, "-")
        tvFooter8?.text = getIndexOrNullDefault(means, 7, "-")

        tvFooterQualityL?.visibility = isQuality.isShow()
        tvFooterQualityH?.visibility = isQuality.isShow()
        tvHeaderQualityL?.visibility = isQuality.isShow()
        tvHeaderQualityH?.visibility = isQuality.isShow()
        if (isQuality) {
            tvHeaderQualityL?.text = "L"
            tvHeaderQualityH?.text = "H"
            tvFooterQualityL?.text = "${getIndexOrNullDefault(means, gradsNum, "-")}"
            tvFooterQualityH?.text = "${getIndexOrNullDefault(means, gradsNum + 1, "-")}"
        }
    }

    /**
     * 更新表和公式
     */
    private fun changeEquation() {
        val values = ArrayList<Entry>()
        if (curProject == null) {
            tvEquation?.text = ""
            lcCurve?.let { chart ->
                val set1 = chart.data.getDataSetByIndex(0) as LineDataSet
                set1.values = mutableListOf()
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.animateXY(300, 300)
            }
        }
        curProject?.let { curve ->
            if (curve.reactionValues != null && curve.reactionValues.isNotEmpty()) {
                values.addAll(getChartEntry(curve))
            }
            lcCurve?.let { chart ->
                if (chart.data != null && chart.data.dataSetCount > 0) {
                    val set1 = chart.data.getDataSetByIndex(0) as LineDataSet
                    set1.values = values
                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                    chart.animateXY(300, 300)
                }
            }
            tvEquation?.text =
                "${
                    getEquation(
                        selectFitterType,
                        mutableListOf(curve.f0, curve.f1, curve.f2, curve.f3)
                    )
                }\n ${getFitGoodness(selectFitterType, curve.fitGoodness)}"
        }
    }


    private fun setTarget() {
        tvHeader6?.visibility = (gradsNum > 5).isShow()
        tvHeader7?.visibility = (gradsNum > 6).isShow()
        tvHeader8?.visibility = (gradsNum > 7).isShow()
        tvHeader1?.text = getIndexOrNullDefault(targets, 0, "-")
        tvHeader2?.text = getIndexOrNullDefault(targets, 1, "-")
        tvHeader3?.text = getIndexOrNullDefault(targets, 2, "-")
        tvHeader4?.text = getIndexOrNullDefault(targets, 3, "-")
        tvHeader5?.text = getIndexOrNullDefault(targets, 4, "-")
        tvHeader6?.text = getIndexOrNullDefault(targets, 5, "-")
        tvHeader7?.text = getIndexOrNullDefault(targets, 6, "-")
        tvHeader8?.text = getIndexOrNullDefault(targets, 7, "-")
    }

    var onFitterTypeChange: ((selectFitterType: FitterType) -> Unit)? = null


    fun showDialog(
        gradsNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?,
        isQuality: Boolean,
        onFitterTypeChange: (selectFitterType: FitterType) -> Unit
    ) {
        initView()
        listenerView()

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
