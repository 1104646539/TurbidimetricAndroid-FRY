package com.wl.turbidimetric.test.debug.motorDebug

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentMotorDebugBinding
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.turbidimetric.util.OnScanResult
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.wllib.LogToFile.i

class MotorDebugFragment :
    BaseFragment<MotorDebugViewModel, FragmentMotorDebugBinding>(R.layout.fragment_motor_debug) {
    override val vm: MotorDebugViewModel by viewModels { MotorDebugViewModelFactory() }
    var spnMotor: SpnSampleAdapter? = null
    var oldOnScanResult: OnScanResult? = null


    override fun onResume() {
        super.onResume()
        oldOnScanResult = ScanCodeUtil.onScanResult
        vm.listener()
    }

    override fun onPause() {
        super.onPause()
        vm.clearListener()
        ScanCodeUtil.onScanResult = oldOnScanResult
        i("onPause")
    }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        vd.vm = vm
        initView()
    }



    private fun initView() {
        spnMotor = SpnSampleAdapter(requireContext(), vm.motor)
        vd.spnMotor.adapter = spnMotor



    }


    companion object {
        @JvmStatic
        fun newInstance() = MotorDebugFragment()
    }
}
