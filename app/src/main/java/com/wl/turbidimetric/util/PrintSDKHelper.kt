package com.wl.turbidimetric.util

import android.content.Context
import android.content.pm.PackageInfo
import com.dynamixsoftware.intentapi.IServiceCallback
import com.dynamixsoftware.intentapi.IntentAPI
import com.dynamixsoftware.printingsdk.IPage
import com.dynamixsoftware.printingsdk.IPrintListener
import com.dynamixsoftware.printingsdk.ISetupPrinterListener
import com.dynamixsoftware.printingsdk.Printer
import com.dynamixsoftware.printingsdk.PrintingSdk
import com.dynamixsoftware.printingsdk.Result
import com.wl.turbidimetric.app.PrinterState
import com.wl.wllib.LogToFile.i


/**
 * 用来管理PrintHand相关的sdk封装使用
 * IntentAPI
 * PrintingSdk
 */
object PrintSDKHelper {
    private var intentApi: IntentAPI? = null
    private var printingSdk: PrintingSdk? = null
    private val packageName1: String = "com.dynamixsoftware.printhand"
    private var mContext: Context? = null

    private var intentApiInitSuccess: Boolean = false
    private var printingSdkInitSuccess: Boolean = false
    private var printingSdkRecentSuccess: Boolean = false

    var printerStateChange: ((PrinterState) -> Unit)? = null
    var printerState: PrinterState = PrinterState.None
        private set(value) {
            field = value
            printerStateChange?.invoke(value)
        }

    @JvmStatic
    fun close() {
        mContext = null
        printerStateChange = null
        printingSdk?.stopService()
        intentApi?.stopService(intentServiceCallback)
        printingSdk = null
        intentApi = null
    }

    @JvmStatic
    fun init(context: Context) {
        mContext = context
        intentApi = IntentAPI(context)
        printingSdk = PrintingSdk(context)
        if (isInstallApk()) {
            initSdkService()
        } else {
            printerState = PrinterState.NotInstallApk
        }
    }

    val intentServiceCallback = object : IServiceCallback.Stub() {
        override fun onServiceConnected() {
            i("runService onServiceConnected")
            intentApiInitSuccess = true

        }

        override fun onServiceDisconnected() {
            i("runService onServiceDisconnected")
            intentApiInitSuccess = false
            printerState = PrinterState.InitSdkFailed
        }

        override fun onRenderLibraryCheck(
            renderLibrary: Boolean,
            fontLibrary: Boolean
        ): Boolean {
            i("runService onRenderLibraryCheck renderLibrary=$renderLibrary fontLibrary=$fontLibrary")
            return true
        }

        override fun onLibraryDownload(progress: Int) {
            i("runService onLibraryDownload")
        }

        override fun onFileOpen(progress: Int, finished: Int) {
            i("runService onFileOpen")

        }

        override fun onPasswordRequired(): String {
            i("runService onPasswordRequired")
            return "123456"
        }

        override fun onError(result: com.dynamixsoftware.intentapi.Result?) {
            i("runService onError result=$result")
            intentApiInitSuccess = false
            printerState = PrinterState.InitSdkFailed
        }
    }
    private val printingServiceCallback = object : com.dynamixsoftware.printingsdk.IServiceCallback {
        override fun onServiceConnected() {
            i("startService onServiceConnected")
            printingSdkInitSuccess = true
            initRecentPrinters()
        }

        override fun onServiceDisconnected() {
            i("startService onServiceDisconnected")
            printingSdkInitSuccess = false
        }
    }

    private fun initSdkService() {
        intentApi?.runService(intentServiceCallback)
        printingSdk?.startService(printingServiceCallback)
    }

    private val recentPrinters = object : ISetupPrinterListener.Stub() {
        override fun start() {
            i("initRecentPrinters start")
        }

        override fun libraryPackInstallationProcess(percent: Int) {
            i("initRecentPrinters libraryPackInstallationProcess percent=$percent")
        }

        override fun finish(result: Result?) {
            i("initRecentPrinters finish result=$result")
            try {
                if (getCurPrinter() == null) {
                    printerState = PrinterState.NotPrinter
                    return
                }
                printingSdkRecentSuccess = result == Result.OK
                if (intentApiInitSuccess && printingSdkInitSuccess && printingSdkRecentSuccess) {
                    printerState = PrinterState.Success
                }
            } catch (e: Exception) {
                printerState = PrinterState.NotPrinter
            }
        }
    }

    /**
     * 初始化最近的打印机
     */
    fun initRecentPrinters() {
        printerState = PrinterState.NotPrinter
        printingSdk?.initRecentPrinters(recentPrinters)
    }

    @JvmStatic
    fun isInstallApk(): Boolean {
        return isAvilible(mContext, packageName1)
    }

    private fun isAvilible(context: Context?, packageName: String): Boolean {
        if (context == null) return false
        val packageManager = context.packageManager //获取packagemanager
        val pinfo: List<PackageInfo> = packageManager.getInstalledPackages(0) //获取所有已安装程序的包信息
        val pName: MutableList<String> = ArrayList() //用于存储所有已安装程序的包名
        //从pinfo中将包名字逐一取出，压入pName list中
        if (pinfo != null) {
            for (i in pinfo.indices) {
                val pn: String = pinfo[i].packageName
                pName.add(pn)
            }
        }
        return pName.contains(packageName) //判断pName中是否有目标程序的包名，有TRUE，没有FALSE
    }


    /**
     * 判断是否准备好直接发送打印任务了
     * 该方法需要在初始化完成之后再用，即 printingSdkInitSuccess 和 intentApiInitSuccess
     */
    @JvmStatic
    fun isPreparePrint(): Boolean {
        if (!isInstallApk()) {
            i("未安装程序")
            return false
        }
        if (!(printingSdkInitSuccess && intentApiInitSuccess && printingSdkRecentSuccess)) return false

        if (printingSdk?.currentPrinter == null) {
            return false
        }
        return true
    }

    /**
     * 显示PrintHand的设置打印机的UI
     * 在第一次设置这个打印机时需要必须手动设置，并联网下载驱动
     */
    @JvmStatic
    fun showSetupPrinterUi() {
        if (isInstallApk()) {
            intentApi?.setupCurrentPrinter()
        }
    }

    @JvmStatic
    fun printImage(pages: List<IPage>, printListener: IPrintListener) {
        i("printImage")
        printingSdk?.print(pages, 1, printListener)
    }

    @JvmStatic
    fun getCurPrinter(): Printer? {
        var p: Printer? = null
        try {
            p = printingSdk?.currentPrinter
        } catch (e: Exception) {
            return null
        }
        return p
    }
}
