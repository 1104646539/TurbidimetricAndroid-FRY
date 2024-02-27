package com.wl.turbidimetric.main

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.MachineState
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.databinding.ActivityMainBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.main.splash.SplashFragment
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.weiqianwllib.OrderUtil
import com.wl.weiqianwllib.upan.StorageState
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.weiqianwllib.upan.StorageUtil.OPEN_DOCUMENT_TREE_CODE
import com.wl.wllib.LogToFile.i
import com.wl.wllib.LogToFile.e
import com.wl.wllib.ktxRunOnBgCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus


class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {
    val TAG = "MainActivity"
    override val vd: ActivityMainBinding by ActivityDataBindingDelegate(R.layout.activity_main)
    override val vm: MainViewModel by viewModels { MainViewModelFactory() }
    var mPermissionIntent: PendingIntent? = null

    private val handler_init_qrcode = 1000
    private val handler_init_upan = 1010
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Mvvmdemo) //恢复原有的样式
        super.onCreate(savedInstanceState)
    }

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                handler_init_qrcode -> {
                    initQrCode()
                }

                handler_init_upan -> {

                }
            }
        }
    }

    private fun showOpenDocumentTree() {
        StorageUtil.showOpenDocumentTree(this, OPEN_DOCUMENT_TREE_CODE)
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        moveTaskToBack(true)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (OPEN_DOCUMENT_TREE_CODE == requestCode) {
            if (data != null && data.data != null) {
                val uri = data.data
                StorageUtil.saveTreeUri(this, StorageUtil.curPath!!, uri!!)
                Log.d(TAG, "onActivityResult path=" + StorageUtil.curPath)
                changeStorageState(StorageState.EXIST)
            } else {
                changeStorageState(StorageState.UNAUTHORIZED)
                Log.d(TAG, "onActivityResult data==null" + (data == null))
            }
        }
        super.onActivityResult(requestCode, resultCode, data)

        //授权一次后重启开机不用再次授权
        if (resultCode != Activity.RESULT_OK) return
        val treeUri = data!!.data
        try {
            grantUriPermission(
                packageName,
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            contentResolver.takePersistableUriPermission(
                treeUri!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            e("异常${e.stackTraceToString()}")
        }
        i(
            TAG,
            "onActivityResult 第一次插入的U盘地址= ： $treeUri"
        )
    }

    private fun initQrCode() {
        EventBus.getDefault().post(EventMsg<Any>(what = EventGlobal.WHAT_INIT_QRCODE))
    }

    override fun init() {
        showSplash()
        listener()
//        initNav()
        initNav2()
        test()
        initTime()
        initUploadClient()
        OrderUtil.showHideNav(this, false)
    }

    private fun initNav2() {
        vd.vp.adapter = MainViewPagerAdapter(this)
        vd.vp.isUserInputEnabled = false
        vd.vp.offscreenPageLimit = 6
        vd.lnv.setItem(
            mutableListOf(
                R.drawable.left_nav_analyse,
                R.drawable.left_nav_datamanager,
                R.drawable.left_nav_matching,
                R.drawable.left_nav_settings
            ),
            mutableListOf(
                R.drawable.left_nav_analyse_selected,
                R.drawable.left_nav_datamanager_selected,
                R.drawable.left_nav_matching_selected,
                R.drawable.left_nav_settings_selected
            ),
            mutableListOf("样本分析", "数据管理", "曲线拟合", "参数设置"),
            R.drawable.left_nav_item_bg, R.drawable.left_nav_item_bg2
        )
        vd.lnv.onItemChangeListener = {
            vm.curIndex.value = it
        }
        vm.curIndex.observe(this) {
            vd.vp.setCurrentItem(it, false)
        }
        vd.tnv.setShutdownListener {
            showShutdownDialog()
        }
    }

    private fun initTime() {
        appVm.listenerTime()
    }

    val splashFragment: SplashFragment = SplashFragment()

    private fun showSplash() {
        supportFragmentManager.beginTransaction()
            .add(R.id.cl_root, splashFragment, SplashFragment.TAG)
            .show(splashFragment).commitAllowingStateLoss()
    }

    private fun hideSplash() {
        supportFragmentManager.findFragmentByTag(SplashFragment.TAG)?.let {
            supportFragmentManager.beginTransaction().hide(it).commitAllowingStateLoss()
        }
    }

//    /**
//     * 初始化导航栏
//     */
//    private fun initNav() {
//        i("initNav")
//        vd.vp.adapter = MainViewPagerAdapter(this)
//        vd.vp.isUserInputEnabled = false
//        vd.vp.offscreenPageLimit = 6
//        vd.rnv.setResIds(
//            R.drawable.icon_shutdown,
//            vm.navItems,
//            R.drawable.icon_logo
//        )
//        vd.rnv.setNavigationSelectedIndexChangeListener { it ->
//            vm.curIndex.value = it
//            i("nav it=$it")
//        }
//        vd.tnv.setShutdownListener {
//            showShutdownDialog()
//        }
//        vm.curIndex.observe(this) {
//            vd.vp.setCurrentItem(it, false)
//        }
//    }


    /**
     * 关机提示
     */
    private fun showShutdownDialog() {
        if (!appVm.testState.isRunning()) {
            shutdownDialog.showPop(this) {
                it.showDialog(
                    "确定要关机吗？请确定仪器检测结束。",
                    "关机",
                    { shutdown() },
                    "取消",
                    { it.dismiss() })
            }
            return
        } else {
            shutdownDialog.showPop(this) {
                it.showDialog("检测过程中不能关机", "我知道了", { it.dismiss() })
            }
        }
    }

    val shutdownDialog by lazy {
        HiltDialog(this)
    }

    fun shutdown() {
        shutdownDialog.showPop(this, isCancelable = false) {
            it.showDialog("即将关机，请等待……", "我知道了", { it.dismiss() })
        }
        lifecycleScope.launch {
            delay(3000)
            vm.shutdown()
        }
    }

    private fun test() {

    }

    /**
     * 监听所有外设
     */
    private fun listener() {
        listenerSDCard()
        listenerView()

    }

    private fun listenerView() {
        lifecycleScope.launch {
            appVm.nowTimeStr.collectLatest {
                vd.tnv.setTime(it)
            }
        }
        lifecycleScope.launch {
            appVm.machineState.collectLatest {
                vd.tnv.setStateMachineSrc(it.id)
                if (it == MachineState.MachineRunningError) {
                    vm.allowRunning()
                }
            }
        }
        lifecycleScope.launch {
            appVm.uploadState.collectLatest {
                vd.tnv.setStateUploadSrc(it.id)
            }
        }
        lifecycleScope.launch {
            appVm.storageState.collectLatest {
                vd.tnv.setStateStorageSrc(it.id)
            }
        }

    }

    /**
     * 初始化 u盘
     */
    private fun listenerSDCard() {
        Log.d(TAG, "listenerSDCard 1")
        if (mPermissionIntent == null) {
//            //其他设备权限的广播
            mPermissionIntent =
                PendingIntent.getBroadcast(this, 3210, Intent(StorageUtil.ACTION_USB_PERMISSION), 0)
            val filter = IntentFilter(StorageUtil.ACTION_USB_PERMISSION)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            registerReceiver(mUsbReceiver, filter)
            //u盘插拔的广播
            val usbFilter = IntentFilter()
            usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED)
            usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
            //            usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            usbFilter.addDataScheme("file")
            registerReceiver(usbFlashDiskReceiver, usbFilter)
        }
        Log.d(TAG, "listenerSDCard 2")

        ktxRunOnBgCache {
            StorageUtil.startInit(this) { allow: Boolean ->
                Log.d(TAG, "listenerSDCard allow=$allow")
                if (!allow) {
                    showOpenDocumentTree()
                } else {
                    changeStorageState(StorageState.EXIST)
                }
                null
            }
        }
        Log.d(TAG, "listenerSDCard 3")

    }

    /**
     * u盘挂载 移除
     */
    private val usbFlashDiskReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val path = intent.data!!.path
            Log.d(TAG, "action=$action path=$path")
            if (action == Intent.ACTION_MEDIA_REMOVED) {
                Log.d(TAG, "u盘 已移除action=\$action")
                changeStorageState(StorageState.NONE)
                //  snack(viewDataBinding.root,"u盘已移除")
            } else if (action == Intent.ACTION_MEDIA_MOUNTED) {
                Log.d(TAG, "u盘 已挂载action=\$action")
                ktxRunOnBgCache {
                    StorageUtil.startInit(context) { allow: Boolean? ->
                        if (!allow!!) {
                            showOpenDocumentTree()
                        } else {
                            changeStorageState(StorageState.EXIST)
                        }
                        null
                    }
                }
            }
        }
    }

    fun changeStorageState(state: StorageState?) {
        StorageUtil.state = state!!
        i("U盘状态=${StorageUtil.state.stateName}")

//        if (state != StorageState.NONE) {
//            vd.rnv.setUpanResId(if (state.isExist()) R.drawable.upan_enable_true else R.drawable.upan_enable_false)
//        } else {
//            vd.rnv.setUpanResId(0)
//        }
        appVm.changeStorageState(state)
//        vd.tvState!!.text = "U盘状态:" + StorageUtil.state.stateName
    }

    private fun initUploadClient() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(3000)
            withContext(Dispatchers.Main) {
                if (HL7Helper.getConfig().openUpload) {
                    HL7Helper.connect(object : OnConnectListener {
                        override fun onConnectResult(connectResult: ConnectResult) {
                            i("onConnectResult connectResult=$connectResult")
                        }

                        override fun onConnectStatusChange(connectStatus: ConnectStatus) {
                            i("onConnectStatusChange connectStatus=$connectStatus")
                            appVm.changeUploadState(connectStatus)
                        }
                    })
                }else{
                    appVm.changeUploadState(ConnectStatus.NONE)
                }
            }
        }
    }

    /**
     * u盘插拔
     */
    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "action=$action")
            if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Log.d(TAG, "设备 已拔出action=$action")
                StorageUtil.remove()
                changeStorageState(StorageState.NONE)
            } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                Log.d(TAG, "设备 已插入action=$action")
                val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                Log.d(TAG, "设备 已插入device=${device} ${device?.productId} ${device?.vendorId}")
                changeStorageState(StorageState.INSERTED)
            }
        }
    }

    override fun onMessageEvent(event: EventMsg<Any>) {
        super.onMessageEvent(event)
        if (event.what == EventGlobal.WHAT_HIDE_SPLASH) {
            hideSplash()
        }else if (event.what == EventGlobal.WHAT_UPLOAD_CHANGE) {
            initUploadClient()
        }
    }
}
