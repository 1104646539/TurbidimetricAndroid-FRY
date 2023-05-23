package com.wl.turbidimetric.home

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentHomeBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.datastore.LocalDataGlobal
import com.wl.turbidimetric.datastore.LocalDataGlobal.cache
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.global.SystemGlobal.cuvetteDoorIsOpen
import com.wl.turbidimetric.global.SystemGlobal.shitTubeDoorIsOpen
import com.wl.turbidimetric.global.SystemGlobal.testState
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.turbidimetric.util.StorageUtil
import com.wl.turbidimetric.view.HiltDialog
import com.wl.wwanandroid.base.BaseFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import kotlin.concurrent.timer


class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>(R.layout.fragment_home) {
    val TAG = "HomeFragment"
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
//        vd.btnGet.setOnClickListener {
////            viewModelScope.launch {
////                Timber.d("2 Key.TakeReagentR2= before}")
////                val d =
////                    LocalDataGlobal.Key.TakeReagentR1.getData(LocalDataGlobal.Default.TakeReagentR1)
////                val TakeReagentR1 = LocalDataGlobal.Key.TakeReagentR1.putData(d + 1)
////                Timber.d("2 Key.TakeReagentR2= after ${TakeReagentR1}")
////            }
//            //写入u盘文件 start
////            val root = File(SystemGlobal.uPath)
////            val f1 = File(root, "kk.txt")
////            f1.writeText("test")
//            //写入u盘文件 end
//        }
        vd.btnGet.setOnClickListener {

        }
        vd.btnGetU.setOnClickListener {
//            Timber.d("Key.TakeReagentR2= before}")
//            val TakeReagentR1 =
//                LocalDataGlobal.Key.TakeReagentR1.getData(LocalDataGlobal.Default.TakeReagentR1)
//            Timber.d("Key.TakeReagentR2= after ${TakeReagentR1}")

//            SerialPortUtil.Instance.test()

//            lifecycleScope.launch {
//                ScanCodeUtil.Instance.startScan()
//                ScanCodeUtil.Instance.onScanResult = null
//            }

//            lifecycleScope.launch {
//                repeat(1000) {
//                    delay(100)
//                    SerialPortUtil.Instance.pierced()
//                }
//            }
//            PrintUtil.Instance.test()

//            PrintUtil.printMatchingQuality(
//                doubleArrayOf(0.0, 1400.0, 681.0, 174.0, 35.0,500.0,1200.0).toList(),
//                nds,
//                doubleArrayOf(0.0, 1000.0, 500.0, 200.0, 48.0,162.0,465.0).toList(),
//                doubleArrayOf(0.00000001, 2.02354123, 1.21235454, 0.212335412),
//                true
//            )
        }


        projectAdapter = HomeProjectAdapter(requireContext(), items)
        vd.spnProject.adapter = projectAdapter
        projectAdapter.notifyDataSetChanged()

        vm.viewModelScope.launch {
            vm.projectDatas.collectLatest {
                items.clear()
                items.addAll(it)
                projectAdapter.notifyDataSetChanged()
            }
        }

        vd.spnProject.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                vm.selectProject = items?.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                vm.selectProject = null
            }
        }

        vm.testMsg.observe(this) {
            vd.tvMsg.text = it.toString()
            Timber.d("$it")
        }
    }

    private fun listener() {
        listenerDialog()

        cuvetteDoorIsOpen.observe(this) {
//            if (testState == TestState.TestFinish && (cuvetteDoorIsOpen.value == true) && (shitTubeDoorIsOpen.value == true)) {
//                vm.showFinishDialog()
//            }

            vd.tvShow.text = "采便管舱门状态:${
                if (shitTubeDoorIsOpen.value == true) {
                    "已开启"
                } else {
                    "已关闭"
                }
            }" +
                    "\t采便管舱门状态${
                        if (cuvetteDoorIsOpen.value == true) {
                            "已开启"
                        } else {
                            "已关闭"
                        }
                    }"
        }
        shitTubeDoorIsOpen.observe(this) {
//            if (testState == TestState.TestFinish && (cuvetteDoorIsOpen.value == true) && (shitTubeDoorIsOpen.value == true)) {
//                vm.showFinishDialog()
//            }

            vd.tvShow.text = "采便管舱门状态:${
                if (shitTubeDoorIsOpen.value == true) {
                    "已开启"
                } else {
                    "已关闭"
                }
            }" +
                    "\t采便管舱门状态${
                        if (cuvetteDoorIsOpen.value == true) {
                            "已开启"
                        } else {
                            "已关闭"
                        }
                    }"
        }
    }


    val items: MutableList<ProjectModel> = mutableListOf()
    lateinit var projectAdapter: HomeProjectAdapter
    private fun test() {
        vd.btnGetU2.setOnClickListener {
//            Log.d(
//                TAG,
//                "requireActivity().requestedOrientation=${requireActivity().requestedOrientation}"
//            )
//            if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //设置
//            } else {
//                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //设置
//            }
            val exist = StorageUtil.isExist()
            Timber.d("exist=$exist")
        }

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
         * 开始检测 比色皿,采便管，试剂不足
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
         * 正常检测 采便管不足
         */
        vm.dialogTestShitTubeDeficiency.observe(this) {
            dialog.show(msg = "采便管不足，是否添加？", confirmMsg = "我已添加", onConfirm = {
                it.dismiss()
                vm.dialogTestShitTubeDeficiencyConfirm()
            }, cancelMsg = "结束检测", onCancel = {
                it.dismiss()
                vm.dialogTestShitTubeDeficiencyCancel()
            }, false)
        }

        /**
         * 检测结束 正常采便管取样完成的提示
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

