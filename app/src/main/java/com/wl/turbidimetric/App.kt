package com.wl.turbidimetric

import android.app.Activity
import android.app.Application
import android.content.Context
import com.lxj.xpopup.XPopup
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.upload.hl7.util.getLocalConfig
import com.wl.turbidimetric.util.CrashHandler
import com.wl.wllib.LogToFile
import com.wl.wllib.ToastUtil
import com.wl.wllib.ktxRunOnBgCache
import java.util.*


class App : Application() {
    private val activityList = mutableListOf<Activity>()

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
            DBManager.init(this)
            initDataStore();
            LogToFile.init()
//            UploadUtil.open()
            initPop()
//
            //没有项目参数的时候，添加一个默认参数
            if (DBManager.ProjectBox.all.isNullOrEmpty()) {
                repeat(1) {
                    DBManager.ProjectBox.put(ProjectModel().apply {
                        reagentNO = it.toString()
                    })
                }
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
//        SystemGlobal.machineTestModel = MachineTestModel.valueOf(CurMachineTestModel)
        SystemGlobal.uploadConfig = getLocalConfig()
    }

    private fun initDataStore() {


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
