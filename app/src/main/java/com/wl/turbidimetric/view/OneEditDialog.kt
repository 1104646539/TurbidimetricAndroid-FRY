package com.wl.turbidimetric.view

import android.content.Context
import android.text.InputType
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
import android.text.method.DigitsKeyListener
import android.widget.EditText
import android.widget.TextView
import com.wl.turbidimetric.R

class OneEditDialog(val context: Context) : BaseDialog(context) {
    lateinit var etContent: EditText
    lateinit var tvTitle: TextView

    init {
        addView(R.layout.dialog_one_edit)
        etContent = getView<EditText>(R.id.et_content)
        tvTitle = getView<TextView>(R.id.tv_title)
    }

    open fun show(
        title: String = "",
        content: String = "",
        confirmMsg: String,
        onConfirm: (OneEditDialog) -> Unit,
        cancelMsg: String,
        onCancel: onClick?,
        isCancelable: Boolean
    ) {
        super.show(confirmMsg, { onConfirm }, cancelMsg, onCancel, isCancelable);
        tvTitle.text = title

        if (!content.isNullOrEmpty()) {
            etContent?.let {
                it.setText(content)
                it.setSelection(content.length);
            }
        }
        btnConfirm.setOnClickListener {
            onConfirm?.invoke(this)
        }
    }

    open fun setEditType(editType: EditType) {
        when (editType) {
            EditType.NUM_POSITIVE -> {
                etContent.inputType = TYPE_NUMBER_FLAG_SIGNED
                etContent.keyListener = DigitsKeyListener(false, false)
            }
            EditType.NUM -> {
                etContent.inputType = TYPE_NUMBER_FLAG_DECIMAL
                etContent.keyListener = DigitsKeyListener(false, true)
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
}
