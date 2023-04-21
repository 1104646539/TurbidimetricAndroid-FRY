package com.wl.turbidimetric.datamanager

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentDataManagerBinding
import com.wl.turbidimetric.datamanager.vm.DataManagerViewModel
import com.wl.turbidimetric.datamanager.vm.DataManagerViewModelFactory
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.wwanandroid.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class DataManagerFragment :
    BaseFragment<DataManagerViewModel, FragmentDataManagerBinding>(R.layout.fragment_data_manager) {
    override val viewModel: DataManagerViewModel by viewModels {
        DataManagerViewModelFactory(
            DataManagerRepository(
                DBManager.testResultBox,
                DBManager.projectBox
            )
        )
    }

    val adapter: DataManagerAdapter by lazy {
        DataManagerAdapter()
    }


    override fun init(savedInstanceState: Bundle?) {
        viewDataBinding.btnInsert.setOnClickListener {
            for (i in 0..1000) {
                val project = ProjectModel(
                    projectName = "便潜血",
                    projectCode = "FOB2",
                    projectLjz = "52",
                    projectUnit = "ml"
                )
                val dr = TestResultModel(
                    testResult = "阴性",
                    concentration = "20",
                    name = "张三${i}",
                    gender = "男",
                    age = "52"
                )
                dr.project.target = project
                val id = viewModel.add(dr)
                Log.d(TAG, "id=${id} dr=${dr}")
            }
        }

        viewDataBinding.btnQuery.setOnClickListener {

            Log.d(TAG, "size1= ${adapter.snapshot().items.size} ")
        }

        viewDataBinding.btnChange.setOnClickListener {
            val row = viewDataBinding.etRow.text.toString().toInt()
            val drw = adapter.snapshot().items[row].copy()
            drw.testResult = "修改过"
            drw.project.target?.projectName += "修改过"
            drw.project.target?.let {
                DBManager.projectBox.put(it)
            }
            val result = viewModel.update(drw)
            Log.d(TAG, "row= ${row} result=${result}")
        }

        viewDataBinding.btnDelete.setOnClickListener {
            val row = viewDataBinding.etRow.text.toString().toInt()
            val result = viewModel.remove(adapter.snapshot().items[row])
            Log.d(TAG, "row= ${row} result=${result}")
        }

        viewDataBinding.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewDataBinding.rv.adapter = adapter

        viewModel.viewModelScope.launch {
            viewModel.items.collectLatest {
                adapter.submitData(it)
            }
        }

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
        fun newInstance() =
            DataManagerFragment()
    }

    override fun initViewModel() {
    }


}
