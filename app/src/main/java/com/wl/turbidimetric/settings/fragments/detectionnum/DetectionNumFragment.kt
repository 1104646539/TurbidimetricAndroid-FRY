package com.wl.turbidimetric.settings.fragments.detectionnum

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentDetectionNumBinding
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetectionNumFragment :
    BaseFragment<DetectionNumViewModel, FragmentDetectionNumBinding>(R.layout.fragment_detection_num) {
    override val vm: DetectionNumViewModel by viewModels()
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
                vd.tietDetectionNum.setText(it.detectionNum.toString())
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
            vm.change(
                vd.tietDetectionNum.text.toString().toLongOrNull() ?: -1,
            )
        }
    }

    companion object {
        @JvmStatic
//        fun newInstance() = ParamsFragment()
        val instance: DetectionNumFragment by lazy { DetectionNumFragment() }
    }
}
