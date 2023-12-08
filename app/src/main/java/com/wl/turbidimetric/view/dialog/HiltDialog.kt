package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.view.MyScrollView

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
    var sv: MyScrollView? = null
    var scMaxHeight:Int = 0
    var scMaxWidth:Int = 0
//    override fun getMaxWidth(): Int {
//        return 0
//    }
//
//    override fun getPopupWidth(): Int {
//        return 0
//    }

    override fun initDialogView() {
        tvMsg = findViewById(R.id.tv_msg)
        sv = findViewById(R.id.sv)
    }



    var iconId: Int = 0
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
        this.sv?.setMaxHeight(scMaxHeight)
//        this.sv?.setMaxWidth(scMaxWidth)
    }

    fun showDialog(
        msg: String,
        confirmText: String = "",
        confirmClick: onClick? = null,
        cancelText: String = "",
        cancelClick: onClick? = null,
        showIcon: Boolean = false,
        iconId: Int = 0,
        scMaxHeight:Int = 0,
    ) {
        this.scMaxHeight = scMaxHeight
        this.msg = msg
        this.confirmText = confirmText
        this.confirmClick = confirmClick
        this.cancelText = cancelText
        this.cancelClick = cancelClick

        this.iconId = iconId
        this.showIcon = showIcon

        if (isCreated) {
            setContent()
        }

        super.show()
    }


}
