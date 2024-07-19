package com.wl.turbidimetric.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.lxj.xpopup.XPopup
import com.wl.turbidimetric.R
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.copyForProject
import com.wl.turbidimetric.ex.getPackageInfo
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.print.ThermalPrintUtil
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.upload.hl7.util.getLocalConfig
import com.wl.turbidimetric.util.CrashHandler
import com.wl.turbidimetric.report.ExportReportHelper
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.report.PdfCreateUtil
import com.wl.turbidimetric.report.PrintHelper
import com.wl.turbidimetric.util.SerialPortIF
import com.wl.turbidimetric.util.SerialPortImpl
import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.wllib.LogToFile
import com.wl.wllib.ToastUtil
import com.wl.wllib.ktxRunOnBgCache
import com.wl.wllib.toTimeStr
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date


class App : Application() {
    private val activityList = mutableListOf<Activity>()
    val mainDao by lazy { ServiceLocator.getDb(this).mainDao() }
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    val serialPort: SerialPortIF by lazy {
        SerialPortImpl(
            SystemGlobal.isCodeDebug,
        )
    }
    val thermalPrintUtil: ThermalPrintUtil by lazy {
        ThermalPrintUtil(BaseSerialPort())
    }
    val printHelper: PrintHelper by lazy {
        PrintHelper(LocalData.ReportIntervalTime, this)
    }
    val imm: InputMethodManager by lazy {
        getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ToastUtil.init(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        initData()
        ktxRunOnBgCache {
            initDataStore()
            LogToFile.init()
            initPop()
//            没有项目参数的时候，添加一个默认参数
            initDB()
//            记录当前的版本
            initVersion()
//            删除检测报告的缓存
            PdfCreateUtil.deleteCacheFolder(File(ExportReportHelper.defaultReportSavePath))
        }
    }

    private fun initVersion() {
        SystemGlobal.versionName = String(
            ((getPackageInfo(this)?.versionName) ?: "").toByteArray(), charset("UTF-8")
        )
        SystemGlobal.versionCode = getPackageInfo(this)?.versionCode ?: 0

        LogToFile.i("versionName=${SystemGlobal.versionName} versionCode=${SystemGlobal.versionCode}")
    }


    private fun initDB() {
        GlobalScope.launch {
            val projectSource = ServiceLocator.provideProjectSource(this@App)
            val ps = mainDao.getProjectModels().first()

            if (ps.isEmpty()) {
                val project = ProjectModel().apply {
                    projectName = "血红蛋白"
                    projectCode = "FOB"
                    projectUnit = "ug/mL"
                    projectLjz = 100
                }
                val project2 = ProjectModel().apply {
                    projectName = "转铁蛋白"
                    projectCode = "FT"
                    projectUnit = "ug/mL"
                    projectLjz = 40
                }
                projectSource.addProject(project)
                projectSource.addProject(project2)
                insertTestCurve(project)
            }
        }
    }

    private suspend fun insertTestCurve(project: ProjectModel) {
        val curveSource = ServiceLocator.provideCurveSource(this@App)
        repeat(1) {
            //三次方
            curveSource.addCurve(CurveModel().apply {
                reagentNO = "${555 + it}"
                f0 = 24.756992920270594
                f1 = 0.8582045209676992
                f2 = -5.707122868757991E-4
                f3 = 1.950833853427454E-7
                fitGoodness = 0.99999997
                fitterType = FitterType.Three.ordinal
                gradsNum = 5
                reactionValues = intArrayOf(-27, 28, 239, 975, 1979)
                targets = doubleArrayOf(0.0, 50.0, 200.0, 500.0, 1000.0)
                yzs = intArrayOf(0, 49, 200, 500, 1000)
                createTime = Date().toTimeStr()
            }.copyForProject(project))
        }
        //线性
        curveSource.addCurve(CurveModel().apply {
            reagentNO = "${556}"
            f0 = 1.614742835
            f1 = -6.930948268
            f2 = 0.0
            f3 = 0.0
            fitGoodness = 0.99999998
            gradsNum = 6
            fitterType = FitterType.Linear.ordinal
            reactionValues = intArrayOf(1, 16, 35, 140, 310, 623)
            targets = doubleArrayOf(0.0, 25.0, 50.0, 200.0, 500.0, 1000.0)
            yzs = intArrayOf(0, 24, 49, 200, 500, 1000)
            createTime = Date().toTimeStr()
        }.copyForProject(project))
        //四参数
        curveSource.addCurve(CurveModel().apply {
            reagentNO = "${557}"
            f0 = -1.818473101
            f1 = 1.253206105
            f2 = 374.2468309
            f3 = 564.4612105
            fitGoodness = 0.99999999
            fitterType = FitterType.Four.ordinal
            gradsNum = 6
            reactionValues = intArrayOf(0, 24, 49, 200, 500, 1000)
            targets = doubleArrayOf(0.0, 25.0, 50.0, 200.0, 500.0, 1000.0)
            yzs = intArrayOf(0, 24, 49, 200, 500, 1000)
            createTime = Date().toTimeStr()
        }.copyForProject(project))
    }

    private fun initPop() {
        XPopup.setAnimationDuration(300)
        XPopup.setPrimaryColor(R.color.themePositiveColor)
    }


    private fun initData() {
        //每次进入软件都会讲当前版本号存入，如果以前的版本号小于当前，说明是第一次打开当前版本
        val packInfo = packageManager.getPackageInfo(packageName, 0)
        if (LocalData.CurrentVersion < packInfo.versionCode) {
            LocalData.CurrentVersion = packInfo.versionCode
        }

    }

    private fun initDataStore() {
        SystemGlobal.uploadConfig = getLocalConfig()
        SystemGlobal.isDebugMode = LocalData.DebugMode
    }

    companion object {
        var instance: App? = null
        const val TAG = "App"
    }

    //添加Activity到容器中
    fun addActivity(activity: Activity) {
        activityList.add(activity)
    }

    //删除Activity到容器中
    fun removeActivity(activity: Activity?) {
        activityList.remove(activity)
    }

    //遍历所有Activity并finish
    fun exit() {
        for (activity in activityList) {
            activity.finish()
        }
        activityList.clear()
    }

    object AppViewModelStoreOwner : ViewModelStoreOwner {
        private val viewModelStore = ViewModelStore()
        private val viewModelProvider: ViewModelProvider by lazy {
            ViewModelProvider(
                this, AppViewModelFactory()
            )
        }

        override fun getViewModelStore(): ViewModelStore {
            return viewModelStore
        }

        fun <T : ViewModel> getAppViewModel(classVm: Class<T>): T {
            return viewModelProvider[classVm]
        }
    }

    class AppViewModelFactory(
        private val localDataDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource(),
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                return AppViewModel(
                    localDataDataSource,
                    App.instance!!.serialPort,
                    App.instance!!.thermalPrintUtil,
                    App.instance!!.printHelper,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
