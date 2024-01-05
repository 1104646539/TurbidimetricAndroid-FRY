package com.wl.turbidimetric.test

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentRepeatabilityBinding
import com.wl.turbidimetric.ex.snack
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.turbidimetric.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.wl.wllib.LogToFile.i

/**
 * 拟合参数
 * @property viewModel MatchingArgsViewModel
 */
class RepeatabilityFragment :
    BaseFragment<RepeatabilityViewModel, FragmentRepeatabilityBinding>(R.layout.fragment_repeatability) {
    val items: MutableList<CurveModel> = mutableListOf()
    lateinit var projectAdapter: HomeProjectAdapter
    override val vm: RepeatabilityViewModel by viewModels {
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
        vm.listener()
        listenerDialog()
        listenerView()

    }

    override fun onStop() {
        vm.clearListener()
        super.onStop()
    }

    private fun listenerView() {
        vm.testMsg.observe(this) {
            i("it=$it")
            vd.tvMsg.text = it
        }
        vm.toastMsg.observe(this) { msg ->
            i("msg=$msg")
            snack(vd.root, msg)
        }

        projectAdapter = HomeProjectAdapter(requireContext(), items)
        vd.spnProject.adapter = projectAdapter
        projectAdapter.notifyDataSetChanged()

        vm.viewModelScope.launch {
            vm.projectDatas.collectLatest {
                items.clear()
                items.addAll(it)
                projectAdapter.notifyDataSetChanged()
            }
        }

        vd.spnProject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                vm.selectProject = items.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                vm.selectProject = null
            }
        }
    }

    private fun listenerDialog() {
        /**
         * 开始检测 比色皿,样本，试剂不足
         */
        vm.getStateNotExistMsg.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                dialog.showPop(requireContext(), isCancelable = false) { d ->
                    d.showDialog(
                        msg = "${vm.getStateNotExistMsg.value}",
                        confirmText = "我已添加", confirmClick = {
                            d.dismiss()
                            vm.dialogGetStateNotExistConfirm()
                        },
                        cancelText = "结束检测", cancelClick = {
                            d.dismiss()
                            vm.dialogGetStateNotExistCancel()
                        }
                    )
                }
            }
        }
        vm.matchingFinishMsg.observe(this) {
            if (it.isNotEmpty()) {
                dialog.showPop(requireContext(), isCancelable = false) { d ->
                    d.showDialog(
                        msg = it,
                        confirmText = "我知道了", confirmClick = {
                            d.dismiss()
                        }
                    )
                }
            }
        }
    }

}
