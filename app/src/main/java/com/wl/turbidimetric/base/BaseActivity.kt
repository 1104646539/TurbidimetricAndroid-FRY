package com.wl.wwanandroid.base

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import com.wl.turbidimetric.global.EventMsg
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class BaseActivity<VM : BaseViewModel, VD : ViewDataBinding> : AppCompatActivity() {

    protected abstract val viewDataBinding: VD
    protected abstract val viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
        EventBus.getDefault().register(this)
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: EventMsg<Any>) {

    }
    abstract fun init()
}
