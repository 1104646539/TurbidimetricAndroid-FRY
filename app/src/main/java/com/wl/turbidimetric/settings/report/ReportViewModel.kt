package com.wl.turbidimetric.settings.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dynamixsoftware.printingsdk.Printer
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.report.PrintSDKHelper
import com.wl.turbidimetric.repository.if2.LocalDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ReportViewModel(
    private val appViewModel: AppViewModel,
    private val localDataDataSource: LocalDataSource
) : BaseViewModel() {
    private val _hiltText = MutableSharedFlow<String>()
    val hiltText = _hiltText.asSharedFlow()

    private val _reportViewModelState = MutableSharedFlow<ReportViewModelState>()
    val reportViewModelState = _reportViewModelState.asSharedFlow()
    fun reset() {
        viewModelScope.launch {
            _reportViewModelState.emit(
                ReportViewModelState(
                    localDataDataSource.getHospitalName(),
                    localDataDataSource.getAutoPrintReceipt(),
                    localDataDataSource.getAutoPrintReport(),
                    localDataDataSource.getReportFileNameBarcode(),
                    PrintSDKHelper.getCurPrinter(),
                    localDataDataSource.getReportIntervalTime()
                )
            )

        }
    }

    fun change(
        hospitalName: String,
        autoPrintReceipt: Boolean,
        autoPrintReport: Boolean,
        reportFileNameBarcode: Boolean,
        reportIntervalTime: Int,
    ) {
        localDataDataSource.setHospitalName(hospitalName)
        localDataDataSource.setAutoPrintReceipt(autoPrintReceipt)
        localDataDataSource.setAutoPrintReport(autoPrintReport)
        localDataDataSource.setReportFileNameBarcode(reportFileNameBarcode)
        if (reportIntervalTime != localDataDataSource.getReportIntervalTime()) {
            localDataDataSource.setReportIntervalTime(reportIntervalTime)
            appViewModel.printHelper.setIntervalTime(reportIntervalTime)
        }
        viewModelScope.launch {
            _hiltText.emit("修改成功")
        }
    }

}

class ReportViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
    private val localDataDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!)
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            return ReportViewModel(appViewModel, localDataDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ReportViewModelState(
    val hospitalName: String,
    val autoPrintReceipt: Boolean,
    val autoPrintReport: Boolean,
    val reportFileNameBarcode: Boolean,
    val curPrinter: Printer?,
    val reportIntervalTime: Int,
)
