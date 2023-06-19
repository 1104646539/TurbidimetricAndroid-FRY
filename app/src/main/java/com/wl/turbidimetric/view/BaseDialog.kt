package com.wl.turbidimetric.view

import android.content.Context
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import com.wl.turbidimetric.R
import com.wl.wllib.DialogUtil

/**
 * 基础的dialog
 * 基于DialogUtil实现的
 * @property context Context
 * @property dialogUtil DialogUtil
 * @constructor
 */
open class BaseDialog(private val context: Context) {
    lateinit var btnConfirm: Button
    lateinit var btnCancel: Button

    var width:Int = 1200
    var height:Int = WindowManager.LayoutParams.WRAP_CONTENT
    protected val dialogUtil: DialogUtil = DialogUtil(context).apply {
        setView(R.layout.dialog_base)
    }

    init {
        btnConfirm = getView<Button>(R.id.btn_confirm)
        btnCancel = getView<Button>(R.id.btn_cancel)
    }

    @JvmName("getDialogUtil1")
    fun getDialogUtil() = dialogUtil
    fun dismiss() {
        dialogUtil?.dismiss()
    }

    fun isShow(): Boolean {
        return dialogUtil?.isShowing ?: false
    }

    open fun addView(viewId: Int?) {
        addView(
            if (viewId == null) null else LayoutInflater.from(context).inflate(viewId, null, false)
        )
    }

    open fun addView(view: View?) {
        view?.let {
            (dialogUtil.getView(R.id.root) as LinearLayout).addView(it, 1)
        }
    }

    open fun show(
        confirmMsg: String = "",
        onConfirm: onClick? = null,
        cancelMsg: String = "",
        onCancel: onClick? = null,
        isCancelable: Boolean = true
    ) {

        btnConfirm.let {
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
        btnCancel.let {
            if (cancelMsg.isNullOrEmpty() || onCancel == null) {
                dialogUtil.getView(R.id.btn_cancel).visibility = View.GONE
            } else {
                dialogUtil.getView(R.id.btn_cancel).visibility = View.VISIBLE
                it.text = cancelMsg
                it.setOnClickListener {
                    onCancel?.invoke(this)
                }
            }
        }
        dialogUtil.setCancelable(isCancelable)
        dialogUtil.show(width,height)
    }

    open fun <T : View> getView(id: Int): T {
        return getDialogUtil().getView(id) as T
    }
}

typealias onClick = (dialog: BaseDialog) -> Unit
