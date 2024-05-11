package com.wl.turbidimetric.base

import android.os.Bundle
import android.transition.Slide
import android.transition.Visibility
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.global.EventMsg
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class BaseActivity<VM : BaseViewModel, VD : ViewDataBinding> : AppCompatActivity() {

    protected abstract val vd: VD
    protected abstract val vm: VM
    val appVm: AppViewModel by lazy { getAppViewModel(AppViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.instance?.addActivity(this)
        vm.init()
        vd.root
        supportActionBar?.hide()
        EventBus.getDefault().register(this)
        init()
        window.enterTransition = buildEnterTransition()
    }

    private fun buildEnterTransition(): Visibility? {
        val enterTransition = Slide()
        enterTransition.setDuration(
            resources.getInteger(R.integer.fragment_transform_time).toLong()
        )
        enterTransition.slideEdge = Gravity.RIGHT
        return enterTransition
    }

    override fun onDestroy() {
        super.onDestroy()
        App.instance?.removeActivity(this)
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: EventMsg<Any>) {

    }

    abstract fun init()
}
