package com.wl.turbidimetric.settings.detectionnum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.repository.if2.LocalDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DetectionNumViewModel(private val localDataDataSource: LocalDataSource) : BaseViewModel() {
    private val _detectionNumUiState = MutableSharedFlow<DetectionNumUiState>()
    val detectionNumUiState = _detectionNumUiState.asSharedFlow()

    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _detectionNumUiState.emit(
                DetectionNumUiState(
                    localDataDataSource.getDetectionNum().toLongOrNull() ?: 0,
                )
            )
        }
    }

    fun change(
        detectionNum: Long,
    ) {
        val verRet = verify(
            detectionNum
        )
        if (verRet.isNotEmpty()) {
            viewModelScope.launch {
                _hiltText.emit("$verRet")
            }
            return
        }
        localDataDataSource.setDetectionNum(detectionNum.toString())
        viewModelScope.launch {
            _detectionNumUiState.emit(
                DetectionNumUiState(
                    localDataDataSource.getDetectionNum().toLongOrNull() ?: 0,
                )
            )
        }

        viewModelScope.launch {
            _hiltText.emit("修改成功")
        }
    }

    private fun verify(
        detectionNum: Long,
    ): String {
        if (detectionNum < 0) {
            return "编号不能为空"
        }

        return ""
    }
}

class DetectionNumViewModelFactory(
    private val localDataDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!)
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetectionNumViewModel::class.java)) {
            return DetectionNumViewModel(localDataDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class DetectionNumUiState(
    val detectionNum: Long,
)
