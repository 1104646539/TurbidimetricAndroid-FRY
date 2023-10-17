package com.wl.turbidimetric.home

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    NONE,//无
    GET_MACHINE_SHOW,//显示 自检中
    GET_MACHINE_DISMISS,//关闭 自检中
    GET_MACHINE_FAILED_SHOW,//显示自检失败对话框
    CUVETTE_DEFICIENCY,//检测结束 比色皿不足对话框
    TEST_FINISH,//检测结束
    SAMPLE_DEFICIENCY,//样本不足
    GET_STATE_NOT_EXIST,//开始检测 比色皿，样本，试剂不存在
    NOTIFY,//其他通知
}
