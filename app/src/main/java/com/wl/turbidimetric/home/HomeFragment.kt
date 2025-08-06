package com.wl.turbidimetric.home

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.App.Companion
import com.wl.turbidimetric.app.AppIntent
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentHomeBinding
import com.wl.turbidimetric.ex.throttle
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.log.DbLogUtil
import com.wl.turbidimetric.log.LogLevel
import com.wl.turbidimetric.log.LogModel
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.Item
import com.wl.turbidimetric.model.SampleState
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.model.TestType
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.service.OnGetPatientCallback
import com.wl.turbidimetric.view.ShelfView
import com.wl.turbidimetric.view.ShelfView5
import com.wl.turbidimetric.view.dialog.GetTestPatientInfoDialog
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.HomeConfigDialog
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
import org.greenrobot.eventbus.EventBus
import java.util.Date

class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>(R.layout.fragment_home) {

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
        private const val TAG = "HomeFragment"
    }

    override val vm: HomeViewModel by viewModels {
        HomeViewModelFactory()
    }

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

        vd.vConfig.post {
            EventBus.getDefault().post((EventMsg<Any>(what = EventGlobal.WHAT_HOME_INIT_FINISH)))
        }
    }


    private fun initView() {

    }

    private fun listener() {
        listenerView()
        listenerData()
        vm.listener()
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
                vd.tvReagentR1Temp.text = it.r1Temp.toString().plus("℃")
            }
        }
        vd.svSample1.clickIndex = { it, item -> showDetails(0, it, item) }
        vd.svSample2.clickIndex = { it, item -> showDetails(1, it, item) }
        vd.svSample3.clickIndex = { it, item -> showDetails(2, it, item) }
        vd.svSample4.clickIndex = { it, item -> showDetails(3, it, item) }

        vd.svSample1.shape = ShelfView.Shape.Circle
        vd.svSample2.shape = ShelfView.Shape.Circle
        vd.svSample3.shape = ShelfView.Shape.Circle
        vd.svSample4.shape = ShelfView.Shape.Circle

        lifecycleScope.launchWhenCreated {
            vm.sampleStates.collectLatest {
                vd.svSample1.itemStates = it[0]
                vd.svSample2.itemStates = it[1]
                vd.svSample3.itemStates = it[2]
                vd.svSample4.itemStates = it[3]
            }
        }

        vd.svCuvette3.shape = ShelfView.Shape.Rectangle
        vd.svCuvette4.shape = ShelfView.Shape.Rectangle

        vd.svCuvette3.clickIndex = { it, item ->
            showDetails(1, it, item)
        }
        vd.svCuvette4.clickIndex = { it, item ->
            showDetails(0, it, item)
        }
        lifecycleScope.launchWhenCreated {
            vm.cuvetteStates.collectLatest {
                vd.svCuvette3.itemStates = it[1]
                vd.svCuvette4.itemStates = it[0]
            }
        }
        lifecycleScope.launchWhenCreated {
            appVm.obTestState.collectLatest {
                changeAnalyseState(it)
            }
        }

        vd.vTest.setOnClickListener {
            if (appVm.testState.isNotPrepare()) {
                vm.dialogGetMachineFailedConfirm()
                toast("自检中……")
            } else if (appVm.testState.isTestRunning()) {
                toast("正在运行，请稍后")
            } else if (vm.selectProject == null) {
                showConfigDialog()
                toast("请选择标曲")
            } else if (!vm.isAuto() && vm.needTestNum <= 0) {
                showConfigDialog()
                toast("请输入检测数量")
            } else {
                vm.clickStart()
            }
        }
        lifecycleScope.launchWhenCreated {
            appVm.obDebugMode.collectLatest {
                vd.btnDebugDialog.visibility = it.isShow()
                vd.tvReagentR1Temp.visibility = it.isShow()
            }
        }
        vd.btnDebugDialog.setOnClickListener {
            debugShowDetailsDialog.showPop(requireContext(), width = 1500) {
                it.showDialog(
                    vm.testMsg.value ?: "",
                    "确定",
                    confirmClick = { d: BasePopupView -> it.dismiss() }.throttle(),
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
        lifecycleScope.launchWhenCreated {
            appVm.obReactionTemp.collectLatest {
                changeAnalyseState(appVm.testState)
            }
        }

    }

    private fun changeAnalyseState(testState: TestState) {
        if (!appVm.testType.isTest() && testState.isRunning()) {
            vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
            vd.tvAnalyse.text = "正在运行……"
            vm.enableView(false)
        } else {
            if (testState.machineStateIng()) {
                vd.tvAnalyse.text = "正在自检"
                vd.tvAnalyse2.text = "(正在自检，请稍后)"
                vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vm.enableView(false)
            } else if (testState.isPreheatTime()) {
                vd.tvAnalyse.text = "正在预热,请稍后"
                vd.tvAnalyse2.text = ""
                vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vm.enableView(false)
            } else if (testState.isRunningError()) {
                vd.tvAnalyse.text = "运行错误"
                vd.tvAnalyse2.text = "(请联系维护人员)"
                vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vm.enableView(false)
            } else if (testState.isNotPrepare()) {
                vd.tvAnalyse.text = "重新自检"
                vd.tvAnalyse2.text = "(请重新自检)"
                vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vm.enableView(true)
            } else if (testState.isRunning()) {
                vd.tvAnalyse.text = "检测中"
                vd.tvAnalyse2.text = "(仓门已锁，请勿打开,请等待检测结束)"
                vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
                vm.enableView(false)
            } else {
//                //当温度不够时，只在第一次时更新显示正在预热
//                if (!appVm.getTempCanBeTest()) {
//                    vd.tvAnalyse.text = "正在预热,请稍后"
//                    vd.tvAnalyse2.text = ""
//                    vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg)
//                    vm.enableView(false)
//                } else {
                //只要有一次温度达标了就不再检查了
                appVm.processIntent(AppIntent.NeedJudgeTempChange(false))
                vd.tvAnalyse.text = "开始分析"
                vd.tvAnalyse2.text = "(点击分析)"
                vd.vTest.setBackgroundResource(R.drawable.shape_analyse_test_bg2)
                vm.enableView(true)
//                }
            }
        }
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

            EventGlobal.WHAT_DETECTION_NUM_CHANGE -> {
                lifecycleScope.launch {
                    vm.changeConfig(
                        vm.selectProject,
                        vm.cuvetteStartPos,
                        vm.getDetectionNum(),
                        vm.needTestNum
                    )
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
                                        confirmClick = { d: BasePopupView ->
                                            it.dismiss()
                                            u("没有待检信息，点击取消")
                                            vm.dialogGetStateNotExistConfirm()
                                        }.throttle(),
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
                                    confirmClick = { d: BasePopupView ->
                                        u("获取待检信息失败，我知道了")
                                        it.dismiss()
                                        vm.dialogGetStateNotExistConfirm()
                                    }.throttle(),
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
            it.showDialog(
                vm.configViewEnable.value ?: true,
                projects,
                vm.selectProject,
                vm.cuvetteStartPos,
                if (vm.detectionNumInput.isNullOrEmpty()) vm.getDetectionNum() else vm.detectionNumInput,
                vm.needTestNum,
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

    private val arraySample = mutableListOf<ShelfView5>()
    private val arrayCuvette = mutableListOf<ShelfView>()
    private fun listenerData() {
        arraySample.add(vd.svSample1)
        arraySample.add(vd.svSample2)
        arraySample.add(vd.svSample3)
        arraySample.add(vd.svSample4)
        arrayCuvette.add(vd.svCuvette4)
        arrayCuvette.add(vd.svCuvette3)
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
                                confirmClick = { baseDialog: BasePopupView -> baseDialog.dismiss() }.throttle(),
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
                        dialogGetMachine.show()
                    }

                    is HomeDialogUiState.GetMachineDismiss -> {
                        dialogGetMachine.dismiss()
                    }
                    /**
                     * 自检失败对话框
                     */
                    is HomeDialogUiState.GetMachineFailedShow -> {
                        lifecycleScope.launch {
                            delay(500)
                            dialog.showPop(requireContext(), isCancelable = false) {
                                it.showDialog(
                                    msg = state.msg,
                                    confirmText = "重新自检",
                                    confirmClick = { baseDialog: BasePopupView ->
                                        baseDialog.dismiss()
                                        vm.dialogGetMachineFailedConfirm()
                                    }.throttle(),
                                    cancelText = "我知道了",
                                    cancelClick = { baseDialog: BasePopupView ->
                                        baseDialog.dismiss()
                                        vm.dialogGetMachineFailedCancel()
                                    }.throttle(),
                                    showIcon = true,
                                    iconId = ICON_HINT,
                                    textGravity = Gravity.LEFT
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
                                msg = "请更换比色皿",
                                confirmText = "我已添加",
                                confirmClick = { baseDialog: BasePopupView ->
                                    it.dismiss()
                                    vm.dialogTestFinishCuvetteDeficiencyConfirm()
                                }.throttle(),
                                cancelText = "结束检测",
                                cancelClick = { baseDialog: BasePopupView ->
                                    it.dismiss()
                                    vm.dialogTestFinishCuvetteDeficiencyCancel()
                                }.throttle(),
                                showIcon = true,
                                iconId = ICON_HINT
                            )
                        }
                    }
                    /**
                     * 开始检测 比色皿,样本，试剂不足
                     */
                    is HomeDialogUiState.GetStateNotExist -> {
                        lifecycleScope.launch {
                            if (SystemGlobal.isCodeDebug) {
                                delay(500)
                            }
                            dialog.showPop(requireContext(), isCancelable = false) {
                                dialog.showDialog(
                                    msg = state.msg,
                                    confirmText = "我已添加",
                                    confirmClick = { baseDialog: BasePopupView ->
                                        it.dismiss()
                                        vm.dialogGetStateNotExistConfirm()
                                    }.throttle(),
                                    cancelText = "结束检测",
                                    cancelClick = { baseDialog: BasePopupView ->
                                        it.dismiss()
                                        vm.dialogGetStateNotExistCancel()
                                    }.throttle(),
                                    showIcon = true,
                                    iconId = ICON_HINT
                                )
                            }
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
                                confirmClick = { baseDialog: BasePopupView ->
                                    it.dismiss()
                                }.throttle(),
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
                                confirmClick = { baseDialog: BasePopupView ->
                                    baseDialog.dismiss()
                                }.throttle(),
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
                                confirmClick = { baseDialog: BasePopupView ->
                                    baseDialog.dismiss()
                                }.throttle(),
                                showIcon = true,
                                iconId = ICON_HINT
                            )
                        }
                    }
                    /**
                     * 填充R1失败
                     */
                    is HomeDialogUiState.FullR1Failed -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = "填充R1失败，R1试剂不足。",
                                confirmText = "重新填充",
                                confirmClick = { baseDialog: BasePopupView ->
                                    baseDialog.dismiss()
                                    vm.fullR1()
                                }.throttle(),
//                                cancelText = "暂不填充",
//                                cancelClick  = { baseDialog: BasePopupView ->
//                                    baseDialog.dismiss()
//                                }.throttle(),
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
        vm.configViewEnable.observe(this@HomeFragment) {
            vd.vTest.isEnabled = it
        }
    }


}

