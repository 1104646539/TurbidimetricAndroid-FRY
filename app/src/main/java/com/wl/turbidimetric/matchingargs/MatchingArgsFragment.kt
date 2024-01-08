package com.wl.turbidimetric.matchingargs

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentMatchingArgsBinding
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal.obTestState
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.view.dialog.CoverProjectDialog
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.MatchingConfigDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.wllib.LogToFile.i
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.view.dialog.ICON_HINT
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    var projects: List<CurveModel>? = null

    companion object {
        @JvmStatic
        fun newInstance() = MatchingArgsFragment()
    }

    override fun initViewModel() {
        vd.model = vm
    }

    private val dialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }
    private val finishCoverDialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    private val adapter: MatchingArgsAdapter by lazy {
        MatchingArgsAdapter()
    }
    private val coverProjectDialog: CoverProjectDialog by lazy {
        CoverProjectDialog(requireContext())
    }

    /**
     * 显示调试时详情的对话框
     */
    private val debugShowDetailsDialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    /**
     * 拟合设置对话框
     */
    private val matchingConfigDialog: MatchingConfigDialog by lazy {
        MatchingConfigDialog(requireContext())
    }

    override fun init(savedInstanceState: Bundle?) {
        initView()
        listener()
    }

    private fun listener() {
        listenerDialog()
        listenerView()
    }

    private fun initView() {
        initCurveView()
        test()
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

    private fun test() {

    }

    private fun listenerView() {
        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.adapter = adapter

        lifecycleScope.launch {
            vm.datas.collectLatest {
                it.filterIndexed { index, _ ->
                    index < 10
                }.let { ret ->
                    projects = ret
                    adapter.submit(ret.toMutableList())
                    if (ret.isNotEmpty()) {
                        adapter.setSelectIndex(ret.lastIndex)
                        adapter.notifyItemChanged(ret.lastIndex)
                    }
                }
//                adapter.submit(it)
                //默认选择最近一个
//                if (adapter.selectPos < 0 && adapter.items.isNotEmpty()) {
//                adapter.setSelectIndex(0)
//                adapter.notifyItemChanged(0)

//                }
            }
        }
//        vm.toastMsg.observe(this) { msg ->
//            i("msg=$msg")
//            snack(vd.root, msg)
//        }

        adapter.onSelectChange = { project ->
            i("选中的=${project}")
            changeCurve(project)
        }

        lifecycleScope.launch {
            obTestState.collectLatest {
                if (it.isRunning()) {
                    vd.btnStart.setBackgroundResource(R.drawable.rip_positive2)
                    vd.btnStart.setText("正在拟合")
                    vm.configEnable.postValue(false)
                } else {
                    vd.btnStart.setBackgroundResource(R.drawable.rip_positive)
                    vd.btnStart.setText("开始拟合")
                    vm.configEnable.postValue(true)
                }
            }
        }
        lifecycleScope.launch {
            vm.curveUiState.collectLatest {
                vd.tvEquationText.text = it.equationText
                vd.tvFitGoodnessText.text = it.fitGoodnessText
            }
        }
        vd.btnStart.setOnClickListener {
            startMatching()
        }
        vd.btnPrint.setOnClickListener {
            vm.print()
        }

        vd.btnDebugDialog.setOnClickListener {
            debugShowDetailsDialog.showPop(requireContext(), width = 1500) {
                it.showDialog(
                    vm.testMsg.value ?: "",
                    "确定",
                    confirmClick = { it.dismiss() }, textGravity = Gravity.LEFT
                )
            }
        }
    }


    private fun startMatching() {
        if (testState.isNotPrepare()) {
            toast("请重新自检")
            return
        } else if (testState.isRunning()) {
            toast("正在检测")
            return
        }
//        vm.showMatchingSettingsDialog()
        showCoverDialog()
    }

    private fun showCoverDialog() {
        if (adapter.items.isNullOrEmpty() || adapter.items.size < 10) {
            vm.clickStart(null)
            return
        }
        coverProjectDialog.showPop(
            requireContext(),
            isCancelable = false,
            width = 1000,
        ) {
            it.show(adapter.items, onConfirm = { projectModel, baseDialog ->
                if (projectModel == null) {
                    toast("未选择覆盖的标曲，取消拟合！")
                    testState = TestState.Normal
                } else {
                    vm.clickStart(projectModel)
                }
                baseDialog.dismiss()
            }, onCancel = {
                toast("未选择覆盖的标曲，取消拟合！")
                testState = TestState.Normal
                it.dismiss()
            })
        }
    }

    /**
     * 显示选中标曲的详情
     * @param project ProjectModel
     */
    private fun changeCurve(project: CurveModel) {
        val values = ArrayList<Entry>()

        if (project.reactionValues != null && project.reactionValues!!.isNotEmpty()) {
            project.reactionValues?.forEachIndexed { i, it ->
                if (i < 5) {
                    values.add(Entry(nds[i].toFloat(), it.toFloat()))
                }
            }
        } else {
            return
        }

        vm.changeSelectProject(project)

        if (vd.lcCurve.data != null && vd.lcCurve.data.dataSetCount > 0) {
            val set1 = vd.lcCurve.data.getDataSetByIndex(0) as LineDataSet
            set1.values = values
            vd.lcCurve.data.notifyDataChanged()
            vd.lcCurve.notifyDataSetChanged()

            vd.lcCurve.animateXY(300, 300)
        }
    }

    private fun listenerDialog() {
        lifecycleScope.launch {
            vm.dialogUiState.collect {
                state->
                when (state.dialogState) {
                    DialogState.GetStateNotExistMsg -> {//开始检测，确实清洗液等
                        dialog.showPop(requireContext()) { dialog ->
                            dialog.showDialog(
                                msg = "${state.msg}",
                                confirmText = "我已添加",
                                confirmClick = {
                                    dialog.dismiss()
                                    vm.dialogGetStateNotExistConfirm()
                                },
                                cancelText = "结束检测",
                                cancelClick = {
                                    dialog.dismiss()
                                    vm.dialogGetStateNotExistCancel()
                                }
                            )
                        }
                    }

                    DialogState.MatchingFinishMsg -> {//检测结束，提示是否保存
//                        vm.saveProject()
                        val msg = state.msg.plus("\n确定保存该条标曲记录？\n\n")
                        finishCoverDialog.showPop(
                            requireContext(),
                            width = 1500,
                            isCancelable = false
                        ) { dialog ->
                            dialog.showDialog(
                                msg = msg,
                                confirmText = "保存", confirmClick = {
                                    vm.saveProject()
                                    dialog.dismiss()
                                }, cancelText = "取消", cancelClick = {
                                    vm.notSaveProject()
                                    dialog.dismiss()
                                }, textGravity = Gravity.LEFT
                            )
                        }

                    }

                    DialogState.ACCIDENT -> {//意外的检测结束等
                        dialog.showPop(requireContext()) { dialog ->
                            dialog.showDialog(
                                msg = "${state.msg}",
                                confirmText = "我知道了",
                                confirmClick = {
                                    dialog.dismiss()

                                }
                            )
                        }
                    }

                    DialogState.MatchingSettings -> {//拟合配置
                        matchingConfigDialog.showPop(requireContext(), width = 1000) { dialog ->
                            dialog.showDialog(
                                vm.projects,
                                vm.autoAttenuation,
                                vm.matchingNum,
                                vm.selectMatchingProject,
                                vm.selectFitterType,
                                vm.targetCons,
                                { matchingNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Int> ->
                                    vm.matchingConfigFinish(matchingNum,autoAttenuation,selectProject,selectFitterType,cons)
                                }
                            ) {}
                        }
                    }
                    DialogState.MatchingState -> {//拟合状态

                    }
                    /**
                     * 命令提示错误，中断所有程序
                     */
                    DialogState.STATE_FAILED->{
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = state.msg,
                                confirmText = "我知道了",
                                confirmClick = { baseDialog ->
                                    baseDialog.dismiss()
                                },
                                showIcon = true, iconId = ICON_HINT
                            )
                        }
                    }
                    else -> {

                    }
                }
            }
        }
        /**
         * 显示调试的数据
         */
        vm.testMsg.observe(this) {
            i("it=$it")
            if (debugShowDetailsDialog.isShow) {
                debugShowDetailsDialog.showPop(
                    requireContext(), width = 1500,
                    isCancelable = true
                ) { d ->
                    d.showDialog(
                        it,
                        "确定",
                        confirmClick = { d.dismiss() },
                        textGravity = Gravity.LEFT
                    )
                }
            }
        }
    }


}


