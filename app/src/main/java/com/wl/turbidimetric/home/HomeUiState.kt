package com.wl.turbidimetric.home


sealed class HomeDialogUiState {
    object None : HomeDialogUiState()

    /**
     * 显示 自检中
     */
    object GetMachineShow : HomeDialogUiState()

    /**
     * 关闭 自检中
     */
    object GetMachineDismiss : HomeDialogUiState()
    /**
     * 显示自检失败对话框
     */
    class GetMachineFailedShow(val msg: String) :
        HomeDialogUiState()

    /**
     * 检测中 比色皿不足对话框
     */
    object CuvetteDeficiency :
        HomeDialogUiState()
    /**
     * 检测结束
     */
    class TestFinish(val msg: String) : HomeDialogUiState()
    /**
     * 开始检测 比色皿，样本，试剂不存在
     */
    class GetStateNotExist(val msg: String) :
        HomeDialogUiState()
    /**
     * 其他通知
     */
    class Notify(val msg: String) : HomeDialogUiState()
    /**
     * 返回状态失败、非法参数、传感器错误、电机错误等的提示
     */
    class StateFailed(val msg: String) : HomeDialogUiState()
}

data class HomeMachineUiState(
    val r1State: Boolean,//R1状态
    val r2State: Int,//R2状态
//    val r2VolumeState:Int ,//R2图片状态
    val cleanoutFluidState: Boolean,//清洗液状态
    val reactionTemp: Double,//反应槽温度
    val r1Temp: Double,//r1温度
)
