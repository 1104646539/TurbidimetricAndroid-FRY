package com.wl.turbidimetric.matchingargs

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
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
    override val vm: MatchingArgsViewModel by viewModels {
        MatchingArgsViewModelFactory()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            MatchingArgsFragment()
    }

    private val dialog: HiltDialog by lazy {
        HiltDialog(requireContext())
    }

    override fun initViewModel() {
        vd.model = vm
    }

    val adapter: MatchingArgsAdapter by lazy {
        MatchingArgsAdapter()
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

        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.adapter = adapter

        lifecycleScope.launch {
            vm.datas.collectLatest {
                Timber.d("项目更新了")
                adapter.submitData(it)
            }
        }
        lifecycleScope.launch {
            adapter.onPagesUpdatedFlow.collectLatest {
                if (adapter.selectPos < 0 && adapter.snapshot().size > 0) {
                    adapter.setSelectIndex(0)
                    adapter.notifyItemChanged(0)
                }
            }
        }

        vm.toastMsg.observe(this) { msg ->
            Timber.d("msg=$msg")
            snack(vd.root, msg)
        }

        adapter.onSelectChange = { project ->
            Timber.d("选中的=${project}")

            val values = ArrayList<Entry>()
            val params = mutableListOf(0f, 50f, 200f, 6000f)
            values.add(Entry(0.0F, 0f))
            values.add(Entry(1.0F, 50f))
            values.add(Entry(2.0F, 200f))
            values.add(Entry(3.0F, 1000f))
            values.add(Entry(4.0F, 6000f))

            val set1 = LineDataSet(values, "")
            set1.setDrawValues(false)
            set1.setDrawIcons(false)
//            set1.setDrawCircleHole(false)
//            set1.setDrawCircles(false)
            set1.label = ""

            val dataSets = java.util.ArrayList<ILineDataSet>()
            dataSets.add(set1) // add the data sets

            val data = LineData(dataSets)

            vd.lcCurve.axisRight.setValueFormatter { value, axis -> "" }
            vd.lcCurve.description.text = ""
            vd.lcCurve.xAxis.isEnabled = false

            val yAxis = vd.lcCurve.axisLeft

//            yAxis.axisMaximum = params.max().toFloat()
//            yAxis.axisMinimum = params.min().toFloat()
            yAxis.setDrawZeroLine(true)

            vd.lcCurve.data = data
            vd.lcCurve.invalidate()
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
