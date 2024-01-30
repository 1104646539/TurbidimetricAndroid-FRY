package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.wl.turbidimetric.R

class SettingItemView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet) {
    private var root: View? = null
    private var clRoot: View? = null
    private var ivIcon: ImageView? = null
    private var tvTitle: TextView? = null

    private var selectable: Boolean = false
    private var selected: Boolean = false

    private var textColor: Int = context.resources.getColor(R.color.white)
    private var text: String? = ""
    private var icon: Int = -1
    private var bg: Int = -1
    var l: OnClickListener? = null

    init {
        root = LayoutInflater.from(context).inflate(R.layout.layout_settings_item, this, true)
        initAttr(context, attributeSet, defStyleAttr)
        isEnabled = true
        initView()
        listenerView()

    }

    private fun initAttr(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(
            attrs, R.styleable.SettingsItemView
        )
        textColor = ta.getColor(
            R.styleable.SettingsItemView_textColor,
            context.resources.getColor(R.color.white)
        )
        text = ta.getString(R.styleable.SettingsItemView_text)
        selectable = ta.getBoolean(R.styleable.SettingsItemView_selectable, false)
        icon = ta.getResourceId(R.styleable.SettingsItemView_icon, -1)
        bg = ta.getResourceId(R.styleable.SettingsItemView_bg, -1)
    }

    private fun listenerView() {
        val defClick = OnClickListener {
            updateSelected()
        }
        super.setOnClickListener(defClick)
    }

    private fun updateSelected() {
        if (selectable) {
            selected = !selected
            isSelected = selected
        }
    }


    private fun initView() {
        root?.apply {
            ivIcon = findViewById(R.id.iv_icon)
            clRoot = findViewById(R.id.cl_root)
            tvTitle = findViewById(R.id.tv_title)
        }

        if (icon > 0) {
            ivIcon?.setImageResource(icon)
        } else {
            ivIcon?.setImageDrawable(null)
        }
        tvTitle?.setText(text)
        tvTitle?.setTextColor(textColor)

        if (bg > 0) {//在这里设背景的原因是有个bug，在xml设置background，selector无效。
            val bd = context.getDrawable(bg)
            background = bd

        }

    }

    override fun setOnClickListener(l: OnClickListener?) {
        val l2 = OnClickListener { v ->
            updateSelected()
            l?.onClick(v)
        }
        super.setOnClickListener(l2)
    }
}
