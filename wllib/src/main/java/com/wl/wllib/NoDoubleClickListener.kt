package com.wl.wllib

import android.view.View
import java.util.*

/**
 * 防止点击按钮过快
 * @property MIN_CLICK_DELAY_TIME Int
 * @property lastClickTime Long
 */
abstract class NoDoubleClickListener : View.OnClickListener {
    private val MIN_CLICK_DELAY_TIME = 1500
    private var lastClickTime: Long = 0

    abstract fun onNoDoubleClick(v: View?)

    override fun onClick(v: View?) {
        val currentTime = Calendar.getInstance().timeInMillis
        if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
            lastClickTime = currentTime
            onNoDoubleClick(v)
        }
    }
}
