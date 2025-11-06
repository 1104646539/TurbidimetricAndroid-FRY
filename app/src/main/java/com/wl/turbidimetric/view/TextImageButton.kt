package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.wl.turbidimetric.R

/**
 * 在一个LinearLayout 中插入ImageView和TextView。横向排布，并且居中，变成一个带有图像和文字的按钮
 */
class TextImageButton : LinearLayout {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr)
    }

    lateinit var tv: TextView
    lateinit var iv: ImageView
    private fun init(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        if (context == null) return
        if (attrs == null) return
        tv = TextView(context)
        iv = ImageView(context)
        val ta = context.obtainStyledAttributes(
            attrs, R.styleable.TextImageButton
        )
        val text = ta.getString(R.styleable.TextImageButton_android_text)
        val src = ta.getResourceId(R.styleable.TextImageButton_android_src, -1)
        val textColor: Int = ta.getColor(
            R.styleable.TextImageButton_android_textColor,
            context.resources.getColor(R.color.textColor)
        )
        val interval = context.resources.getDimension(R.dimen.text_image_button_interval)
        val textSize = ta.getDimension(
            R.styleable.TextImageButton_android_textSize,
            context.resources.getDimension(R.dimen.text_size_default)
        )

        tv.text = text
        tv.setTextColor(textColor)
        tv.textSize = textSize
        tv.setPadding(interval.toInt(), 0, 0, 0)
        if (src != -1) {
            iv.setImageResource(src)
        }
        addView(iv)
        addView(tv)
        gravity = Gravity.CENTER

    }

    fun setText(text: String) {
        tv.text = text
    }

    fun setTextStyle(testStyle: Int) {
        tv.setTypeface(null, testStyle)
    }


    fun setImage(@DrawableRes id: Int) {
        iv.setImageResource(id)
    }

}
