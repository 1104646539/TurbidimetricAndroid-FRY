package com.wl.turbidimetric

import android.app.Activity
import android.app.Application
import android.content.Context
import com.lxj.xpopup.XPopup
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.MainRoomDatabase
import com.wl.turbidimetric.ex.copyForProject
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.upload.hl7.util.getLocalConfig
import com.wl.turbidimetric.util.CrashHandler
import com.wl.turbidimetric.util.FitterType
import com.wl.wllib.LogToFile
import com.wl.wllib.ToastUtil
import com.wl.wllib.ktxRunOnBgCache
import com.wl.wllib.toTimeStr
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date


class App : Application() {
    private val activityList = mutableListOf<Activity>()
    val database by lazy { MainRoomDatabase.getDatabase(this) }
    val mainDao by lazy { database.mainDao() }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ToastUtil.init(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        initData()
        ktxRunOnBgCache {
//            DBManager.init(this)
            initDataStore()
            LogToFile.init()
//            UploadUtil.open()
            initPop()
//
            //没有项目参数的时候，添加一个默认参数
            initDB()

        }
    }

    private fun initDB() {
        GlobalScope.launch {
            val ps = mainDao.getProjectModels().first()

            if (ps.isEmpty()) {
                val project = ProjectModel().apply {
                    projectName = "项目1"
                    projectCode = "FOB"
                    projectUnit = "ug/mL"
                    projectLjz = 100
                }
                mainDao.insertProjectModel(project)
                insertTestCurve(project)
            }
        }
    }

    private suspend fun insertTestCurve(project: ProjectModel) {
        repeat(10) {
            //三次方
            mainDao.insertCurveModel(CurveModel().apply {
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
//        //线性
//        mainDao.insertCurveModel(CurveModel().apply {
//            reagentNO = "${556}"
//            f0 = 1.614742835
//            f1 = -6.930948268
//            f2 = 0.0
//            f3 = 0.0
//            fitGoodness = 0.99999997
//            gradsNum = 6
//            fitterType = FitterType.Linear.ordinal
//            reactionValues = intArrayOf(1, 16, 35, 140, 310,623)
//            targets = doubleArrayOf(0.0, 25.0,50.0, 200.0, 500.0, 1000.0)
//            yzs = intArrayOf(0, 49, 200, 500, 1000)
//            createTime = Date().toTimeStr()
//        }.copyForProject(project))
//        //四参数
//        mainDao.insertCurveModel(CurveModel().apply {
//            reagentNO = "${557}"
//            f0 = -1.818473101
//            f1 = 1.253206105
//            f2 = 374.2468309
//            f3 = 564.4612105
//            fitGoodness = 0.99999997
//            fitterType = FitterType.Four.ordinal
//            gradsNum = 6
//            reactionValues = intArrayOf(0, 49, 200, 500, 1000)
//            targets = doubleArrayOf(0.0, 50.0, 200.0, 500.0, 1000.0)
//            yzs = intArrayOf(0, 49, 200, 500, 1000)
//            createTime = Date().toTimeStr()
//        }.copyForProject(project))
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

}
