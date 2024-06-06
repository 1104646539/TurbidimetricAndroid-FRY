package com.wl.turbidimetric.util

import android.view.View
import java.io.Serializable

/**
 * 属性动画view包装类
 */
class ViewWrapper(private val mTarget: View) : Serializable {
    var width: Int
        get() = mTarget.layoutParams.width
        set(width) {
            mTarget.layoutParams.width = width
            mTarget.requestLayout()
        }
    var height: Int
        get() = mTarget.layoutParams.height
        set(height) {
            mTarget.layoutParams.height = height
            mTarget.requestLayout()
        }
}
