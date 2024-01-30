package com.wl.turbidimetric.settings.fragments.detectionnum

import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.datastore.LocalData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DetectionNumViewModel : BaseViewModel() {
    private val _detectionNumUiState = MutableSharedFlow<DetectionNumUiState>()
    val detectionNumUiState = _detectionNumUiState.asSharedFlow()

    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _detectionNumUiState.emit(
                DetectionNumUiState(
                    LocalData.DetectionNum.toLongOrNull() ?: 0,
                )
            )
        }
    }

    fun change(
        detectionNum: Long,
    ) {
        val verRet = verify(
            detectionNum,
        )
        if (verRet.isNotEmpty()) {
            viewModelScope.launch {
                _hiltText.emit("$verRet")
            }
            return
        }
        LocalData.DetectionNum = detectionNum.toString()

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

data class DetectionNumUiState(
    val detectionNum: Long,
)
