package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
import android.text.method.DigitsKeyListener
import android.widget.EditText
import android.widget.TextView
import com.wl.turbidimetric.R

class OneEditDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_one_edit) {
    var etContent: EditText? = null
    var tvTitle: TextView? = null

    var title: String? = null
    var content: String? = null

    open fun showDialog(
        title: String = "",
        content: String = "",
        confirmMsg: String,
        onConfirm: (OneEditDialog) -> Unit,
        cancelMsg: String,
        onCancel: onClick?,
    ) {

        this.title = title
        this.content = content
        this.confirmText = confirmMsg
        this.confirmClick = { onConfirm.invoke(this) }
        this.cancelText = cancelMsg
        this.cancelClick = onCancel
        if (isCreated) {
            setContent()
        }
        super.show()
    }

    open fun setEditType(editType: EditType) {
        when (editType) {
            EditType.NUM_POSITIVE -> {
                etContent?.inputType = TYPE_NUMBER_FLAG_SIGNED
                etContent?.keyListener = DigitsKeyListener(false, false)
            }
            EditType.NUM -> {
                etContent?.inputType = TYPE_NUMBER_FLAG_DECIMAL
                etContent?.keyListener = DigitsKeyListener(false, true)
            }
            else -> {
            }
        }

    }

    enum class EditType {
        NUM_POSITIVE,//正整数
        NUM,//带符号，带小数的
        TEXT,//文字
    }

    override fun initDialogView() {
        etContent = findViewById(R.id.et_content)
        tvTitle = findViewById(R.id.tv_title)
    }

    override fun setContent() {
        super.setContent()
        tvTitle?.text = title

        if (!content.isNullOrEmpty()) {
            etContent?.let {
                it.setText(content)
                it.setSelection(content!!.length)
            }
        }
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
