package com.wl.turbidimetric.settings.detectionnum

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentDetectionNumBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class DetectionNumFragment :
    BaseFragment<DetectionNumViewModel, FragmentDetectionNumBinding>(R.layout.fragment_detection_num) {
    override val vm: DetectionNumViewModel by viewModels { DetectionNumViewModelFactory() }
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
            vm.detectionNumUiState.collectLatest {
                appVm.changeDetectionNum(it.detectionNum)
                vd.tietDetectionNum.setText(it.detectionNum.toString())
                EventBus.getDefault().post(EventMsg<String>(EventGlobal.WHAT_DETECTION_NUM_CHANGE))
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
            (vd.tietDetectionNum.text.toString().toLongOrNull() ?: -1).let {
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
                vm.change(it)
            }
        }
    }

    companion object {
        @JvmStatic
//        fun newInstance() = ParamsFragment()
        val instance: DetectionNumFragment by lazy { DetectionNumFragment() }
    }
}
