package com.wl.turbidimetric.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout.LayoutParams
import androidx.activity.viewModels
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lxj.xpopup.XPopup
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppIntent
import com.wl.turbidimetric.app.MachineState
import com.wl.turbidimetric.app.PrinterState
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.databinding.ActivityMainBinding
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.login.LoginFragment
import com.wl.turbidimetric.main.splash.SplashFragment
import com.wl.turbidimetric.report.PrintSDKHelper
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.view.CustomBubbleAttachLoginPopup
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
import kotlin.math.hypot
import kotlin.math.max


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
    private val mCurrentPosition = FloatArray(2)

    private val shutdownDialog by lazy {
        HiltDialog(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        i("onCreate savedInstanceState=${savedInstanceState}")
        App.instance?.serialPort?.open(lifecycleScope)
        App.instance?.thermalPrintUtil?.open(lifecycleScope)
        App.instance?.printHelper?.open(lifecycleScope, appVm.getReportIntervalTime())
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
//        initShutDown()
//        showSplash()
        listener()
        initNavigation()
//        test()
        initTime()
        initUploadClient()
        initPrintSDK()
        OrderUtil.showHideNav(this, false)
        showLoginUI(isFirstTime = true)
    }

    private fun showLoginUI(isFirstTime: Boolean = false) {
        if (isFirstTime) {
            // 第一次显示：直接添加Fragment并执行揭示动画
            vd.llRoot.visibility = View.INVISIBLE
            val loginFragment = LoginFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.rl_root, loginFragment, "login")
                .commitNow()

            loginFragment.view?.post {
                animateLoginFragmentReveal(loginFragment)
            }
        } else {
            // 切换用户：缩小揭示效果
            // 1. 生成底部主界面的快照
            val snapshot = captureRootSnapshot()
            if (snapshot != null) {
                vd.ivStart.setImageBitmap(snapshot)
            }

            // 2. 把快照显示在最顶层，并确保它在最前面
            vd.ivStart.visibility = View.VISIBLE
            vd.ivStart.bringToFront()

            // 3. 添加LoginFragment到底层（此时被快照遮住）
            val loginFragment = LoginFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.rl_root, loginFragment, "login")
                .commitNow()

            // 4. 隐藏最底部的主界面View
            vd.llRoot.visibility = View.INVISIBLE

            // 5. 快照以登录视图为圆心逐渐缩小（300ms），揭示登录页面
            loginFragment.view?.post {
                animateSnapshotShrinkReveal()
            }
        }
    }

    /**
     * 快照缩小揭示动画 - 以登录按钮为圆心逐渐缩小，揭示登录页面
     */
    private fun animateSnapshotShrinkReveal() {
        // 确保快照在最顶层
        vd.ivStart.bringToFront()

        // 获取登录按钮位置作为动画圆心
        val location = IntArray(2)
        val rootLocation = IntArray(2)
        vd.rlRoot.getLocationInWindow(rootLocation)

        val loginView = vd.tnv.getUserLogin()
        var centerX = vd.rlRoot.width / 2
        var centerY = vd.rlRoot.height / 2

        if (loginView != null) {
            loginView.getLocationInWindow(location)
            centerX = location[0] - rootLocation[0] + loginView.width / 2
            centerY = location[1] - rootLocation[1] + loginView.height / 2
        }

        // 计算起始半径（覆盖整个屏幕）
        val startRadius = hypot(
            max(centerX, vd.rlRoot.width - centerX).toDouble(),
            max(centerY, vd.rlRoot.height - centerY).toDouble()
        ).toFloat()

        // 创建圆形揭示动画（从大到小缩小）
        val anim = ViewAnimationUtils.createCircularReveal(
            vd.ivStart,
            centerX,
            centerY,
            startRadius,
            0f
        )

        anim.duration = 600
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 动画结束后清理快照
                vd.ivStart.visibility = View.GONE
                vd.ivStart.setImageBitmap(null)
            }
        })
        anim.start()
    }

    /**
     * 登录界面揭示动画 - 从屏幕正中间圆形放大显示
     */
    private fun animateLoginFragmentReveal(loginFragment: Fragment) {
        val loginRootView = loginFragment.view ?: return

        // 1. 捕获登录界面的快照
        val snapshot = captureViewSnapshot(loginRootView)
        if (snapshot != null) {
            vd.ivStart.setImageBitmap(snapshot)
        }

        // 2. 暂时隐藏登录Fragment的真实视图
        loginRootView.visibility = View.INVISIBLE

        // 3. 圆心固定为屏幕正中间
        val centerX = vd.rlRoot.width / 2
        val centerY = vd.rlRoot.height / 2

        // 4. 计算动画半径（从圆心到屏幕最远角的距离）
        val endRadius = hypot(
            max(centerX, vd.rlRoot.width - centerX).toDouble(),
            max(centerY, vd.rlRoot.height - centerY).toDouble()
        ).toFloat()

        // 5. 创建圆形揭示动画（从0开始放大）
        val anim = ViewAnimationUtils.createCircularReveal(
            vd.ivStart,
            centerX,
            centerY,
            0f,
            endRadius
        )

        vd.ivStart.visibility = View.VISIBLE
        anim.duration = 600
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 6. 动画结束后显示真正的登录界面并清理
                loginRootView.visibility = View.VISIBLE
                vd.ivStart.visibility = View.GONE
                vd.ivStart.setImageBitmap(null)
            }
        })
        anim.start()
    }

    /**
     * 捕获任意View的快照
     */
    private fun captureViewSnapshot(view: View): Bitmap? {
        if (view.width == 0 || view.height == 0) return null

        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private fun showAnimStart() {
        val anim = ViewAnimationUtils.createCircularReveal(
            vd.ivStart,
            -100,
            -100,
            (vd.rlRoot.width * 1.2).toFloat(),
            0.0.toFloat()
        )
        vd.ivStart.visibility = View.VISIBLE
        anim.duration = 800
        anim.interpolator = AccelerateDecelerateInterpolator()
        //监听动画结束，进行回调
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                vd.ivStart.visibility = View.GONE
            }
        })
        anim.startDelay = 600
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
            R.drawable.left_nav_selected_bg
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
    private fun showPopupView(attachView: View?, content: String, duration: Long = 5000) {
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

    /**
     * 显示一个气泡popup在view的下方
     * @param attachView View?
     * @param content String
     * @param duration Long
     */
    private fun showPopupLoginView(attachView: View?, content: String, duration: Long = 5000) {
        if (attachView == null) return
        val contentView = CustomBubbleAttachLoginPopup(this, content, {
            //切换用户
            if (appVm.testState.isRunning()) {
                toast("仪器正在运行")
                return@CustomBubbleAttachLoginPopup
            }
            showLoginUI()
        })
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
                appVm.userState.collectLatest {
                    vd.tnv.setUser("登录用户：${it?.userName}")
                }
                if (appVm.userModel?.isAdmin() == true) {
                    appVm.isDebugMode = true
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

                        is MainState.ShowPopupViewForLogin -> {
                            showPopupLoginView(
                                vd.tnv.getUserLogin(),
                                "${it.userModel?.showLevel()}"
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
            launch {


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
        vd.tnv.getUserLogin()?.setOnClickListener {
            vm.processIntent(MainIntent.ShowPopupViewForLogin)
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
//                hideSplash()
                initShutDown()
            }

            EventGlobal.WHAT_UPLOAD_CHANGE -> {//上传状态变更
                initUploadClient()
            }

            EventGlobal.WHAT_HOME_INIT_FINISH -> {//首屏加载完毕，执行显示主页面动画
//                showAnimStart()
            }

            EventGlobal.WHAT_HOME_ADD_PRINT_ANIM -> {//添加打印动画
                if (event.data is PrintAnimParams) {
                    addPrintWorkAnim(event.data)
                }
            }

            EventGlobal.WHAT_LOGIN_SUCCESS -> {//登录成功，初始化主页
                showMainUI()
            }
        }
    }

    private fun showMainUI() {
        val loginFragment = supportFragmentManager.findFragmentByTag("login") as? Fragment

        // 1. 确保llRoot可见并绘制完成，然后生成快照
        vd.llRoot.visibility = View.VISIBLE
        vd.llRoot.post {
            // 2. 生成llRoot的快照
            val snapshot = captureRootSnapshot()
            if (snapshot != null) {
                vd.ivStart.setImageBitmap(snapshot)
            }

            // 3. 隐藏llRoot，不让它显示
            vd.llRoot.visibility = View.INVISIBLE

            // 4. 同时执行：淡出登录界面(200ms) + 圆形揭示动画(800ms)
            animateLoginFragmentFadeOut(loginFragment)
            showLoginRevealAnimation {
                // 5. 动画结束后显示llRoot并清理
                vd.llRoot.visibility = View.VISIBLE
                vd.ivStart.visibility = View.GONE
                vd.ivStart.setImageBitmap(null)

                // 6. 完全移除LoginFragment
                if (loginFragment != null) {
                    supportFragmentManager.beginTransaction().remove(loginFragment).commitNow()
                }
            }
        }
    }

    /**
     * 登录界面淡出动画 - 200ms淡出效果
     */
    private fun animateLoginFragmentFadeOut(loginFragment: Fragment?) {
        if (loginFragment == null) return

        val loginRootView = loginFragment.view
        if (loginRootView == null) {
            supportFragmentManager.beginTransaction().hide(loginFragment).commitAllowingStateLoss()
            return
        }

        // 纯淡出动画，200ms
        ObjectAnimator.ofFloat(loginRootView, "alpha", 1f, 0f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 动画结束后隐藏Fragment，使用commitAllowingStateLoss避免状态保存后崩溃
                    supportFragmentManager.beginTransaction().hide(loginFragment).commitAllowingStateLoss()
                    // 恢复透明度
                    loginRootView.alpha = 1f
                }
            })
            start()
        }
    }

    /**
     * 显示登录成功后的圆形揭示动画
     * @param onAnimationEnd 动画结束回调
     */
    private fun showLoginRevealAnimation(onAnimationEnd: () -> Unit) {
        // 获取登录按钮的位置作为动画圆心
        val loginView = vd.tnv.getUserLogin()
        val location = IntArray(2)

        if (loginView != null) {
            loginView.getLocationInWindow(location)
        } else {
            // 如果没有找到登录按钮，使用屏幕右上角作为默认位置
            location[0] = vd.rlRoot.width
            location[1] = 0
        }

        val centerX = location[0] + (loginView?.width ?: 0) / 2
        val centerY = location[1] + (loginView?.height ?: 0) / 2

        // 计算动画半径（从圆心到屏幕最远角的距离）
        val endRadius = hypot(
            max(centerX, vd.rlRoot.width - centerX).toDouble(),
            max(centerY, vd.rlRoot.height - centerY).toDouble()
        ).toFloat()

        // 创建圆形揭示动画（从0开始放大）
        val anim = ViewAnimationUtils.createCircularReveal(
            vd.ivStart,
            centerX,
            centerY,
            0f,
            endRadius
        )

        vd.ivStart.visibility = View.VISIBLE
        anim.duration = 1000
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd.invoke()
            }
        })
        anim.start()
    }

    /**
     * 捕获llRoot的快照
     */
    private fun captureRootSnapshot(): Bitmap? {
        if (vd.llRoot.width == 0 || vd.llRoot.height == 0) return null

        val bitmap = Bitmap.createBitmap(
            vd.llRoot.width,
            vd.llRoot.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vd.llRoot.draw(canvas)

        return bitmap
    }
}
