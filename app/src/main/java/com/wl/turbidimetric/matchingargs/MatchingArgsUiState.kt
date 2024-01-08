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
    /**
     * 无
     */
    None,

    /**
     * 开始检测 比色皿，样本，试剂,清洗液不存在
     */
    GetStateNotExistMsg,

    /**
     * 拟合质控结束提示
     */
    MatchingFinishMsg,

    /**
     * 意外错误等
     */
    ACCIDENT,

    /**
     * 拟合配置
     */
    MatchingSettings,

    /**
     * 拟合状态对话框
     */
    MatchingState,

    /**
     * 返回状态失败、非法参数、传感器错误、电机错误等的提示
     */
    STATE_FAILED,
}
