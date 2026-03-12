package com.wl.turbidimetric.test.debug.debugSettings

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentDebugSettingsBinding
import com.wl.turbidimetric.datastore.LocalDataGlobal
import com.wl.turbidimetric.ex.toast
import com.wl.wllib.LogToFile.i

class DebugSettingsFragment :
    BaseFragment<DebugSettingsViewModel, FragmentDebugSettingsBinding>(R.layout.fragment_debug_settings) {
    override val vm: DebugSettingsViewModel by viewModels { DebugSettingsViewModelFactory() }

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
        vd.btnSave.setOnClickListener {
            verify(
                vm.tempLowLimit.value?.toIntOrNull() ?: LocalDataGlobal.Default.TempLowLimit,
                vm.tempUpLimit.value?.toIntOrNull() ?: LocalDataGlobal.Default.TempUpLimit,
                vm.preheatTime.value?.toIntOrNull() ?: 0,
            ).let {
                if (it.isNotEmpty()) {
                    toast(it)
                    return@setOnClickListener
                }
                vm.saveConfig()
                toast("保存成功", Toast.LENGTH_LONG)
            }
        }
    }

    private fun verify(tempLowLimit: Int, tempUpLimit: Int, preheatTime: Int): String {
        var msg = ""
        if (preheatTime < 10) {
            msg = "预热时间必须大于10 "
        }
        if (tempLowLimit > tempUpLimit) {
            msg = "下限不能高于上限"
        }
        return msg;
    }


    companion object {
        @JvmStatic
        fun newInstance() = DebugSettingsFragment()
    }
}
