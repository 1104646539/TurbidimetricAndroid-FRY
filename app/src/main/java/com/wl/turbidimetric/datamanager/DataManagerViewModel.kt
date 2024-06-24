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
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.main.MainActivity.PrintAnimParams
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.repository.if2.ProjectSource
import com.wl.turbidimetric.repository.if2.TestResultSource
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.util.ExportExcelHelper
import com.wl.turbidimetric.util.ExportReportHelper
import com.wl.turbidimetric.util.PrintHelper
import com.wl.turbidimetric.util.PrintSDKHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus


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
    private val _exportReportSelectedUiState =
        MutableStateFlow<ExportReportSelectedUIState>(ExportReportSelectedUIState.None)
    val exportReportSelectedUIState = _exportReportSelectedUiState.asStateFlow()
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

            is DataManagerIntent.UploadSelected -> {
                upload(intent.items)
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

    private fun upload(items: List<TestResultAndCurveModel>) {
        viewModelScope.launch {
            _uploadUIState.emit(UploadUIState.Loading)
            dataUploadVerify(items).also { verifyRet ->
                if (verifyRet.isEmpty()) {
                    if (HL7Helper.isConnected()) {
                        HL7Helper.uploadTestResult(items) { count, success, failed ->
                            launch {
                                _uploadUIState.emit(UploadUIState.Success("上传结束，本次上传共${count}条，成功${success}条,失败${failed}条"))
                            }
                        }
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
            _exportReportSelectedUiState.emit(ExportReportSelectedUIState.Loading)
            dataEmptyVerify(items).also { verifyRet ->
                if (verifyRet.isEmpty()) {
                    ExportReportHelper.exportReport(
                        appViewModel.getApplication(),
                        items,
                        appViewModel.getHospitalName(),
                        viewModelScope,
                        false,
                        { count, successCount, failedCount ->
                            launch {
                                _exportReportSelectedUiState.emit(
                                    ExportReportSelectedUIState.Success(
                                        "导出报告完成，本次导出总数${count}条,成功${successCount}条,失败${failedCount}条"
                                    )
                                )
                            }
                        }, { err ->
                            launch {
                                _exportReportSelectedUiState.emit(
                                    ExportReportSelectedUIState.Success(
                                        "导出报告失败 $err"
                                    )
                                )
                            }
                        }
                    )
                } else {
                    _exportReportSelectedUiState.emit(ExportReportSelectedUIState.Failed(verifyRet))
                }
            }
        }
    }

    private fun printReport(params: PrintAnimParams, items: List<TestResultAndCurveModel>) {
        viewModelScope.launch {
            _printReportUIState.emit(PrintReportUIState.Loading)
            dataEmptyVerify(items).also { verifyRet ->
                if (verifyRet.isEmpty()) {
                    when (PrintSDKHelper.printerState) {
                        PrinterState.Success -> {
                            PrintHelper.addPrintWork(items, appViewModel.getHospitalName(), false)
                            EventBus.getDefault()
                                .post(EventMsg(EventGlobal.WHAT_HOME_ADD_PRINT_ANIM, params))
                            _printReportUIState.emit(PrintReportUIState.Success("添加到打印队列成功"))
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
            //step1、显示加载框
            _exportExcelUIState.emit(ExportExcelUIState.Loading)
            //step2、 获取数据
            val data = if (exportAll) {
                val condition = conditionModel.value
                getFilterAll(condition)
            } else {
                items
            }
            //step3、 验证 导出 等待结果
            val err = exportExcelVerify(data)
            if (err.isEmpty()) {
                ExportExcelHelper.export(
                    appViewModel.getApplication(),
                    data,
                    { msg ->
                        launch(Dispatchers.Main) {
                            _exportExcelUIState.emit(ExportExcelUIState.Success("导出成功,文件保存在 $msg"))
                        }
                    },
                    { error ->
                        launch(Dispatchers.Main) {
                            _exportExcelUIState.emit(ExportExcelUIState.Failed("导出失败,$error"))
                        }
                    })
            } else {
                _exportExcelUIState.emit(ExportExcelUIState.Failed("导出失败,$err"))
            }
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
            _printUIState.emit(PrintUIState.Loading)
            dataEmptyVerify(items).also { ret ->
                if (ret.isEmpty()) {
                    PrintUtil.printTest(items)
                    _printUIState.emit(PrintUIState.Success())
                } else {
                    _printUIState.emit(PrintUIState.Failed(ret))
                }
            }

        }
    }


    sealed class ExportExcelUIState {
        object None : ExportExcelUIState()
        object Loading : ExportExcelUIState()
        data class Success(val msg: String) : ExportExcelUIState()
        data class Failed(val err: String) : ExportExcelUIState()
    }


    sealed class PrintUIState {
        object None : PrintUIState()
        object Loading : PrintUIState()
        data class Success(val msg: String = "") : PrintUIState()
        data class Failed(val err: String) : PrintUIState()
    }

    sealed class ExportReportSelectedUIState {
        object None : ExportReportSelectedUIState()
        object Loading : ExportReportSelectedUIState()
        data class Success(val msg: String = "") : ExportReportSelectedUIState()
        data class Failed(val err: String) : ExportReportSelectedUIState()
    }

    sealed class UploadUIState {
        object None : UploadUIState()
        object Loading : UploadUIState()
        data class Success(val msg: String = "") : UploadUIState()
        data class Failed(val err: String) : UploadUIState()
    }


    sealed class PrintReportUIState {
        object None : PrintReportUIState()
        object Loading : PrintReportUIState()
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

        data class UploadSelected(val items: List<TestResultAndCurveModel>) :
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

