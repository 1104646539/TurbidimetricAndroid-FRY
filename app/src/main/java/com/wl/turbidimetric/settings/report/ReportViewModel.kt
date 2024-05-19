package com.wl.turbidimetric.settings.report

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

class ReportViewModel(private val localDataDataSource: LocalDataSource) : BaseViewModel() {
    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()

    private val _reportViewModelState = MutableSharedFlow<ReportViewModelState>()
    val reportViewModelState = _reportViewModelState.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _reportViewModelState.emit(
                ReportViewModelState(
                    localDataDataSource.getHospitalName(),
                    localDataDataSource.getAutoPrintReceipt()
                )
            )

        }
    }

    fun change(
        hospitalName: String,
        autoPrintReceipt: Boolean,
    ) {
        localDataDataSource.setHospitalName(hospitalName)
        localDataDataSource.setAutoPrintReceipt(autoPrintReceipt)
        viewModelScope.launch {
            _hiltText.emit("修改成功")
        }
    }

}

class ReportViewModelFactory(
    private val localDataDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!)
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            return ReportViewModel(localDataDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ReportViewModelState(
    val hospitalName: String,
    val autoPrintReceipt: Boolean,
)
