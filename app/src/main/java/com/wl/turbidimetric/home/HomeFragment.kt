package com.wl.turbidimetric.home

import android.app.ProgressDialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentHomeBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal.obTestState
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.view.HiltDialog
import com.wl.turbidimetric.view.HomeConfigDialog
import com.wl.turbidimetric.view.HomeDetailsDialog
import com.wl.wwanandroid.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber


class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>(R.layout.fragment_home) {
    val TAG = "HomeFragment"

    /**
     * 最新的项目
     */
    val projects: MutableList<ProjectModel> = mutableListOf()
    private val homeDetailsDialog by lazy {
        HomeDetailsDialog(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate");
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView");
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy");
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
        listener()

        vm.goGetMachineState()
        vm.viewModelScope.launch {
            vm.projectDatas.collectLatest {
                projects.clear()
                projects.addAll(it)

                vm.recoverSelectProject(projects)
            }
        }
    }

    val r2VolumeIds =
        intArrayOf(
            R.drawable.icon_r2_0,
            R.drawable.icon_r2_1,
            R.drawable.icon_r2_2,
            R.drawable.icon_r2_3,
        )

    /**
     * 显示调试时详情的对话框
     */
    val debugShowDetailsDialog: HiltDialog by lazy {
        HiltDialog(requireContext()).apply {
            width = 1500
        }
    }

    /**
     * 显示配置对话框
     */
    val homeConfigDialog: HomeConfigDialog by lazy {
        HomeConfigDialog(requireContext())
    }

    private fun listener() {
        val dm = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(dm)
        Timber.d("dm.densityDpi=${dm.densityDpi} ${dm.widthPixels} ${dm.heightPixels} ${dm.scaledDensity}")
        listenerDialog()
        listenerView()

    }

    private fun listenerView() {
        vm.r1State.observe(this) {
            Timber.d("r1State=$it")

            vd.ivR1.setImageResource(if (it?.not() == true) R.drawable.icon_r1_empty else R.drawable.icon_r1_full)
        }
        vm.r2State.observe(this) {
            Timber.d("r2State=$it")

        }
        vm.r2VolumeState.observe(this) {
            Timber.d("r2Volume=$it")

            if ((it ?: 0) in r2VolumeIds.indices) {
                vd.ivR2.setImageResource(r2VolumeIds[it])
            } else {
                vd.ivR2.setImageResource(r2VolumeIds[0])
            }
        }
        vm.cleanoutFluidState.observe(this) {
            Timber.d("cleanoutFluidState=$it")
            vd.ivCleanoutFluid.setImageResource(if (it?.not() == true) R.drawable.icon_cleanout_fluid_empty else R.drawable.icon_cleanout_fluid_full)
        }
        vm.reactionTemp.observe(this) {
            Timber.d("reactionTemp=$it test=${(it?.toString() ?: "0").plus("℃")}")
            vd.tvTemp.text = (it?.toString() ?: "0").plus("℃")
        }
        vm.r1Temp.observe(this) {
            Timber.d("r1Temp=$it")
//            vd.tvTemp.text = "R1=${vm.r1State.value} R2=${vm.r2State.value} R2量=${vm.r2Volume.value} 清洗液=${vm.cleanoutFluidState.value} 反应槽温度=${vm.reactionTemp.value} R1温度=${vm.r1Temp.value}"
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
        vm.samplesStates.observe(this) {
            vd.ssv.sampleStates = it[0]
            vd.ssv2.sampleStates = it[1]
            vd.ssv3.sampleStates = it[2]
            vd.ssv4.sampleStates = it[3]
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
        vm.cuvetteStates.observe(this) {
            vd.csv.cuvetteStates = it[3]
            vd.csv2.cuvetteStates = it[2]
            vd.csv3.cuvetteStates = it[1]
            vd.csv4.cuvetteStates = it[0]
        }

        obTestState.observe(this) {
            if (it != TestState.None && it != TestState.TestFinish) {
                vd.btnStart.setBackgroundResource(R.drawable.rip_positive2)
                vd.btnStart.text = "正在分析"
                vm.enableView(false)
            } else {
                vd.btnStart.setBackgroundResource(R.drawable.rip_positive)
                vd.btnStart.text = "分析"
                vm.enableView(true)
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
            debugShowDetailsDialog.show(
                vm.testMsg.value ?: "",
                "确定",
                onConfirm = { it.dismiss() },
                gravity = Gravity.LEFT
            )
        }

        vd.btnConfig.setOnClickListener {
            showConfigDialog()
        }

    }

    /**
     * 显示配置对话框
     */
    private fun showConfigDialog() {
        Timber.d("showConfigDialog before")
        homeConfigDialog.show(
            vm.selectProjectEnable.value ?: true,
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
                        return@show
                    }
                    //选择的项目变更
                    vm.changeConfig(projectModel, skipNum, detectionNum, sampleNum)
                    baseDialog.dismiss()
                }
            },
            {
                it.dismiss()
            })
        Timber.d("showConfigDialog after")

    }

    /**
     * 显示样本详情
     * @param item SampleItem?
     */
    private fun showDetailsDialog(item: HomeViewModel.SampleItem?) {
        homeDetailsDialog.show(item)
    }

    /**
     * 显示比色皿详情
     * @param item SampleItem?
     */
    private fun showDetailsDialog(item: HomeViewModel.CuvetteItem?) {
        homeDetailsDialog.show(item)
    }


    private fun test() {
//        vd.btnGetU2.setOnClickListener {
////            Log.d(
////                TAG,
////                "requireActivity().requestedOrientation=${requireActivity().requestedOrientation}"
////            )
////            if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
////                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //设置
////            } else {
////                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //设置
////            }
//            val exist = StorageUtil.isExist()
//            Timber.d("exist=$exist")
//        }

    }

    private fun listenerDialog() {

        /**
         * 自检中
         */
        vm.dialogGetMachine.observe(this) { show ->
            if (show) {
                dialogGetMachine.show()
            } else {
                dialogGetMachine.dismiss()
            }
        }
        /**
         * 自检失败对话框
         */
        vm.getMachineFailedMsg.observe(this) { show ->
            dialog.show(
                msg = show,
                confirmMsg = "重新自检",
                onConfirm = {
                    it.dismiss()
                    vm.dialogGetMachineFailedConfirm()
                },
                cancelMsg = "我知道了",
                onCancel = {
                    it.dismiss()
                    vm.dialogGetMachineFailedCancel()
                }, false
            ).takeIf {
                show.isNotEmpty()
            }
        }
        /**
         * 检测结束后 比色皿不足
         */
        vm.dialogTestFinishCuvetteDeficiency.observe(this) {
            dialog.show(msg = "比色皿检测结束，是否添加？", confirmMsg = "我已添加", onConfirm = {
                it.dismiss()
                vm.dialogTestFinishCuvetteDeficiencyConfirm()
            }, cancelMsg = "结束检测", onCancel = {
                it.dismiss()
                vm.dialogTestFinishCuvetteDeficiencyCancel()
            }, false)
        }
        /**
         * 开始检测 比色皿,样本，试剂不足
         */
        vm.getStateNotExistMsg.observe(this) { show ->
            dialog.show(
                msg = show,
                confirmMsg = "我已添加",
                onConfirm = {
                    it.dismiss()
                    vm.dialogGetStateNotExistConfirm()
                },
                cancelMsg = "结束检测",
                onCancel = {
                    it.dismiss()
                    vm.dialogGetStateNotExistCancel()
                }, false
            ).takeIf {
                show.isNotEmpty()
            }
        }
        /**
         * 正常检测 样本不足
         */
        vm.dialogTestSampleDeficiency.observe(this) {
            dialog.show(msg = "样本不足，是否添加？", confirmMsg = "我已添加", onConfirm = {
                it.dismiss()
                vm.dialogTestSampleDeficiencyConfirm()
            }, cancelMsg = "结束检测", onCancel = {
                it.dismiss()
                vm.dialogTestSampleDeficiencyCancel()
            }, false)
        }

        /**
         * 检测结束 正常样本取样完成的提示
         */
        vm.dialogTestFinish.observe(this) { show ->
            if (show) {
                dialog.show(msg = "检测结束", confirmMsg = "确定", onConfirm = {
                    it.dismiss()
                })
            } else {
                dialog.dismiss()
            }
        }
        /**
         * 显示调试的数据
         */
        vm.testMsg.observe(this) {
            if (debugShowDetailsDialog.isShow()) {
                debugShowDetailsDialog.show(
                    it,
                    "确定",
                    onConfirm = { it.dismiss() },
                    gravity = Gravity.LEFT
                )
            }
        }
        /**
         * 显示信息
         */
        vm.toastMsg.observe(this) { msg ->
            Timber.d("msg=$msg")
            snack(vd.root, msg)
        }
    }

    private val dialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }
    private val dialogGetMachine: ProgressDialog by lazy {
        ProgressDialog(requireContext()).apply {
            setMessage("自检中")
            setCancelable(false)
        }
    }
}

