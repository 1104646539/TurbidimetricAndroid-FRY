package com.wl.turbidimetric.settings.params

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.repository.DefaultLocalDataDataSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ParamsViewModel constructor(
    private val localDataSource: LocalDataSource
) : BaseViewModel() {
    private val _paramsUiState = MutableSharedFlow<ParamsUiState>()
    val paramsUiState = _paramsUiState.asSharedFlow()

    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _paramsUiState.emit(
                ParamsUiState(
                    localDataSource.getTakeReagentR1(),
                    localDataSource.getTakeReagentR2(),
                    localDataSource.getSamplingVolume(),
                    localDataSource.getSamplingProbeCleaningDuration(),
                    localDataSource.getStirProbeCleaningDuration(),
                    localDataSource.getStirDuration(),
                    localDataSource.getTest1DelayTime(),
                    localDataSource.getTest2DelayTime(),
                    localDataSource.getTest3DelayTime(),
                    localDataSource.getTest4DelayTime(),
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
        if (verRet.isNotEmpty()) {
            viewModelScope.launch {
                _hiltText.emit("$verRet")
            }
            return
        }
        localDataSource.setTakeReagentR1(r1Volume)
        localDataSource.setTakeReagentR2(r2Volume)
        localDataSource.setSamplingVolume(samplingVolume)
        localDataSource.setSamplingProbeCleaningDuration(samplingProbeCleaningDuration)
        localDataSource.setStirProbeCleaningDuration(stirProbeCleaningDuration)
        localDataSource.setStirDuration(stirDuration)
        localDataSource.setTest1DelayTime(test1DelayTime)
        localDataSource.setTest2DelayTime(test2DelayTime)
        localDataSource.setTest3DelayTime(test3DelayTime)
        localDataSource.setTest4DelayTime(test4DelayTime)

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

class ParamsViewModelFactory(
    private val localDataSource: LocalDataSource = DefaultLocalDataDataSource()
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParamsViewModel::class.java)) {
            return ParamsViewModel(
                localDataSource
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
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
