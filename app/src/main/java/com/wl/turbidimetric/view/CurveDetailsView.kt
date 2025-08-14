package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fondesa.recyclerviewdivider.dividerBuilder
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getChartEntry
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.matchingargs.MatchingArgsInfoAdapter
import com.wl.turbidimetric.model.CurveModel

class CurveDetailsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet) {
    private var root: View? = null
    private var rvParams: RecyclerView? = null
    private var lcCurve: LineChart? = null
    private var tvEmpty: TextView? = null

    private val bgGray = getResource().getColor(R.color.bg_gray)
    private val textColor = getResource().getColor(R.color.textColor)
    private val lineColor = getResource().getColor(R.color.themePositiveColor)
    private val matchingArgsInfoAdapter: MatchingArgsInfoAdapter by lazy {
        MatchingArgsInfoAdapter()
    }

    init {
        root = LayoutInflater.from(context).inflate(R.layout.layout_curve_details, this, true)
        initView()
        listenerView()

    }

    private fun listenerView() {

    }

    private fun initView() {
        rvParams = root?.findViewById(R.id.rv_params);
        lcCurve = root?.findViewById(R.id.lc_curve);
        tvEmpty = root?.findViewById(R.id.tv_empty);

        initCurveView()
        initParamsView()

    }

    fun setChartHeight(height: Int) {
        lcCurve?.let { view ->
            view.layoutParams = FrameLayout.LayoutParams(view.layoutParams.width, height)
        }
    }
    fun updateTextSize(type1Size: Float, type2Size: Float) {
        matchingArgsInfoAdapter.updateTextSize(type1Size,type2Size)
    }

    fun update(curve: CurveModel?) {
        updateChart(curve)
        updateParams(curve)
    }

    private fun updateParams(curve: CurveModel?) {
        matchingArgsInfoAdapter.update(curve)
    }

    private fun updateChart(curve: CurveModel?) {
//        curve?.let { curve ->
        val values = ArrayList<Entry>()
        if (curve?.reactionValues != null && curve.reactionValues.isNotEmpty()) {
            values.addAll(getChartEntry(curve))
            tvEmpty?.visibility = View.GONE
            lcCurve?.visibility = View.VISIBLE
        } else {
            tvEmpty?.visibility = View.VISIBLE
            lcCurve?.visibility = View.INVISIBLE
            return
        }
        lcCurve?.let { lcCurve ->
            if (lcCurve.data != null && lcCurve.data.dataSetCount > 0) {
                val set1 = lcCurve.data.getDataSetByIndex(0) as LineDataSet
                set1.values = values
                lcCurve.data.notifyDataChanged()
                lcCurve.notifyDataSetChanged()

                lcCurve.animateXY(300, 300)
            }
        }
//        }
    }

    private fun initParamsView() {
        rvParams?.let { rvParams ->
            rvParams.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            rvParams.adapter = matchingArgsInfoAdapter
            rvParams.addItemDecoration(
                context.dividerBuilder()
                    .apply {
                        color(resources.getColor(R.color.black2))
                        size(1)
                    }
                    .build()
            )
        }
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
            val xAxis = lcCurve!!.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM //数字显示在下方
            xAxis.gridColor = bgGray
            xAxis.axisLineColor = bgGray
            xAxis.textColor = textColor

            //竖轴 L是左边的，R是右边的
            val yAxisL = lcCurve!!.axisLeft
            val yAxisR = lcCurve!!.axisRight
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
}
