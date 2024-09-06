package com.wl.turbidimetric.settings.params

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.addTextChangedListener
import androidx.databinding.adapters.TextViewBindingAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentParamsBinding
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.isShow
import com.wl.turbidimetric.view.dialog.showPop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ParamsFragment :
    BaseFragment<ParamsViewModel, FragmentParamsBinding>(R.layout.fragment_params) {
    override val vm: ParamsViewModel by viewModels { ParamsViewModelFactory() }
    private val hiltDialog by lazy { HiltDialog(requireContext()) }
    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        listenerView()
        listenerData()
        vm.reset()
    }

    private fun listenerData() {
        lifecycleScope.launch {
            vm.paramsUiState.collectLatest {
                vd.tietR1.setText(it.r1Volume.toString())
                vd.tietR2.setText(it.r2Volume.toString())
                vd.tietSampling.setText(it.samplingVolume.toString())
                vd.tietSamplingProbeCleaningTime.setText(it.samplingProbeCleaningDuration.toString())
                vd.tietStirProbeCleaningTime.setText(it.stirProbeCleaningDuration.toString())
                vd.tietStirTime.setText(it.stirDuration.toString())
                vd.tietTest1DelayTime.setText((it.test1DelayTime / 1000).toString())
                vd.tietTest2DelayTime.setText((it.test2DelayTime / 1000).toString())
                vd.tietTest3DelayTime.setText((it.test3DelayTime / 1000).toString())
                vd.tietTest4DelayTime.setText((it.test4DelayTime / 1000).toString())
                vd.tietReactionTime.setText((it.reactionTime / 1000).toString())
            }
        }

        lifecycleScope.launch {
            vm.hiltText.collectLatest { hilt ->
                hiltDialog.showPop(requireContext()) { dialog ->
                    dialog.showDialog(hilt, confirmText = "确定", confirmClick = {
                        dialog.dismiss()
                    })
                }
            }
        }

        lifecycleScope.launch {
            appVm.obDebugMode.collectLatest { debug ->
                debug?.let { show ->
                    vd.tilTest1DelayTime.visibility = show.isShow()
                    vd.tilTest2DelayTime.visibility = show.isShow()
                    vd.tilTest3DelayTime.visibility = show.isShow()
                    vd.tilTest4DelayTime.visibility = show.isShow()
                    vd.tilSamplingProbeCleaningTime.visibility = show.isShow()
                    vd.tilStirProbeCleaningTime.visibility = show.isShow()
                    vd.tilStirTime.visibility = show.isShow()
                }
                if (debug) {
                    vd.tietReactionTime.removeTextChangedListener(tw)

                    vd.tietTest2DelayTime.addTextChangedListener(tw)
                    vd.tietTest1DelayTime.addTextChangedListener(tw)

                    vd.tilReactionTime.isEnabled = false

                    //因为以前依赖的view隐藏了所有重新设置约束
                    val cs = ConstraintSet()
                    cs.clone(vd.clRoot)
                    cs.connect(
                        vd.tilReactionTime.id,
                        ConstraintSet.TOP,
                        vd.tilTest4DelayTime.id,
                        ConstraintSet.TOP
                    )
                    cs.connect(
                        vd.tilReactionTime.id,
                        ConstraintSet.LEFT,
                        vd.tilTest2DelayTime.id,
                        ConstraintSet.LEFT
                    )
                    cs.applyTo(vd.clRoot)

                    val cs2 = ConstraintSet()
                    cs2.clone(vd.clRoot)
                    cs2.connect(
                        vd.btnChange.id,
                        ConstraintSet.TOP,
                        vd.tilTest4DelayTime.id,
                        ConstraintSet.BOTTOM
                    )
                    cs2.applyTo(vd.clRoot)
                } else {
                    vd.tietTest1DelayTime.removeTextChangedListener(tw)
                    vd.tietTest2DelayTime.removeTextChangedListener(tw)

                    vd.tilReactionTime.isEnabled = true
                    vd.tietReactionTime.addTextChangedListener(tw)

                    //因为以前依赖的view隐藏了所有重新设置约束
                    val cs = ConstraintSet()
                    cs.clone(vd.clRoot)
                    cs.connect(
                        vd.tilReactionTime.id, ConstraintSet.TOP, vd.tilR1.id, ConstraintSet.BOTTOM
                    )
                    cs.connect(
                        vd.tilReactionTime.id, ConstraintSet.LEFT, vd.tilR1.id, ConstraintSet.LEFT
                    )
                    cs.applyTo(vd.clRoot)

                    val cs2 = ConstraintSet()
                    cs2.clone(vd.clRoot)
                    cs2.connect(
                        vd.btnChange.id,
                        ConstraintSet.TOP,
                        vd.tilReactionTime.id,
                        ConstraintSet.BOTTOM
                    )
                    cs2.applyTo(vd.clRoot)
                }
            }
        }
    }

    private val tw = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            onTestTimeChange()
        }
    }

    /**
     * 调试模式下更改第一次和第二次时，更新反应时间
     * 用户模式下更改反应时间是，更新第二次
     */
    private fun onTestTimeChange() {
        var t1 = (vd.tietTest1DelayTime.text.toString().toLongOrNull() ?: 0) * 1000
        var t2 = (vd.tietTest2DelayTime.text.toString().toLongOrNull() ?: 0) * 1000
        var r = (vd.tietReactionTime.text.toString().toLongOrNull() ?: 0) * 1000
        if (appVm.isDebugMode) {
            r = t2 - t1
            vd.tietReactionTime.setText((r / 1000).toString())
        } else {
            t2 = r + t1
            vd.tietTest2DelayTime.setText((t2 / 1000).toString())
        }

    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            vm.reset()
        }
    }

    private fun listenerView() {
        vd.btnChange.setOnClickListener {
            vm.change(
                vd.tietR1.text.toString().toIntOrNull() ?: 0,
                vd.tietR2.text.toString().toIntOrNull() ?: 0,
                vd.tietSampling.text.toString().toIntOrNull() ?: 0,
                vd.tietSamplingProbeCleaningTime.text.toString().toIntOrNull() ?: 0,
                vd.tietStirProbeCleaningTime.text.toString().toIntOrNull() ?: 0,
                vd.tietStirTime.text.toString().toIntOrNull() ?: 0,
                (vd.tietTest1DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
                (vd.tietTest2DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
                (vd.tietTest3DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
                (vd.tietTest4DelayTime.text.toString().toLongOrNull() ?: 0) * 1000,
                (vd.tietReactionTime.text.toString().toLongOrNull() ?: 0) * 1000,
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ParamsFragment()
    }
}
