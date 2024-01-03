package com.wl.turbidimetric.settings

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentSettingsBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.MachineTestModel
import com.wl.turbidimetric.project.list.ProjectListActivity
import com.wl.turbidimetric.test.TestActivity
import com.wl.turbidimetric.test.debug.DebugActivity
import com.wl.turbidimetric.upload.view.UploadSettingsActivity
import com.wl.turbidimetric.util.ExportLogHelper
import com.wl.turbidimetric.view.*
import com.wl.turbidimetric.view.dialog.*
import com.wl.weiqianwllib.OrderUtil
import com.wl.wwanandroid.base.BaseFragment
import com.wl.wwanandroid.base.BaseViewModel
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsFragment :
    BaseFragment<BaseViewModel, FragmentSettingsBinding>(R.layout.fragment_settings) {
    private val TAG = "SettingsFragment"
    private var show = false

    /**
     * 点击显示调试模式的功能
     */
    var clickOrderCount = 5
    var clickOrder = 0

    val handler = object : Handler() {
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    override val vm: BaseViewModel by lazy { BaseViewModel() }

    private val machineTestModelDialog by lazy {
        MachineTestModelDialog(requireContext())
    }

    private val paramsDialog by lazy {
        ParamsDialog(requireContext())
    }

    /**
     * 等待任务对话框
     */
    val waitDialog by lazy {
        HiltDialog(requireContext())
    }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        initView()
        listener()
    }

    private fun initView() {


    }

    private fun listener() {
        listenerView()
        listenerOb()
    }

    private fun listenerOb() {
        launchAndRepeatWithViewLifecycle {
            SystemGlobal.obDebugMode.collectLatest {
                i("obDebugMode $it")
                vd.llDebugMode.visibility = if (it) View.VISIBLE else View.GONE
            }
        }
    }

    private fun listenerView() {
        vd.tvOrderSettings.setOnClickListener {
            showDebugModeView()
        }
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
            OrderUtil.showHideNav(requireActivity(), true)
        }
        vd.tvUpload.setOnClickListener {
            startUpload()
        }
        vd.tvExportLog.setOnClickListener {
            exportLog()
        }
        vd.tvDebug.setOnClickListener {
            debug()
        }
        vd.tvProjectList.setOnClickListener {
            projectList()
        }
        val versionAndroid = String(
            ((getPackageInfo(requireContext())?.versionName) ?: "").toByteArray(),
            charset("UTF-8")
        )

        vd.tvSoftVersionAndroid.text =
            "上位机版本:${versionAndroid} 发布版本:1"
        vd.tvSoftVersionMcu.text = "MCU版本:${SystemGlobal.mcuVersion}"

    }

    private fun projectList() {
        startActivity(Intent(requireContext(),ProjectListActivity::class.java))
    }

    /**
     * 启动调试页面
     */
    private fun debug() {
        if (isTestRunning()) {
            toast("正在检测，请稍后")
            return
        }
        startActivity(Intent(requireContext(), DebugActivity::class.java))
    }

    /**
     * 导出日志到文件
     */
    private fun exportLog() {
        waitDialog.showPop(requireContext()) { dialog ->
            //step1、 显示等待对话框
            dialog.showDialog("正在导出,请等待……", confirmText = "", confirmClick = {})

            lifecycleScope.launch(Dispatchers.IO) {
                //step2、 导出 等待结果
                ExportLogHelper.export(requireContext(), { file1, file2 ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        dialog.showDialog("导出成功,文件保存在\n$file1\n$file2", "确定", { d ->
                            d.dismiss()
                        })
                    }
                }, { err ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        dialog.showDialog("导出失败,$err", "确定", { d ->
                            d.dismiss()
                        })
                    }
                })

            }
        }
    }

    /**
     * 清零任务
     */
    private val runnable_order: Runnable = Runnable {
        clickOrder = 0
    }

    /**
     * 5s内连续点击几次其他设置后，打开调试模式
     */
    private fun showDebugModeView() {
        handler.removeCallbacks(runnable_order)
        clickOrder++
        handler.postDelayed(runnable_order, 5000)
        if (clickOrder >= clickOrderCount) {
            LocalData.DebugMode = !LocalData.DebugMode
            SystemGlobal.isDebugMode = LocalData.DebugMode
            clickOrder = 0
        }
    }

    private fun startUpload() {
        startActivity(Intent(requireContext(), UploadSettingsActivity::class.java))
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
            OrderUtil.showHideNav(requireActivity(), false)
            show = false
        } else {
            OrderUtil.showHideNav(requireActivity(), true)
            show = true
        }
    }


    /**
     * 进入重复性测试
     */
    private fun showRepeatability() {
        if (isTestRunning()) {
            toast("正在检测，请稍后")
            return
        }
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
        machineTestModelDialog.showPop(requireContext(), isCancelable = false) {
            it.showDialog(
                MachineTestModel.valueOf(LocalData.CurMachineTestModel),
                LocalData.SampleExist,
                LocalData.ScanCode,
                { machineTestModel, sampleExist, scanCode, baseDialog ->
                    if (isTestRunning()) {
                        toast("正在检测，请稍后")
                        return@showDialog
                    }

                    LocalData.CurMachineTestModel = machineTestModel.name
                    if (isAuto(machineTestModel)) {
                        LocalData.SampleExist = sampleExist
                        LocalData.ScanCode = scanCode
                    }
                    i("machineTestModel=$machineTestModel sampleExist=$sampleExist scanCode=$scanCode")
                    baseDialog?.dismiss()
                },
                {
                    it.dismiss()
                })
        }

    }

    /**
     * 显示编号设置
     */
    private fun showDetectionNumDialog() {
        detectionNumDialog.showPop(requireContext(), isCancelable = false) {
            detectionNumDialog.showDialog("请输入编号", "${LocalData.DetectionNum}", "确定", l@{
                val content = it.etContent?.text.toString()
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
            })
        }
    }


    /**
     * 显示参数设置
     */
    private fun showParamsSettingDialog() {
        paramsDialog.showPop(requireContext(), isCancelable = false) {
            paramsDialog.showDialog(
                LocalData.TakeReagentR1,
                LocalData.TakeReagentR2,
                LocalData.SamplingVolume,
                LocalData.SamplingProbeCleaningDuration,
                LocalData.StirProbeCleaningDuration,
                LocalData.StirDuration,
                LocalData.Test1DelayTime,
                LocalData.Test2DelayTime,
                LocalData.Test3DelayTime,
                LocalData.Test4DelayTime,
                { takeR1: Int, takeR2: Int, samplingVolume: Int, samplingProbeCleaningTime: Int, stirProbeCleaningTime: Int, stirDuration: Int, test1DelayTime: Long, test2DelayTime: Long, test3DelayTime: Long, test4DelayTime: Long, baseDialog: BasePopupView ->
                    LocalData.TakeReagentR1 = takeR1
                    LocalData.TakeReagentR2 = takeR2
                    LocalData.SamplingVolume = samplingVolume
                    LocalData.SamplingProbeCleaningDuration = samplingProbeCleaningTime
                    LocalData.StirProbeCleaningDuration = stirProbeCleaningTime
                    LocalData.StirDuration = stirDuration
                    LocalData.Test1DelayTime = test1DelayTime
                    LocalData.Test2DelayTime = test2DelayTime
                    LocalData.Test3DelayTime = test3DelayTime
                    LocalData.Test4DelayTime = test4DelayTime
                    baseDialog.dismiss()

                },
                {
                    it.dismiss()
                })
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
