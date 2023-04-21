package com.wl.turbidimetric

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.wl.turbidimetric.datamanager.DataManagerFragment
import com.wl.turbidimetric.home.HomeFragment
import com.wl.turbidimetric.matchingargs.MatchingArgsFragment
import com.wl.turbidimetric.settings.SettingsFragment
import com.wl.wwanandroid.base.BaseViewModel

class MainViewModel : BaseViewModel() {
    val curIndex = MutableLiveData<Int>(0)
    val prevIndex = MutableLiveData<Int>(-1)
    val fragments: MutableLiveData<List<Fragment>> = MutableLiveData(mutableListOf(
        HomeFragment.newInstance(),
        DataManagerFragment.newInstance(),
        MatchingArgsFragment.newInstance(),
        SettingsFragment.newInstance()
    ))
    val ids = MutableLiveData<List<Int>>(
        mutableListOf(
            R.drawable.icon_navigation_home,
            R.drawable.icon_navigation_datamanager,
            R.drawable.icon_navigation_parameterlist,
            R.drawable.icon_navigation_settings,
        )
    )

    init {

    }
}
