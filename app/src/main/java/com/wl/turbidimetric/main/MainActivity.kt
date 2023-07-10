package com.wl.turbidimetric.main

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.res.Configuration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.MainViewModel
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ActivityMainBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.MatchingArgState
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.util.DocumentsUtils
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.turbidimetric.util.StorageUtil
import com.wl.turbidimetric.view.HiltDialog
import com.wl.wllib.QRCodeUsbHid
import com.wl.wllib.QRCodeUtil
import com.wl.wllib.UsbFlashDiskUtil
import com.wl.wllib.UsbToSerialPortUtil
import com.wl.wwanandroid.base.BaseActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets

class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {
    val TAG = "MainActivity"
    override val vd: ActivityMainBinding by ActivityDataBindingDelegate(R.layout.activity_main)
    override val vm: MainViewModel by viewModels()

//    private val vb: ActivityMainBinding by ActivityDataBindingDelegate(R.layout.activity_main)

    var mPermissionIntent: PendingIntent? = null
    private lateinit var usbManager: UsbManager

    /**
     * usb权限广播
     */
    val ACTION_USB_PERMISSION = "com.wl.USB_PERMISSION"

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
                    initUpan()
                }
            }
        }
    }

    private fun initUpan() {
//        if (DocumentsUtils.checkWritableRootPath(
//                this,
//                SystemGlobal.uPath
//            )
//        ) {   //检查sd卡/u盘路径是否有 权限 没有显示dialog
//            showOpenDocumentTree()
//        } else {
//            //有权限
//            val root = File(SystemGlobal.uPath)
//            for (f in root.list()) {
//                Timber.d( "f=${f}")
//            }
//        }

    }

    private fun showOpenDocumentTree() {
        Log.e("showOpenDocumentTree", "start check sd card...")
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = getSystemService(
                StorageManager::class.java
            )
            val volume: StorageVolume? = sm.getStorageVolume(File(StorageUtil.curPath))
            if (volume != null) {
                intent = volume.createAccessIntent(null)
            }
        }
        Log.e("showOpenDocumentTree", "intent=$intent")
        if (intent == null) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        startActivityForResult(intent, DocumentsUtils.OPEN_DOCUMENT_TREE_CODE)
    }

    override fun onResume() {
        super.onResume()
        //授权后重新获取

        val context: Context = this
        DocumentsUtils.`as` = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(StorageUtil.curPath, null)

    }

    override fun onBackPressed() {
//        super.onBackPressed()
        moveTaskToBack(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DocumentsUtils.OPEN_DOCUMENT_TREE_CODE -> if (data != null && data.data != null) {
                val uri = data.data
                DocumentsUtils.saveTreeUri(this, StorageUtil.curPath, uri)
                StorageUtil.startInit(this) {
                    Timber.d("onActivityResult=$it")
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)


//        //授权一次后重启开机不用再次授权
//        if (resultCode != Activity.RESULT_OK) return
//        val treeUri = data!!.data
//        val pickedDir = DocumentFile.fromTreeUri(
//            this,
//            treeUri!!
//        )
//        grantUriPermission(
//            packageName,
//            treeUri,
//            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//        )
//        contentResolver.takePersistableUriPermission(
//            treeUri,
//            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//        )
//        val YourAudioFile = pickedDir!!.findFile("YourAudioFileNameGoesHere")
    }

    private fun initQrCode() {
        SystemGlobal.qrCode = QRCodeUtil(this)

        EventBus.getDefault().post(EventMsg<Any>(what = EventGlobal.WHAT_INIT_QRCODE))

    }

    override fun init() {
        supportActionBar?.hide()
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        listener()
        getAllDeviceRegister()
        StorageUtil.startInit(this) {
            showOpenDocumentTree()
        }
        initNav()


        test()
    }

    /**
     * 初始化导航栏
     */
    private fun initNav() {

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
            Timber.d("nav it=$it")
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
        if ((TestState.None == SystemGlobal.testState || TestState.None == SystemGlobal.testState)
            && (MatchingArgState.None == SystemGlobal.matchingTestState || MatchingArgState.Finish == SystemGlobal.matchingTestState)
        ) {

            shutdownDialog.show("确定要关机吗？请确定仪器检测结束。", "关机", { shutdown() }, "取消", { it.dismiss() })
            return
        } else {
            shutdownDialog.show("检测过程中不能关机", "我知道了", { it.dismiss() })
        }

    }

    val shutdownDialog by lazy {
        HiltDialog(this)
    }

    fun shutdown() {
        shutdownDialog.show("即将关机，请等待……", "我知道了", { it.dismiss() })

        lifecycleScope.launch {
            delay(3000)
            SerialPortUtil.Instance.shutdown()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.d("onConfigurationChanged")
    }


    private fun test() {

    }

    private fun getAllDeviceRegister() {
        val usbDevices: HashMap<String, UsbDevice> = usbManager.deviceList
        val iterator: Iterator<String> = usbDevices.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val usbDevice = usbDevices[key]
            Timber.d("getAllDeviceRegister =" + usbDevice.toString())
            if (usbDevice!!.vendorId == UsbToSerialPortUtil.VENDOR_PL2303) {
                getPermission(usbDevice)
            } else if (usbDevice!!.vendorId == UsbToSerialPortUtil.VENDOR_FT) {
                getPermission(usbDevice)
            } else if (usbDevice!!.vendorId == QRCodeUsbHid.VendorID) {
                getPermission(usbDevice)
            } else if (UsbFlashDiskUtil.isUSBFlashDisk(usbDevice)) {
                getPermission(usbDevice)
            }
        }
    }

    private fun getPermission(usbDevice: UsbDevice) {
        usbManager.requestPermission(usbDevice, mPermissionIntent)
    }

    private fun listener() {
        listenerSDCard()

    }

    private fun listenerSDCard() {
        if (mPermissionIntent == null) {
            //其他设备权限的广播
            mPermissionIntent =
                PendingIntent.getBroadcast(this, 3210, Intent(ACTION_USB_PERMISSION), 0)
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            registerReceiver(mUsbReceiver, filter)
            //其他设备，二维码设备插拔的广播
//            IntentFilter filter2 = new IntentFilter();
//            filter2.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//            filter2.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
//            mActivity.registerReceiver(mUsbReceiver2, filter2);
            //u盘插拔的广播
            val usbFilter = IntentFilter()
            usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED)
            usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
            //            usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            usbFilter.addDataScheme("file")
            registerReceiver(usbFlashDiskReceiver, usbFilter)
        }
    }

    val usbFlashDiskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action
            val path = intent.data?.path
            StorageUtil.startInit(context!!) {
                showOpenDocumentTree()
            }

//            var temp =DocumentFile.fromTreeUri(context!!,Uri.parse(path!!))
//            Timber.d( "temp=$temp path=${path}")
//            SystemGlobal.uPath = path
//            StorageUtil.setCurPath(path)
            Timber.d("action=$action path=${path}")

            if (action.equals(Intent.ACTION_MEDIA_REMOVED)) {
                Timber.d("u盘 已移除action=$action")
                //  snack(viewDataBinding.root,"u盘已移除")
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Timber.d("u盘 已插入action=$action")
                mHandler.sendEmptyMessage(handler_init_upan)
                // snack(viewDataBinding.root,"u盘已插入")
            }
        }
    }
    val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action
            Timber.d("action=$action")

            if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Timber.d(TAG, "设备 拔出 action=$action")
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                Timber.d("设备 插入 action=$action")
            } else if (action.equals(ACTION_USB_PERMISSION)) {
                Timber.d("usb action=$action")
                val device =
                    intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (device != null) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device.vendorId == UsbToSerialPortUtil.VENDOR_PL2303) {
                            mHandler.sendEmptyMessage(handler_init_qrcode)
                        }
                    }
                }
            }
        }
    }

    /**
     * 通过反射调用获取内置存储和外置sd卡根路径(通用)
     *
     * @param mContext    上下文
     * @param is_removale 是否可移除，false返回内部存储路径，true返回外置SD卡路径
     * @return
     */
    fun getStoragePath(mContext: Context, is_removale: Boolean): String? {
        var path: String? = null
        //使用getSystemService(String)检索一个StorageManager用于访问系统存储功能。
        val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var storageVolumeClazz: Class<*>? = null
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList: Method = mStorageManager.javaClass.getMethod("getVolumeList")
            val getPath: Method = storageVolumeClazz.getMethod("getPath")
            val isRemovable: Method = storageVolumeClazz.getMethod("isRemovable")
            val result: Array<StorageVolume> =
                getVolumeList.invoke(mStorageManager)!! as Array<StorageVolume>
            for (i in result.indices) {
                val storageVolumeElement: Any = result[i]
                path = getPath.invoke(storageVolumeElement) as String
                val removable = isRemovable.invoke(storageVolumeElement) as Boolean
                Timber.d("path=$path")
                if (is_removale == removable) {
                    return path
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return path
    }


}
