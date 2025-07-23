package com.wl.turbidimetric.home

import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.Item


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

    /**
     * 填充R1失败
     */
    class FullR1Failed() : HomeDialogUiState()

}

data class HomeMachineUiState(
    val r1State: Boolean,//R1状态
    val r2State: Int,//R2状态
//    val r2VolumeState:Int ,//R2图片状态
    val cleanoutFluidState: Boolean,//清洗液状态
    val reactionTemp: Double,//反应槽温度
    val r1Temp: Double,//r1温度
)

data class HomeConfigUiState(
    val curveModel: CurveModel?,//当前选择的曲线
    val startNum: String,//起始编号
    val cuvetteStartPos: Int,//跳过比色皿
    val detectionNum: Int,//检测数量

)

data class HomeDetailsUiState(
    val shelfIndex: Int,//选中的架的排数
    val curFocIndex: Int,//选中的架的下标
    val item: Item,//详情
    val hiltDetails: Boolean,//是否隐藏详情
)
