package com.wl.turbidimetric

import android.app.Application
import com.tencent.mmkv.MMKV
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.datastore.LocalDataGlobal
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.ex.put
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.FileTree
import com.wl.turbidimetric.util.ScanCodeUtil
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wllib.ToastUtil
import timber.log.Timber


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        MMKV.initialize(this)
        ToastUtil.init(this)
        initData()
        DBManager.init(this)
        SerialPortUtil.Instance.open()
        ScanCodeUtil.Instance.open()
        Timber.plant(FileTree.instance)
        initDataStore();


        //没有项目参数的时候，添加一个默认参数
        if (DBManager.ProjectBox.all.isNullOrEmpty()) {
            DBManager.ProjectBox.put(ProjectModel())
        }
    }


    private fun initData() {
        if (LocalData.getFirstOpen()) {
            LocalData.setFirstOpen(false)
            LocalDataGlobal.Key.TakeReagentR1.put(60)
            LocalDataGlobal.Key.TakeReagentR2.put(300)
            LocalDataGlobal.Key.DetectionNum.put("1")
        }
    }
 
    private fun initDataStore() {


    }

    companion object {
        var instance: App? = null
    }

}
