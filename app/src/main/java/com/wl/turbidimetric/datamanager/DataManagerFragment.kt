package com.wl.turbidimetric.datamanager

import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentDataManagerBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.PD
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.util.ExportExcelHelper
import com.wl.turbidimetric.view.dialog.*
import com.wl.wllib.LogToFile.i
import com.wl.wllib.LogToFile.u
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*


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
    val conditionDialog by lazy {
        ConditionDialog(requireContext())
    }

    /**
     * 删除对话框
     */
    val deleteDialog by lazy {
        HiltDialog(requireContext())
    }

    /**
     * 等待任务对话框
     */
    val waitDialog by lazy {
        HiltDialog(requireContext())
    }

    override fun init(savedInstanceState: Bundle?) {
        initView()
        listener()
//        test()
    }

    private fun initView() {
        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
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
        lifecycleScope.launch {
            val testdatas = createTestData()
            vm.add(testdatas)
        }
    }

    private fun createPrintData(): List<List<String>> {
        return createTestData().map {
            mutableListOf<String>().apply {
                add("${it.concentration}")
                add("${it.testResult}")
                add("${it.name}")
                add("${it.gender}")
                add("${it.age}")
                add("${it.detectionNum}")
                add("${it.testOriginalValue1}")
                add("${it.testOriginalValue2}")
                add("${it.testOriginalValue3}")
                add("${it.testOriginalValue4}")
                add("${it.testValue1}")
                add("${it.testValue2}")
                add("${it.testValue3}")
                add("${it.testValue4}")
                add("${it.testTime}")
                add("${it.absorbances}")
            }
        }
    }

    private fun createTestData(): List<TestResultModel> {
        return mutableListOf<TestResultModel>().apply {
            for (i in 0..100) {
                val dr = TestResultModel(
                    testResult = (i % 2 == 0).PD("阳性", "阴性"),
                    concentration = 66 + i,
                    absorbances = "121120".toBigDecimal(),
                    name = (i % 2 == 0).PD("张三", "李四"),
                    gender = (i % 2 == 0).PD("男", "女"),
                    age = (i % 90).toString(),
                    detectionNum = LocalData.getDetectionNumInc(),
                    testOriginalValue1 = 52111,
                    testOriginalValue2 = 52112,
                    testOriginalValue3 = 52113,
                    testOriginalValue4 = 52114,
                    testValue1 = "52.31".toBigDecimal(),
                    testValue2 = "52.32".toBigDecimal(),
                    testValue3 = "52.33".toBigDecimal(),
                    testValue4 = "52.34".toBigDecimal(),
                    testTime = Date().time,
                    deliveryTime = "",
                    deliveryDepartment = "",
                    deliveryDoctor = "",
                    sampleBarcode = "ABCD$i",
                    curveOwnerId = 0
                )
                add(dr)
            }
        }
    }


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
            exportExcelSelected()
        }
        vd.btnExportExcelAll.setOnClickListener {
            u("导出Excel全部")
            exportExcelAll()
        }
        vd.btnUpload.setOnClickListener {
            upload()
        }
        lifecycleScope.launch {
            vm.resultSize.collectLatest {
                vd.tvCount.text = "(${it}条)"
            }
        }

        adapter.onLongClick = { id ->
            u("详情$id")
            if (id > 0) {
                lifecycleScope.launch {
                    val result = vm.getTestResultAndCurveModelById(id)
                    result?.let {
                        resultDialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(result) {
                                lifecycleScope.launch {
                                    vm.update(it)
                                }
                                true
                            }
                        }
                    }
                }
            } else {
                toast("ID错误")
            }
        }

        vd.btnPrint.setOnClickListener {
            u("打印")
            print()
        }
        adapter.onSelectChange = { pos, selected ->

        }
    }

    private fun getBacklog2() {

    }

    private fun getBacklog() {

    }

    /**
     * 上传
     */
    private fun upload() {
        val results = getSelectData()
        var verifyRet: String? = null
        //验证上传数据
        if (verifyUploadData(results).also { verifyRet = it } != null) {
            toast("$verifyRet")
            return
        }

        //批量上传
        waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
            dialog.showDialog("请等待……", confirmText = "", confirmClick = {})

            lifecycleScope.launch(Dispatchers.IO) {
                if (HL7Helper.isConnected()) {
                    HL7Helper.uploadTestResult(results) { count, success, failed ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            dialog.showDialog(
                                "上传结束，本次上传共${count}条，成功${success}条,失败${failed}条",
                                confirmText = "我知道了",
                                confirmClick = { d ->
                                    d.dismiss()
                                })
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        dialog.dismiss()
                    }
                    i("上传未连接")
                }
            }
        }
    }

    /**
     * 验证上传的数据
     * @param results List<TestResultModel>?
     * @return String?
     */
    private fun verifyUploadData(results: List<TestResultAndCurveModel>?): String? {

        if (results.isNullOrEmpty()) {
            return "请选择数据"
        }

        if (appVm.testState.isTestRunning() && SystemGlobal.uploadConfig.autoUpload) {
            return "请等待检测结束后上传"
        }

        if (results.any {
                it.result.curveOwnerId <= 0
            }) {
            return "没有检测项目"
        }

        if (results.any {
                it.result.testResult.isNullOrEmpty()
            }) {
            return "未检测的数据"
        }
        return null
    }

    private fun listenerData() {
        vd.btnDelete.setOnClickListener {
            vm.showDeleteDialog.postValue(true)
        }

        vm.showDeleteDialog.observe(this) {
            if (it) {
                deleteDialog.showPop(requireContext(), isCancelable = false) {
                    it.showDialog("确定要删除数据吗?", "确定", {
                        val results = getSelectData()
                        it.dismiss()
                        if (!results.isNullOrEmpty()) {
                            lifecycleScope.launch {
                                vm.clickDeleteDialogConfirm(results.map { it.result })
                            }
                        }
                    }, "取消", {
                        it.dismiss()
                    }, showIcon = true, iconId = ICON_HINT)
                }
            }
        }

        lifecycleScope.launch {
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
    }

    private fun print() {
        val results = getSelectData()
        if (results.isNullOrEmpty()) {
            toast("请选择数据")
            return
        }

        PrintUtil.printTest(results)
    }

    private fun exportExcelSelected() {
        exportExcel(false)
    }


    private fun exportExcelAll() {
        exportExcel(true)
    }

    /**
     * 导出数据到U盘 （excel格式.xls）
     * @param exportAll Boolean
     * @param dialog HiltDialog
     * @param items List<TestResultModel>?
     */
    private fun exportExcel(exportAll: Boolean) {
        waitDialog.showPop(requireContext()) { dialog ->
            //step1、 显示等待对话框
            dialog.showDialog("正在导出,请等待……", confirmText = "", confirmClick = {})
            lifecycleScope.launch(Dispatchers.IO) {
                //step2、 获取数据
                val data = if (exportAll) {
                    val condition = vm.conditionModel.value
                    vm.getFilterAll(condition)
                } else {
                    getSelectData()
                }
                //step3、 导出 等待结果
                val err = exportExcelVerify(data)
                if (err.isEmpty()) {
                    ExportExcelHelper.export(
                        requireContext(),
                        data,
                        { msg ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                dialog.showDialog("导出成功,文件保存在 $msg", "确定", { d ->
                                    d.dismiss()
                                })
                            }
                        },
                        { it ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                dialog.showDialog("导出失败,$it", "确定", { d ->
                                    d.dismiss()
                                })
                            }
                        })
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        dialog.showDialog("导出失败,$err", "确定", { d ->
                            d.dismiss()
                        })
                    }
                }
            }
        }
    }

    /**
     * 验证
     */
    private fun exportExcelVerify(items: List<TestResultAndCurveModel>?): String {
        return if (items.isNullOrEmpty()) {
            "请选择数据"
        } else if (items.size > 4000) {
            "一次操作不能大于4000条，当前操作了${items.size}条"
        } else {
            ""
        }
    }


    private fun getSelectData(): List<TestResultAndCurveModel> {
        return adapter.getSelectedItems()
    }

    /**
     * 显示筛选对话框
     */
    private fun showConditionDialog() {
        conditionDialog.showPop(requireContext(), isCancelable = false) {
            it.showDialog({ conditionModel ->
                lifecycleScope.launch {
                    vm.conditionChange(conditionModel)
                    it.dismiss()
                }
                i("conditionModel=$conditionModel")
            }, {
                it.dismiss()
            })
        }
    }

    var datasJob: Job? = null
    private suspend fun queryData(condition: ConditionModel) {
        datasJob?.cancelAndJoin()
        datasJob = lifecycleScope.launch {
            vm.item(condition).collectLatest {
                i("---监听到了变化---condition=$condition")
                vm.conditionChange(condition)
                adapter.submitData(it)
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

    val TAG = "DataManagerFragment"
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
    }

    override fun initViewModel() {
    }


}
