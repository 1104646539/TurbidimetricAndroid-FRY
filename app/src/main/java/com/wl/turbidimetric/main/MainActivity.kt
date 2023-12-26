package com.wl.turbidimetric.main

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ActivityMainBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.weiqianwllib.upan.DocumentsUtils
import com.wl.weiqianwllib.upan.StorageState
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.weiqianwllib.upan.StorageUtil.OPEN_DOCUMENT_TREE_CODE
import com.wl.wllib.LogToFile.i
import com.wl.wllib.ktxRunOnBgCache
import com.wl.wwanandroid.base.BaseActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {
    val TAG = "MainActivity"
    override val vd: ActivityMainBinding by ActivityDataBindingDelegate(R.layout.activity_main)
    override val vm: MainViewModel by viewModels()

    var mPermissionIntent: PendingIntent? = null

    private val handler_init_qrcode = 1000
    private val handler_init_upan = 1010

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

    override fun onMessageEvent(event: EventMsg<Any>) {
        super.onMessageEvent(event)
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
    }

    private fun initQrCode() {
        EventBus.getDefault().post(EventMsg<Any>(what = EventGlobal.WHAT_INIT_QRCODE))
    }

    override fun init() {
        listener()
        initNav()
        test()
    }

    /**
     * 初始化导航栏
     */
    private fun initNav() {
        i("initNav")
        vd.vp.adapter = MainViewPagerAdapter(this)
        vd.vp.isUserInputEnabled = false
        vd.vp.offscreenPageLimit = 6
        vd.rnv.setResIds(
            R.drawable.icon_shutdown,
            vm.navItems,
            R.drawable.icon_logo
        )
        vd.rnv.setNavigationSelectedIndexChangeListener { it ->
            vm.curIndex.value = it
            i("nav it=$it")
        }
        vd.rnv.setNavigationShutdownListener {
//            toast("点击关机……")
            showShutdownDialog()
        }
        vm.curIndex.observe(this) {
            vd.vp.setCurrentItem(it, false)
        }
    }


    /**
     * 关机提示
     */
    private fun showShutdownDialog() {
        if (!SystemGlobal.testState.isRunning()) {
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
            SerialPortUtil.shutdown()
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
            Log.d(TAG, "action=\$action path=$path")
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

        if (state != StorageState.NONE) {
            vd.rnv.setUpanResId(if (state.isExist()) R.drawable.upan_enable_true else R.drawable.upan_enable_false)
        } else {
            vd.rnv.setUpanResId(0)
        }
//        vd.tvState!!.text = "U盘状态:" + StorageUtil.state.stateName
    }

    /**
     * u盘插拔
     */
    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "action=$action")
            if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Log.d(TAG, "设备 已拔出action=\$action")
                StorageUtil.remove()
                changeStorageState(StorageState.NONE)
            } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                Log.d(TAG, "设备 已插入action=\$action")
                changeStorageState(StorageState.INSERTED)
            }
        }
    }
}
