package com.wl.turbidimetric.matchingargs

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentMatchingArgsBinding
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal.matchingTestState
import com.wl.turbidimetric.global.SystemGlobal.obMatchingTestState
import com.wl.turbidimetric.model.MatchingArgState
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.view.CoverProjectDialog
import com.wl.turbidimetric.view.HiltDialog
import com.wl.wwanandroid.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.wl.wllib.LogToFile.i

/**
 * 拟合参数
 * @property viewModel MatchingArgsViewModel
 */
class MatchingArgsFragment :
    BaseFragment<MatchingArgsViewModel, FragmentMatchingArgsBinding>(R.layout.fragment_matching_args) {
    override val vm: MatchingArgsViewModel by viewModels {
        MatchingArgsViewModelFactory()
    }
    private val bgGray = getResource().getColor(R.color.bg_gray)
    private val textColor = getResource().getColor(R.color.textColor)
    private val lineColor = getResource().getColor(R.color.themePositiveColor)

    companion object {
        @JvmStatic
        fun newInstance() =
            MatchingArgsFragment()
    }

    private val dialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    override fun initViewModel() {
        vd.model = vm
    }

    private val adapter: MatchingArgsAdapter by lazy {
        MatchingArgsAdapter()
    }

    /**
     * 显示调试时详情的对话框
     */
    private val debugShowDetailsDialog: HiltDialog by lazy {
        HiltDialog(requireContext()).apply {
            width = 1500
        }
    }

    override fun init(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                initView()
                listener()
            }
        }

    }

    private fun listener() {
        listenerDialog()
        listenerView()
    }

    private fun initView() {
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

        test()
    }

    private fun test() {

    }

    private fun listenerView() {
        /**
         * 显示调试的数据
         */
        vm.testMsg.observe(this) {
            i("it=$it")
            if (debugShowDetailsDialog.isShow()) {
                debugShowDetailsDialog.show(it, "确定", onConfirm = { it.dismiss() })
            }
        }

        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.adapter = adapter

        lifecycleScope.launch {
            vm.datas.collectLatest {
                adapter.submit(it)
                //默认选择最近一个
//                if (adapter.selectPos < 0 && adapter.items.isNotEmpty()) {
                adapter.setSelectIndex(0)
                adapter.notifyItemChanged(0)
//                }
            }
        }
        vm.toastMsg.observe(this) { msg ->
            i("msg=$msg")
            snack(vd.root, msg)
        }

        adapter.onSelectChange = { project ->
            i("选中的=${project}")
            changeCurve(project)
        }

        obMatchingTestState.observe(this) {
            if (it != MatchingArgState.None && it != MatchingArgState.Finish) {
                vd.btnStart.setBackgroundResource(R.drawable.rip_positive2)
                vd.btnStart.setText("正在拟合")
                vm.qualityEnable.postValue(false)
                vm.reagentNoEnable.postValue(false)
            } else {
                vd.btnStart.setBackgroundResource(R.drawable.rip_positive)
                vd.btnStart.setText("开始拟合")
                vm.qualityEnable.postValue(true)
                vm.reagentNoEnable.postValue(true)
            }
        }
        vd.btnStart.setOnClickListener {
            startMatching();
//            dialog.show("asdfasdfasfasdflkjaaaaaaaaaakjllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllasdffafsdfasf","确定",{},"取消",{})
        }
        vd.btnPrint.setOnClickListener {
            vm.print()
        }

//        vd.btnDebug2.setOnClickListener {
//            DBManager.ProjectBox.put(ProjectModel().apply {
//                reagentNO = "5452"
//                reactionValues = intArrayOf(60, 91, 2722, 11722, 27298)
//                f0 = 9.702673786
//                f1 = 0.7425860767
//                f2 = -4.513632E-4
//                f3 = 1.406E-7
//                projectLjz = 100
//                fitGoodness = 0.9998
//                createTime = Date().toLongString()
//            })
//        }

        vd.btnDebugDialog.setOnClickListener {
            debugShowDetailsDialog.show(vm.testMsg.value ?: "", "确定", onConfirm = { it.dismiss() })
        }
    }

    val coverProjectDialog: CoverProjectDialog by lazy {
        CoverProjectDialog(requireContext())
    }

    private fun startMatching() {
        if (matchingTestState != MatchingArgState.None && matchingTestState != MatchingArgState.Finish) {
            toast("正在拟合")
            return
        }

        showCoverDialog()
    }

    private fun showCoverDialog() {
        if (adapter.items.isNullOrEmpty() || adapter.items.size < 10) {
            vm.clickStart(null)
            return
        }
        coverProjectDialog.show(adapter.items, onConfirm = { projectModel, baseDialog ->
            if (projectModel == null) {
                toast("未选择覆盖的标曲，取消拟合！")
                matchingTestState == MatchingArgState.None
            } else {
                vm.clickStart(projectModel)
            }
            baseDialog.dismiss()
        }, onCancel = {
            toast("未选择覆盖的标曲，取消拟合！")
            matchingTestState == MatchingArgState.None
            it.dismiss()
        })
    }

    /**
     * 显示选中标曲的详情
     * @param project ProjectModel
     */
    private fun changeCurve(project: ProjectModel) {
        val values = ArrayList<Entry>()

        if (project.reactionValues != null && project.reactionValues!!.isNotEmpty() && project.reactionValues!!.size == 5) {
            project.reactionValues?.forEachIndexed { i, it ->
                values.add(Entry(nds[i].toFloat(), it.toFloat()))
            }
        } else {
            return
        }


        //方程和拟合度
        vm.equationText.postValue(
            "Y=${project.f0.scale(8)}+${project.f1.scale(8)}x+${
                project.f2.scale(
                    8
                )
            }x²+${project.f3.scale(8)}x³"
        )
        vm.fitGoodnessText.postValue("R²=${project.fitGoodness.scale(6)}")


        if (vd.lcCurve.data != null &&
            vd.lcCurve.data.dataSetCount > 0
        ) {
            val set1 = vd.lcCurve.data.getDataSetByIndex(0) as LineDataSet
            set1.values = values
            vd.lcCurve.data.notifyDataChanged()
            vd.lcCurve.notifyDataSetChanged()

            vd.lcCurve.animateXY(300, 300)
        }
    }

    private fun listenerDialog() {
        /**
         * 开始检测 比色皿,样本，试剂不足
         */
        vm.getStateNotExistMsg.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                dialog.show(
                    msg = "${vm.getStateNotExistMsg.value}",
                    confirmMsg = "我已添加", onConfirm = {
                        it.dismiss()
                        vm.dialogGetStateNotExistConfirm()
                    },
                    cancelMsg = "结束检测", onCancel = {
                        it.dismiss()
                        vm.dialogGetStateNotExistCancel()
                    }
                )
            }
        }
        vm.matchingFinishMsg.observe(this) {
            if (it.isNotEmpty()) {
                vm.saveProject()
//                val msg = it.plus("确定保存该条标曲记录？")
//                dialog.show(
//                    msg = msg,
//                    confirmMsg = "保存", onConfirm = {
//                        vm.saveProject()
//                        it.dismiss()
//                    }, cancelMsg = "取消", onCancel = {
//                        it.dismiss()
//                    }
//                )
            }
        }
    }

}
