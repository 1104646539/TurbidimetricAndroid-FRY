package com.wl.turbidimetric.test.debug.motorDebug

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentMotorDebugBinding
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MotorDebugFragment :
        BaseFragment<MotorDebugViewModel, FragmentMotorDebugBinding>(
                R.layout.fragment_motor_debug
        ) {
    override val vm: MotorDebugViewModel by viewModels { MotorDebugViewModelFactory() }
    var spnMotor: SpnSampleAdapter? = null

    override fun onResume() {
        super.onResume()
        vm.listener()
    }

    override fun onPause() {
        super.onPause()
        vm.clearListener()
        i("onPause")
    }

    override fun initViewModel() {}

    override fun init(savedInstanceState: Bundle?) {
        vd.vm = vm
        initView()
    }

    private fun initView() {
        spnMotor = SpnSampleAdapter(requireContext(), vm.motor)
        vm.viewModelScope.launch {
            vm.MotorIndexFlow.collectLatest {
                if (it < 13) {
                    vd.cbReset.text = "复位"
                    vd.cbForward.text = "正向"
                    vd.cbBackward.visibility = View.VISIBLE
                    vd.tietMotorParams.visibility = View.VISIBLE
                } else {
                    vd.cbReset.text = "开"
                    vd.cbForward.text = "关"
                    vd.cbBackward.visibility = View.GONE
                    vd.tietMotorParams.visibility = View.GONE
                    if(vd.cbBackward.isChecked){
                        vd.cbReset.isChecked = true
                    }
                }
            }
        }
        vd.spnMotor.adapter = spnMotor
    }

    companion object {
        @JvmStatic fun newInstance() = MotorDebugFragment()
    }
}
