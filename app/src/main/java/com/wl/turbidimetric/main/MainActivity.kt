package com.wl.turbidimetric.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout.LayoutParams
import androidx.activity.viewModels
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import com.lxj.xpopup.XPopup
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppIntent
import com.wl.turbidimetric.app.MachineState
import com.wl.turbidimetric.app.PrinterState
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.databinding.ActivityMainBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.main.splash.SplashFragment
import com.wl.turbidimetric.report.PrintSDKHelper
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.view.CustomBubbleAttachPopup
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.isShow
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.weiqianwllib.OrderUtil
import com.wl.weiqianwllib.upan.StorageState
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.weiqianwllib.upan.StorageUtil.OPEN_DOCUMENT_TREE_CODE
import com.wl.wllib.LogToFile.i
import com.wl.wllib.ktxRunOnBgCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus


class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {
    companion object {
        private const val TAG = "MainActivity"

        /**
         * 打印机初始化
         */
        private const val HANDLER_INIT_QRCODE = 1000

        /**
         * 选择打印机后返回的
         */
        private const val PRINTER_SELECTION_REQUEST_CODE = 12345

        /**
         * u盘设备插入监听
         */
        private const val USB_PERMISSION_REQUEST_CODE = 3210
    }

    override val vd: ActivityMainBinding by ActivityDataBindingDelegate(R.layout.activity_main)
    override val vm: MainViewModel by viewModels { MainViewModelFactory() }
    private var mPermissionIntent: PendingIntent? = null
    private val splashFragment: SplashFragment = SplashFragment()
    private val mCurrentPosition = FloatArray(2)

    private val shutdownDialog by lazy {
        HiltDialog(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        i("onCreate savedInstanceState=${savedInstanceState}")
        App.instance?.serialPort?.open(lifecycleScope)
        App.instance?.thermalPrintUtil?.open(lifecycleScope)
        App.instance?.printHelper?.open(lifecycleScope)
        App.instance?.scanCodeUtil?.open(lifecycleScope)
        setTheme(R.style.Theme_Mvvmdemo) //恢复原有的样式
        super.onCreate(savedInstanceState)
    }


    private val mHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            HANDLER_INIT_QRCODE -> initQrCode()
        }
        true
    }

    private fun sendShowOpenDocumentTreeIntent() {
        vm.processIntent(MainIntent.ShowOpenDocumentTree)
    }

    private fun showOpenDocumentTree() {
        StorageUtil.showOpenDocumentTree(this, OPEN_DOCUMENT_TREE_CODE)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            TAG,
            "onActivityResult data==null ${data == null} resultCode=$resultCode requestCode=$requestCode"
        )
        if (requestCode == OPEN_DOCUMENT_TREE_CODE) {
            val uri = data?.data
            if (uri != null && resultCode == RESULT_OK) {
                StorageUtil.saveTreeUri(this, StorageUtil.curPath!!, uri)
                Log.d(TAG, "onActivityResult path=${StorageUtil.curPath}")
                changeStorageState(StorageState.EXIST)
            } else {
                changeStorageState(StorageState.UNAUTHORIZED)
                Log.d(TAG, "onActivityResult data==null ${data == null}")
            }
        }

        supportFragmentManager.fragments.forEach {
            it.onActivityResult(
                requestCode,
                resultCode,
                data
            )
        }

        if (resultCode == Activity.RESULT_FIRST_USER && requestCode == PRINTER_SELECTION_REQUEST_CODE) {
            PrintSDKHelper.initRecentPrinters()
        }

        if (resultCode == RESULT_OK && data?.data != null) {
            handleUriPermission(data.data!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        ScanCodeUtil.onScanResult = null
        appVm.scanCodeUtil.close()
        App.instance?.serialPort?.close()
//        App.instance?.thermalPrintUtil?.close()
        unregisterReceiver(usbFlashDiskReceiver)
        unregisterReceiver(mUsbReceiver)
        appVm.printHelper.onSizeChange = null
        PrintSDKHelper.close()
        HL7Helper.setOnConnectListener2(null)
        HL7Helper.disconnect()
    }

    private fun handleUriPermission(treeUri: Uri) {
        try {
            grantUriPermission(
                packageName,
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Log.i(TAG, "onActivityResult Granted URI permission for: $treeUri")
        } catch (e: Exception) {
            Log.e(TAG, "Exception granting URI permission: ${e.stackTraceToString()}")
        }
    }

    private fun initQrCode() {
        EventBus.getDefault().post(EventMsg<Any>(what = EventGlobal.WHAT_INIT_QRCODE))
    }

    override fun init() {
        initShutDown()
        showSplash()
        listener()
        initNavigation()
//        test()
        initTime()
        initUploadClient()
        initPrintSDK()
//        OrderUtil.showHideNav(this, false)
    }

    private fun showAnimStart() {
        val anim = ViewAnimationUtils.createCircularReveal(
            vd.ivStart,
            -100,
            -100,
            (vd.rlRoot.width * 1.5).toFloat(),
            0.0.toFloat()
        )
        vd.ivStart.visibility = View.VISIBLE
        anim.duration = 800
        anim.interpolator = LinearInterpolator()
        //监听动画结束，进行回调
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                vd.ivStart.visibility = View.GONE
            }
        })

        anim.start()
    }

    private fun initPrintSDK() {
        PrintSDKHelper.printerStateChange = { state ->
            appVm.processIntent(AppIntent.PrinterStateChange(state))
        }
        appVm.printHelper.onSizeChange = { size ->
            appVm.processIntent(AppIntent.PrintNumChange(size))
        }
        PrintSDKHelper.init(this)
    }

    private fun initNavigation() {
        vd.vp.adapter = MainViewPagerAdapter(this)
        vd.vp.isUserInputEnabled = false
        vd.vp.offscreenPageLimit = 6
        vd.lnv.setItem(
            SystemGlobal.navItems,
            R.drawable.left_nav_item_bg,
            R.drawable.left_nav_item_bg2
        )
        vd.lnv.onItemChangeListener = {
            vm.processIntent(MainIntent.ChangeNavCurIndex(it))
        }

    }

    private fun initShutDown() {
        vd.tnv.setShutdownListener {
            showShutdownDialog()
        }
    }

    /**
     * 显示一个气泡popup在view的下方，该view只显示文字提示
     * @param attachView View?
     * @param content String
     * @param duration Long
     */
    private fun showPopupView(attachView: View?, content: String, duration: Long = 3000) {
        if (attachView == null) return
        val contentView = CustomBubbleAttachPopup(this, content)
        XPopup.Builder(this)
            .hasShadowBg(true)
            .isTouchThrough(true)
            .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
            .atView(attachView)
            .isCenterHorizontal(true)
            .hasShadowBg(false) // 去掉半透明背景
            .asCustom(contentView)
            .show()
            .also {
                mHandler.postDelayed({ it.dismiss() }, duration)
            }
    }

    private fun initTime() {
        vm.processIntent(MainIntent.ListenerTime)
    }


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

    /**
     * 关机提示
     */
    private fun showShutdownDialog() {
        if (!appVm.testState.isRunning()) {
            shutdownDialog.showPop(this, isCancelable = false) { dialog ->
                dialog.showDialog(
                    "确定要关机吗？请确定仪器检测结束。",
                    "关机",
                    {
                        shutdown()
                        dialog.showDialog(
                            "正在执行关机动作，请稍后……",
                        )

                    },
                    "取消",
                    { it.dismiss() })
            }
        } else {
            shutdownDialog.showPop(this) { dialog ->
                dialog.showDialog("检测过程中不能关机", "我知道了", { it.dismiss() })
            }
        }
    }


    /**
     * 显示关机动画
     */
    private fun showShutdownView(onAnimFinish: () -> Unit) {
        val view = View(this)
        view.setBackgroundColor(Color.BLACK)
        val lp = LayoutParams(vd.rlRoot.width, vd.rlRoot.height)
        view.alpha = 0f
        vd.rlRoot.addView(view, lp)
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            addListener(onEnd = {
                i("结束")
                onAnimFinish.invoke()
            })
        }.setDuration(2000).start()
    }


    private fun shutdown() {
        vm.processIntent(MainIntent.ShutDown)
    }

    private fun test() {

    }

    /**
     * 监听所有外设
     */
    private fun listener() {
        listenerUpan()
        listenerView()

    }

    private fun listenerView() {
        lifecycleScope.launchWhenStarted {
            launch {
                appVm.nowTimeStr.collectLatest {
                    vd.tnv.setTime(it)
                }
            }
            launch {
                appVm.machineState.collectLatest {
                    vd.tnv.setStateMachineSrc(it.id)
                    if (it == MachineState.MachineRunningError) {
                        vm.processIntent(MainIntent.AllowRunning)
                    }
                }
            }
            launch {
                appVm.uploadState.collectLatest {
                    vd.tnv.setStateUploadSrc(it.id)
                }
            }
            launch {
                appVm.storageState.collectLatest {
                    vd.tnv.setStateStorageSrc(it.id)
                    if (it != com.wl.turbidimetric.app.StorageState.None) {
                        vm.processIntent(MainIntent.ShowPopupViewForStorageState)
                    }
                }
            }
            launch {
                appVm.printerState.collectLatest {
                    vd.tnv.setStatePrinterSrc(it.id)
                    vd.tnv.setPrintNumVisibility((it == PrinterState.Success).isShow())
                }
            }
            launch {
                appVm.printNum.collectLatest {
                    vd.tnv.setPrintNum(it)
                }
            }
            launch {
                vm.uiState.collectLatest {
                    when (it) {
                        MainState.None -> {}
                        MainState.ShowOpenDocumentTree -> {
                            showOpenDocumentTree()
                        }

                        is MainState.ShowPopupViewForUploadState -> {
                            showPopupView(vd.tnv.getStateUpload(), it.uploadState.str)
                        }

                        is MainState.ShowPopupViewForStorageState -> {
                            showPopupView(vd.tnv.getStateStorage(), it.storageState.str)
                        }

                        is MainState.ShowPopupViewForMachineState -> {
                            showPopupView(vd.tnv.getStateMachine(), it.machineState.str)
                        }

                        is MainState.ShowPopupViewForPrinterState -> {
                            showPopupView(
                                vd.tnv.getStatePrinter(),
                                "${it.printerState.str}\n还有${it.printNum}个打印任务正在等待"
                            )
                        }
                    }
                }
            }
            launch {
                vm.curIndex.collectLatest {
                    i("curIndex=$it")
                    vd.vp.setCurrentItem(it, false)
                    vd.lnv.selectIndexChange(it)
                }
            }
        }

        vd.tnv.getStateUpload()?.setOnClickListener {
            vm.processIntent(MainIntent.ShowPopupViewForUploadState)
        }
        vd.tnv.getStateMachine()?.setOnClickListener {
            vm.processIntent(MainIntent.ShowPopupViewForMachineState)
        }
        vd.tnv.getStateStorage()?.setOnClickListener {
            vm.processIntent(MainIntent.ShowPopupViewForStorageState)
        }
        vd.tnv.getStatePrinter()?.setOnClickListener {
            vm.processIntent(MainIntent.ShowPopupViewForPrinterState)
        }
    }

    private fun getTopPrint(): View? {
        return vd.tnv.getStatePrinter()
    }


    /**
     * 添加打印任务动画
     */
    fun addPrintWorkAnim(params: PrintAnimParams) {
        val targetView = getTopPrint() ?: return

        val animatedIcon = ImageView(this).apply {
            setImageResource(R.drawable.icon_report)
            layoutParams = LinearLayout.LayoutParams(40, 40)
        }
        vd.rlRoot.addView(animatedIcon)

        val parentLocation = IntArray(2)
        vd.rlRoot.getLocationInWindow(parentLocation)


        val endLoc = IntArray(2)
        targetView.getLocationInWindow(endLoc)

        val startX = (params.formX - parentLocation[0] + params.formWidth / 2).toFloat()
        val startY = (params.formY - parentLocation[1]).toFloat()

        val toX = (endLoc[0] - parentLocation[0] + targetView.width / 5).toFloat()
        val toY = (endLoc[1] - parentLocation[1]).toFloat()

        val path = Path().apply {
            moveTo(startX, startY)
            quadTo((startX + toX) / 2, startY, toX, toY)
        }
        val pathMeasure = PathMeasure(path, false)

        val valueAnimator = ValueAnimator.ofFloat(0f, pathMeasure.length).apply {
            duration = 600
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                val currentPosition = FloatArray(2)
                pathMeasure.getPosTan(value, currentPosition, null)
                animatedIcon.translationX = currentPosition[0]
                animatedIcon.translationY = currentPosition[1]
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    vd.rlRoot.removeView(animatedIcon)
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        valueAnimator.start()
    }

    /**
     * 初始化 u盘
     */
    private fun listenerUpan() {
        if (mPermissionIntent == null) {
//            //其他设备权限的广播
            mPermissionIntent = PendingIntent.getBroadcast(
                App.instance,
                USB_PERMISSION_REQUEST_CODE,
                Intent(StorageUtil.ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            val filter = IntentFilter(StorageUtil.ACTION_USB_PERMISSION).apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            registerReceiver(mUsbReceiver, filter)
            //u盘插拔的广播
            val usbFilter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addDataScheme("file")
            }
            registerReceiver(usbFlashDiskReceiver, usbFilter)
        }

        lifecycleScope.launchWhenCreated {
            StorageUtil.startInit(App.instance!!) { allow: Boolean ->
                Log.d(TAG, "listenerSDCard allow=$allow")
                if (!allow) {
                    sendShowOpenDocumentTreeIntent()
                } else {
                    changeStorageState(StorageState.EXIST)
                }
            }
        }
    }

    /**
     * u盘挂载 移除
     */
    private val usbFlashDiskReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val path = intent.data!!.path
            i(TAG, "action=$action path=$path")
            if (action == Intent.ACTION_MEDIA_REMOVED) {
                i(TAG, "u盘 已移除action=\$action")
                changeStorageState(StorageState.NONE)
            } else if (action == Intent.ACTION_MEDIA_MOUNTED) {
                i(TAG, "u盘 已挂载action=\$action")
                ktxRunOnBgCache {
                    StorageUtil.startInit(context) { allow: Boolean? ->
                        if (!allow!!) {
                            sendShowOpenDocumentTreeIntent()
                        } else {
                            changeStorageState(StorageState.EXIST)
                        }
                    }
                }
            }
        }
    }

    fun changeStorageState(state: StorageState) {
        StorageUtil.state = state
        vm.processIntent(MainIntent.ChangeStorageState(state))
    }

    private fun initUploadClient() {
        lifecycleScope.launchWhenCreated {
            delay(10000)
            withContext(Dispatchers.Main) {
                if (HL7Helper.getConfig().openUpload) {
                    HL7Helper.connect(object : OnConnectListener {
                        override fun onConnectResult(connectResult: ConnectResult) {
                            i("onConnectResult connectResult=$connectResult")
                            if (connectResult is ConnectResult.AlreadyConnected) {
                                changeUploadState(ConnectStatus.CONNECTED)
                            }
                        }

                        override fun onConnectStatusChange(connectStatus: ConnectStatus) {
                            i("onConnectStatusChange connectStatus=$connectStatus")
                            changeUploadState(connectStatus)
                            SystemGlobal.connectStatus = connectStatus
                        }
                    })
                } else {
                    changeUploadState(ConnectStatus.NONE)
                }
            }
        }
    }

    private fun changeUploadState(connected: ConnectStatus) {
        appVm.processIntent(AppIntent.UploadStateChange(connected))
    }

    /**
     * u盘插拔
     */
    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            i(TAG, "action=$action")
            if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                i(TAG, "设备 已拔出action=$action")
                StorageUtil.remove()
                changeStorageState(StorageState.NONE)
            } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                i(TAG, "设备 已插入device=${device} ${device?.productId} ${device?.vendorId}")
                changeStorageState(StorageState.INSERTED)
            }
        }
    }

    data class PrintAnimParams(val formX: Int, val formY: Int, val formWidth: Int)

    override fun onMessageEvent(event: EventMsg<Any>) {
        super.onMessageEvent(event)
        when (event.what) {
            EventGlobal.WHAT_HIDE_SPLASH -> {//自检结束
                hideSplash()
                initShutDown()
            }

            EventGlobal.WHAT_UPLOAD_CHANGE -> {//上传状态变更
                initUploadClient()
            }

            EventGlobal.WHAT_HOME_INIT_FINISH -> {//首屏加载完毕，执行显示主页面动画
                showAnimStart()
            }

            EventGlobal.WHAT_HOME_ADD_PRINT_ANIM -> {//首屏加载完毕，执行显示主页面动画
                if (event.data is PrintAnimParams) {
                    addPrintWorkAnim(event.data)
                }
            }
        }
    }
}
