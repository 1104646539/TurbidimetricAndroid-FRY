package com.wl.turbidimetric.home

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentHomeBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal.obTestState
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.upload.service.OnGetPatientCallback
import com.wl.turbidimetric.view.dialog.*
import com.wl.wwanandroid.base.BaseFragment
import kotlinx.coroutines.launch
import com.wl.wllib.LogToFile.i
import com.wl.wllib.toLongTimeStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.Date

class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>(R.layout.fragment_home) {
    private val TAG = "HomeFragment"

    /**
     * 最新的项目
     */
    private val projects: MutableList<CurveModel> = mutableListOf()
    private val r2VolumeIds = intArrayOf(
        R.drawable.icon_r2_0,
        R.drawable.icon_r2_1,
        R.drawable.icon_r2_2,
        R.drawable.icon_r2_3,
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
        vd.model = vm
    }


    override fun init(savedInstanceState: Bundle?) {
        test()

        initView()
        listener()
        initUploadClient()
        vm.goGetMachineState()
        vm.goGetVersion()

        launchAndRepeatWithViewLifecycle {
            vm.projectDatas.collectLatest {
                it.filterIndexed { index, projectModel ->
                    index < 10
                }.let { ret ->
                    projects.clear()
                    projects.addAll(ret.toMutableList())

                    vm.recoverSelectProject(projects)
                }
            }
        }


    }

    private fun initUploadClient() {
        HL7Helper.connect(object : OnConnectListener {
            override fun onConnectResult(connectResult: ConnectResult) {
                i("onConnectResult connectResult=$connectResult")
            }

            override fun onConnectStatusChange(connectStatus: ConnectStatus) {
                i("onConnectStatusChange connectStatus=$connectStatus")
            }
        })
    }

    private fun initView() {

    }

    private fun listener() {
        listenerView()
        listenerDialog()
    }

    private fun listenerView() {
        launchAndRepeatWithViewLifecycle {
            vm.testMachineUiState.collectLatest {
                vd.ivR1.setImageResource(if (it.r1State.not()) R.drawable.icon_r1_empty else R.drawable.icon_r1_full)
                if (it.r2State in r2VolumeIds.indices) {
                    vd.ivR2.setImageResource(r2VolumeIds[it.r2State])
                } else {
                    vd.ivR2.setImageResource(r2VolumeIds[0])
                }
                vd.ivCleanoutFluid.setImageResource(if (it.cleanoutFluidState.not() == true) R.drawable.icon_cleanout_fluid_empty else R.drawable.icon_cleanout_fluid_full)
                vd.tvTemp.text = it.reactionTemp.toString().plus("℃")
            }
        }
        vd.ssv.label = "1"
        vd.ssv2.label = "2"
        vd.ssv3.label = "3"
        vd.ssv4.label = "4"
        vd.ssv.clickIndex = { it, item ->
            toast("样本 index=$it item=$item")
            showDetailsDialog(item)
        }
        vd.ssv2.clickIndex = { it, item ->
            toast("样本 index2=$it item=$item")
            showDetailsDialog(item)
        }
        vd.ssv3.clickIndex = { it, item ->
            toast("样本 index3=$it item=$item")
            showDetailsDialog(item)
        }
        vd.ssv4.clickIndex = { it, item ->
            toast("样本 index4=$it item=$item")
            showDetailsDialog(item)
        }
        launchAndRepeatWithViewLifecycle {
            vm.sampleStates.collectLatest {
                vd.ssv.sampleStates = it[0]
                vd.ssv2.sampleStates = it[1]
                vd.ssv3.sampleStates = it[2]
                vd.ssv4.sampleStates = it[3]
            }
        }

        vd.csv.label = "4"
        vd.csv2.label = "3"
        vd.csv3.label = "2"
        vd.csv4.label = "1"
        vd.csv.clickIndex = { it, item ->
            toast("比色皿 index=$it item=$item")
            showDetailsDialog(item)
        }
        vd.csv2.clickIndex = { it, item ->
            toast("比色皿 index2=$it item=$item")
            showDetailsDialog(item)
        }
        vd.csv3.clickIndex = { it, item ->
            toast("比色皿 index3=$it item=$item")
            showDetailsDialog(item)
        }
        vd.csv4.clickIndex = { it, item ->
            toast("比色皿 index4=$it item=$item")
            showDetailsDialog(item)
        }
        launchAndRepeatWithViewLifecycle {
            vm.cuvetteStates.collectLatest {
                vd.csv.cuvetteStates = it[3]
                vd.csv2.cuvetteStates = it[2]
                vd.csv3.cuvetteStates = it[1]
                vd.csv4.cuvetteStates = it[0]
            }
        }
        launchAndRepeatWithViewLifecycle {
            obTestState.collectLatest {
                if (it.isNotPrepare()) {
                    vd.btnStart.setBackgroundResource(R.drawable.rip_positive2)
                    vd.btnStart.setText("重新自检")
                    vm.enableView(true)
                } else if (it.isRunning()) {
                    vd.btnStart.setBackgroundResource(R.drawable.rip_positive2)
                    vd.btnStart.setText("正在检测")
                    vm.enableView(false)
                } else {
                    vd.btnStart.setBackgroundResource(R.drawable.rip_positive)
                    vd.btnStart.setText("分析")
                    vm.enableView(true)
                }
            }
        }

        vd.btnStart.setOnClickListener {
            if (vm.selectProject == null) {
                showConfigDialog()
                toast("请选择标曲")
            } else if (!isAuto() && vm.needSamplingNum <= 0) {
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
                )
            }

        }

        vd.btnConfig.setOnClickListener {
            showConfigDialog()
        }
        vd.btnGetTestPatientInfo.setOnClickListener {
            showGetTestPatientInfo()
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
        condition1: String,
        condition2: String,
        type: GetPatientType
    ) {
        waitDialog.showPop(requireContext()) { hilt ->
            hilt.showDialog("正在获取信息，请等待……")
            HL7Helper.getPatientInfo(GetPatientCondition(condition1, condition2, type),
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
                                            vm.dialogGetStateNotExistConfirm()
                                        },
                                    )
                                }
                            } else {
                                patientInfoDialog.showPop(
                                    requireContext(),
                                    width = 1000,
                                    isCancelable = false
                                ) { pi ->
                                    pi.showPatient(patients, {
                                        toast("点击确定")
                                        pi.dismiss()
                                    }, {
                                        toast("点击取消")
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
            it.showDialog(vm.selectProjectEnable.value ?: true,
                vm.editDetectionNumEnable.value ?: true,
                vm.skipCuvetteEnable.value ?: true,
                projects,
                vm.selectProject,
                vm.cuvetteStartPos,
                if (vm.detectionNumInput.isNullOrEmpty()) LocalData.DetectionNum else vm.detectionNumInput,
                vm.needSamplingNum,
                { projectModel, skipNum, detectionNum, sampleNum, baseDialog ->
                    if (projectModel == null) {
                        toast("请选择标曲")
                    } else {
                        if (isTestRunning()) {
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
    private fun showDetailsDialog(item: HomeViewModel.SampleItem?) {
        homeDetailsDialog.showPop(requireContext()) {
            it.showDialog(item)
        }
    }

    /**
     * 显示比色皿详情
     * @param item SampleItem?
     */
    private fun showDetailsDialog(item: HomeViewModel.CuvetteItem?) {
        homeDetailsDialog.showPop(requireContext()) {
            it.showDialog(item)
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

    private fun listenerDialog() {
        lifecycleScope.launch {
            /**
             * 显示调试的数据
             */
            vm.testMsg.observe(viewLifecycleOwner) {
                debugShowDetailsDialog.let { dialog ->
                    if (dialog.isShow) {
                        dialog.showPop(requireContext(), width = 1500) { d ->
                            d.showDialog(
                                it, "确定", confirmClick = { it.dismiss() },
                                showIcon = false
                            )
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            vm.dialogUiState.collectLatest { state ->
                i("launchAndRepeatWithViewLifecycle state=${state.dialogState}")
                when (state.dialogState) {
                    /**
                     * 自检中
                     */
                    DialogState.GET_MACHINE_SHOW -> {
                        dialogGetMachine.show()
                    }
                    DialogState.GET_MACHINE_DISMISS -> {
                        dialogGetMachine.dismiss()
                    }
                    /**
                     * 自检失败对话框
                     */
                    DialogState.GET_MACHINE_FAILED_SHOW -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = state.dialogMsg,
                                confirmText = "重新自检",
                                confirmClick = {
                                    it.dismiss()
                                    vm.dialogGetMachineFailedConfirm()
                                },
                                cancelText = "我知道了",
                                cancelClick = {
                                    it.dismiss()
                                    vm.dialogGetMachineFailedCancel()
                                }, showIcon = true, iconId = ICON_HINT
                            )
                        }

                    }
                    /**
                     * 检测结束后 比色皿不足
                     */
                    DialogState.CUVETTE_DEFICIENCY -> {
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
                                }, showIcon = true, iconId = ICON_HINT
                            )
                        }
                    }
                    /**
                     * 开始检测 比色皿,样本，试剂不足
                     */
                    DialogState.GET_STATE_NOT_EXIST -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            dialog.showDialog(
                                msg = state.dialogMsg,
                                confirmText = "我已添加",
                                confirmClick = {
                                    it.dismiss()
                                    vm.dialogGetStateNotExistConfirm()
                                },
                                cancelText = "结束检测",
                                cancelClick = {
                                    it.dismiss()
                                    vm.dialogGetStateNotExistCancel()
                                }, showIcon = true, iconId = ICON_HINT
                            )
                        }
                    }

                    /**
                     * 正常检测 样本不足
                     */
                    DialogState.SAMPLE_DEFICIENCY -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(
                                msg = "样本不足，是否添加？",
                                confirmText = "我已添加",
                                confirmClick = {
                                    it.dismiss()
                                    vm.dialogTestSampleDeficiencyConfirm()
                                },
                                cancelText = "结束检测",
                                cancelClick = {
                                    it.dismiss()
                                    vm.dialogTestSampleDeficiencyCancel()
                                })
                        }
                    }
                    /**
                     * 检测结束 正常样本取样完成的提示
                     */
                    DialogState.TEST_FINISH -> {
                        dialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(msg = "检测结束", confirmText = "确定", confirmClick = {
                                it.dismiss()
                            }, showIcon = true, iconId = ICON_FINISH)
                        }
                    }
                    /**
                     * 通知
                     */
                    DialogState.NOTIFY -> {
                        toast(state.dialogMsg)
                    }
                }
            }
        }
    }


}

