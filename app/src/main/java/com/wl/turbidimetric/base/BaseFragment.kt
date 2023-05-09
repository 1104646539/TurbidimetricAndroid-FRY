package com.wl.wwanandroid.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.wl.turbidimetric.global.EventMsg
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

open abstract class BaseFragment<VM : ViewModel, VD : ViewDataBinding>(@LayoutRes val layoutId: Int) : Fragment() {
    protected abstract val vm: VM
    lateinit var vd: VD
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DataBindingUtil.inflate<VD>(inflater, layoutId, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            vd = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        init(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    abstract fun initViewModel()

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }
    abstract fun init(savedInstanceState: Bundle?)

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: EventMsg<Any>) {

    }
}
