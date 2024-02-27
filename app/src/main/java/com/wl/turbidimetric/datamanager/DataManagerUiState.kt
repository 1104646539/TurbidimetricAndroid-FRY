package com.wl.turbidimetric.datamanager

import com.wl.turbidimetric.home.HomeDialogUiState
import com.wl.turbidimetric.model.TestResultAndCurveModel

sealed class DataManagerUiState {
    object None : DataManagerUiState()
    object DeleteDialog : DataManagerUiState()
     class ResultDetailsDialog(val model: TestResultAndCurveModel) :
        DataManagerUiState()
}
