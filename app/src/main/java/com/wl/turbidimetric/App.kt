package com.wl.turbidimetric

import android.app.Activity
import android.app.Application
import android.content.Context
import com.lxj.xpopup.XPopup
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.MainRoomDatabase
import com.wl.turbidimetric.db.putTestResultAndCurve
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.upload.hl7.util.getLocalConfig
import com.wl.turbidimetric.util.CrashHandler
import com.wl.wllib.LogToFile
import com.wl.wllib.ToastUtil
import com.wl.wllib.ktxRunOnBgCache
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


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
            val ps = mainDao.getProjectModels()
            if (ps.isEmpty()) {
                repeat(1) {
                    mainDao.insertProjectModel(ProjectModel().apply {
                        projectName = "项目1"
                        projectCode = "FOB"
                        projectUnit = "ug/mL"
                        projectLjz = 100
                    })
                }
                mainDao.insertCurveModel(CurveModel().apply {
                    reagentNO = "999"
                    f0 = 14.32525697891957
                    f1 = 1.1568311508309208
                    f2 = -9.761226454206153E-4
                    f3 = 4.993717916686672E-7
                    reactionValues = intArrayOf(0,49,200,500,1000)
                })
            }
        }
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
