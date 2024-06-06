package com.wl.turbidimetric.matchingargs

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.lxj.xpopup.XPopup
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentMatchingArgsBinding
import com.wl.turbidimetric.ex.*
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
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.wllib.LogToFile.u
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


    override fun init(savedInstanceState: Bundle?) {
        initView()
        listener()
    }

    private fun listener() {
        listenerData()
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
        set1.enableDashedLine(10f, 0f, 0f)//线连成虚线
        set1.circleColors = listOf(lineColor)
        set1.circleRadius = 1f //点的半径
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
            }
        }

        adapter.onSelectChange = { project ->
            u("选中的=${project}")
            changeCurve(project)
        }

        lifecycleScope.launch {
            appVm.obTestState.collectLatest {
                if (!appVm.testType.isMatchingArgs() && it.isRunning()) {
                    vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                    vd.tvMatching.setText("正在运行……")
                    vm.configEnable.postValue(false)
                } else {
                    if (it.isRunning()) {
                        vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                        vd.tvMatching.setText("生成曲线中……")
                        vm.configEnable.postValue(false)
                    } else {
                        vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg2)
                        vd.tvMatching.setText("开始拟合")
                        vm.configEnable.postValue(true)
                    }
                }
            }
        }
        lifecycleScope.launch {
            vm.curveUiState.collectLatest {
                vd.tvEquationText.text = it.equationText
                vd.tvFitgoodnessText.text = it.fitGoodnessText
            }
        }
        vd.vMatching.setOnClickListener {
            u("开始拟合")
            startMatching()
        }
        vd.vConfig.setOnClickListener {
            u("拟合配置")
            startMatching()
//            if (appVm.testState.isTestRunning()) {
//                toast("正在运行，请稍后")
//            } else {
//                vm.showMatchingSettingsDialog()
//            }
        }
        vd.btnPrint.setOnClickListener {
            u("打印")
            vm.print()
        }
        lifecycleScope.launch {
            appVm.obTestState.collectLatest {
                if (it.isRunning() && matchingConfigDialog.isShow) {
                    matchingConfigDialog.testingUI()
                } else {
                    matchingConfigDialog.noTestUI()
                }
            }
        }
    }


    private fun startMatching() {
        vm.startMatching()
    }

    /**
     * 显示选择要覆盖曲线的对话框
     */
    private fun showCoverDialog() {
        coverProjectDialog.showPop(
            requireContext(),
            isCancelable = false,
            width = 1000,
        ) {
            it.show(adapter.items, onConfirm = { projectModel, baseDialog ->
                if (projectModel == null) {
                    toast("未选择覆盖的标曲，取消拟合！")
                    appVm.testState = TestState.Normal
                } else {
                    vm.saveCoverCurve(projectModel)
                    vm.showMatchingSettingsDialog()
                }
                baseDialog.dismiss()
            }, onCancel = {
                toast("未选择覆盖的标曲，取消拟合！")
                appVm.testState = TestState.Normal
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

        if (project.reactionValues != null && project.reactionValues.isNotEmpty()) {
            values.addAll(getChartEntry(project))
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

    private fun listenerData() {
        lifecycleScope.launch {
            vm.dialogUiState.collect { state ->
                when (state) {
                    is MatchingArgsDialogUiState.GetStateNotExistMsg -> {//开始检测，确实清洗液等
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

                    is MatchingArgsDialogUiState.MatchingFinishMsg -> {//检测结束，提示是否保存
                        showMatchingFinishDialog(
                            state.reagnetNo,
                            state.gradsNum,
                            state.abss,
                            state.targets,
                            state.means,
                            state.selectFitterType,
                            state.curProject,
                            state.isQuality
                        )
                    }

                    is MatchingArgsDialogUiState.Accident -> {//意外的检测结束等
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

                    is MatchingArgsDialogUiState.CloseMatchingStateDialog -> {//关闭拟合中的对话框
                        if (matchingConfigDialog.isShow) {
                            matchingConfigDialog.dismiss()
                        }
                    }

                    is MatchingArgsDialogUiState.MatchingSettings -> {//拟合配置
                        showMatchingConfigDialog(
                            state.reagentNo,
                            state.quality,
                            state.projects,
                            state.autoAttenuation,
                            state.gradsNum,
                            state.selectProject,
                            state.selectFitterType,
                            state.targetCons
                        )
                    }

                    is MatchingArgsDialogUiState.MatchingState -> {//拟合中状态
                        showMatchingStateDialog(
                            state.gradsNum,
                            state.abss,
                            state.targets,
                            state.quality,
                            state.means,
                            state.selectFitterType,
                            state.curProject,
                        )
                    }

                    is MatchingArgsDialogUiState.MatchingCoverCurve -> {//选择要覆盖的曲线
                        showCoverDialog()
                    }
                    /**
                     * 命令提示错误，中断所有程序
                     */
                    is MatchingArgsDialogUiState.StateFailed -> {
//                        dialog.showPop(requireContext(), isCancelable = false) {
//                            it.showDialog(
//                                msg = state.msg,
//                                confirmText = "我知道了",
//                                confirmClick = { baseDialog ->
//                                    baseDialog.dismiss()
//                                },
//                                showIcon = true, iconId = ICON_HINT
//                            )
//                        }
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
        /**
         * 拟合配置信息
         */
        lifecycleScope.launch {
            vm.matchingConfigUiState.collectLatest {
                vd.tvSettingsReagentNo.text = "序号：${it.reagentNo}"
                vd.tvSettingsQuality.text = "同时质控：${if (it.quality) "是" else "否"}"
                vd.tvSettingsProject.text = "选择项目：${it.project?.projectName ?: "-"}"
                vd.tvSettingsGradsNum.text = "梯度数量：${it.gradsNum}"
                vd.tvSettingsFun.text = "选择方程：${it.selectFitterType.showName}"
                vd.tvSettingsAutoAttenuation.text =
                    "自动稀释：${if (it.autoAttenuation) "是" else "否"}"
                vd.tvSettingsTargetCon.text = "目标浓度：${it.targetCons.joinToString("  ")}"
            }
        }
    }

    val matchingConfigDialog: MatchingConfigDialog by lazy {
        return@lazy XPopup.Builder(requireActivity())
            .dismissOnTouchOutside(false)
            .dismissOnBackPressed(false)
            .autoOpenSoftInput(false)
            .autoFocusEditText(false)
            .isDestroyOnDismiss(false)
            .asCustom(MatchingConfigDialog(requireContext())) as MatchingConfigDialog
    }

    /**
     * 显示拟合设置对话框
     */
    private fun showMatchingConfigDialog(
        reagentNo: String,
        quality: Boolean,
        projects: List<ProjectModel>,
        autoAttenuation: Boolean,
        gradsNum: Int = 5,
        selectProject: ProjectModel? = null,
        selectFitterType: FitterType = FitterType.Three,
        targetCons: List<Double> = mutableListOf(),
    ) {
        matchingConfigDialog.showDialogStep1(
            reagentNo,
            quality,
            projects,
            autoAttenuation,
            gradsNum,
            selectProject,
            selectFitterType,
            targetCons,
            { reagentNo: String,
              quality: Boolean, matchingNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Double> ->
                vm.matchingConfigFinish(
                    reagentNo,
                    quality,
                    matchingNum,
                    autoAttenuation,
                    selectProject,
                    selectFitterType,
                    cons
                )
            },
        ) {
            it.dismiss()
        }
    }

    /**
     * 显示拟合中状态对话框
     */
    private fun showMatchingStateDialog(
        gradsNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        quality: Boolean,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?
    ) {
        matchingConfigDialog.showDialogStep2(
            gradsNum, abss, targets, means, selectFitterType, curProject, quality, {//开始
                vm.clickStart()
//                vm.showMatchingSettingsDialog()
            }, {//拟合结束
                vm.showSaveMatchingDialog()
            },
            {
                vm.changeFitterType(it)
            },
            SystemGlobal.isDebugMode,
            {
                u("调试框")
                debugShowDetailsDialog.showPop(requireContext(), width = 1500) {
                    it.showDialog(
                        vm.testMsg.value ?: "",
                        "确定",
                        confirmClick = { it.dismiss() }, textGravity = Gravity.LEFT
                    )
                }
            }
        )
    }

    /**
     * 显示拟合结果对话框
     */
    private fun showMatchingFinishDialog(
        reagnetNo: String,
        gradsNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?,
        isQuality: Boolean,
    ) {
        matchingConfigDialog.showDialogStep3(
            reagnetNo,
            gradsNum,
            abss,
            targets,
            means,
            selectFitterType,
            curProject,
            isQuality,
            {
                vm.showMatchingStateDialog()
            }, {
                vm.saveProject()
                it.dismiss()
            },
            {
                vm.notSaveProject()
                it.dismiss()
            })
    }


}


