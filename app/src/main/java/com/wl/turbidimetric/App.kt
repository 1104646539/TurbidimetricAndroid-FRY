package com.wl.turbidimetric

import android.app.Application
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.model.ProjectModel
import com.wl.wllib.ToastUtil


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
//        MMKV.initialize(this)
        ToastUtil.init(this)
        initData()
        DBManager.init(this)
//        Timber.plant(FileTree.instance)
        initDataStore();
//        LogToFile.init()

        //没有项目参数的时候，添加一个默认参数
        if (DBManager.ProjectBox.all.isNullOrEmpty()) {
            repeat(1) {
                DBManager.ProjectBox.put(ProjectModel().apply {
                    reagentNO = it.toString()
                })
            }
        }
    }


    private fun initData() {
        //每次进入软件都会讲当前版本号存入，如果以前的版本号小于当前，说明是第一次打开当前版本
        val packInfo = packageManager.getPackageInfo(packageName, 0)
        if (LocalData.CurrentVersion < packInfo.versionCode) {
            LocalData.CurrentVersion = packInfo.versionCode
        }
//        SystemGlobal.machineTestModel = MachineTestModel.valueOf(CurMachineTestModel)

    }

    private fun initDataStore() {


    }

    companion object {
        var instance: App? = null
    }

}
