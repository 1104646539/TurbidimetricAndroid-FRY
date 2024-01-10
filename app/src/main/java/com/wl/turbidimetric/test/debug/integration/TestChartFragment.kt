package com.wl.turbidimetric.test.debug.integration

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentTestChartBinding
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.ex.toast
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TestChartFragment :
    BaseFragment<TestChartViewModel, FragmentTestChartBinding>(R.layout.fragment_test_chart) {
    override val vm: TestChartViewModel by viewModels()
    private val bgGray = getResource().getColor(R.color.bg_gray)
    private val textColor = getResource().getColor(R.color.textColor)
    private val lineColor = getResource().getColor(R.color.themePositiveColor)

    val temp = mutableListOf<Int>()
    override fun onResume() {
        super.onResume()
        vm.listener()
    }

    override fun onPause() {
        super.onPause()
        vm.clearListener()
        vm.stopIntervalTest()
        i("onPause")
    }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        initCurveView()
        listener()
    }

    private fun initCurveView() {
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
        val xAxis = vd.lcCurve.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM //数字显示在下方
        xAxis.gridColor = bgGray
        xAxis.axisLineColor = bgGray
        xAxis.textColor = textColor

        //竖轴 L是左边的，R是右边的
        val yAxisL = vd.lcCurve.axisLeft
        val yAxisR = vd.lcCurve.axisRight
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

        vd.lcCurve.animateXY(1000, 1000)

        //数据集的文字不显示，不启用
        vd.lcCurve.description.isEnabled = false
        //数据集的色块不显示，不启用
        vd.lcCurve.legend.isEnabled = false
        //空数据显示文字
        vd.lcCurve.setNoDataText("无数据")
        //设置数据，更新
        vd.lcCurve.data = data
    }

    /**
     * 显示选中标曲的详情
     * @param project ProjectModel
     */
    private fun changeCurve(points: MutableList<Int>) {
        val values = ArrayList<Entry>()

        points.forEachIndexed { index, item ->
            values.add(Entry(index.toFloat(), item.toFloat()))
        }

        if (vd.lcCurve.data != null && vd.lcCurve.data.dataSetCount > 0) {
            val set1 = vd.lcCurve.data.getDataSetByIndex(0) as LineDataSet
            set1.values = values
            vd.lcCurve.data.notifyDataChanged()
            vd.lcCurve.notifyDataSetChanged()

            vd.lcCurve.animateXY(300, 300)
        }
    }

    private fun listener() {
        listenerView()
        listenerEvent()
    }

    private fun listenerView() {
        lifecycleScope.launch {
            vm.point.observe(this@TestChartFragment) {
                temp.add(0, it)
                vd.tvMsg.post {
                    vd.tvMsg.text = temp.joinToString()
//                    vd.sv.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
        lifecycleScope.launch {
            vm.testMsg.collectLatest {
                toast(it)
            }
        }
    }

    private fun listenerEvent() {
        vd.btnStartIntervalTest.setOnClickListener {
            vm.startIntervalTest(vd.tetIntervalDuration.text.toString())
        }
        vd.btnStopIntervalTest.setOnClickListener {
            vm.stopIntervalTest()
        }
        vd.btnCreateChart.setOnClickListener {
            changeCurve(temp)
        }
        vd.btnClearChart.setOnClickListener {
            vd.tvMsg.text = ""
            temp.clear()
            changeCurve(temp)
        }
    }

    override fun onDestroyView() {
        vm.stopIntervalTest()
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance() = TestChartFragment()
    }
}
