package com.wl.turbidimetric.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import com.wl.turbidimetric.App
import com.wl.turbidimetric.global.EventMsg

abstract class BaseActivity<VM : BaseViewModel, VD : ViewDataBinding> : AppCompatActivity() {

    protected abstract val vd: VD
    protected abstract val vm: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.instance?.addActivity(this)
        vm.init()
        vd.root
        supportActionBar?.hide()
//        EventBus.getDefault().register(this)
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        App.instance?.removeActivity(this)
//        EventBus.getDefault().unregister(this)
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: EventMsg<Any>) {

    }

    abstract fun init()
}
