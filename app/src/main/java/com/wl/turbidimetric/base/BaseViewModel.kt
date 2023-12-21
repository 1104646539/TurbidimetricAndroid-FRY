package com.wl.wwanandroid.base

import androidx.lifecycle.ViewModel
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


open class BaseViewModel : ViewModel() {

    open fun init(): Unit {}

    init {
//        EventBus.getDefault().register(this)
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    open fun onMessageEvent(event: EventMsg<Any>) {
//
//    }

}
