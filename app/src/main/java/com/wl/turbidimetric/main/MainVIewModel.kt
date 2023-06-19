package com.wl.turbidimetric

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.wl.turbidimetric.datamanager.DataManagerFragment
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.home.HomeFragment
import com.wl.turbidimetric.matchingargs.MatchingArgsFragment
import com.wl.turbidimetric.settings.SettingsFragment
import com.wl.turbidimetric.test.TestDataFragment
import com.wl.turbidimetric.view.NavigationView
import com.wl.wwanandroid.base.BaseViewModel

class MainViewModel : BaseViewModel() {
    val curIndex = MutableLiveData<Int>(0)

    val navItems = SystemGlobal.navItems

    init {

    }
}
