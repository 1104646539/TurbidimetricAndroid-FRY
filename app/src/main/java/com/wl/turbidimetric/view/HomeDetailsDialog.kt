package com.wl.turbidimetric.view

import android.content.Context
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.model.CuvetteState
import com.wl.turbidimetric.model.SampleState
import com.wl.turbidimetric.model.TestResultModel

class HomeDetailsDialog(val context: Context) : BaseDialog(context) {
    val tvResultID: TextView
    val tvDetectionNum: TextView
    val tvLabel: TextView
    val tvLabelHilt: TextView
    val tvState: TextView

    init {
        addView(R.layout.dialog_home_details)
        tvResultID = getView(R.id.tv_result_id) as TextView
        tvDetectionNum = getView(R.id.tv_detection_num) as TextView
        tvLabel = getView(R.id.tv_label) as TextView
        tvLabelHilt = getView(R.id.tv_label_hilt) as TextView
        tvState = getView(R.id.tv_state) as TextView
    }

    fun show(
        item: HomeViewModel.CuvetteItem?
    ) {
        if (item == null) return
        super.show("确定", { dismiss() }, "", null, true)
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

    fun show(
        item: HomeViewModel.SampleItem?
    ) {
        if (item == null) return
        super.show("确定", { dismiss() }, "", null, true)
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

    private fun show(
        resultID: String,
        detectionNum: String,
        label: String,
        labelHilt: String,
        state: String,
        testResultModel: TestResultModel?
    ) {

        tvState.text = state
        tvResultID.text = resultID
        tvLabel.text = label
        tvLabelHilt.text = labelHilt
        tvDetectionNum.text = detectionNum

    }
}
