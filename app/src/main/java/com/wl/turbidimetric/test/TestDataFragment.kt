package com.wl.turbidimetric.test

import android.os.Bundle
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.QuickAdapterHelper
import com.chad.library.adapter.base.loadState.LoadState
import com.chad.library.adapter.base.loadState.trailing.TrailingLoadStateAdapter
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentTestDataBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.TestResultModel_
import com.wl.turbidimetric.view.ResultDetailsDialog
import com.wl.wwanandroid.base.BaseFragment
import kotlinx.coroutines.launch

class TestDataFragment :
    BaseFragment<TestDataViewModel, FragmentTestDataBinding>(R.layout.fragment_test_data) {
    override val vm: TestDataViewModel by viewModels()
    val adapter: TestDataAdapter by lazy {
        TestDataAdapter()
    }

    override fun initViewModel() {
    }

    override fun init(savedInstanceState: Bundle?) {
        vd.btnDelete.setOnClickListener {
            val result = adapter.items[5]
            DBManager.TestResultBox.remove(result)
        }
        vd.btnInsert.setOnClickListener {
            val re = TestResultModel(detectionNum = LocalData.getDetectionNumInc())
            DBManager.TestResultBox.put(re)
            adapter.add(0, re)
//            adapter.notifyItemInserted(0)
        }
        vd.btnInsert2.setOnClickListener {
            lifecycleScope.launch {
                val list = mutableListOf<TestResultModel>()
                repeat(1000) {
                    list.add(TestResultModel(detectionNum = LocalData.getDetectionNumInc()))
                }
                DBManager.TestResultBox.put(list)
                adapter.addAll(0, list)
//                adapter.notifyItemRangeInserted(0, list.size)
            }
        }

        vd.btnClean.setOnClickListener {
//            DBManager.TestResultBox.removeAll()
        }

        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val itemDividerMode = DividerItemDecoration(
            requireContext(), LinearLayout.VERTICAL
        ).apply { setDrawable(resources.getDrawable(R.drawable.item_hori_divider)!!) }
        vd.rv.addItemDecoration(
            itemDividerMode
        )
        helper = QuickAdapterHelper.Builder(adapter)
            .setTrailingLoadStateAdapter(object :
                TrailingLoadStateAdapter.OnTrailingListener {
                override fun onLoad() {
                    // 执行加载更多的操作，通常都是网络请求
                    page++
                    query()
                }

                override fun onFailRetry() {
                    // 加载失败后，点击重试的操作，通常都是网络请求

                }

                override fun isAllowLoading(): Boolean {
                    // 是否允许触发“加载更多”，通常情况下，下拉刷新的时候不允许进行加载更多
                    return !isRefresh
                }
            }).build()

        vd.rv.adapter = helper.adapter


        vd.srl.setOnRefreshListener {
            isRefresh = true
            page = 0
            query()
        }

        adapter.onLongClick = { id ->
            val result =
                DBManager.TestResultBox.get(id)
            result?.let {
                resultDialog.show(result) {
                    true
                }
            }
        }
        query()
    }

    private val resultDialog by lazy {
        ResultDetailsDialog(requireContext())
    }
    var page: Long = 0
    val pageSize: Long = 100
    var isLoading = false
    var isRefresh = true
    val datas = mutableListOf<TestResultModel>()
    lateinit var helper: QuickAdapterHelper
    var maxPage: Long = 0
    fun query() {
//        val count = DBManager.TestResultBox.query().build().count()
//        val p = count / pageSize
//        page = if (p * pageSize == count) {
//            p
//        } else {
//            p + 1
//        }
        val result = DBManager.TestResultBox.query().orderDesc(
            TestResultModel_.id
        ).build().find(page * pageSize, pageSize)
        if (isLoading) {
            isLoading = false
            datas.addAll(result)
            if (result.size >= pageSize) {
                page++
            }
            if (!result.isNullOrEmpty()) {
//                adapter.addAll(result)
                adapter.items.toMutableList().also {
                    it.addAll(result)
                    adapter.submitList(result, commitCallback = {
                        helper.trailingLoadState = LoadState.NotLoading(false)
                    })
                }
//                adapter.notifyItemRangeInserted(oldPos, result.size)
            } else {

            }
        } else {
            isRefresh = false
            datas.clear()
            datas.addAll(result)
            page = 0
            if (!result.isNullOrEmpty()) {
                adapter.submitList(result, commitCallback = {
                    helper.trailingLoadState = LoadState.NotLoading(false)
                })
//                adapter.notifyItemRangeInserted(oldPos, result.size)
            }
            vd.srl.isRefreshing = false
        }
//        if (maxPage > page) {

//        } else {
//            helper.trailingLoadState = LoadState.NotLoading(true)
//        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            TestDataFragment()
    }
}
