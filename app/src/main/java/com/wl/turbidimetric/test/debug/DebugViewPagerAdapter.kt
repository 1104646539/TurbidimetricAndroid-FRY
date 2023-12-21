package com.wl.turbidimetric.test.debug

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wl.turbidimetric.test.debug.integration.IntegrationFragment
import com.wl.turbidimetric.test.debug.singlecmd.SingleCmdFragment

class DebugViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                SingleCmdFragment.newInstance()
            }
            else -> {
                IntegrationFragment.newInstance()
            }
        }
    }
}
