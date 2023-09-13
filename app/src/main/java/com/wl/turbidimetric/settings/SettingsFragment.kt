package com.wl.turbidimetric.settings

import android.app.Activity
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
import com.wl.turbidimetric.view.*
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

    val paramsDialog by lazy {
        ParamsDialog(requireContext())
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
            requireActivity().showHideNav(true)
        }
        vd.tvSoftVersion.text = "上位机版本:${requireContext().packageName} 发布版本:1"
    }

    private fun showLauncher() {
        startActivity(getLauncher())
    }

    /**
     * 显示和隐藏导航栏
     * @param showNav Boolean
     */
    private fun showHideNav() {
        //设置广播发送隐藏虚拟按键命令
        if (show) {
            requireActivity().showHideNav(false)
            show = false;
        } else {
            requireActivity().showHideNav(true)
            show = true;
        }
    }


    /**
     * 进入重复性测试
     */
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
        paramsDialog.show(
            LocalData.TakeReagentR1,
            LocalData.TakeReagentR2,
            LocalData.SamplingVolume,
            LocalData.SamplingProbeCleaningDuration,
            LocalData.StirProbeCleaningDuration,
            { takeR1: Int, takeR2: Int, samplingVolume: Int, samplingProbeCleaningDuration: Int, stirProbeCleaningDuration: Int, baseDialog: BaseDialog ->
                LocalData.TakeReagentR1 = takeR1
                LocalData.TakeReagentR2 = takeR2
                LocalData.SamplingVolume = samplingVolume
                LocalData.SamplingProbeCleaningDuration = samplingProbeCleaningDuration
                LocalData.StirProbeCleaningDuration = stirProbeCleaningDuration
                baseDialog.dismiss()
            },
            {
                it.dismiss()
            })
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
