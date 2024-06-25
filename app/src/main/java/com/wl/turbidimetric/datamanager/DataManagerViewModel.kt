package com.wl.turbidimetric.datamanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.app.PrinterState
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.calcShowTestResult
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.main.MainActivity.PrintAnimParams
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.repository.if2.ProjectSource
import com.wl.turbidimetric.repository.if2.TestResultSource
import com.wl.turbidimetric.util.PrintSDKHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class DataManagerViewModel(
    private val appViewModel: AppViewModel,
    private val projectRepository: ProjectSource,
    private val testResultRepository: TestResultSource
) : BaseViewModel() {

    /**
     * 筛选条件
     */
    private val _conditionModel = MutableStateFlow(ConditionModel())
    val conditionModel: StateFlow<ConditionModel> = _conditionModel.asStateFlow()

    /**
     * 结果数量
     */
    private val _resultSize = MutableStateFlow(0L)
    val resultSize: StateFlow<Long> = _resultSize.asStateFlow()

    private val _exportExcelUIState = MutableStateFlow<ExportExcelUIState>(ExportExcelUIState.None)
    val exportExcelUIState = _exportExcelUIState.asStateFlow()
    private val _printUIState = MutableStateFlow<PrintUIState>(PrintUIState.None)
    val printUIState = _printUIState.asStateFlow()
    private val _printReportUIState = MutableStateFlow<PrintReportUIState>(PrintReportUIState.None)
    val printReportUIState = _printReportUIState.asStateFlow()
    private val _exportReportUiState =
        MutableStateFlow<ExportReportUIState>(ExportReportUIState.None)
    val exportReportUIState = _exportReportUiState.asStateFlow()
    private val _uploadUIState = MutableStateFlow<UploadUIState>(UploadUIState.None)
    val uploadUIState = _uploadUIState.asStateFlow()
    private val _deleteResultUIState =
        MutableStateFlow<DeleteResultUIState>(DeleteResultUIState.None)
    val deleteResultUIState = _deleteResultUIState.asStateFlow()
    private val _resultDetailsUIState =
        MutableStateFlow<ResultDetailsUIState>(ResultDetailsUIState.None)
    val resultDetailsUIState = _resultDetailsUIState.asStateFlow()
    private val _conditionUIState =
        MutableStateFlow<ConditionUIState>(ConditionUIState.None)
    val conditionUIState = _conditionUIState.asStateFlow()

    fun item(condition: ConditionModel): Flow<PagingData<TestResultAndCurveModel>> {
        viewModelScope.launch {
            _resultSize.value = testResultRepository.countTestResultAndCurveModels(condition)
        }
        return testResultRepository.listenerTestResult(condition).cachedIn(viewModelScope)
    }

    suspend fun update(testResult: TestResultModel): Int {
        return testResultRepository.updateTestResult(testResult)
    }

    suspend fun update(model: TestResultAndCurveModel): Int {
        return testResultRepository.updateTestResult(model.result)
    }

    suspend fun getTestResultAndCurveModelById(id: Long): TestResultAndCurveModel {
        return testResultRepository.getTestResultAndCurveModelById(id)
    }

    suspend fun add(testResult: TestResultModel): Long {
        return testResultRepository.addTestResult(testResult)
    }

    suspend fun add(testResult: List<TestResultModel>) {
        testResultRepository.addTestResults(testResult)
    }

    private suspend fun remove(testResults: List<TestResultModel>) {
        testResultRepository.removeTestResult(testResults)
    }

    private suspend fun deleteDialogConfirm(results: List<TestResultModel>) {
        remove(results)
    }

    private suspend fun getFilterAll(condition: ConditionModel): List<TestResultAndCurveModel> {
        return testResultRepository.getAllTestResult(condition)
    }

    fun conditionChange(conditionModel: ConditionModel) {
        viewModelScope.launch {
            _conditionModel.value = conditionModel
            _resultSize.value = testResultRepository.countTestResultAndCurveModels(conditionModel)
        }
    }


    fun deleteResult() {
        viewModelScope.launch {
            _deleteResultUIState.emit(DeleteResultUIState.ShowDialog)
        }
    }

    fun processIntent(intent: DataManagerIntent) {
        when (intent) {
            is DataManagerIntent.ExportExcelAll -> {
                exportExcel(exportAll = true)
            }

            is DataManagerIntent.PrintSelected -> {
                print(intent.items)
            }

            is DataManagerIntent.PrintReportSelected -> {
                printReport(intent.params, intent.items)
            }

            is DataManagerIntent.ExportReportSelected -> {
                exportReport(intent.items)
            }

            is DataManagerIntent.Upload -> {
                upload(intent.isConnected, intent.items)
            }

            is DataManagerIntent.DeleteResult -> {
                deleteResultItem(intent.items)
            }

            is DataManagerIntent.ResultDetailsUpdate -> {
                resultDetailsUpdate(intent.isDebug, intent.item)
            }

            is DataManagerIntent.ConditionUpdate -> {
                conditionChange(intent.condition)
            }
        }

    }

    fun showConditionDialog() {
        viewModelScope.launch {
            _conditionUIState.emit(ConditionUIState.ShowDialog(_conditionModel.value))
        }
    }

    fun showResultDetailsDialog(id: Long) {
        viewModelScope.launch {
            val ret = getTestResultAndCurveModelById(id)
            _resultDetailsUIState.emit(ResultDetailsUIState.ShowDialog(ret))
        }
    }

    private fun resultDetailsUpdate(debug: Boolean, item: TestResultAndCurveModel) {
        viewModelScope.launch {
            if (debug) {
                val newResult = calcShowTestResult(
                    item.result.concentration,
                    item.curve?.projectLjz ?: 0
                )
                item.result.testResult = newResult
            }
            val ret = update(item)
            if (ret > 0) {
                _resultDetailsUIState.emit(ResultDetailsUIState.Success("更新成功"))
            } else {
                _resultDetailsUIState.emit(ResultDetailsUIState.Failed("更新失败"))
            }
        }
    }

    private fun deleteResultItem(items: List<TestResultAndCurveModel>) {
        viewModelScope.launch {
            _deleteResultUIState.emit(DeleteResultUIState.Loading)
            dataEmptyVerify(items).also { verifyRet ->
                if (verifyRet.isEmpty()) {
                    deleteDialogConfirm(items.map { it.result })
                    _deleteResultUIState.emit(DeleteResultUIState.Success())
                } else {
                    _deleteResultUIState.emit(DeleteResultUIState.Failed(verifyRet))
                }
            }
        }
    }

    private fun upload(isConnected: Boolean, items: List<TestResultAndCurveModel>) {
        viewModelScope.launch {
            dataUploadVerify(items).also { verifyRet ->
                if (verifyRet.isEmpty()) {
                    if (isConnected) {
                        _uploadUIState.emit(UploadUIState.Upload(items))
                    } else {
                        _uploadUIState.emit(UploadUIState.Failed("上传未连接"))
                    }
                } else {
                    _uploadUIState.emit(UploadUIState.Failed(verifyRet))
                }
            }
        }
    }

    private fun dataUploadVerify(items: List<TestResultAndCurveModel>): String {
        dataEmptyVerify(items).also {
            if (it.isNotEmpty())
                return it
        }
        if (appViewModel.testState.isTestRunning() && SystemGlobal.uploadConfig.autoUpload) {
            return "请等待检测结束后上传"
        }

        if (items.any {
                it.result.curveOwnerId <= 0
            }) {
            return "没有检测项目"
        }

        if (items.any {
                it.result.testResult.isNullOrEmpty()
            }) {
            return "未检测的数据"
        }
        return ""
    }

    private fun exportReport(items: List<TestResultAndCurveModel>) {
        viewModelScope.launch {
            dataEmptyVerify(items).also { verifyRet ->
                if (verifyRet.isEmpty()) {
                    _exportReportUiState.emit(ExportReportUIState.Export(items))
                } else {
                    _exportReportUiState.emit(ExportReportUIState.Failed(verifyRet))
                }
            }
        }
    }

    private fun printReport(params: PrintAnimParams, items: List<TestResultAndCurveModel>) {
        viewModelScope.launch {
            dataEmptyVerify(items).also { verifyRet ->
                if (verifyRet.isEmpty()) {
                    when (PrintSDKHelper.printerState) {
                        PrinterState.Success -> {
                            _printReportUIState.emit(PrintReportUIState.PrintReport(params, items))
                        }

                        PrinterState.None, PrinterState.InitSdkFailed -> {
                            _printReportUIState.emit(PrintReportUIState.Failed("打印未初始化"))
                        }

                        PrinterState.NotInstallApk -> {
                            _printReportUIState.emit(PrintReportUIState.Failed("打印程序未安装"))
                        }

                        PrinterState.NotPrinter -> {
                            _printReportUIState.emit(PrintReportUIState.NoSelectedPrinter("未设置打印机，请先设置打印机"))
                        }
                    }
                } else {
                    _printReportUIState.emit(PrintReportUIState.Failed(verifyRet))
                }
            }
        }
    }


    /**
     * 验证 打印热敏
     */
    private fun dataEmptyVerify(items: List<TestResultAndCurveModel>?): String {
        return if (items.isNullOrEmpty()) {
            "请选择数据"
        } else {
            ""
        }
    }

    private fun exportExcel(
        exportAll: Boolean = false,
        items: List<TestResultAndCurveModel> = emptyList()
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            //step1、 获取数据
            val data = if (exportAll) {
                val condition = conditionModel.value
                getFilterAll(condition)
            } else {
                items
            }
            //step2、 验证 导出 等待结果
            val err = exportExcelVerify(data)
            if (err.isEmpty()) {
                _exportExcelUIState.emit(ExportExcelUIState.Export(data))
            } else {
                _exportExcelUIState.emit(ExportExcelUIState.Failed("导出失败,$err"))
            }
        }
    }

    /**
     * 导出excel成功
     * @param msg String
     */
    fun exportExcelSuccess(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _exportExcelUIState.emit(ExportExcelUIState.Success(msg))
        }
    }

    /**
     * 导出excel失败
     * @param error String
     */
    fun exportExcelFailed(error: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _exportExcelUIState.emit(ExportExcelUIState.Failed(error))
        }
    }

    /**
     * 导出报告成功
     * @param msg String
     */
    fun exportReportSuccess(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _exportReportUiState.emit(ExportReportUIState.Success(msg))
        }
    }

    /**
     * 导出报告失败
     * @param error String
     */
    fun exportReportFailed(error: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _exportReportUiState.emit(ExportReportUIState.Failed(error))
        }
    }

    /**
     * 验证 导出excel
     */
    private fun exportExcelVerify(items: List<TestResultAndCurveModel>?): String {
        dataEmptyVerify(items).also {
            if (it.isNotEmpty())
                return it
        }
        return if (items!!.size > 4000) {
            "一次操作不能大于4000条，当前操作了${items.size}条"
        } else {
            ""
        }
    }

    /**
     * 打印热敏
     * @param items List<TestResultAndCurveModel>
     */
    private fun print(items: List<TestResultAndCurveModel>) {
        viewModelScope.launch {
            dataEmptyVerify(items).also { ret ->
                if (ret.isEmpty()) {
                    _printUIState.emit(PrintUIState.Print(items))
                } else {
                    _printUIState.emit(PrintUIState.Failed(ret))
                }
            }

        }
    }

    /**
     * 上传完成
     * @param msg String
     */
    fun uploadResultSuccess(msg: String) {
        viewModelScope.launch {
            _uploadUIState.emit(UploadUIState.Success(msg))
        }
    }

    /**
     * 打印热敏成功
     * @param msg String
     */
    fun printSuccess(msg: String) {
        viewModelScope.launch {
            _printUIState.emit(PrintUIState.Success(msg))
        }
    }

    /**
     * 打印热敏失败
     * @param msg String
     */
    fun printFailed(msg: String) {
        viewModelScope.launch {
            _printUIState.emit(PrintUIState.Failed(msg))
        }
    }


    sealed class ExportExcelUIState {
        object None : ExportExcelUIState()
        data class Export(val item: List<TestResultAndCurveModel>) : ExportExcelUIState()
        data class Success(val msg: String) : ExportExcelUIState()
        data class Failed(val err: String) : ExportExcelUIState()
    }


    sealed class PrintUIState {
        object None : PrintUIState()
        data class Print(val items: List<TestResultAndCurveModel>) : PrintUIState()
        data class Success(val msg: String = "") : PrintUIState()
        data class Failed(val err: String) : PrintUIState()
    }

    sealed class ExportReportUIState {
        object None : ExportReportUIState()
        data class Export(val item: List<TestResultAndCurveModel>) : ExportReportUIState()
        data class Success(val msg: String = "") : ExportReportUIState()
        data class Failed(val err: String) : ExportReportUIState()
    }

    sealed class UploadUIState {
        object None : UploadUIState()
        data class Upload(val items: List<TestResultAndCurveModel>) : UploadUIState()
        data class Success(val msg: String = "") : UploadUIState()
        data class Failed(val err: String) : UploadUIState()
    }


    sealed class PrintReportUIState {
        object None : PrintReportUIState()
        data class PrintReport(
            val params: PrintAnimParams,
            val items: List<TestResultAndCurveModel>
        ) : PrintReportUIState()

        data class Success(val msg: String = "") : PrintReportUIState()
        data class NoSelectedPrinter(val msg: String = "") : PrintReportUIState()
        data class Failed(val err: String) : PrintReportUIState()
    }

    sealed class DeleteResultUIState {
        object None : DeleteResultUIState()
        object Loading : DeleteResultUIState()
        object ShowDialog : DeleteResultUIState()
        data class Success(val msg: String = "") : DeleteResultUIState()
        data class Failed(val err: String) : DeleteResultUIState()
    }

    sealed class ResultDetailsUIState {
        object None : ResultDetailsUIState()
        data class ShowDialog(val item: TestResultAndCurveModel) : ResultDetailsUIState()
        data class Success(val msg: String = "") : ResultDetailsUIState()
        data class Failed(val err: String) : ResultDetailsUIState()
    }

    sealed class ConditionUIState {
        object None : ConditionUIState()
        data class ShowDialog(val condition: ConditionModel) : ConditionUIState()
    }

    sealed class DataManagerIntent {
        object ExportExcelAll : DataManagerIntent()
        data class ExportExcelSelected(val items: List<TestResultAndCurveModel>) :
            DataManagerIntent()

        data class PrintSelected(val items: List<TestResultAndCurveModel>) :
            DataManagerIntent()

        data class PrintReportSelected(
            val params: PrintAnimParams,
            val items: List<TestResultAndCurveModel>
        ) :
            DataManagerIntent()

        data class ExportReportSelected(val items: List<TestResultAndCurveModel>) :
            DataManagerIntent()

        data class Upload(
            val isConnected: Boolean,
            val items: List<TestResultAndCurveModel>
        ) :
            DataManagerIntent()

        data class DeleteResult(val items: List<TestResultAndCurveModel>) :
            DataManagerIntent()

        data class ResultDetailsUpdate(val isDebug: Boolean, val item: TestResultAndCurveModel) :
            DataManagerIntent()

        data class ConditionUpdate(val condition: ConditionModel) :
            DataManagerIntent()

    }
}

class DataManagerViewModelFactory(
    private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
    private val projectRepository: ProjectSource = ServiceLocator.provideProjectSource(App.instance!!),
    private val testResultRepository: TestResultSource = ServiceLocator.provideTestResultSource(App.instance!!)
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DataManagerViewModel::class.java)) {
            return DataManagerViewModel(appViewModel, projectRepository, testResultRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

