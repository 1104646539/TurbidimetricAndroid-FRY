package com.wl.turbidimetric.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wl.turbidimetric.datamanager.DataManagerFragment
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.home.HomeFragment
import com.wl.turbidimetric.matchingargs.MatchingArgsFragment
import com.wl.turbidimetric.settings.SettingsFragment
import com.wl.turbidimetric.test.repeatablitylity.RepeatabilityFragment
import com.wl.turbidimetric.test.TestDataFragment

class MainViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return SystemGlobal.navItems.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                HomeFragment.newInstance()
            }
            1 -> {
                DataManagerFragment.newInstance()
            }
            2 -> {
                MatchingArgsFragment.newInstance()
            }
            3 -> {
                SettingsFragment.newInstance()
            }
            4 -> {
                RepeatabilityFragment.newInstance()
            }
            else -> {
                TestDataFragment.newInstance()
            }
        }
    }
}
