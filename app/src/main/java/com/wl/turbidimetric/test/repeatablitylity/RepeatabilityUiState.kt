package com.wl.turbidimetric.test.repeatablitylity


data class RepeatabilityUiState(
    val dialogState: DialogState,
    val dialogMsg: String
)
enum class DialogState{
    NONE,

    /**
     * 开始检测 比色皿，样本，试剂不存在
     */
    GetStateNotExist,

    /**
     * 结束
     */
    TestFinish,
    /**
     * 返回状态失败、非法参数、传感器错误、电机错误等的提示
     */
    StateFailed,
}
