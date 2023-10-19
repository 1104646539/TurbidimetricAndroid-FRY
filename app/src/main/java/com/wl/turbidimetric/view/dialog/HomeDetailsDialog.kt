package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.widget.TextView
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.model.CuvetteState
import com.wl.turbidimetric.model.SampleState
import com.wl.turbidimetric.model.TestResultModel

class HomeDetailsDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_home_details) {
    var tvResultID: TextView? = null
    var tvDetectionNum: TextView? = null
    var tvLabel: TextView? = null
    var tvLabelHilt: TextView? = null
    var tvState: TextView? = null


    fun showDialog(
        item: HomeViewModel.CuvetteItem?
    ) {
        if (item == null) return
        this.confirmText = "确定"
        this.confirmClick = { dismiss() }

        show(
            (item.testResult?.id ?: "-").toString(),
            item.testResult?.detectionNum ?: "-",
            item.sampleID ?: "-",
            "对应样本序号:",
            getState(item.state),
            item.testResult
        )
    }

    private fun getState(state: CuvetteState): String {
        return when (state) {
            CuvetteState.None -> "未知"
            CuvetteState.Skip -> "跳过"
            CuvetteState.DripSample -> "已加样"
            CuvetteState.DripReagent -> "已加试剂"
            CuvetteState.Stir -> "等待检测"
            CuvetteState.Test1 -> "正在检测"
            CuvetteState.Test2 -> "正在检测"
            CuvetteState.Test3 -> "正在检测"
            CuvetteState.Test4 -> "检测结束"
            else -> {
                "未知"
            }
        }
    }

    fun showDialog(
        item: HomeViewModel.SampleItem?
    ) {
        if (item == null) return
        this.confirmText = "确定"
        this.confirmClick = { dismiss() }
        show(
            (item.testResult?.id ?: "-").toString(),
            item.testResult?.detectionNum ?: "-",
            item.cuvetteID ?: "-",
            "对应比色皿序号:",
            getState(item.state),
            item.testResult
        )
    }

    private fun getState(state: SampleState): String {
        return when (state) {
            SampleState.None -> "未知"
            SampleState.Exist -> "存在"
            SampleState.ScanSuccess -> "扫码成功"
            SampleState.ScanFailed -> "扫码失败"
            SampleState.Pierced -> "已刺破"
            SampleState.Sampling -> "已取样"
            else -> {
                "未知"
            }
        }
    }

    var resultID: String? = null
    var detectionNum: String? = null
    var label: String? = null
    var labelHilt: String? = null
    var state: String? = null
    var testResultModel: TestResultModel? = null
    private fun show(
        resultID: String,
        detectionNum: String,
        label: String,
        labelHilt: String,
        state: String,
        testResultModel: TestResultModel?
    ) {
        this.resultID = resultID
        this.detectionNum = detectionNum
        this.label = label
        this.labelHilt = labelHilt
        this.state = state
        this.testResultModel = testResultModel

        super.show()
    }

    override fun initDialogView() {
        tvResultID = findViewById(R.id.tv_result_id)
        tvDetectionNum = findViewById(R.id.tv_detection_num)
        tvLabel = findViewById(R.id.tv_label)
        tvLabelHilt = findViewById(R.id.tv_label_hilt)
        tvState = findViewById(R.id.tv_state)
    }

    override fun setContent() {
        super.setContent()
        tvState?.text = state
        tvResultID?.text = resultID
        tvLabel?.text = label
        tvLabelHilt?.text = labelHilt
        tvDetectionNum?.text = detectionNum
    }

}
