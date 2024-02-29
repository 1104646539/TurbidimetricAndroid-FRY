package com.wl.turbidimetric.settings

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentSettingsBinding
import com.wl.turbidimetric.ex.*
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.mcuupdate.McuUpdateHelper
import com.wl.turbidimetric.mcuupdate.UpdateResult
import com.wl.turbidimetric.project.list.ProjectListActivity
import com.wl.turbidimetric.settings.detectionnum.DetectionNumFragment
import com.wl.turbidimetric.settings.network.NetworkFragment
import com.wl.turbidimetric.settings.params.ParamsFragment
import com.wl.turbidimetric.settings.testmode.TestModeFragment
import com.wl.turbidimetric.test.TestActivity
import com.wl.turbidimetric.test.debug.DebugActivity
import com.wl.turbidimetric.upload.view.UploadSettingsActivity
import com.wl.turbidimetric.util.ExportLogHelper
import com.wl.turbidimetric.view.*
import com.wl.turbidimetric.view.dialog.*
import com.wl.weiqianwllib.OrderUtil
import com.wl.wllib.LogToFile.i
import com.wl.wllib.LogToFile.u
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsFragment constructor() :
    BaseFragment<SettingsViewModel, FragmentSettingsBinding>(R.layout.fragment_settings) {
    private val TAG = "SettingsFragment"
    private var show = false
    private val mcuUpdateHelper by lazy { McuUpdateHelper(appVm) }

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

    override val vm: SettingsViewModel by viewModels { SettingsViewModelFactory() }
    private val mcuUpdateHiltDialog by lazy {
        HiltDialog(requireContext())
    }

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
        lifecycleScope.launch {
            SystemGlobal.obDebugMode.collectLatest {
                i("obDebugMode $it")
                it.isShow()?.let {
                    vd.sivDebug.visibility = it
                    vd.sivLauncher.visibility = it
                    vd.sivNav.visibility = it
                    vd.sivExportLog.visibility = it
                    vd.sivDebugTitle.visibility = it
                }
            }
        }
    }

    private fun listenerView() {
        vd.sivOrderSettings.setOnClickListener {
            u("其他设置")
            showDebugModeView()
        }
        vd.sivParamsSetting.setOnClickListener {
            u("参数设置")
            changeContent(ParamsFragment.instance)
            vd.wgpSelectable.setSelected(it.id)
        }
        vd.sivDetectionNumSetting.setOnClickListener {
            u("编号设置")
            changeContent(DetectionNumFragment.instance)
            vd.wgpSelectable.setSelected(it.id)
        }

        vd.sivMachineTestMode.setOnClickListener {
            u("检测模式设置")
            changeContent(TestModeFragment.instance)
            vd.wgpSelectable.setSelected(it.id)
        }
        vd.sivMachineNetwork.setOnClickListener {
            u("本机网络设置")
            changeContent(NetworkFragment.instance)
            vd.wgpSelectable.setSelected(it.id)
        }

        vd.sivRepeatability.setOnClickListener {
            u("重复性测试")
            showRepeatability()
        }
        vd.sivNav.setOnClickListener {
            u("显示隐藏导航栏")
            showHideNav()
        }
        vd.sivLauncher.setOnClickListener {
            u("打开桌面")
            showLauncher()
            OrderUtil.showHideNav(requireActivity(), true)
        }
        vd.sivUpload.setOnClickListener {
            u("上传设置")
            startUpload()
        }
        vd.sivExportLog.setOnClickListener {
            u("导出日志")
            exportLog()
        }
        vd.sivDebug.setOnClickListener {
            u("调试")
            debug()
        }
        vd.sivProjectList.setOnClickListener {
            u("项目设置")
            projectList()
//            vd.wgpSelectable.setSelected(it.id)
        }
        vd.tvSoftVersionMcu.setOnClickListener {
            u("MCU升级")
            showHiltMcuUpdate()
        }
        val versionAndroid = String(
            ((getPackageInfo(requireContext())?.versionName) ?: "").toByteArray(),
            charset("UTF-8")
        )

        vd.tvSoftVersionAndroid.text =
            "上位机版本:${versionAndroid} \n发布版本:1"
        vd.tvSoftVersionMcu.text = "MCU版本:${SystemGlobal.mcuVersion}"

        vd.sivParamsSetting.performClick()
    }

    private fun showHiltMcuUpdate() {
        mcuUpdateHiltDialog.showPop(requireContext(), isCancelable = false) {
            it.showDialog(
                "是否升级MCU，请确定已经插入含升级文件的U盘",
                confirmText = "升级",
                confirmClick = {
                    mcuUpdate()
                },
                cancelText = "取消",
                cancelClick = { it.dismiss() })
        }
    }

    private fun mcuUpdate() {
        mcuUpdateHiltDialog.showPop(requireContext(), isCancelable = false) {
            it.showDialog("请等待……")
        }
        mcuUpdateHelper.update(Dispatchers.IO, Dispatchers.Main, lifecycleScope) {
            if (it is UpdateResult.Failed) {
                mcuUpdateHiltDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                    dialog.showDialog(
                        "升级失败,${it.msg}",
                        confirmText = "确定",
                        confirmClick = {
                            it.dismiss()
                        })
                }
            } else {
                mcuUpdateHiltDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                    dialog.showDialog(
                        "升级成功,请重启仪器生效",
                        confirmText = "确定",
                        confirmClick = {
                            it.dismiss()
                        })
                }
            }
        }
    }

    var curFragment: Fragment? = null
    private fun changeContent(fragment: Fragment) {
        if (curFragment != null && curFragment == fragment) {
            return
        }
        val bt = childFragmentManager.beginTransaction()
        if (curFragment != null) {
            bt.hide(curFragment!!)
            curFragment = null
        }

        if (childFragmentManager.findFragmentByTag(fragment::class.java.name) == null) {
            bt.add(R.id.fl_content, fragment, fragment::class.java.name).show(fragment)
        } else {
            bt.show(fragment)
        }
        curFragment = fragment

        bt.commit()
    }

    private fun projectList() {
        startActivity(Intent(requireContext(), ProjectListActivity::class.java))
    }

    /**
     * 启动调试页面
     */
    private fun debug() {
        if (appVm.testState.isTestRunning()) {
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
            vm.setTestModel(!vm.getTestModel())
            SystemGlobal.isDebugMode = vm.getTestModel()
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
        if (appVm.testState.isTestRunning()) {
            toast("正在检测，请稍后")
            return
        }
        startActivity(
            Intent(requireContext(), TestActivity::class.java).putExtra(
                TestActivity.flag, TestActivity.flag_Repeatability
            )
        )
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
