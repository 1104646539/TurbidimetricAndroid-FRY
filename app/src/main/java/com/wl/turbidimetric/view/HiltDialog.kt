package com.wl.turbidimetric.view

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.wllib.DialogUtil

/**
 * 用来显示提示的对话框
 * @property context Context
 * @constructor
 */
open class HiltDialog(private val context: Context) : BaseDialog(context) {
    init {
        (getDialogUtil().getView(R.id.root).layoutParams)?.let {
            (it as MarginLayoutParams).setMargins(
                0,
                context.resources.getDimension(R.dimen.margin_top).toInt(),
                0,
                0
            )
        }
        addView(R.layout.dialog_hint)
        getDialogUtil().getView(R.id.iv_icon).visibility = View.VISIBLE
        width = 800
    }

    open fun show(
        msg: String = "",
        confirmMsg: String = "",
        onConfirm: onClick? = null,
        cancelMsg: String = "",
        onCancel: onClick? = null,
        isCancelable: Boolean = true,
        gravity: Int = Gravity.CENTER_HORIZONTAL
    ) {
        getDialogUtil().getView(R.id.root).minimumWidth = width
        super.show(confirmMsg, onConfirm, cancelMsg, onCancel, isCancelable)
        getView<TextView>(R.id.tv_msg).let {
            if (msg.isNullOrEmpty()) {
                it.visibility = View.GONE
            } else {
                it.text = msg
                it.visibility = View.VISIBLE
                it.gravity = gravity
            }
        }
    }
}
