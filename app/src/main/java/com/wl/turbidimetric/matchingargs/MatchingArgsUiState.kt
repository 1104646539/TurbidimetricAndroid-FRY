package com.wl.turbidimetric.matchingargs

data class MatchingArgsDialogUiState(
    val dialogState: DialogState,
    val msg: String
)

data class MatchingArgsCurveUiState(
    /**
     * 显示选中项目的拟合公式
     */
    val equationText: String,
    /**
     * 显示选中项目的拟合度 R²
     */
    val fitGoodnessText: String,
)

/**
 * 对话框状态
 */
enum class DialogState {
    None,//无
    GetStateNotExistMsg,//开始检测 比色皿，样本，试剂,清洗液不存在
    MatchingFinishMsg,//拟合质控结束提示
}
