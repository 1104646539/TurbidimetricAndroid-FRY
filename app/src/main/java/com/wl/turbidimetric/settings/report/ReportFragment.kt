package com.wl.turbidimetric.settings.report

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.PrinterState
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentReportBinding
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.report.PrintSDKHelper
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReportFragment :
    BaseFragment<ReportViewModel, FragmentReportBinding>(R.layout.fragment_report) {
    override val vm: ReportViewModel by viewModels { ReportViewModelFactory() }
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
            vm.hiltText.collectLatest { hilt ->
                hiltDialog.showPop(requireContext()) { dialog ->
                    dialog.showDialog(hilt, confirmText = "确定", confirmClick = {
                        dialog.dismiss()
                    })
                }
            }
        }
        lifecycleScope.launch {
            vm.reportViewModelState.collectLatest { state ->
                hiltDialog.showPop(requireContext()) { dialog ->
                    vd.tietHospitalName.setText(state.hospitalName)
                    vd.tietDetectionDoctor.setText(state.detectionDoctor)
                    vd.cbAutoPrintReceipt.isChecked = state.autoPrintReceipt
                    vd.cbAutoPrintReport.isChecked = state.autoPrintReport
                    if (state.reportFileNameBarcode) {
                        vd.rgReportFileName.check(R.id.rb_fileName_barcode)
                    } else {
                        vd.rgReportFileName.check(R.id.rb_fileName_num)
                    }
                    if (state.curPrinter == null) {
                        vd.tvCurPrinter.text = "当前打印机:无"
                    } else {
                        vd.tvCurPrinter.text = "当前打印机:${state.curPrinter.name}"
                    }
                    vd.tietReportIntervalTime.setText(state.reportIntervalTime.toString())
                }
            }
        }
        lifecycleScope.launch {
            appVm.printerState.collectLatest {
                if (it == PrinterState.Success) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(2000)
                        vm.reset()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_FIRST_USER) {
            if (requestCode == 12345) {
                vm.reset()
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
            changeConfig()
        }
        vd.btnSetupPrinter.setOnClickListener {
            PrintSDKHelper.showSetupPrinterUi()
        }
    }

    private fun changeConfig() {
        val intervalTime = vd.tietReportIntervalTime.text?.toString()?.toIntOrNull() ?: 0
        if (intervalTime < 30) {
            toast("报告间隔时长不能小于等于30S")
            return
        }
        vm.change(
            vd.tietHospitalName.text?.toString() ?: "",
            vd.tietDetectionDoctor.text?.toString() ?: "",
            vd.cbAutoPrintReceipt.isChecked,
            vd.cbAutoPrintReport.isChecked,
            vd.rbFileNameBarcode.isChecked,
            intervalTime,
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReportFragment()
    }
}
