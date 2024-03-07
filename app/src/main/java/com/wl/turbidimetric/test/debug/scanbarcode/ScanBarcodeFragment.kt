package com.wl.turbidimetric.test.debug.scanbarcode

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentTestScanBarcodeBinding
import com.wl.turbidimetric.util.OnScanResult
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScanBarcodeFragment :
    BaseFragment<ScanBarcodeViewModel, FragmentTestScanBarcodeBinding>(R.layout.fragment_test_scan_barcode) {
    override val vm: ScanBarcodeViewModel by viewModels { ScanBarcodeViewModelFactory() }
    var oldOnScanResult: OnScanResult? = null
    private val hiltDialog: HiltDialog by lazy { HiltDialog(requireContext()) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

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
        listener()
    }


    private fun listener() {
        listenerView()
        listenerEvent()
    }

    private fun listenerView() {
        lifecycleScope.launch {
            vm.testMsg.collectLatest {
                vd.tvHilt.text = it
            }
        }
        lifecycleScope.launch {
            vm.hiltMsg.collectLatest {
                if (it.isNotEmpty()) {
                    hiltDialog.showPop(requireContext()) { dialog ->
                        dialog.showDialog(it, "确定", confirmClick = {
                            it.dismiss()
                        })
                    }
                }
            }
        }

    }

    private fun listenerEvent() {
        vd.btnStartTest.setOnClickListener {
            vm.startTest()
        }
        vd.btnStopTest.setOnClickListener {
            vm.stopTest()
        }
    }

    override fun onDestroyView() {
        vm.stopTest()
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScanBarcodeFragment()
    }
}
