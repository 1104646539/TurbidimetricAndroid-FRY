package com.wl.turbidimetric

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import com.wl.turbidimetric.databinding.ActivityMainBinding
import com.wl.turbidimetric.ex.put
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.util.DocumentsUtils
import com.wl.wllib.*
import com.wl.wwanandroid.base.BaseActivity
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.Date
import java.util.Random

class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {
    val TAG = "MainActivity"
    override val viewDataBinding: ActivityMainBinding by ActivityDataBindingDelegate(R.layout.activity_main)
    override val viewModel: MainViewModel by viewModels()

    private val vb: ActivityMainBinding by ActivityDataBindingDelegate(R.layout.activity_main)

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
        if (DocumentsUtils.checkWritableRootPath(
                this,
                SystemGlobal.uPath
            )
        ) {   //检查sd卡/u盘路径是否有 权限 没有显示dialog
            showOpenDocumentTree()
        } else {
            //有权限

            val root = File(SystemGlobal.uPath)
            for (f in root.list()) {
                Log.d(TAG, "f=${f}")
            }

        }


    }

    private fun showOpenDocumentTree() {
        Log.e("showOpenDocumentTree", "start check sd card...")
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = getSystemService(
                StorageManager::class.java
            )
            val volume: StorageVolume? = sm.getStorageVolume(File(SystemGlobal.uPath))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DocumentsUtils.OPEN_DOCUMENT_TREE_CODE -> if (data != null && data.data != null) {
                val uri = data.data
                DocumentsUtils.saveTreeUri(this, SystemGlobal.uPath, uri)
                Log.i(
                    ContentValues.TAG,
                    "DocumentsUtils.OPEN_DOCUMENT_TREE_CODE ： $uri"
                )
            }
        }
        super.onActivityResult(requestCode, resultCode, data)


        //授权一次后重启开机不用再次授权
        if (resultCode != Activity.RESULT_OK) return
        val treeUri = data!!.data
        val pickedDir = DocumentFile.fromTreeUri(
            this,
            treeUri!!
        )
        grantUriPermission(
            packageName,
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val YourAudioFile = pickedDir!!.findFile("YourAudioFileNameGoesHere")
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

        vb.rnv.setResIds(viewModel.ids.value!!.toIntArray())
        vb.rnv.setRightNavigationSelectedIndexChangeListener { it ->
            viewModel.curIndex.value = it
        }
        viewModel.curIndex.observe(this) { it ->
            if (it == viewModel.prevIndex.value) {
                return@observe
            }
            Log.d(
                TAG,
                "it=${it} viewModel.prevIndex=${viewModel.prevIndex.value}"
            )
            val fbt = supportFragmentManager.beginTransaction();
            val curF = viewModel.fragments.value!![it]
            viewModel.prevIndex.value?.let {
                if (it >= 0) {
                    val lastF = viewModel.fragments.value!![viewModel.prevIndex.value!!]
                    //隐藏以前的
                    fbt.hide(lastF)
                }
            }
            //恢复现在的，如果没添加过就添加
            if (!curF.isAdded) {
                fbt.add(R.id.fl_content, curF).show(curF)
            } else {
                fbt.show(curF)
            }
            viewModel.prevIndex.value = it
            fbt.commitAllowingStateLoss()
        }

//        test()
    }

    private fun test() {

        for (i in 1..10) {
            val project = ProjectModel().apply {
                a1 = 0.1
                a2 = 0.2
                x0 = 0.3
                p = 0.4
                createTime = Date().time
            }
            project.put()
        }
    }

    private fun getAllDeviceRegister() {
        val usbDevices: HashMap<String, UsbDevice> = usbManager.getDeviceList()
        val iterator: Iterator<String> = usbDevices.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val usbDevice = usbDevices[key]
            LogToFile.d(TAG, "getAllDeviceRegister =" + usbDevice.toString())
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
            SystemGlobal.uPath = intent.data?.path
            Log.d(TAG, "action=$action SystemGlobal.uPath=${SystemGlobal.uPath}")
            Log.d(TAG, "SystemGlobal.uPath=${SystemGlobal.uPath}")
            if (action.equals(Intent.ACTION_MEDIA_REMOVED)) {
                Log.d(TAG, "u盘 已移除action=$action")

            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Log.d(TAG, "u盘 已插入action=$action")
                mHandler.sendEmptyMessage(handler_init_upan)
            }
        }
    }
    val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action
            Log.d(TAG, "action=$action")

            if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Log.d(TAG, "设备 拔出 action=$action")
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                Log.d(TAG, "设备 插入 action=$action")
            } else if (action.equals(ACTION_USB_PERMISSION)) {
                Log.d(TAG, "usb action=$action")
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

}
