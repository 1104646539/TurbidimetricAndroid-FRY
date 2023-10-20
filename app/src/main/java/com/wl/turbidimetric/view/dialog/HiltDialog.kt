package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.wl.turbidimetric.R

const val ICON_HINT = R.drawable.icon_dialog_hint
const val ICON_FINISH = R.drawable.icon_dialog_detection_finish

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

    var iconId: Int = 0;
    var showIcon = false
    override fun getResId(): Int {
        return iconId
    }

    override fun showIcon(): Boolean {
        return showIcon
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
        showIcon: Boolean = false,
        iconId: Int = 0
    ) {
        this.msg = msg;
        this.confirmText = confirmText
        this.confirmClick = confirmClick
        this.cancelText = cancelText
        this.cancelClick = cancelClick

        this.iconId = iconId
        this.showIcon = showIcon

        if (isShow) {
            setContent()
        }

        super.show()
    }


}
