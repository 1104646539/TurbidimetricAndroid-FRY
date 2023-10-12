package com.wl.turbidimetric.datamanager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentDataManagerBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.ex.PD
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.TestResultModel_
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.util.ExportExcelHelper
import com.wl.turbidimetric.view.ConditionDialog
import com.wl.turbidimetric.view.HiltDialog
import com.wl.turbidimetric.view.ResultDetailsDialog
import com.wl.wllib.LogToFile.i
import com.wl.wwanandroid.base.BaseFragment
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        i("onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun init(savedInstanceState: Bundle?) {
        i("init")
        listener()
        initView()


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
        lifecycleScope.launch {
            val con: Query<TestResultModel> = DBManager.TestResultBox.query().orderDesc(
                TestResultModel_.id
            ).build()
            queryData(con)
        }
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadState ->
                if (loadState.source.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && adapter.itemCount < 1) {
                    vd.rv?.isVisible = false
                    vd.empty?.isVisible = true
                } else {
                    vd.rv?.isVisible = true
                    vd.empty?.isVisible = false
                }
            }
        }
        adapter.onSelectChange = { pos, selected ->

        }
    }

    fun test() {
//        val testdatas = createTestData()
//        DBManager.TestResultBox.put(testdatas)
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
                    gender = "",
                    age = "",
                    detectionNum = LocalData.getDetectionNumInc(),
                    testOriginalValue1 = 52111,
                    testOriginalValue2 = 52112,
                    testOriginalValue3 = 52113,
                    testOriginalValue4 = 52114,
                    testValue1 = "52.31".toBigDecimal(),
                    testValue2 = "52.32".toBigDecimal(),
                    testValue3 = "52.33".toBigDecimal(),
                    testValue4 = "52.34".toBigDecimal(),
                    testTime = Date().time
                )
                add(dr)
            }
        }
    }


    private fun listener() {
        test()


        vd.btnDelete.setOnClickListener {
            vm.showDeleteDialog.postValue(true)
        }

        vm.showDeleteDialog.observe(this) {
            if (it) {
                deleteDialog.show("确定要删除数据吗?", "确定", {
                    val results = getSelectData()
                    it.dismiss()
                    if (!results.isNullOrEmpty()) {
                        vm.clickDeleteDialogConfirm(results)
                    }
                }, "取消", {
                    it.dismiss()
                }, false)
            }
        }
        vd.btnInsert.setOnClickListener {

        }

        vd.btnCondition.setOnClickListener {
            showConditionDialog()
        }

        vd.btnExportExcel.setOnClickListener {
            exportExcel()
        }

        adapter.onLongClick = { id ->
            if (id > 0) {
                val result = DBManager.TestResultBox.get(id)
                result?.let {
                    resultDialog.show(result) {
                        vm.update(it)
                        true
                    }
                }
            } else {
                toast("ID错误")
            }
        }

        vd.btnPrint.setOnClickListener {
            print()
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

    /**
     * 验证 导出数据到Excel
     */
    private fun exportExcelVerify() {
        val selectData = getSelectData()
        if (selectData.isNullOrEmpty()) {
            toast("请选择数据")
            return
        }
        selectData?.let {
            exportExcel()
        }
    }

    /**
     * 导出数据到Excel
     */
    private fun exportExcel() {
        lifecycleScope.launch {
            getSelectData()?.let { it ->
                ExportExcelHelper.export(requireContext(),it, { toast("成功$it") }, { toast(it) })
            }
        }
    }

    private fun getSelectData(): List<TestResultModel>? {
//        val all = adapter.snapshot().map { it!! }
//        val select = all.filter { it?.isSelect ?: false }
//
//        i("all=${all.size} select=${select.size}")
//        return select

        return adapter.getSelectedItems()
    }

    /**
     * 显示筛选对话框
     */
    private fun showConditionDialog() {
        conditionDialog.show({ conditionModel ->
            lifecycleScope.launch {
                queryData(conditionModel.buildQuery())
            }
            conditionDialog.dismiss()
            i("conditionModel=$conditionModel")

        }, {
            conditionDialog.dismiss()
        }, true)

    }

    var datasJob: Job? = null
    private suspend fun queryData(condition: Query<TestResultModel>) {
        datasJob?.cancelAndJoin()
        datasJob = lifecycleScope.launch {
            vm.item(condition).collectLatest {
                i("---监听到了变化---condition=$condition")
                adapter?.submitData(it)

                withContext(Dispatchers.Main) {
                    vd.rv.scrollToPosition(0)
                }
            }
        }
    }

    private fun ConditionModel.buildQuery(): Query<TestResultModel> {
        val condition: QueryBuilder<TestResultModel> = DBManager.TestResultBox.query().orderDesc(
            TestResultModel_.id
        )

        if (name.isNotEmpty()) {
            condition.contains(
                TestResultModel_.name,
                name,
                QueryBuilder.StringOrder.CASE_INSENSITIVE
            )
        }
        if (qrcode.isNotEmpty()) {
            condition.contains(
                TestResultModel_.sampleQRCode,
                qrcode,
                QueryBuilder.StringOrder.CASE_INSENSITIVE
            )
        }
        if (conMin != 0) {
            condition.greaterOrEqual(TestResultModel_.concentration, conMin.toLong())
        }
        if (conMax != 0) {
            condition.lessOrEqual(TestResultModel_.concentration, conMax.toLong())
        }
        if (testTimeMin != 0L) {
            condition.greaterOrEqual(TestResultModel_.testTime, testTimeMin)
        }
        if (testTimeMax != 0L) {
            condition.lessOrEqual(TestResultModel_.testTime, testTimeMax)
        }


        if (results.isNotEmpty()) {
            condition.`in`(
                TestResultModel_.testResult,
                results,
                QueryBuilder.StringOrder.CASE_INSENSITIVE
            )
        }

        return condition.build()
    }

    private val resultDialog by lazy {
        ResultDetailsDialog(requireContext())
    }

    val TAG = "DataManagerFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate");
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView");
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        Log.d(TAG, "hidden=$hidden｝");
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy");
    }


    companion object {
        @JvmStatic
        fun newInstance() = DataManagerFragment()
    }

    override fun initViewModel() {
    }


}
