package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * 自定义的可以修改最大高度的
 * ScrollView
 * @property maxHeight Int
 */
class MyScrollView : ScrollView {
    private var maxHeight = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height: Int = measuredHeight
        val width: Int = measuredWidth
        if (maxHeight in 1 until height) {
            setMeasuredDimension(width, maxHeight)
        }
    }

    fun setMaxHeight(height: Int) {
        maxHeight = height
    }

}
