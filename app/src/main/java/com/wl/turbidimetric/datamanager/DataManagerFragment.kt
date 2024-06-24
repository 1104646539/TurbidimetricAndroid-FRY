package com.wl.turbidimetric.datamanager

import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentDataManagerBinding
import com.wl.turbidimetric.ex.calcShowTestResult
import com.wl.turbidimetric.ex.getPrintParamsAnim
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.util.PrintSDKHelper
import com.wl.turbidimetric.view.dialog.*
import com.wl.wllib.LogToFile.i
import com.wl.wllib.LogToFile.u
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest


/**
 * 数据管理
 */
class DataManagerFragment :
    BaseFragment<DataManagerViewModel, FragmentDataManagerBinding>(R.layout.fragment_data_manager) {

    init {
        i("init create")
    }


    override val vm: DataManagerViewModel by viewModels {
        DataManagerViewModelFactory()
    }

    val adapter: DataManagerAdapter by lazy {
        DataManagerAdapter()
    }

    /**
     * 筛选对话框
     */
    private val conditionDialog by lazy {
        ConditionDialog(requireContext())
    }

    /**
     * 等待任务对话框
     */
    private val waitDialog by lazy {
        HiltDialog(requireContext())
    }
    private var datasJob: Job? = null
    override fun init(savedInstanceState: Bundle?) {
        initView()
        listener()
//        test()
    }

    private fun initView() {
        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.itemAnimator = DefaultItemAnimator()
        val itemDividerMode = DividerItemDecoration(
            requireContext(), android.widget.LinearLayout.VERTICAL
        ).apply { setDrawable(resources.getDrawable(R.drawable.item_hori_divider)!!) }
        vd.rv.addItemDecoration(
            itemDividerMode
        )

        val loadStateAdapter = adapter.withLoadStateFooter(DataManagerLoadStateAdapter())
        vd.rv.adapter = loadStateAdapter
    }

    fun test() {
//        lifecycleScope.launch {
//            val testdatas = createTestData()
//            vm.add(testdatas)
//        }
    }

//    private fun createPrintData(): List<List<String>> {
//        return createTestData().map {
//            mutableListOf<String>().apply {
//                add("${it.concentration}")
//                add("${it.testResult}")
//                add("${it.name}")
//                add("${it.gender}")
//                add("${it.age}")
//                add("${it.detectionNum}")
//                add("${it.testOriginalValue1}")
//                add("${it.testOriginalValue2}")
//                add("${it.testOriginalValue3}")
//                add("${it.testOriginalValue4}")
//                add("${it.testValue1}")
//                add("${it.testValue2}")
//                add("${it.testValue3}")
//                add("${it.testValue4}")
//                add("${it.testTime}")
//                add("${it.absorbances}")
//            }
//        }
//    }
//
//    private fun createTestData(): List<TestResultModel> {
//        return mutableListOf<TestResultModel>().apply {
//            for (i in 0..100) {
//                val dr = TestResultModel(
//                    testResult = (i % 2 == 0).PD("阳性", "阴性"),
//                    concentration = 66 + i,
//                    absorbances = "121120".toBigDecimal(),
//                    name = (i % 2 == 0).PD("张三", "李四"),
//                    gender = (i % 2 == 0).PD("男", "女"),
//                    age = (i % 90).toString(),
//                    detectionNum = LocalData.getDetectionNumInc(),
//                    testOriginalValue1 = 52111,
//                    testOriginalValue2 = 52112,
//                    testOriginalValue3 = 52113,
//                    testOriginalValue4 = 52114,
//                    testValue1 = "52.31".toBigDecimal(),
//                    testValue2 = "52.32".toBigDecimal(),
//                    testValue3 = "52.33".toBigDecimal(),
//                    testValue4 = "52.34".toBigDecimal(),
//                    testTime = Date().time,
//                    deliveryTime = "",
//                    deliveryDepartment = "",
//                    deliveryDoctor = "",
//                    sampleBarcode = "ABCD$i",
//                    curveOwnerId = 0
//                )
//                add(dr)
//            }
//        }
//    }


    private fun listener() {
        listenerView()
        listenerData()
    }

    private fun listenerView() {

        vd.btnCondition.setOnClickListener {
            u("数据筛选")
            showConditionDialog()
        }

        vd.btnExportExcel.setOnClickListener {
            u("导出Excel")
            exportExcelAll()
        }
        vd.btnUpload.setOnClickListener {
            u("上传")
            upload()
        }
        lifecycleScope.launch {
            vm.resultSize.collectLatest {
                vd.tvCount.text = "(${it}条)"
            }
        }

        adapter.onLongClick = { id ->
            u("详情$id")
            vm.showResultDetailsDialog(id)
        }

        vd.btnPrint.setOnClickListener {
            u("打印")
            print()
        }
        vd.btnDelete.setOnClickListener {
            u("删除")
            delete()
        }
        vd.btnExportPdf.setOnClickListener {
            u("导出报告")
            exportReport()
        }
        vd.btnPrintPdf.setOnClickListener {
            u("打印报告")
            printReport()
        }

    }


    private fun printReport() {
        val data = getSelectData()
        vm.processIntent(
            DataManagerViewModel.DataManagerIntent.PrintReportSelected(
                vd.btnPrintPdf.getPrintParamsAnim(),
                data
            )
        )
    }

    private fun exportReport() {
        val data = getSelectData()
        vm.processIntent(DataManagerViewModel.DataManagerIntent.ExportExcelSelected(data))
    }

    private fun delete() {
        vm.deleteResult()
    }


    /**
     * 上传
     */
    private fun upload() {
        val results = getSelectData()
        vm.processIntent(DataManagerViewModel.DataManagerIntent.UploadSelected(results))
    }


    private fun listenerData() {

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { loadState ->
                if (loadState.source.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && adapter.itemCount < 1) {
                    vd.rv.isVisible = false
                    vd.empty.isVisible = true
                } else {
                    vd.rv.isVisible = true
                    vd.empty.isVisible = false
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            vm.conditionModel.collectLatest {
                queryData(it)
            }
        }
        lifecycleScope.launchWhenCreated {
            SystemGlobal.obDebugMode.collectLatest {
                vd.btnDelete.visibility = it.isShow()
            }
        }

        lifecycleScope.launchWhenCreated {
            launch {
                vm.exportExcelUIState.collectLatest {
                    when (it) {
                        is DataManagerViewModel.ExportExcelUIState.Failed -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.err, "确定", { d ->
                                    d.dismiss()
                                })
                            }
                        }

                        DataManagerViewModel.ExportExcelUIState.Loading -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(
                                    "正在导出,请等待……",
                                    confirmText = "",
                                    confirmClick = {})
                            }
                        }

                        DataManagerViewModel.ExportExcelUIState.None -> {

                        }

                        is DataManagerViewModel.ExportExcelUIState.Success -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.msg, "确定", { d ->
                                    d.dismiss()
                                })
                            }
                        }
                    }
                }
            }
            launch {
                vm.printUIState.collectLatest {
                    when (it) {
                        is DataManagerViewModel.PrintUIState.Failed -> {
                            waitDialog.showPop(requireContext()) { dialog ->
                                dialog.showDialog(it.err, "确定", { dialog ->
                                    dialog.dismiss()
                                })
                            }
                        }

                        DataManagerViewModel.PrintUIState.Loading -> {}
                        DataManagerViewModel.PrintUIState.None -> {}
                        is DataManagerViewModel.PrintUIState.Success -> {

                        }
                    }
                }
            }
            launch {
                vm.printReportUIState.collectLatest {
                    when (it) {
                        is DataManagerViewModel.PrintReportUIState.Failed -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.err, "确定", { dialog.dismiss() })
                            }
                        }

                        DataManagerViewModel.PrintReportUIState.Loading -> {}
                        is DataManagerViewModel.PrintReportUIState.NoSelectedPrinter -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.msg, "选择打印机", {
                                    PrintSDKHelper.showSetupPrinterUi()
                                    dialog.dismiss()
                                }, "取消", { dialog.dismiss() })
                            }
                        }

                        DataManagerViewModel.PrintReportUIState.None -> {}
                        is DataManagerViewModel.PrintReportUIState.Success -> {

                        }
                    }
                }
            }
            launch {
                vm.exportReportSelectedUIState.collectLatest {
                    when (it) {
                        is DataManagerViewModel.ExportReportSelectedUIState.Failed -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.err, "确定", { dialog.dismiss() })
                            }
                        }

                        DataManagerViewModel.ExportReportSelectedUIState.Loading -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog("正在导出报告，请稍后……")
                            }
                        }

                        DataManagerViewModel.ExportReportSelectedUIState.None -> {

                        }

                        is DataManagerViewModel.ExportReportSelectedUIState.Success -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.msg)
                            }
                        }
                    }
                }
            }

            launch {
                vm.uploadUIState.collectLatest {
                    when (it) {
                        is DataManagerViewModel.UploadUIState.Failed -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.err, "确定", { dialog.dismiss() })
                            }
                        }

                        DataManagerViewModel.UploadUIState.Loading -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog("正在上传，请稍后……")
                            }
                        }

                        DataManagerViewModel.UploadUIState.None -> {}
                        is DataManagerViewModel.UploadUIState.Success -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.msg, "确定", { dialog.dismiss() })
                            }
                        }
                    }
                }
            }

            launch {
                vm.deleteResultUIState.collectLatest {
                    when (it) {
                        is DataManagerViewModel.DeleteResultUIState.Failed -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.err, "确定", { dialog.dismiss() })
                            }
                        }

                        DataManagerViewModel.DeleteResultUIState.Loading -> {}
                        DataManagerViewModel.DeleteResultUIState.None -> {}
                        DataManagerViewModel.DeleteResultUIState.ShowDialog -> {
                            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(
                                    "确定要删除数据吗？删除后不可恢复！",
                                    "确定",
                                    {
                                        vm.processIntent(
                                            DataManagerViewModel.DataManagerIntent.DeleteResult(
                                                getSelectData()
                                            )
                                        )
                                        dialog.dismiss()
                                    }, "取消", { dialog.dismiss() })
                            }
                        }

                        is DataManagerViewModel.DeleteResultUIState.Success -> {
                            if (waitDialog != null && waitDialog.isShow) {
                                waitDialog.dismiss()
                            }
                        }
                    }
                }
            }
            launch {
                vm.resultDetailsUIState.collectLatest {
                    when (it) {
                        is DataManagerViewModel.ResultDetailsUIState.Failed -> {
                            i("${it.err}")
                        }

                        DataManagerViewModel.ResultDetailsUIState.None -> {}
                        is DataManagerViewModel.ResultDetailsUIState.ShowDialog -> {
                            resultDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                                dialog.showDialog(it.item, SystemGlobal.isDebugMode) { result ->
                                    vm.processIntent(
                                        DataManagerViewModel.DataManagerIntent.ResultDetailsUpdate(
                                            SystemGlobal.isDebugMode,
                                            result
                                        )
                                    )
                                    true
                                }
                            }
                        }

                        is DataManagerViewModel.ResultDetailsUIState.Success -> {
                            i("${it.msg}")
                        }
                    }
                }
            }
            launch {
                vm.conditionUIState.collectLatest {
                    when (it) {
                        DataManagerViewModel.ConditionUIState.None -> {

                        }

                        is DataManagerViewModel.ConditionUIState.ShowDialog -> {
                            conditionDialog.showPop(
                                requireContext(),
                                isCancelable = false
                            ) { dialog ->
                                dialog.showDialog({ conditionModel ->
                                    vm.processIntent(
                                        DataManagerViewModel.DataManagerIntent.ConditionUpdate(
                                            conditionModel
                                        )
                                    )
                                    dialog.dismiss()
                                    i("conditionModel=$conditionModel")
                                }, {
                                    dialog.dismiss()
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 打印热敏
     */
    private fun print() {
        val results = getSelectData()
        vm.processIntent(DataManagerViewModel.DataManagerIntent.PrintSelected(results))
    }


    /**
     * 导出数据到U盘 （excel格式.xls）
     * 导出的为选中的
     */
    private fun exportExcelSelected() {
        vm.processIntent(
            DataManagerViewModel.DataManagerIntent.ExportExcelSelected(
                getSelectData()
            )
        )
    }

    /**
     * 导出数据到U盘 （excel格式.xls）
     * 导出的为全部筛选的
     */
    private fun exportExcelAll() {
        vm.processIntent(DataManagerViewModel.DataManagerIntent.ExportExcelAll)
    }


    private fun getSelectData(): List<TestResultAndCurveModel> {
        return adapter.getSelectedItems()
    }

    /**
     * 显示筛选对话框
     */
    private fun showConditionDialog() {
        vm.showConditionDialog()
    }


    private suspend fun queryData(condition: ConditionModel) {
        datasJob?.cancelAndJoin()
        datasJob = lifecycleScope.launch {
            vm.item(condition).collectLatest {
                i("---监听到了变化---condition=$condition")
                vm.conditionChange(condition)
                adapter.submitData(lifecycle, it)
                //刷新后移动到顶部，只在没有移动的时候移动
                withContext(Dispatchers.Main) {
                    if (vd.rv.scrollState == RecyclerView.SCROLL_STATE_IDLE)
                        vd.rv.scrollToPosition(0)
                }
            }
        }
    }


    private val resultDialog by lazy {
        ResultDetailsDialog(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        Log.d(TAG, "hidden=$hidden｝")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }


    companion object {
        @JvmStatic
        fun newInstance() = DataManagerFragment()
        private const val TAG = "DataManagerFragment"
    }

    override fun initViewModel() {
    }


}
