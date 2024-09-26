package com.wl.turbidimetric.settings.testmode

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.AppIntent
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentTestModeBinding
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.isShow
import com.wl.turbidimetric.view.dialog.showPop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TestModeFragment :
    BaseFragment<TestModeViewModel, FragmentTestModeBinding>(R.layout.fragment_test_mode) {
    override val vm: TestModeViewModel by viewModels { TestModeViewModelFactory() }
    private val hiltDialog by lazy { HiltDialog(requireContext()) }
    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        listenerView()
        listenerData()
        vm.reset()
    }

    private fun listenerData() {
        lifecycleScope.launch {
            vm.testModeUiState.collectLatest {
                if (it.machineTestModel == MachineTestModel.Auto) {
                    vd.rbAuto?.isChecked = true
                } else {
                    vd.rbManualSampling?.isChecked = true
                }

                vd.rbAuto?.setOnCheckedChangeListener { buttonView, isChecked ->
                    vd.llAuto?.visibility = isChecked.isShow()
                }
                vd.llAuto?.visibility = vd.rbAuto?.isChecked.isShow()
                vd.cbScanCode?.isChecked = it.scanCode == true
            }
        }

        lifecycleScope.launch {
            vm.hiltText.collectLatest { hilt ->
                hiltDialog.showPop(requireContext()) { dialog ->
                    dialog.showDialog(hilt, confirmText = "确定", confirmClick = {
                        dialog.dismiss()
                    })
                }
            }
        }

    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            vm.reset()
        }
    }

    private fun listenerView() {
        vd.btnChange.setOnClickListener {
            if (appVm.testState.isRunning()) {
                hiltDialog.showPop(requireContext()) { dialog ->
                    dialog.showDialog(
                        "检测中不能更改，请等待检测结束",
                        confirmText = "确定",
                        confirmClick = {
                            dialog.dismiss()
                        })
                }
                return@setOnClickListener
            }
            val machineTestModel = if (vd.rbAuto.isChecked) {
                MachineTestModel.Auto
            } else {
                MachineTestModel.ManualSampling
            }
            vm.change(
                machineTestModel,
                true,//原来是是否使用样本管传感器，后面崔总说取消这个功能，一定要用
                vd.cbScanCode.isChecked,
            )
            appVm.processIntent(AppIntent.MachineTestModelChange(machineTestModel))
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TestModeFragment()
    }
}
