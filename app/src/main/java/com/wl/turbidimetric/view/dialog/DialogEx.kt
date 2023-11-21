package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView

/**
 * 快捷的打开对话框
 * @receiver T
 * @param context Context
 * @param width Int 期望的宽度
 * @param height Int 期望的高度
 * @param isCancelable Boolean 点击弹窗外可取消
 * @param la Function1<[@kotlin.ParameterName] T, Unit>
 */
inline fun <T : BasePopupView> T.showPop(
    context: Context,
    width: Int = 500,
    isCancelable: Boolean = true,
    la: ((popup: T) -> Unit)
) {
    val d = XPopup.Builder(context)
        .maxWidth(width)
        .dismissOnTouchOutside(isCancelable)
        .dismissOnBackPressed(isCancelable)
        .autoOpenSoftInput(false)
        .autoFocusEditText(false)
//        .isRequestFocus(false)
        .asCustom(this)
    la.invoke(d as T)
}

fun Boolean?.isShow(): Int {
    if (this == null) return View.GONE
    return if (this) View.VISIBLE else View.GONE
}
