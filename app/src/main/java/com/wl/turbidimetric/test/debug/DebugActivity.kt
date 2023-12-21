package com.wl.turbidimetric.test.debug

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ActivityDebugBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.main.MainViewPagerAdapter
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.wwanandroid.base.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class DebugActivity : BaseActivity<DebugViewModel, ActivityDebugBinding>() {
    override val vd: ActivityDebugBinding by ActivityDataBindingDelegate(R.layout.activity_debug)
    override val vm: DebugViewModel by viewModels()
    override fun init() {
        listener()
        vd.vp.adapter = DebugViewPagerAdapter(this)
        vd.vp.isUserInputEnabled = false
        vd.vp.offscreenPageLimit = 1


        vd.tl.addTab(vd.tl.newTab().setText("单步调试"))
        vd.tl.addTab(vd.tl.newTab().setText("组合调试"))

        vd.tl.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                vd.vp.setCurrentItem(tab?.position ?: 0, false)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

    }

    private fun listener() {
        listenerView()
        listenerEvent()
    }

    private fun listenerEvent() {
        vd.btnClear.setOnClickListener {
            vd.tvMsg.text = ""
        }
    }

    private fun listenerView() {
        lifecycleScope.launch {
            vm.originalDataMsg.collectLatest {
                vd.tvMsg.append(it)
                vd.nsv.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
    override fun onStart() {
        EventBus.getDefault().post(EventMsg(EventGlobal.WHAT_GET_TEMP_CHANGE,false))
        super.onStart()
        vm.listener()
    }

    override fun onStop() {
        EventBus.getDefault().post(EventMsg(EventGlobal.WHAT_GET_TEMP_CHANGE,true))
        super.onStop()
        vm.clearListener()
    }
}
