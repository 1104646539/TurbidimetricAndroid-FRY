package com.wl.turbidimetric.settings.report

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentReportBinding
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
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
                    vd.cbAutoPrintReceipt.isChecked = state.autoPrintReceipt
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
            vm.change(vd.tietHospitalName.text?.toString() ?: "", vd.cbAutoPrintReceipt.isChecked)
        }
    }

    companion object {
        @JvmStatic
        val instance: ReportFragment by lazy { ReportFragment() }
    }
}
