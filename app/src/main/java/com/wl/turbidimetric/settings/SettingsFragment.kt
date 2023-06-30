package com.wl.turbidimetric.settings

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentSettingsBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.test.TestActivity
import com.wl.turbidimetric.view.BaseDialog
import com.wl.turbidimetric.view.HiltDialog
import com.wl.turbidimetric.view.MachineTestModelDialog
import com.wl.turbidimetric.view.OneEditDialog
import com.wl.wwanandroid.base.BaseFragment
import com.wl.wwanandroid.base.BaseViewModel
import timber.log.Timber

class SettingsFragment :
    BaseFragment<BaseViewModel, FragmentSettingsBinding>(R.layout.fragment_settings) {
    val TAG = "SettingsFragment"
    var show = false

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    override val vm: BaseViewModel by lazy { BaseViewModel() }

    val machineTestModelDialog by lazy {
        MachineTestModelDialog(requireContext())
    }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        listener()
    }

    private fun listener() {
        vd.tvParamsSetting.setOnClickListener {
            showParamsSettingDialog()

        }
        vd.tvDetectionNumSetting.setOnClickListener {
            showDetectionNumDialog()
        }

        vd.tvMachineTestMode.setOnClickListener {
            showMachineTestModelDialog()
        }

        vd.tvRepeatability.setOnClickListener {
            showRepeatability()
        }
        vd.tvNav.setOnClickListener {
            showHideNav()
        }
        vd.tvLauncher.setOnClickListener {
            showLauncher()
        }
    }

    private fun showLauncher() {
        startActivity(getLauncher())
    }


    private fun showHideNav() {
        //设置广播发送隐藏虚拟按键命令
        val showIntent = Intent()
        showIntent.action = "com.android.intent.action.NAVBAR_SHOW"
        if (show) {
            showIntent.putExtra("cmd", "hide")
            show = false;
        } else {
            showIntent.putExtra("cmd", "show")
            show = true;
        }
        requireActivity().sendOrderedBroadcast(showIntent, null)
    }

    private fun showRepeatability() {
        startActivity(
            Intent(requireContext(), TestActivity::class.java).putExtra(
                TestActivity.flag, TestActivity.flag_Repeatability
            )
        )
    }

    /**
     * 显示仪器检测模式对话框
     */
    private fun showMachineTestModelDialog() {
        machineTestModelDialog.show(MachineTestModel.valueOf(LocalData.CurMachineTestModel),
            LocalData.SampleExist,
            LocalData.ScanCode,
            { machineTestModel, sampleExist, scanCode, baseDialog ->
                if (isTestRunning()) {
                    toast("正在检测，请稍后")
                    return@show
                }

                LocalData.CurMachineTestModel = machineTestModel.name
                if (isAuto(machineTestModel)) {
                    LocalData.SampleExist = sampleExist
                    LocalData.ScanCode = scanCode
                }
                Timber.d("machineTestModel=$machineTestModel sampleExist=$sampleExist scanCode=$scanCode")
                baseDialog?.dismiss()
            },
            {
                it.dismiss()
            })

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
            LocalData.DetectionNum = content
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

            LocalData.TakeReagentR1 = takeR1.toInt()
            LocalData.TakeReagentR2 = takeR2.toInt()
            LocalData.SamplingVolume = sampling.toInt()
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
