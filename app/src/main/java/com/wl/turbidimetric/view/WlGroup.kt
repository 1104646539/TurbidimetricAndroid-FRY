package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.wl.turbidimetric.R

class WlGroup @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributeSet) {
    var ids: String? = ""
    var hashMap = mutableMapOf<Int, SettingItemView>()
    var mRootView: ViewGroup? = null;

    init {
        val ta = context.obtainStyledAttributes(
            attributeSet, R.styleable.WlGroup
        )
        ids = ta.getString(R.styleable.WlGroup_ids)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initIds()
    }

    private fun initIds() {
        if (ids.isNullOrEmpty()) {
            return
        }
        ids?.let {
            val idsStr = it.split(",")
            if (parent != null) {
                mRootView = parent as ViewGroup

                idsStr.forEach { referenceId ->
                    val rscId = context.resources.getIdentifier(
                        referenceId, "id",
                        context.packageName
                    )
                    val view = mRootView!!.findViewById<SettingItemView>(rscId)
                    hashMap[rscId] = view
//                    Log.d("TAG", "initIds: rscId=$rscId view=$view")
                }
            }
        }
    }

    public fun setSelected(id: Int) {
        hashMap.forEach { k, v ->
            v.isSelected = (id == k)
            v.updateTextColor()
        }
    }


}
