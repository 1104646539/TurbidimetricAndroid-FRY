package com.wl.turbidimetric.test

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentMatchingArgsBinding
import com.wl.turbidimetric.databinding.FragmentRepeatabilityBinding
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
class RepeatabilityFragment :
    BaseFragment<RepeatabilityViewModel, FragmentRepeatabilityBinding>(R.layout.fragment_repeatability) {
    override val vm: RepeatabilityViewModel by viewModels{
        RepeatabilityViewModelFactory()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RepeatabilityFragment()
    }

    private val dialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    override fun initViewModel() {
        vd.model = vm
    }

    override fun init(savedInstanceState: Bundle?) {
        listenerDialog()
        listenerView()
    }

    private fun listenerView() {
        vm.testMsg.observe(this) {
            Timber.d("it=$it")
            vd.tvMsg.text = it
        }
        vm.toastMsg.observe(this) { msg ->
            Timber.d("msg=$msg")
            snack(vd.root, msg)
        }
    }

    private fun listenerDialog() {
        /**
         * 开始检测 比色皿,采便管，试剂不足
         */
        vm.getStateNotExistMsg.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                dialog.show(
                    msg = "${vm.getStateNotExistMsg.value}",
                    confirmMsg = "我已添加", onConfirm = {
                        it.dismiss()
                        vm.dialogGetStateNotExistConfirm()
                    },
                    cancelMsg = "结束检测", onCancel = {
                        it.dismiss()
                        vm.dialogGetStateNotExistCancel()
                    }
                )
            }
        }
        vm.matchingFinishMsg.observe(this) {
            if (it.isNotEmpty()) {
                dialog.show(
                    msg = it,
                    confirmMsg = "我知道了", onConfirm = {
                        it.dismiss()
                    }
                )
            }
        }
    }

}
