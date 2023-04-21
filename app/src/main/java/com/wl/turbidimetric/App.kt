package com.wl.turbidimetric

import android.app.Application
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.datastore.LocalDataGlobal
import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.ex.putIntData
import com.wl.turbidimetric.util.FileTree
import com.wl.turbidimetric.util.SerialPortUtil
import com.wl.wllib.ToastUtil
import timber.log.Timber


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        ToastUtil.init(this)
        initData()
        DBManager.init(this)
        SerialPortUtil.Instance.open()
        Timber.plant(FileTree.instance)
        initDataStore();

    }

    private fun initData() {
        if (LocalData.getFirstOpen()) {
            LocalData.setFirstOpen(false)
            LocalDataGlobal.Key.TakeReagentR1.putIntData(60)
            LocalDataGlobal.Key.TakeReagentR2.putIntData(300)
        }
    }

    private fun initDataStore() {


    }

    companion object {
        var instance: App? = null
    }

}
