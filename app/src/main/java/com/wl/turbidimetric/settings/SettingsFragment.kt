package com.wl.turbidimetric.settings

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentSettingsBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.isNum
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.view.BaseDialog
import com.wl.turbidimetric.view.HiltDialog
import com.wl.turbidimetric.view.OneEditDialog
import com.wl.wwanandroid.base.BaseFragment
import com.wl.wwanandroid.base.BaseViewModel

class SettingsFragment :
    BaseFragment<BaseViewModel, FragmentSettingsBinding>(R.layout.fragment_settings) {
    val TAG = "SettingsFragment"

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    override val vm: BaseViewModel by lazy { BaseViewModel() }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        listener()
    }

    private fun listener() {
        vd.tvParamsSetting.setOnClickListener {
            showParamsSettingDialog()

        }
        vd.tvTestHint.setOnClickListener {
            testHiltDialog.show("测试", "确定", {
                toast("确定")
                it.dismiss()
            }, "取消", {
                toast("取消")
                it.dismiss()
            })
        }
        vd.tvDetectionNumSetting.setOnClickListener {
            showDetectionNumDialog()
        }
    }

    /**
     * 显示编号设置
     */
    private fun showDetectionNumDialog() {
        detectionNumDialog.show("请输入编号", "${LocalData.DetectionNum}", "确定", l@{
            val content = it.etContent.text.toString()
            if (content.trim().isNullOrEmpty()) {
                toast("请输入数字")
                return@l
            }
            if (!content.isNum()) {
                toast("请输入数字")
                return@l
            }
            LocalData.DetectionNum=content
            it.dismiss()
        }, "取消", {
            it.dismiss()
        }, true)
    }

    /**
     * 显示参数设置
     */
    private fun showParamsSettingDialog() {
        val etTakeR1 = paramsDialog.getView<EditText>(R.id.et_take_r1)
        val etTakeR2 = paramsDialog.getView<EditText>(R.id.et_take_r2)
        val etSampling = paramsDialog.getView<EditText>(R.id.et_sampling)
        paramsDialog.show("确定", l@{
            val takeR1 = etTakeR1.text.toString()
            val takeR2 = etTakeR2.text.toString()
            val sampling = etSampling.text.toString()

            if (takeR1.trim().isNullOrEmpty() || takeR2.trim().isNullOrEmpty() || sampling.trim()
                    .isNullOrEmpty()
            ) {
                toast("请输入数字")
                return@l
            }
            if (!takeR1.isNum() || !takeR2.isNum() || !sampling.isNum()) {
                toast("请输入数字")
                return@l
            }

            LocalData.TakeReagentR1=takeR1.toInt()
            LocalData.TakeReagentR2=takeR2.toInt()
            LocalData.SamplingVolume=sampling.toInt()
            it.dismiss()
        }, "取消", {
            it.dismiss()
        })

        etTakeR1.setText(LocalData.TakeReagentR1.toString())
        etTakeR1.selectionLast()
        etTakeR2.setText(LocalData.TakeReagentR2.toString())
        etTakeR2.selectionLast()
        etSampling.setText(LocalData.SamplingVolume.toString())
        etSampling.selectionLast()
    }

    val paramsDialog by lazy {
        BaseDialog(requireContext()).apply {
            addView(R.layout.dialog_params_settings)
        }
    }
    val testHiltDialog by lazy {
        HiltDialog(requireContext())
    }
    val detectionNumDialog by lazy {
        OneEditDialog(requireContext()).apply {
            setEditType(OneEditDialog.EditType.NUM_POSITIVE)
        }
    }
}
