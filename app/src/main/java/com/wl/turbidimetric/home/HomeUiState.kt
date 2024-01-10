package com.wl.turbidimetric.home

data class HomeDialogUiState(
    val dialogState: DialogState,
    val dialogMsg: String
)

data class HomeMachineUiState(
    val r1State: Boolean,//R1状态
    val r2State: Int,//R2状态
//    val r2VolumeState:Int ,//R2图片状态
    val cleanoutFluidState: Boolean,//清洗液状态
    val reactionTemp: Double,//反应槽温度
    val r1Temp: Double,//r1温度
)

enum class DialogState {
    /**
     * 无
     */
    None,

    /**
     * 显示 自检中
     */
    GetMachineShow,

    /**
     * 关闭 自检中
     */
    GetMachineDismiss,

    /**
     * 显示自检失败对话框
     */
    GetMachineFailedShow,

    /**
     * 检测结束 比色皿不足对话框
     */
    CuvetteDeficiency,

    /**
     * 检测结束
     */
    TestFinish,

    /**
     * 样本不足
     */
    SampleDeficiency,

    /**
     * 开始检测 比色皿，样本，试剂不存在
     */
    GetStateNotExist,

    /**
     * 其他通知
     */
    Notify,

    /**
     * 返回状态失败、非法参数、传感器错误、电机错误等的提示
     */
    StateFailed,
}
