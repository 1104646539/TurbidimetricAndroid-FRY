package com.wl.turbidimetric.view

import android.content.Context
import android.view.View
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.wllib.DialogUtil

class HiltDialog(private val context: Context) {
    private val dialogUtil: DialogUtil by lazy {
        DialogUtil(context).apply {
            setView(R.layout.dialog_hint)
        }
    }

    fun dismiss() {
        dialogUtil?.dismiss()
    }

    fun isShow(): Boolean {
        return dialogUtil?.isShowing
    }

    open fun show(
        msg: String = "",
        confirmMsg: String = "",
        onConfirm: onClick? = null,
        cancelMsg: String = "",
        onCancel: onClick? = null,
        isCancelable: Boolean = true
    ) {
        (dialogUtil.getView(R.id.tv_msg) as TextView).let {
            if (msg.isNullOrEmpty()) {
                it.visibility = View.GONE
            } else {
                it.visibility = View.VISIBLE
                it.text = msg
            }
        }

        (dialogUtil.getView(R.id.btn_confirm) as TextView).let {
            if (confirmMsg.isNullOrEmpty() || onConfirm == null) {
                it.visibility = View.GONE
            } else {
                it.visibility = View.VISIBLE
                it.text = confirmMsg
                it.setOnClickListener {
                    onConfirm?.invoke(this)
                }
            }
        }
        (dialogUtil.getView(R.id.btn_cancel) as TextView).let {
            if (cancelMsg.isNullOrEmpty() || onCancel == null) {
                dialogUtil.getView(R.id.btn_ll2).visibility = View.GONE
            } else {
                dialogUtil.getView(R.id.btn_ll2).visibility = View.VISIBLE
                it.text = cancelMsg
                it.setOnClickListener {
                    onCancel?.invoke(this)
                }
            }
        }
        dialogUtil.setCancelable(isCancelable)
        dialogUtil.show()
    }

}
typealias onClick = (hiltDialog: HiltDialog) -> Unit
