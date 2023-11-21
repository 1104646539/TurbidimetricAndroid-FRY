package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class MyScrollView : ScrollView {
    private var maxHeight = 0
    private var maxWidth = 0

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height: Int = measuredHeight
        val width: Int = measuredWidth
        var h = height
        if (maxHeight in 1 until height) {
            h = maxHeight
        }
        var w = width
        if (w in 1 until width) {
            w = maxWidth
        }
        if (h == maxHeight || w == maxWidth) {
            setMeasuredDimension(width, maxHeight)
        }
    }

    fun setMaxHeight(height: Int) {
        maxHeight = height
    }

    fun setMaxWidth(width: Int) {
        maxWidth = width
    }

    companion object {
        const val TAG = "MyScrollView"
    }
}
