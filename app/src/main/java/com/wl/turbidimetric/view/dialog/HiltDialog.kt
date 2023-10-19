package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.wl.turbidimetric.R

/**
 * 用来显示提示的对话框
 * @property context Context
 * @constructor
 */
open class HiltDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_hint) {
    var tvMsg: TextView? = null
    var msg: String? = null
    override fun initDialogView() {
        tvMsg = findViewById(R.id.tv_msg)

    }

    override fun setContent() {
        super.setContent()
        tvMsg?.let {
            if (msg.isNullOrEmpty()) {
                it.visibility = View.GONE
            } else {
                it.text = msg
                it.visibility = View.VISIBLE
            }
        }
    }

    fun showDialog(
        msg: String,
        confirmText: String,
        confirmClick: onClick,
        cancelText: String = "",
        cancelClick: onClick? = null,
    ) {
        this.msg = msg;
        this.confirmText = confirmText
        this.confirmClick = confirmClick
        this.cancelText = cancelText
        this.cancelClick = cancelClick

        if (isShow) {
            setContent()
        }

        super.show()
    }


}
