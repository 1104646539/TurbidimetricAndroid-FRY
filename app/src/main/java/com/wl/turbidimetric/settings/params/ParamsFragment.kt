package com.wl.turbidimetric.settings.params

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentParamsBinding
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ParamsFragment :
    BaseFragment<ParamsViewModel, FragmentParamsBinding>(R.layout.fragment_params) {
    override val vm: ParamsViewModel by viewModels()
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
            vm.paramsUiState.collectLatest {
                vd.tietR1.setText(it.r1Volume.toString())
                vd.tietR2.setText(it.r2Volume.toString())
                vd.tietSampling.setText(it.samplingVolume.toString())
                vd.tietSamplingProbeCleaningTime.setText(it.samplingProbeCleaningDuration.toString())
                vd.tietStirProbeCleaningTime.setText(it.stirProbeCleaningDuration.toString())
                vd.tietStirTime.setText(it.stirDuration.toString())
                vd.tietTest1DelayTime.setText((it.test1DelayTime / 1000).toString())
                vd.tietTest2DelayTime.setText((it.test2DelayTime / 1000).toString())
                vd.tietTest3DelayTime.setText((it.test3DelayTime / 1000).toString())
                vd.tietTest4DelayTime.setText((it.test4DelayTime / 1000).toString())
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
                vd.tietR1.text.toString().toIntOrNull() ?: 0,
                vd.tietR2.text.toString().toIntOrNull() ?: 0,
                vd.tietSampling.text.toString().toIntOrNull() ?: 0,
                vd.tietSamplingProbeCleaningTime.text.toString().toIntOrNull() ?: 0,
                vd.tietStirProbeCleaningTime.text.toString().toIntOrNull() ?: 0,
                vd.tietStirTime.text.toString().toIntOrNull() ?: 0,
                (vd.tietTest1DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
                (vd.tietTest2DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
                (vd.tietTest3DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
                (vd.tietTest4DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
            )
        }
    }

    companion object {
        @JvmStatic
//        fun newInstance() = ParamsFragment()
        val instance: ParamsFragment by lazy { ParamsFragment() }
    }
}
