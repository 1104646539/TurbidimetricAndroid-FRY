package com.wl.turbidimetric.settings.fragments.params

import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ParamsViewModel : BaseViewModel() {
    private val _paramsUiState = MutableSharedFlow<ParamsUiState>()
    val paramsUiState = _paramsUiState.asSharedFlow()

    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _paramsUiState.emit(
                ParamsUiState(
                    LocalData.TakeReagentR1,
                    LocalData.TakeReagentR2,
                    LocalData.SamplingVolume,
                    LocalData.SamplingProbeCleaningDuration,
                    LocalData.StirProbeCleaningDuration,
                    LocalData.StirDuration,
                    LocalData.Test1DelayTime,
                    LocalData.Test2DelayTime,
                    LocalData.Test3DelayTime,
                    LocalData.Test4DelayTime,
                )
            )
        }
    }

    fun change(
        r1Volume: Int,
        r2Volume: Int,
        samplingVolume: Int,
        samplingProbeCleaningDuration: Int,
        stirProbeCleaningDuration: Int,
        stirDuration: Int,
        test1DelayTime: Long,
        test2DelayTime: Long,
        test3DelayTime: Long,
        test4DelayTime: Long
    ) {
        val verRet = verify(
            r1Volume,
            r2Volume,
            samplingVolume,
            samplingProbeCleaningDuration,
            stirProbeCleaningDuration,
            stirDuration,
            test1DelayTime,
            test2DelayTime,
            test3DelayTime,
            test4DelayTime
        )
        if(verRet.isNotEmpty()){
            viewModelScope.launch {
                _hiltText.emit("$verRet")
            }
            return
        }
        LocalData.TakeReagentR1 = r1Volume
        LocalData.TakeReagentR2 = r2Volume
        LocalData.SamplingVolume = samplingVolume
        LocalData.SamplingProbeCleaningDuration = samplingProbeCleaningDuration
        LocalData.StirProbeCleaningDuration = stirProbeCleaningDuration
        LocalData.StirDuration = stirDuration
        LocalData.Test1DelayTime = test1DelayTime
        LocalData.Test2DelayTime = test2DelayTime
        LocalData.Test3DelayTime = test3DelayTime
        LocalData.Test4DelayTime = test4DelayTime

        viewModelScope.launch {
            _hiltText.emit("修改成功")
        }
    }

    private fun verify(
        r1Volume: Int,
        r2Volume: Int,
        samplingVolume: Int,
        samplingProbeCleaningDuration: Int,
        stirProbeCleaningDuration: Int,
        stirDuration: Int,
        test1DelayTime: Long,
        test2DelayTime: Long,
        test3DelayTime: Long,
        test4DelayTime: Long
    ): String {
        if (r1Volume !in 0..500) {
            return "R1量必须小于500"
        }
        if (r2Volume !in 0..200) {
            return "R1量必须小于200"
        }
        if (samplingVolume !in 0..500) {
            return "取样量必须小于500"
        }
        if (samplingProbeCleaningDuration !in 0..5000) {
            return "取样针清洗时长必须小于5000"
        }
        if (stirProbeCleaningDuration !in 0..5000) {
            return "搅拌针清洗时长必须小于5000"
        }
        if (stirDuration !in 0..5000) {
            return "搅拌时长必须小于5000"
        }
        if (test1DelayTime !in 30000..50000) {
            return "第一次检测时间必须大于30s"
        }
//        if (test2DelayTime < 130000) {
//            return "第二次检测时间必须大于130s"
//        }


        return ""
    }
}

data class ParamsUiState(
    val r1Volume: Int,
    val r2Volume: Int,
    val samplingVolume: Int,
    val samplingProbeCleaningDuration: Int,
    val stirProbeCleaningDuration: Int,
    val stirDuration: Int,
    val test1DelayTime: Long,
    val test2DelayTime: Long,
    val test3DelayTime: Long,
    val test4DelayTime: Long,
)
