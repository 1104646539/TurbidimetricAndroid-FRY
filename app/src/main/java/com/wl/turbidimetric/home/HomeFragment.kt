package com.wl.turbidimetric.home

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentHomeBinding
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.Item
import com.wl.turbidimetric.model.SampleState
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.service.OnGetPatientCallback
import com.wl.turbidimetric.view.ShelfView
import com.wl.turbidimetric.view.dialog.GetTestPatientInfoDialog
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.HomeConfigDialog
import com.wl.turbidimetric.view.dialog.HomeDetailsDialog
import com.wl.turbidimetric.view.dialog.ICON_FINISH
import com.wl.turbidimetric.view.dialog.ICON_HINT
import com.wl.turbidimetric.view.dialog.PatientInfoDialog
import com.wl.turbidimetric.view.dialog.isShow
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.wllib.LogToFile.i
import com.wl.wllib.LogToFile.u
import com.wl.wllib.toLongTimeStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>(R.layout.fragment_home) {
    private val TAG = "HomeFragment"

    /**
     * 最新的项目
     */
    private val projects: MutableList<CurveModel> = mutableListOf()
    private val r2VolumeIds = intArrayOf(
        R.drawable.state_reagent_r2_0,
        R.drawable.state_reagent_r2_1,
        R.drawable.state_reagent_r2_2,
        R.drawable.state_reagent_r2_3,
    )

    /**
     * 首页显示样本和比色皿详情的对话框
     */
    private val homeDetailsDialog: HomeDetailsDialog by lazy { HomeDetailsDialog(requireContext()) }

    /**
     * 显示调试时详情的对话框
     */
    private val debugShowDetailsDialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    /**
     * 显示配置对话框
     */
    private val homeConfigDialog: HomeConfigDialog by lazy {
        HomeConfigDialog(requireContext())
    }

    private val dialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    /**
     * 获取待检信息
     */
    private val getTestPatientInfoDialog: GetTestPatientInfoDialog by lazy {
        GetTestPatientInfoDialog(requireContext())
    }
    private val dialogGetMachine: ProgressDialog by lazy {
        ProgressDialog(requireContext()).apply {
            setMessage("自检中")
            setCancelable(false)
        }
    }
    private val waitDialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    /**
     * 待检信息列表
     */
    private val patientInfoDialog: PatientInfoDialog by lazy {
        PatientInfoDialog(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onStart() {
        super.onStart()
        i("onStart ${Date().toLongTimeStr()}")
    }


    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    override val vm: HomeViewModel by viewModels {
        HomeViewModelFactory()
    }

    override fun initViewModel() {
    }


    override fun init(savedInstanceState: Bundle?) {
//        test()
//
        initView()
        listener()

        vm.goGetMachineState()
        vm.goGetVersion()
        lifecycleScope.launch {
            vm.projectDatas.collectLatest {
                it.let { ret ->
                    projects.clear()
                    projects.addAll(ret.toMutableList())

                    vm.recoverSelectProject(projects)
                }
            }
        }


    }


    private fun initView() {

    }

    private fun listener() {
        listenerView()
        listenerData()
    }

    private fun listenerView() {
        lifecycleScope.launch {
            vm.testMachineUiState.collectLatest {
                vd.ivReagentR1.setImageResource(if (it.r1State.not()) R.drawable.state_reagent_r1_empty else R.drawable.state_reagent_r1_full)
                if (it.r2State in r2VolumeIds.indices) {
                    vd.ivReagentR2.setImageResource(r2VolumeIds[it.r2State])
                } else {
                    vd.ivReagentR2.setImageResource(r2VolumeIds[0])
                }
                vd.ivCleanoutFluid.setImageResource(if (it.cleanoutFluidState.not()) R.drawable.state_cleanout_fluid_empty else R.drawable.state_cleanout_fluid_full)
                vd.tvTemp.text = it.reactionTemp.toString().plus("℃")
            }
        }
        vd.svSample1.clickIndex = { it, item ->
            showDetails(0, it, item)
        }
        vd.svSample2.clickIndex = { it, item ->
            showDetails(1, it, item)
        }
        vd.svSample3.clickIndex = { it, item ->
            showDetails(2, it, item)
        }
        vd.svSample4.clickIndex = { it, item ->
            showDetails(3, it, item)
        }
        vd.svSample1.shape = ShelfView.Shape.Circle
        vd.svSample2.shape = ShelfView.Shape.Circle
        vd.svSample3.shape = ShelfView.Shape.Circle
        vd.svSample4.shape = ShelfView.Shape.Circle
        lifecycleScope.launch {
            vm.sampleStates.collectLatest {
                vd.svSample1.itemStates = it[0]
                vd.svSample2.itemStates = it[1]
                vd.svSample3.itemStates = it[2]
                vd.svSample4.itemStates = it[3]
            }
        }
        vd.svCuvette1.shape = ShelfView.Shape.Rectangle
        vd.svCuvette2.shape = ShelfView.Shape.Rectangle
        vd.svCuvette3.shape = ShelfView.Shape.Rectangle
        vd.svCuvette4.shape = ShelfView.Shape.Rectangle
        vd.svCuvette1.clickIndex = { it, item ->
            showDetails(3, it, item)
        }
        vd.svCuvette2.clickIndex = { it, item ->
            showDetails(2, it, item)
        }
        vd.svCuvette3.clickIndex = { it, item ->
            showDetails(1, it, item)
        }
        vd.svCuvette4.clickIndex = { it, item ->
            showDetails(0, it, item)
        }
        lifecycleScope.launch {
            vm.cuvetteStates.collectLatest {
                vd.svCuvette1.itemStates = it[3]
                vd.svCuvette2.itemStates = it[2]
                vd.svCuvette3.itemStates = it[1]
                vd.svCuvette4.itemStates = it[0]
            }
        }
        lifecycleScope.launch {
            appVm.obTestState.collectLatest {
                if (!appVm.testType.isTest() && it.isRunning()) {
                    vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                    vd.tvAnalyse.setText("正在运行……")
                    vm.enableView(false)
                } else {
                    if (it.machineStateIng()) {
                        vd.tvAnalyse.setText("正在自检")
                        vd.tvAnalyse2.setText("(正在自检，请稍后)")
                        vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                        vm.enableView(true)
                    } else if (it.isNotPrepare()) {
                        vd.tvAnalyse.setText("重新自检")
                        vd.tvAnalyse2.setText("(请重新自检)")
                        vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                        vm.enableView(true)
                    } else if (it.isRunning()) {
                        vd.tvAnalyse.setText("检测中")
                        vd.tvAnalyse2.setText("(仓门已锁，请勿打开,请等待检测结束)")
                        vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                        vm.enableView(false)
                    } else {
                        vd.tvAnalyse.setText("分析")
                        vd.tvAnalyse2.setText("(点击分析)")
                        vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg2)
                        vm.enableView(true)
                    }
                }
            }
        }

        vd.vTest.setOnClickListener {
            u("开始")
            if (appVm.testState.isTestRunning()) {
                toast("正在运行，请稍后")
            } else if (vm.selectProject == null) {
                showConfigDialog()
                toast("请选择标曲")
            } else if (!vm.isAuto() && vm.needSamplingNum <= 0) {
                showConfigDialog()
                toast("请输入检测数量")
            } else {
                vm.clickStart()
            }
        }
        vd.btnDebugDialog.setOnClickListener {
            debugShowDetailsDialog.showPop(requireContext(), width = 1500) {
                it.showDialog(
                    vm.testMsg.value ?: "",
                    "确定",
                    confirmClick = { it.dismiss() },
                    scMaxHeight = 600,
                    textGravity = Gravity.LEFT
                )
            }

        }

        vd.vConfig.setOnClickListener {
            u("设置")
            if (appVm.testState.isTestRunning()) {
                toast("正在运行，请稍后")
            } else {
                showConfigDialog()
            }
        }
//        vd.btnGetTestPatientInfo.setOnClickListener {
//            u("获取待检信息")
//            showGetTestPatientInfo()
//        }
    }

    override fun onMessageEvent(event: EventMsg<Any>) {
        super.onMessageEvent(event)
        when (event.what) {
            EventGlobal.WHAT_GET_TEMP_CHANGE -> {
                if (event.data is Boolean) {
                    vm.allowTemp = event.data
                }
            }

            EventGlobal.WHAT_PROJECT_ADD -> {
                lifecycleScope.launch {
                    vm.selectLastProject()
                }
            }

            else -> {}
        }
    }

    private fun showGetTestPatientInfo() {
        getTestPatientInfoDialog.showPop(
            requireContext(),
            width = 600,
        ) { tpiDialog ->
            tpiDialog.show { condition1, condition2, type ->
                tpiDialog.dismiss()
                startGetTestPatientInfo(condition1, condition2, type)
            }
        }

    }

    private fun startGetTestPatientInfo(
        condition1: String, condition2: String, type: GetPatientType
    ) {
        waitDialog.showPop(requireContext()) { hilt ->
            hilt.showDialog("正在获取信息，请等待……")
            HL7Helper.getPatientInfo(
                GetPatientCondition(condition1, condition2, type),
                object : OnGetPatientCallback {
                    override fun onGetPatientSuccess(patients: List<Patient>?) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            hilt.dismiss()
                            if (patients.isNullOrEmpty()) {
                                dialog.showPop(requireContext(), isCancelable = true) {
                                    dialog.showDialog(
                                        msg = "没有待检信息",
                                        confirmText = "我知道了",
                                        confirmClick = {
                                            it.dismiss()
                                            u("没有待检信息，点击取消")
                                            vm.dialogGetStateNotExistConfirm()
                                        },
                                    )
                                }
                            } else {
                                patientInfoDialog.showPop(
                                    requireContext(), width = 1000, isCancelable = false
                                ) { pi ->
                                    pi.showPatient(patients, {
                                        u("待检信息,点击确定")
                                        pi.dismiss()
                                    }, {
                                        u("待检信息,点击取消")
                                        pi.dismiss()
                                    })
                                }
                            }
                        }
                    }

                    override fun onGetPatientFailed(code: Int, msg: String) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            hilt.dismiss()
                            dialog.showPop(requireContext(), isCancelable = false) {
                                dialog.showDialog(
                                    msg = "$code $msg",
                                    confirmText = "我知道了",
                                    confirmClick = {
                                        u("获取待检信息失败，我知道了")
                                        it.dismiss()
                                        vm.dialogGetStateNotExistConfirm()
                                    },
                                )
                            }
                        }
                    }
                })
        }
    }

    /**
     * 显示配置对话框
     */
    private fun showConfigDialog() {
        i("showConfigDialog before")
        homeConfigDialog.showPop(requireContext(), width = 1000) {
            it.showDialog(vm.configViewEnable.value ?: true,
                projects,
                vm.selectProject,
                vm.cuvetteStartPos,
                if (vm.detectionNumInput.isNullOrEmpty()) vm.getDetectionNum() else vm.detectionNumInput,
                vm.needSamplingNum,
                vm.isAuto(),
                { projectModel, skipNum, detectionNum, sampleNum, baseDialog ->
                    if (projectModel == null) {
                        toast("请选择标曲")
                    } else {
                        if (appVm.testState.isTestRunning()) {
                            toast("正在检测，请稍后")
                            return@showDialog
                        }
                        //选择的项目变更
                        vm.changeConfig(projectModel, skipNum, detectionNum, sampleNum)
                        baseDialog.dismiss()
                    }
                },
                {
                    it.dismiss()
                })
        }
        i("showConfigDialog after")

    }

    /**
     * 显示样本详情
     * @param item SampleItem?
     */
    private fun showDetails(shelfIndex: Int, curFocIndex: Int, item: Item?) {
        u("showDetailsDialog $item")

        item?.let {
            vm.selectFocChange(shelfIndex, curFocIndex, it)
        }
    }


    private fun test() {
//        val project = ProjectModel().apply {
//            f0 = 22.834258698728
//            f1 = 0.835288925322
//            f2 = -0.000596016621
//            f3 = 2.491525485E-07
//            reagentNO = "99"
//        }
//        DBManager.ProjectBox.put(project)
    }

    var arraySample = mutableListOf<ShelfView>()
    var arrayCuvette = mutableListOf<ShelfView>()
    private fun listenerData() {
        arraySample.add(vd.svSample1)
        arraySample.add(vd.svSample2)
        arraySample.add(vd.svSample3)
        arraySample.add(vd.svSample4)
        arrayCuvette.add(vd.svCuvette4)
        arrayCuvette.add(vd.svCuvette3)
        arrayCuvette.add(vd.svCuvette2)
        arrayCuvette.add(vd.svCuvette1)
        lifecycleScope.launch {
            /**
             * 显示调试的数据
             */
            vm.testMsg.observe(viewLifecycleOwner) {
                debugShowDetailsDialog.let { dialog ->
                    if (dialog.isShow) {
                        dialog.showPop(requireContext(), width = 1500) { d ->
                            d.showDialog(
                                it,
                                "确定",
                                confirmClick = { it.dismiss() },
                                showIcon = false,
                                textGravity = Gravity.LEFT
                            )
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            vm.dialogUiState.collect { state ->
                i("launchAndRepeatWithViewLifecycle state=${state}")
                when (state) {
                    /**
                     * 自检中
                     */
                    is HomeDialogUiState.GetMachineShow -> {
//                        dialogGetMachine.show()
                    }

                    is HomeDialogUiState.GetMachineDismiss -> {
//                        dialogGetMachine.dismiss()
                    }
                    /**
                     * 自检失败对话框
                     */
                    is HomeDialogUiState.GetMachineFailedShow -> {
                        lifecycleScope.launch {
                            delay(300)
                            dialog.showPop(requireContext(), isCancelable = false) {
                                it.showDialog(
                                    msg = state.msg,
                                    confirmText = "重新自检",
                                    confirmClick = { baseDialog ->
                                        baseDialog.dismiss()
                                        vm.dialogGetMachineFailedConfirm()
                                    },
                                    cancelText = "我知道了",
                                    cancelClick = { baseDialog ->
                                        baseDialog.dismiss()
                                        vm.dialogGetMachineFailedCancel()
                                    },
                                    showIcon = true,
                                    iconId = ICON_HINT
                                )
                            }
                        }
                    }
                    /**
                     * 正常检测 比色皿不足
                     */
                    is HomeDialogUiState.CuvetteDeficiency -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = "比色皿检测结束，是否添加？",
                                confirmText = "我已添加",
                                confirmClick = {
                                    it.dismiss()
                                    vm.dialogTestFinishCuvetteDeficiencyConfirm()
                                },
                                cancelText = "结束检测",
                                cancelClick = {
                                    it.dismiss()
                                    vm.dialogTestFinishCuvetteDeficiencyCancel()
                                },
                                showIcon = true,
                                iconId = ICON_HINT
                            )
                        }
                    }
                    /**
                     * 开始检测 比色皿,样本，试剂不足
                     */
                    is HomeDialogUiState.GetStateNotExist -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            dialog.showDialog(
                                msg = state.msg,
                                confirmText = "我已添加",
                                confirmClick = {
                                    it.dismiss()
                                    vm.dialogGetStateNotExistConfirm()
                                },
                                cancelText = "结束检测",
                                cancelClick = {
                                    it.dismiss()
                                    vm.dialogGetStateNotExistCancel()
                                },
                                showIcon = true,
                                iconId = ICON_HINT
                            )
                        }
                    }
                    /**
                     * 检测结束 正常样本取样完成的提示
                     */
                    is HomeDialogUiState.TestFinish -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = if (state.msg.isEmpty()) "检测结束" else "检测结束,${state.msg}",
                                confirmText = "确定",
                                confirmClick = {
                                    it.dismiss()
                                },
                                showIcon = true,
                                iconId = ICON_FINISH
                            )
                        }
                    }
                    /**
                     * 通知
                     */
                    is HomeDialogUiState.Notify -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = state.msg,
                                confirmText = "我知道了",
                                confirmClick = { baseDialog ->
                                    baseDialog.dismiss()
                                },
                                showIcon = true,
                                iconId = ICON_HINT
                            )
                        }
                    }
                    /**
                     * 命令提示错误，中断所有程序
                     */
                    is HomeDialogUiState.StateFailed -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = state.msg,
                                confirmText = "我知道了",
                                confirmClick = { baseDialog ->
                                    baseDialog.dismiss()
                                },
                                showIcon = true,
                                iconId = ICON_HINT
                            )
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            vm.configUiState.collectLatest {
                vd.tvSettingsCurve.text =
                    "选择标曲:   ${if (it.curveModel == null) "无" else it.curveModel.reagentNO}"
                vd.tvSettingsSkip.text = "跳过比色皿:   ${it.cuvetteStartPos}"
                vd.tvSettingsStartNum.text = "起始编号:   ${it.startNum}"
                vd.tvSettingsTestNum.text = "检测数量:   ${it.detectionNum}"
            }
        }
        lifecycleScope.launch {
            vm.itemDetailsUiState.collectLatest {
                //更新详情显示
                vd.gpDetails.visibility = (!it.hiltDetails).isShow()
                vd.tvDetailsState.text = "${it.item.state.state ?: "-"}"
                vd.tvDetailsNo.text = "${it.item.id ?: "-"}"
                vd.tvDetailsNum.text = "${it.item.testResult?.detectionNum ?: "-"}"
                vd.tvDetailsBarcode.text = "${it.item.testResult?.sampleBarcode ?: "-"}"
                vd.tvDetailsId.text = "${it.item.testResult?.resultId ?: "-"}"
                vd.tvDetailsResult.text = "${it.item.testResult?.testResult ?: "-"}"

                arraySample.forEach { v ->
                    v.curFocIndex = -1
                }
                arrayCuvette.forEach { v ->
                    v.curFocIndex = -1
                }
                if (it.item.state is SampleState) {
                    vd.tvDetailsNoHilt.text = "对应比色皿序号："

                    arraySample[it.shelfIndex].curFocIndex = it.curFocIndex
                } else {
                    vd.tvDetailsNoHilt.text = "对应样本序号："

                    arrayCuvette[it.shelfIndex].curFocIndex = it.curFocIndex
                }

            }
        }

        lifecycleScope.launch {
            appVm.machineTestModel.collectLatest {
                vd.tvSettingsTestNum.visibility = (!vm.isAuto()).isShow()
            }
        }
        lifecycleScope.launch {
            appVm.detectionNum.collectLatest {
                vd.tvSettingsStartNum.text = "起始编号:   ${it.toString()}"
            }
        }
    }


}

