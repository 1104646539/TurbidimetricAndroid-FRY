package com.wl.turbidimetric.main

import androidx.lifecycle.MutableLiveData
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.base.BaseViewModel

class MainViewModel : BaseViewModel() {
    val curIndex = MutableLiveData<Int>(0)

    val navItems = SystemGlobal.navItems

    init {

    }
}
