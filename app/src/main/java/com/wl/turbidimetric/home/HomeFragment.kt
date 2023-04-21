package com.wl.turbidimetric.home

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.fragment.app.viewModels
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentHomeBinding
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.turbidimetric.view.HiltDialog
import com.wl.wwanandroid.base.BaseFragment
import timber.log.Timber


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

    override val viewModel: HomeViewModel by viewModels()
    override fun initViewModel() {
        viewDataBinding.model = viewModel
    }

    override fun init(savedInstanceState: Bundle?) {
        test()
        listenerDialog()
        //自检
        viewModel.goGetMachineState()

        viewDataBinding.btnGet.setOnClickListener {
//            viewModelScope.launch {
//                Timber.d("2 Key.TakeReagentR2= before}")
//                val d =
//                    LocalDataGlobal.Key.TakeReagentR1.getData(LocalDataGlobal.Default.TakeReagentR1)
//                val TakeReagentR1 = LocalDataGlobal.Key.TakeReagentR1.putData(d + 1)
//                Timber.d("2 Key.TakeReagentR2= after ${TakeReagentR1}")
//            }
            //写入u盘文件 start
//            val root = File(SystemGlobal.uPath)
//            val f1 = File(root, "kk.txt")
//            f1.writeText("test")
            //写入u盘文件 end
        }

        viewDataBinding.btnGetU.setOnClickListener {
//            Timber.d("Key.TakeReagentR2= before}")
//            val TakeReagentR1 =
//                LocalDataGlobal.Key.TakeReagentR1.getData(LocalDataGlobal.Default.TakeReagentR1)
//            Timber.d("Key.TakeReagentR2= after ${TakeReagentR1}")

            SerialPortUtil.Instance.test()
        }

        viewDataBinding.btnGetU2.setOnClickListener {}

        projectAdapter = HomeProjectAdapter(requireContext(), items)
        viewDataBinding.spnProject.adapter = projectAdapter
        projectAdapter.notifyDataSetChanged()

        viewModel.projectDatas.observe(this) {
            items.clear()
            items.addAll(it)
            projectAdapter.notifyDataSetChanged()
        }

        viewDataBinding.spnProject.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                viewModel.selectProject = items?.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.selectProject = null
            }
        }

        viewModel.testMsg.observe(this) {
            viewDataBinding.tvMsg.text = it.toString()
            Timber.d("$it")
        }
    }

    val items: MutableList<ProjectModel> = mutableListOf()
    lateinit var projectAdapter: HomeProjectAdapter
    private fun test() {


    }

    private fun listenerDialog() {
        /**
         * 自检中
         */
        viewModel.dialogGetMachine.observe(this) { show ->
            if (show) {
                dialogGetMachine.show()
            } else {
                dialogGetMachine.dismiss()
            }
        }
        /**
         * 自检失败对话框
         */
        viewModel.dialogGetMachineFailed.observe(this) { show ->
            if (show) {
                dialog.show(msg = "${viewModel.getMachineFailedMsg.value}",
                    confirmMsg = "重新自检",
                    onConfirm = {
                        it.dismiss()
                        viewModel.dialogGetMachineFailedConfirm()
                    },
                    cancelMsg = "我知道了",
                    onCancel = {
                        it.dismiss()
                        viewModel.dialogGetMachineFailedCancel()
                    })

                viewModel.dialogGetMachineFailed.postValue(false)
            }
        }
        /**
         * 检测结束后 比色皿不足
         */
        viewModel.dialogTestFinishCuvetteDeficiency.observe(this) { show ->
            if (show) {
                dialog.show(msg = "比色皿检测结束，是否添加？", confirmMsg = "我已添加", onConfirm = {
                    it.dismiss()
                    viewModel.dialogTestFinishCuvetteDeficiencyConfirm()
                }, cancelMsg = "结束检测", onCancel = {
                    it.dismiss()
                    viewModel.dialogTestFinishCuvetteDeficiencyCancel()
                })
                viewModel.dialogTestFinishCuvetteDeficiency.postValue(false)
            }
        }
        /**
         * 开始检测 比色皿,采便管，试剂不足
         */
        viewModel.dialogGetStateNotExist.observe(this) { show ->
            if (show) {
                dialog.show(msg = "${viewModel.getStateNotExistMsg}",
                    confirmMsg = "我已添加",
                    onConfirm = {
                        it.dismiss()
                        viewModel.dialogGetStateNotExistConfirm()
                    },
                    cancelMsg = "结束检测",
                    onCancel = {
                        it.dismiss()
                        viewModel.dialogGetStateNotExistCancel()
                    })
                viewModel.dialogGetStateNotExist.postValue(false)
            }
        }
        /**
         * 正常检测 采便管不足
         */
        viewModel.dialogTestShitTubeDeficiency.observe(this) { show ->
            if (show) {
                dialog.show(msg = "采便管不足，是否添加？", confirmMsg = "我已添加", onConfirm = {
                    it.dismiss()
                    viewModel.dialogTestShitTubeDeficiencyConfirm()
                }, cancelMsg = "结束检测", onCancel = {
                    it.dismiss()
                    viewModel.dialogTestShitTubeDeficiencyCancel()
                })
                viewModel.dialogTestShitTubeDeficiency.postValue(false)
            }
        }

        /**
         * 检测结束 正常采便管取样完成的提示
         */
        viewModel.dialogTestFinish.observe(this) { show ->
            if (show) {
                dialog.show(msg = "检测结束", confirmMsg = "确定", onConfirm = {
                    it.dismiss()
                })
                viewModel.dialogTestFinish.postValue(false)
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

