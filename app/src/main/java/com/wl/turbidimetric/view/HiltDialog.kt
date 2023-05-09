package com.wl.turbidimetric.view

import android.content.Context
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
    }

    open fun show(
        msg: String = "",
        confirmMsg: String = "",
        onConfirm: onClick? = null,
        cancelMsg: String = "",
        onCancel: onClick? = null,
        isCancelable: Boolean = true
    ) {
        super.show(confirmMsg, onConfirm, cancelMsg, onCancel, isCancelable)
        getView<TextView>(R.id.tv_msg).let {
            if (msg.isNullOrEmpty()) {
                it.visibility = View.GONE
            } else {
                it.text = msg
                it.visibility = View.VISIBLE
            }
        }
    }
}
//open class HiltDialog(private val context: Context) {
//    private val dialogUtil: DialogUtil by lazy {
//        DialogUtil(context).apply {
//            setView(R.layout.dialog_hint)
//        }
//    }
//
//    fun dismiss() {
//        dialogUtil?.dismiss()
//    }
//
//    fun isShow(): Boolean {
//        return dialogUtil?.isShowing ?: false
//    }
//
//    open fun show(
//        msg: String = "",
//        confirmMsg: String = "",
//        onConfirm: onClick? = null,
//        cancelMsg: String = "",
//        onCancel: onClick? = null,
//        isCancelable: Boolean = true
//    ) {
//        (dialogUtil.getView(R.id.tv_msg) as TextView).let {
//            if (msg.isNullOrEmpty()) {
//                it.visibility = View.GONE
//            } else {
//                it.visibility = View.VISIBLE
//                it.text = msg
//            }
//        }
//
//        (dialogUtil.getView(R.id.btn_confirm) as TextView).let {
//            if (confirmMsg.isNullOrEmpty() || onConfirm == null) {
//                it.visibility = View.GONE
//            } else {
//                it.visibility = View.VISIBLE
//                it.text = confirmMsg
//                it.setOnClickListener {
//                    onConfirm?.invoke(this)
//                }
//            }
//        }
//        (dialogUtil.getView(R.id.btn_cancel) as TextView).let {
//            if (cancelMsg.isNullOrEmpty() || onCancel == null) {
//                dialogUtil.getView(R.id.btn_cancel).visibility = View.GONE
//            } else {
//                dialogUtil.getView(R.id.btn_cancel).visibility = View.VISIBLE
//                it.text = cancelMsg
//                it.setOnClickListener {
//                    onCancel?.invoke(this)
//                }
//            }
//        }
//        dialogUtil.setCancelable(isCancelable)
//        dialogUtil.show()
//    }
//
//}
//typealias onClick = (hiltDialog: HiltDialog) -> Unit
