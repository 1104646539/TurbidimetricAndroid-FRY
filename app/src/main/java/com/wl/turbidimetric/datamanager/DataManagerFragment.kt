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
import com.wl.turbidimetric.ex.toLongString
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.TestResultModel_
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.util.ExcelUtils
import com.wl.turbidimetric.util.ExportExcelUtil
import com.wl.turbidimetric.view.ConditionDialog
import com.wl.turbidimetric.view.ResultDetailsDialog
import com.wl.wwanandroid.base.BaseFragment
import io.objectbox.kotlin.equal
import io.objectbox.kotlin.greaterOrEqual
import io.objectbox.kotlin.lessOrEqual
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


/**
 * 数据管理
 */
class DataManagerFragment :
    BaseFragment<DataManagerViewModel, FragmentDataManagerBinding>(R.layout.fragment_data_manager) {
    init {
        Timber.d("init create")
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun init(savedInstanceState: Bundle?) {
        Timber.d("init")
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
                    testTime = Date().toLongString()
                )
                add(dr)
            }
        }
    }


    private fun listener() {
        test()
        //        vd.btnInsert.setOnClickListener {

//        }
//
//        vd.btnQuery.setOnClickListener {
//
//            Log.d(TAG, "size1= ${adapter.snapshot().items.size} ")
//        }
//
//        vd.btnChange.setOnClickListener {
//            val row = vd.etRow.text.toString().toInt()
//            val drw = adapter.snapshot().items[row].copy()
//            drw.testResult = "修改过"
//            drw.project.target?.projectName += "修改过"
//            drw.project.target?.let {
//                DBManager.projectBox.put(it)
//            }
//            val result = viewModel.update(drw)
//            Log.d(TAG, "row= ${row} result=${result}")
//        }

        vd.btnDelete.setOnClickListener {
//            val row = vd.etRow.text.toString().toInt()
//            val result = viewModel.remove(adapter.snapshot().items[row])
//            Log.d(TAG, "row= ${row} result=${result}")
//            val result = adapter.snapshot().items[5]
            val results = adapter.snapshot().filter { it?.isSelect ?: false }
            if (!results.isNullOrEmpty()) DBManager.TestResultBox.remove(results)
        }
        vd.btnInsert.setOnClickListener {
//            DBManager.TestResultBox.put(TestResultModel(detectionNum = LocalData.getDetectionNumInc()))

        }
        vd.btnInsert2.setOnClickListener {
//            lifecycleScope.launch {
//                val testDatas = createTestData()
//                val fileName: String =
//                    "/sdcard/" + "simpleWrite2 " + System.currentTimeMillis() + ".xlsx"
//                Timber.d("starttime = ${Date()}")
//                ExcelUtils.initExcel(fileName, arrayOf())
//                ExcelUtils.writeObjListToExcel(testDatas, fileName);
//                Timber.d("endtime = ${Date()}")
//            }

//            lifecycleScope.launch {
//                val list = mutableListOf<TestResultModel>()
//                repeat(300) {
//                    list.add(TestResultModel(detectionNum = LocalData.getDetectionNumInc()))
//                }
//                DBManager.TestResultBox.put(list)
//            }
        }
        vd.btnCondition.setOnClickListener {
            showConditionDialog()
        }
        vd.btnClean.setOnClickListener {
            DBManager.TestResultBox.removeAll()
        }
        vd.btnExportExcel.setOnClickListener {
            exportExcel()
        }

//        adapter.stateRestorationPolicy =
//            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

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
            val results = adapter.snapshot().filter { it?.isSelect ?: false }
            PrintUtil.printTest(results)
        }

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
            getSelectData()?.let {
                ExportExcelUtil.printExcel(it, { toast("成功$it") }, { toast("$it") })
            }
        }
    }

    private fun getSelectData(): List<TestResultModel>? {
        return adapter.snapshot().filter { it?.isSelect ?: false }.map { it!! }
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
            Timber.d("conditionModel=$conditionModel")

        }, {
            conditionDialog.dismiss()
        }, true)

    }

    var datasJob: Job? = null
    private suspend fun queryData(condition: Query<TestResultModel>) {
        datasJob?.cancelAndJoin()
        datasJob = lifecycleScope.launch {
            vm.item(condition).collectLatest {
                Timber.d("---监听到了变化---condition=$condition")
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
            condition.equal(TestResultModel_.name, name, QueryBuilder.StringOrder.CASE_INSENSITIVE)
        }
        if (qrcode.isNotEmpty()) {
            condition.equal(
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
