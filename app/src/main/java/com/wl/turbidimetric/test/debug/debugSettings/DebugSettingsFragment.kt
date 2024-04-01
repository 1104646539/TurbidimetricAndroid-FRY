package com.wl.turbidimetric.test.debug.debugSettings

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentDebugSettingsBinding
import com.wl.wllib.LogToFile.i

class DebugSettingsFragment :
    BaseFragment<DebugSettingsViewModel, FragmentDebugSettingsBinding>(R.layout.fragment_debug_settings) {
    override val vm: DebugSettingsViewModel by viewModels{DebugSettingsViewModelFactory()}

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        i("onPause")
    }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        vd.vm = vm
        initView()
    }

    private fun initView() {

        vm.looperTest.observe(this){
            vm.changeLooperTest(it)
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = DebugSettingsFragment()
    }
}
