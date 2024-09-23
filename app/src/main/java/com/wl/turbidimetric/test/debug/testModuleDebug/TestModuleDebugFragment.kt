package com.wl.turbidimetric.test.debug.testModuleDebug

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentTestChartBinding
import com.wl.turbidimetric.databinding.FragmentTestModuleDebugBinding
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.ex.toast
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TestModuleDebugFragment :
    BaseFragment<TestModuleDebugViewModel, FragmentTestModuleDebugBinding>(R.layout.fragment_test_module_debug) {
    override val vm: TestModuleDebugViewModel by viewModels { TestModuleDebugViewModelFactory() }

    val temp = mutableListOf<Int>()
    override fun onResume() {
        super.onResume()
        vm.listener()
    }

    override fun onPause() {
        super.onPause()
        vm.clearListener()
        i("onPause")
    }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        listener()
    }


    private fun listener() {
        listenerView()
        listenerEvent()
    }

    private fun listenerView() {
        lifecycleScope.launch {
            vm.testMsg.collectLatest {
//                toast(it)
                vd.tvMsg.setText(it)
            }
        }

    }

    private fun listenerEvent() {
        vd.btnStartIntervalTest.setOnClickListener {
            vm.startIntervalTest(
                vd.tetCuvetteIntervalDuration.text.toString(),
                vd.tetCuvetteDelayDuration.text.toString()
            )
        }
        vd.btnClear.setOnClickListener {
            vm.clearMsg()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance() = TestModuleDebugFragment()
    }
}
