package com.wl.turbidimetric.matchingargs

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.wllib.LogToFile.i
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.view.MatchingConfigLayout
import com.wl.turbidimetric.view.dialog.MatchingConfigDialog
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
        vm.listener()
        listenerData()
        listenerView()
    }

    private fun initView() {
        test()
    }


    private fun test() {

    }

    private fun listenerView() {
        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.adapter = adapter

//        val gm = GridLayoutManager(requireContext(), 1, LinearLayoutManager.VERTICAL, false)
//        gm.spanSizeLookup = object : SpanSizeLookup() {
//            override fun getSpanSize(position: Int): Int {
//                return if (position == 0) 1 else 2
//            }
//        }
//        vd.rvParams.layoutManager =
//            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        vd.rvParams.adapter = matchingArgsInfoAdapter
//        vd.rvParams.addItemDecoration(
//            requireContext().dividerBuilder()
//                .apply {
//                    color(resources.getColor(R.color.black2))
//                    size(1)
//                }
//                .build()
//        )


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
                    projects?.let { ps ->
                        vm.updateCurves(ps)
                    }
                }
            }
        }

        adapter.onSelectChange = { project ->
            u("选中的=${project}")
            changeCurve(project)
        }

        lifecycleScope.launch {
            launch {
                appVm.obTestState.collectLatest {
                    changeAnalyseState(it)
                }
            }
            launch {
                appVm.obReactionTemp.collectLatest {
                    changeAnalyseState(appVm.testState)
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
        vm.configEnable.observe(this@MatchingArgsFragment) {
            enableView(it)
        }
    }

    private fun enableView(enable: Boolean) {
        vd.vMatching.isEnabled = enable
        vd.vConfig.isEnabled = enable
    }

    private fun changeAnalyseState(testState: TestState) {
        if (!appVm.testType.isMatchingArgs() && testState.isRunning()) {
            vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg)
            vd.tvMatching.setText("正在运行……")
            vm.configEnable.postValue(false)
        } else {
            if (testState.isRunning()) {
                vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vd.tvMatching.setText("生成曲线中……")
                vm.configEnable.postValue(false)
            } else if (testState.isRunningError()) {
                vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vd.tvMatching.setText("运行错误")
                vm.configEnable.postValue(false)
            } else if (!appVm.getTempCanBeTest()) {
                vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vd.tvMatching.setText("正在预热")
                vm.configEnable.postValue(false)
            } else {
                vd.vMatching.setBackgroundResource(R.drawable.shape_analyse_test_bg2)
                vd.tvMatching.setText("开始拟合")
                vm.configEnable.postValue(true)
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
        vd.cdvDetails.update(project)
    }

    private fun listenerData() {
        lifecycleScope.launch {
            vm.dialogUiState.collect { state ->
                i("state=$state")
                when (state) {
                    is MatchingArgsDialogUiState.GetStateNotExistMsg -> {//开始检测，确实清洗液等
                        dialog.showPop(requireContext(), isCancelable = false) { dialog ->
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

                    is MatchingArgsDialogUiState.HiltNotSaveDialog -> {//二次提示是否不保存已经拟合的结果
                        dialog.showPop(requireContext(), isCancelable = false) { dialog ->
                            dialog.showDialog(
                                msg = "${state.msg}",
                                confirmText = "确定",
                                confirmClick = {
                                    dialog.dismiss()
                                    vm.clearMatchingState()
                                },
                                cancelText = "取消",
                                cancelClick = {
                                    dialog.dismiss()
                                }
                            )
                        }
                    }

                    is MatchingArgsDialogUiState.Accident -> {//意外的检测结束等
                        dialog.showPop(requireContext(), isCancelable = false) { dialog ->
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
                            state.curves,
                            state.qualityLow1,
                            state.qualityLow2,
                            state.qualityHigh1,
                            state.qualityHigh2,
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
                            state.isError,
                            state.matchingType,
                            state.qualityLow1,
                            state.qualityLow2,
                            state.qualityHigh1,
                            state.qualityHigh2,
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
        curves: MutableList<CurveModel>,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
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
            { matchingType: MatchingConfigLayout.MatchingType, selectCurve: CurveModel?, qualityLow1: Int, qualityLow2: Int, qualityHigh1: Int, qualityHigh2: Int, reagentNo: String,
              quality: Boolean, matchingNum: Int, autoAttenuation: Boolean, selectProject: ProjectModel?, selectFitterType: FitterType, cons: List<Double> ->
                vm.matchingConfigFinish(
                    matchingType,
                    selectCurve,
                    qualityLow1,
                    qualityLow2,
                    qualityHigh1,
                    qualityHigh2,
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
        isError: Boolean,
        matchingType: MatchingConfigLayout.MatchingType,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
        gradsNum: Int,
        abss: MutableList<Double>,
        targets: List<Double>,
        quality: Boolean,
        means: List<Double>,
        selectFitterType: FitterType,
        curProject: CurveModel?
    ) {
        matchingConfigDialog.showDialogStep2(isError,
            matchingType, qualityLow1, qualityLow2, qualityHigh1, qualityHigh2,
            gradsNum, abss, targets, means, selectFitterType, curProject, quality, {
                //开始
                vm.clickStart()
            },
            {//拟合完成、保存
                vm.showSaveMatchingDialog()
            },
            {//打印质控结果
                    result ->
                vm.printQuality(result)
            },
            {//放弃拟合、不保存
                vm.notSaveProject()
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


}


