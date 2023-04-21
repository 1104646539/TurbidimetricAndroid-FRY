package com.wl.turbidimetric.matchingargs

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentMatchingArgsBinding
import com.wl.turbidimetric.ex.snack
import com.wl.turbidimetric.view.HiltDialog
import com.wl.wwanandroid.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 拟合参数
 * @property viewModel MatchingArgsViewModel
 */
class MatchingArgsFragment :
    BaseFragment<MatchingArgsViewModel, FragmentMatchingArgsBinding>(R.layout.fragment_matching_args) {
    override val viewModel: MatchingArgsViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance() =
            MatchingArgsFragment()
    }

    private val dialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    override fun initViewModel() {
        viewDataBinding.model = viewModel
    }

    val adapter: MatchingArgsAdapter by lazy {
        MatchingArgsAdapter()
    }

    override fun init(savedInstanceState: Bundle?) {

        listenerDialog()
        listenerView()
    }

    private fun listenerView() {
        viewModel.testMsg.observe(this) {
            viewDataBinding.tvMsg.text = it
        }

        viewDataBinding.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewDataBinding.rv.adapter = adapter

        viewModel.viewModelScope.launch {
            viewModel.datas.collectLatest {
                adapter.submitData(it)
            }
        }
        viewModel.toastMsg.observe(this) { msg ->
            Timber.d("msg=$msg")
            snack(viewDataBinding.root, msg)
        }
    }

    private fun listenerDialog() {
        /**
         * 开始检测 比色皿,采便管，试剂不足
         */
        viewModel.dialogGetStateNotExist.observe(this) { show ->
            if (show) {
                dialog.show(
                    msg = "${viewModel.getStateNotExistMsg.value}",
                    confirmMsg = "我已添加", onConfirm = {
                        it.dismiss()
                        viewModel.dialogGetStateNotExistConfirm()
                    },
                    cancelMsg = "结束检测", onCancel = {
                        it.dismiss()
                        viewModel.dialogGetStateNotExistCancel()
                    }
                )
                viewModel.dialogGetStateNotExist.postValue(false)
            }
        }
    }

}
