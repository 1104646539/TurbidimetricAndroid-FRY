package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getIndexOrNullDefault
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.ex.nds
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.matchingargs.MatchingStateAdapter
import com.wl.turbidimetric.util.FitterType
import com.wl.wllib.LogToFile.i

/**
 * 显示拟合中的状态
 *
 * 比如已经拟合过的结果
 */
class MatchingStateDialog(val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_matching_state) {
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
    var tvFooter1: TextView? = null
    var tvFooter2: TextView? = null
    var tvFooter3: TextView? = null
    var tvFooter4: TextView? = null
    var tvFooter5: TextView? = null
    var tvFooter6: TextView? = null
    var tvFooter7: TextView? = null
    var tvFooter8: TextView? = null
    var rv: RecyclerView? = null
    var selectFitterType: FitterType = FitterType.Three
    var tvEquation: TextView? = null
    var lcCurve: LineChart? = null

    var abss: MutableList<MutableList<Double>> = mutableListOf()
    var matchingNum: Int = 5
    var targets: MutableList<Double> = mutableListOf()
    var means: MutableList<Double> = mutableListOf()

    var adapter: MatchingStateAdapter? = null
    private val bgGray = getResource().getColor(R.color.bg_gray)
    private val textColor = getResource().getColor(R.color.textColor)
    private val lineColor = getResource().getColor(R.color.themePositiveColor)
    override fun initDialogView() {
        vHeader = findViewById(R.id.incHeader)
        vFooter = findViewById(R.id.incFooter)
        rv = findViewById(R.id.rv)
        tvEquation = findViewById(R.id.tvEquation)
        lcCurve = findViewById(R.id.lcCurve)
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
        }
        initChart()
        i("$tvHeaderTitle $tvFooterTitle")
    }

    private fun initChart() {
        lcCurve?.let { chart ->
            val set1 = LineDataSet(arrayListOf(), null)
            set1.setDrawValues(false)//不绘制值在点上
            set1.setDrawIcons(false)//不绘制值icon在点上
            set1.color = lineColor
            set1.label = ""
            set1.enableDashedLine(6f, 6f, 0f)//线连成虚线
            set1.circleColors = listOf(lineColor)
            set1.circleRadius = 4f //点的半径
            set1.setDrawCircleHole(false)

            //数据集,一个数据集一条线
            val data = LineData(set1)

            //横向的轴
            val xAxis = chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM //数字显示在下方
            xAxis.gridColor = bgGray
            xAxis.axisLineColor = bgGray
            xAxis.textColor = textColor

            //竖轴 L是左边的，R是右边的
            val yAxisL = chart.axisLeft
            val yAxisR = chart.axisRight
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

            chart.animateXY(1000, 1000)

            //数据集的文字不显示，不启用
            chart.description?.isEnabled = false
            //数据集的色块不显示，不启用
            chart.legend?.isEnabled = false
            //空数据显示文字
            chart.setNoDataText("无数据")
            //设置数据，更新
            chart.data = data
        }
    }

    override fun setContent() {
        super.setContent()

        tvHeaderTitle?.text = "目标值"
        tvFooterTitle?.text = "平均值"

        setTarget()
        setMean()


        adapter = MatchingStateAdapter(matchingNum, abss)
        rv?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rv?.adapter = adapter

        changeEquation()

    }

    private fun setMean() {
        tvFooter6?.visibility = (matchingNum > 5).isShow()
        tvFooter7?.visibility = (matchingNum > 6).isShow()
        tvFooter8?.visibility = (matchingNum > 7).isShow()
        tvFooter1?.text = getIndexOrNullDefault(means, 0, "-")
        tvFooter2?.text = getIndexOrNullDefault(means, 1, "-")
        tvFooter3?.text = getIndexOrNullDefault(means, 2, "-")
        tvFooter4?.text = getIndexOrNullDefault(means, 3, "-")
        tvFooter5?.text = getIndexOrNullDefault(means, 4, "-")
        tvFooter6?.text = getIndexOrNullDefault(means, 5, "-")
        tvFooter7?.text = getIndexOrNullDefault(means, 6, "-")
        tvFooter8?.text = getIndexOrNullDefault(means, 7, "-")

    }

    /**
     * 更新表和公式
     */
    private fun changeEquation() {
        val values = ArrayList<Entry>()
        if (means != null && means.isNotEmpty()) {
            means.forEachIndexed { i, it ->
                if (i < matchingNum) {
                    values.add(Entry(targets[i].toFloat(), it.toFloat()))
                }
            }
        } else {
            return
        }
        lcCurve?.let { chart ->
            if (chart.data != null && lcCurve?.data?.dataSetCount!! > 0) {
                val set1 = chart.data.getDataSetByIndex(0) as LineDataSet
                set1.values = values
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.animateXY(300, 300)
            }
        }
    }

    private fun setTarget() {
        tvHeader6?.visibility = (matchingNum > 5).isShow()
        tvHeader7?.visibility = (matchingNum > 6).isShow()
        tvHeader8?.visibility = (matchingNum > 7).isShow()
        tvHeader1?.text = getIndexOrNullDefault(targets, 0, "-")
        tvHeader2?.text = getIndexOrNullDefault(targets, 1, "-")
        tvHeader3?.text = getIndexOrNullDefault(targets, 2, "-")
        tvHeader4?.text = getIndexOrNullDefault(targets, 3, "-")
        tvHeader5?.text = getIndexOrNullDefault(targets, 4, "-")
        tvHeader6?.text = getIndexOrNullDefault(targets, 5, "-")
        tvHeader7?.text = getIndexOrNullDefault(targets, 6, "-")
        tvHeader8?.text = getIndexOrNullDefault(targets, 7, "-")
    }

    fun showDialog(
        matchingNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        means: List<Double>,
        selectFitterType: FitterType
    ) {
        this.matchingNum = matchingNum
        this.abss.clear()
        this.abss.addAll(abss)
        this.targets.clear()
        this.targets.addAll(targets)
        this.means.clear()
        this.means.addAll(means)
        this.selectFitterType = selectFitterType

        this.confirmText = "添加拟合数据"
        this.confirmClick = {
            toast("click confirm")
        }
        this.confirmText2 = "拟合"
        this.confirmClick2 = {
            toast("click matching")
        }
        this.cancelText = "取消"
        this.cancelClick = {
            toast("click cancel")
        }

        if (isCreated) {
            setContent()
        }
        super.show()
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }
}
