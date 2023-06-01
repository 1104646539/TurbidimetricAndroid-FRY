package com.wl.turbidimetric.datamanager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentDataManagerBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.TestResultModel_
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.view.ResultDetailsDialog
import com.wl.wwanandroid.base.BaseFragment
import io.objectbox.query.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
    }

    private fun listener() {
        //        vd.btnInsert.setOnClickListener {
//            for (i in 0..1000) {
//                val project = ProjectModel(
//                    projectName = "便潜血",
//                    projectCode = "FOB2",
//                    projectLjz = "52",
//                    projectUnit = "ml"
//                )
//                val dr = TestResultModel(
//                    testResult = "阴性",
//                    concentration = "20",
//                    name = "张三${i}",
//                    gender = "男",
//                    age = "52"
//                )
//                dr.project.target = project
//                val id = viewModel.add(dr)
//                Log.d(TAG, "id=${id} dr=${dr}")
//            }
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
            DBManager.TestResultBox.put(TestResultModel(detectionNum = LocalData.getDetectionNumInc()))
        }
        vd.btnInsert2.setOnClickListener {
            lifecycleScope.launch {
                val list = mutableListOf<TestResultModel>()
                repeat(300) {
                    list.add(TestResultModel(detectionNum = LocalData.getDetectionNumInc()))
                }
                DBManager.TestResultBox.put(list)
            }
        }

        vd.btnClean.setOnClickListener {
            DBManager.TestResultBox.removeAll()
        }
        vd.btnQuery.setOnClickListener {
            lifecycleScope.launch {
                val condition: Query<TestResultModel> = DBManager.TestResultBox.query().order(
                    TestResultModel_.id
                ).build()
                queryData(condition)
            }
        }
        vd.btnQuery2.setOnClickListener {
            lifecycleScope.launch {
                val condition: Query<TestResultModel> = DBManager.TestResultBox.query().orderDesc(
                    TestResultModel_.id
                ).build()
                queryData(condition)
            }
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

    var datasJob: Job? = null
    private suspend fun queryData(condition: Query<TestResultModel>) {
        datasJob?.cancelAndJoin()
        datasJob = lifecycleScope.launch {
            vm.item(condition).collectLatest {
                Timber.d("---监听到了变化---condition=$condition")
                adapter?.submitData(it)
//                withContext(Dispatchers.Main) {
//                    vd.rv.scrollToPosition(0)
//                }
            }
        }
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
