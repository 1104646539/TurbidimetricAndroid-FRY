package com.wl.turbidimetric.test.debug.singlecmd

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentSingleCmdBinding
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.model.SampleType
import com.wl.turbidimetric.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SingleCmdFragment :
    BaseFragment<SingleCmdViewModel, FragmentSingleCmdBinding>(R.layout.fragment_single_cmd) {
    override val vm: SingleCmdViewModel by viewModels { SingleCmdViewModelFactory() }
    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        listenerEvent()
        listenerView()
    }

    private fun listenerView() {
        lifecycleScope.launch {
            vm.testMsg.collectLatest {
                if (it.isNotEmpty()) {
                    toast(it)
                }
            }
        }
        lifecycleScope.launch {
            vm.resultMsg.collectLatest {
                vd.tvMsg.text = it
            }
        }
        lifecycleScope.launch {
            vm.enable.observe(this@SingleCmdFragment) {
                vd.btnGetMachineState.isEnabled = it
                vd.btnMoveSampleShelf.isEnabled = it
                vd.btnMoveCuvetteShelf.isEnabled = it
                vd.btnMoveSample.isEnabled = it
                vd.btnMoveCuvetteDripSample.isEnabled = it
                vd.btnMoveCuvetteDripReagent.isEnabled = it
                vd.btnMoveCuvetteTest.isEnabled = it
                vd.btnSampling.isEnabled = it
                vd.btnDripSample.isEnabled = it
                vd.btnTakeReagent.isEnabled = it
                vd.btnDripReagent.isEnabled = it
                vd.btnStir.isEnabled = it
                vd.btnTest.isEnabled = it
                vd.btnStirProbeCleaning.isEnabled = it
                vd.btnSamplingProbeCleaning.isEnabled = it
                vd.btnPierced.isEnabled = it
                vd.btnSqueezing.isEnabled = it
                vd.btnMoveCuvetteShelfReset.isEnabled = it
                vd.btnGetMcuVersion.isEnabled = it
                vd.btnTemp.isEnabled = it
                vd.btnGetState.isEnabled = it
                vd.btnMoveSampleShelfReset.isEnabled = it
            }
        }
    }

    private fun listenerEvent() {

        vd.btnGetMachineState.setOnClickListener {
            vm.getMachineState()
        }
        vd.btnMoveSampleShelf.setOnClickListener {
            vm.moveSampleShelf(vd.tetMoveSampleShelfStep.text.toString())
        }
        vd.btnMoveCuvetteShelf.setOnClickListener {
            vm.moveCuvetteShelf(vd.tetMoveCuvetteShelfStep.text.toString())
        }
        vd.btnMoveSample.setOnClickListener {
            vm.moveSample(vd.tetMoveSample.text.toString(), vd.rbForwardSample.isChecked)
        }
        vd.btnMoveCuvetteDripSample.setOnClickListener {
            vm.moveCuvetteDripSample(
                vd.tetMoveCuvetteDripSample.text.toString(),
                vd.rbForwardCuvetteDripSample.isChecked
            )
        }
        vd.btnMoveCuvetteDripReagent.setOnClickListener {
            vm.moveCuvetteDripReagent(
                vd.tetMoveCuvetteDripReagent.text.toString(),
                vd.rbForwardCuvetteDripReagent.isChecked
            )
        }
        vd.btnMoveCuvetteTest.setOnClickListener {
            vm.moveCuvetteTest(
                vd.tetMoveCuvetteTest.text.toString(),
                vd.rbForwardCuvetteTest.isChecked
            )
        }

        vd.btnSampling.setOnClickListener {
            vm.sampling(
                vd.tetSamplingVolume.text.toString(),
                if (vd.rbSamplingSample.isChecked) SampleType.SAMPLE else SampleType.CUVETTE
            )
        }
        vd.btnDripSample.setOnClickListener {
            vm.dripSample(
                vd.tetDripSampleVolume.text.toString(),
                vd.rbDripSampleBlendingY.isChecked,
                vd.rbDripSampleInplaceY.isChecked
            )
        }
        vd.btnTakeReagent.setOnClickListener {
            vm.takeReagent(vd.tetTakeReagentR1.text.toString(), vd.tetTakeReagentR2.text.toString())
        }
        vd.btnDripReagent.setOnClickListener {
            vm.dripReagent(vd.tetDripReagentR1.text.toString(), vd.tetDripReagentR2.text.toString())
        }
        vd.btnStir.setOnClickListener {
            vm.stir(vd.tetStir.text.toString())
        }
        vd.btnTest.setOnClickListener {
            vm.test()
        }
        vd.btnStirProbeCleaning.setOnClickListener {
            vm.stirProbeCleaning(vd.tetStirProbeCleaningDuration.text.toString())
        }
        vd.btnSamplingProbeCleaning.setOnClickListener {
            vm.samplingProbeCleaning(vd.tetSamplingProbeCleaningDuration.text.toString())
        }
        vd.btnPierced.setOnClickListener {
            vm.pierced()
        }
        vd.btnSqueezing.setOnClickListener {
            vm.squeezing()
        }
        vd.btnShutdown.setOnClickListener {
            vm.shutdown()
        }
        vd.btnGetMcuVersion.setOnClickListener {
            vm.getMcuVersion()
        }
        vd.btnTemp.setOnClickListener {
            vm.getTemp()
        }
        vd.btnGetState.setOnClickListener {
            vm.getState()
        }
        vd.btnMoveSampleShelfReset.setOnClickListener {
            vm.moveSampleShelf("0")
        }
        vd.btnMoveCuvetteShelfReset.setOnClickListener {
            vm.moveCuvetteShelf("0")
        }
    }

    override fun onResume() {
        super.onResume()
        vm.listener()
    }

    override fun onPause() {
        super.onPause()
        vm.clearListener()
    }

    companion object {
        @JvmStatic
        fun newInstance() = SingleCmdFragment()
    }
}
