package com.wl.turbidimetric.matchingargs

import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.view.MatchingConfigLayout

sealed class MatchingArgsDialogUiState {
    /**
     * 无
     */
    object None : MatchingArgsDialogUiState()

    /**
     * 开始检测 比色皿，样本，试剂,清洗液不存在
     */
    class GetStateNotExistMsg(val msg: String) : MatchingArgsDialogUiState()

    /**
     * 拟合质控结束提示
     */
    class MatchingFinishMsg(
        val reagnetNo: String,
        val gradsNum: Int,
        val abss: MutableList<MutableList<Double>>,
        val targets: List<Double>,
        val means: List<Double>,
        val selectFitterType: FitterType,
        val curProject: CurveModel?,
        val isQuality: Boolean,
    ) : MatchingArgsDialogUiState()

    /**
     * 意外错误等
     */
    class Accident(val msg: String) : MatchingArgsDialogUiState()

    /**
     * 拟合选择覆盖曲线
     */
    class MatchingCoverCurve(val msg: String) : MatchingArgsDialogUiState()

    /**
     * 拟合配置
     */
    class MatchingSettings(
        val curves: MutableList<CurveModel>,
        val qualityLow1: Int,
        val qualityLow2: Int,
        val qualityHigh1: Int,
        val qualityHigh2: Int,
        val reagentNo: String,
        val quality: Boolean,
        val projects: List<ProjectModel>,
        val autoAttenuation: Boolean,
        val gradsNum: Int = 5,
        val selectProject: ProjectModel? = null,
        val selectFitterType: FitterType = FitterType.Three,
        val targetCons: List<Double> = mutableListOf(),
    ) : MatchingArgsDialogUiState()

    /**
     * 拟合状态对话框
     */
    class MatchingState(
        val isError: Boolean,
        val matchingType: MatchingConfigLayout.MatchingType,
        val qualityLow1: Int,
        val qualityLow2: Int,
        val qualityHigh1: Int,
        val qualityHigh2: Int,
        val gradsNum: Int,
        val abss: MutableList<Double>,
        val targets: List<Double>,
        val means: List<Double>,
        val selectFitterType: FitterType,
        val curProject: CurveModel?,
        val quality: Boolean
    ) : MatchingArgsDialogUiState()

    /**
     * 返回状态失败、非法参数、传感器错误、电机错误等的提示
     */
    class StateFailed(val msg: String) : MatchingArgsDialogUiState()

    /**
     * 关闭拟合中的对话框，在未拟合就点击结束的时候用
     */
    class CloseMatchingStateDialog(val msg: String) : MatchingArgsDialogUiState()

    /**
     * 二次提示是否不保存结果，当已经拟合完成了点击不保存时使用
     */
    class HiltNotSaveDialog(val msg: String) : MatchingArgsDialogUiState()

}

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

data class MatchingConfigUiState(
    val project: ProjectModel?,
    val autoAttenuation: Boolean,
    val gradsNum: Int,
    val selectFitterType: FitterType,
    val targetCons: MutableList<Double>,
    val quality: Boolean,
    val reagentNo: String
)
