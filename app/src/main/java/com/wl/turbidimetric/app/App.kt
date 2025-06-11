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
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.copyForProject
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.ex.getPackageInfo
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.log.DbLogUtil
import com.wl.turbidimetric.log.LogLevel
import com.wl.turbidimetric.log.LogModel
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.GlobalConfig
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.print.ThermalPrintUtil
import com.wl.turbidimetric.report.ExportReportHelper
import com.wl.turbidimetric.report.PdfCreateUtil
import com.wl.turbidimetric.report.PrintHelper
import com.wl.turbidimetric.repository.DefaultLocalDataDataSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.LogListDataSource
import com.wl.turbidimetric.util.CrashHandler
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.util.SerialPortIF
import com.wl.turbidimetric.util.SerialPortImpl
import com.wl.weiqianwllib.serialport.BaseSerialPort
import com.wl.wllib.LogToFile
import com.wl.wllib.LogToFile.i
import com.wl.wllib.ToastUtil
import com.wl.wllib.ktxRunOnBgCache
import com.wl.wllib.toTimeStr
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import kotlin.math.log


class App : Application() {
    private val activityList = mutableListOf<Activity>()
    val mainDao by lazy { ServiceLocator.getDb(this).mainDao() }
    val globalDao by lazy { ServiceLocator.getDb(this).globalDao() }
    val logDao by lazy { ServiceLocator.getLogDb(this, path = DbLogUtil.path).logDao() }
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
        PrintHelper(this)
    }
    val scanCodeUtil: ScanCodeUtil by lazy {
        ScanCodeUtil()
    }
    val appVm: AppViewModel by lazy {
        getAppViewModel(AppViewModel::class.java)
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
//            删除检测报告的缓存
            PdfCreateUtil.deleteCacheFolder(File(ExportReportHelper.defaultReportSavePath))
        }
        DbLogUtil.init(logDao)

    }


    private fun initDB() {
        GlobalScope.launch {
            val projectSource = ServiceLocator.provideProjectSource(this@App)
            val ps = mainDao.getProjectModels().first()

            if (ps.isEmpty()) {
                val project = ProjectModel().apply {
                    projectName = "血红蛋白"
                    projectCode = "FOB"
                    projectUnit = "ng/mL"
                    projectLjz = 100
                    grads = doubleArrayOf(0.0, 50.0, 200.0, 500.0, 1000.0)
                }
                val project2 = ProjectModel().apply {
                    projectName = "转铁蛋白"
                    projectCode = "FT"
                    projectUnit = "ng/mL"
                    projectLjz = 40
                }
                projectSource.addProject(project)
                projectSource.addProject(project2)
//                insertTestCurve(project)
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
            f0 = 171479.92881
            f1 = -0.76618
            f2 = 52921.78949
            f3 =  -3.62716
            fitGoodness =  0.99945
            fitterType = FitterType.Four.ordinal
            gradsNum = 6
            reactionValues = intArrayOf(0, 795, 2424, 3851, 5435, 7808)
            targets = doubleArrayOf(0.0, 50.0, 200.0, 400.0, 600.0, 1000.0)
            yzs = intArrayOf(0, 24, 49, 200, 500, 1000)
            createTime = Date().toTimeStr()
        }.copyForProject(project))
    }

    private fun initPop() {
        XPopup.setAnimationDuration(300)
        XPopup.setPrimaryColor(R.color.themePositiveColor)
    }


    private fun initData() {
        val globalConfigs = globalDao.getAllGlobalConfig()
        if (globalConfigs.isEmpty()) {//添加两个，第一个是默认参数，第二个才是可以修改的当前参数
            globalDao.insertGlobalConfig(GlobalConfig(DefaultLocalDataDataSource.backupsId))
            globalDao.insertGlobalConfig(GlobalConfig(DefaultLocalDataDataSource.defaultId))
        }
        getPackageInfo(this@App)?.let {
            appVm.initVersion(it)
        }
    }

    private fun initDataStore() {
        appVm.initDataStore()
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
        private val localDataDataSource: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!),
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                return AppViewModel(
                    localDataDataSource,
                    App.instance!!.serialPort,
                    App.instance!!.thermalPrintUtil,
                    App.instance!!.printHelper,
                    App.instance!!.scanCodeUtil,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
