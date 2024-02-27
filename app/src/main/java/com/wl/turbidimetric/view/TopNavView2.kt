package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.wl.turbidimetric.R

class TopNavView2 @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet) {
    private var root: View? = null
    private var tvTitle: TextView? = null
    private var llBack: View? = null
    private var tvRight1: TextView? = null

    init {
        root = LayoutInflater.from(context).inflate(R.layout.layout_main_top_nav2, this, true)
        initView()
        listenerView()

    }

    private fun listenerView() {


    }

    fun setTitle(title: String) {
        tvTitle?.text = title
    }

    fun setOnBack(onClickListener: OnClickListener) {
        llBack?.setOnClickListener(onClickListener)
    }

    fun setRight1(
        text: String, onClickListener: OnClickListener
    ) {
        tvRight1?.text = text
        tvRight1?.setOnClickListener(onClickListener)
        tvRight1?.visibility = View.VISIBLE
    }

    private fun initView() {
        root?.apply {
            tvTitle = findViewById(R.id.tv_title)
            llBack = findViewById(R.id.ll_back)
            tvRight1 = findViewById(R.id.tv_right1)
        }
    }


}
