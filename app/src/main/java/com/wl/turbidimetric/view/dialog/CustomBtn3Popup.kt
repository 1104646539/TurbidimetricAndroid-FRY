package com.wl.turbidimetric.view.dialog

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.lxj.xpopup.animator.PopupAnimator
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.core.CenterPopupView
import com.wl.turbidimetric.R

/**
 * 带三个按钮的dialog
 * @property ctx Context
 * @property viewId Int
 * @property btnConfirm Button?
 * @property btnConfirm2 Button?
 * @property btnCancel Button?
 * @property confirmText String?
 * @property confirmClick Function1<[@kotlin.ParameterName] BasePopupView, Unit>?
 * @property confirmText2 String?
 * @property confirmClick2 Function1<[@kotlin.ParameterName] BasePopupView, Unit>?
 * @property cancelText String?
 * @property cancelClick Function1<[@kotlin.ParameterName] BasePopupView, Unit>?
 * @constructor
 */
abstract class CustomBtn3Popup(val ctx: Context, val viewId: Int) : CenterPopupView(ctx) {
    var btnConfirm: Button? = null
    var btnConfirm2: Button? = null
    var btnCancel: Button? = null
    var ivIcon: ImageView? = null

    var confirmText: String? = null
    var confirmClick: onClick? = null

    var confirmText2: String? = null
    var confirmClick2: onClick? = null

    var cancelText: String? = null
    var cancelClick: onClick? = null
    val imm: InputMethodManager =
        ctx.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var layoutRootView: LinearLayout? = null

    // 返回自定义弹窗的布局
    override fun getImplLayoutId(): Int {
        return R.layout.dialog_base
    }

    // 执行初始化操作，比如：findView，设置点击，或者任何你弹窗内的业务逻辑
    override fun onCreate() {
        super.onCreate()
        layoutRootView = (findViewById<LinearLayout>(R.id.root))
        LayoutInflater.from(context).inflate(viewId, null)?.let {
            layoutRootView!!.addView(it, 1)
        }
        btnConfirm = findViewById(R.id.btn_confirm)
        btnConfirm2 = findViewById(R.id.btn_confirm2)
        btnCancel = findViewById(R.id.btn_cancel)
        ivIcon = findViewById(R.id.iv_icon)

        initDialogView()

        setContent()
    }

    override fun dismiss() {
        imm.hideSoftInputFromWindow(rootView.windowToken, 0)
        super.dismiss()
    }

    abstract fun initDialogView()

    abstract fun getResId(): Int
    abstract fun showIcon(): Boolean

    open fun setContent() {
        btnConfirm?.visibility = confirmText?.isNotEmpty().isShow()

        confirmClick?.let { click -> btnConfirm?.setOnClickListener { click.invoke(this) } }

        btnConfirm2?.visibility = confirmText2?.isNotEmpty().isShow()
        confirmClick2?.let { click -> btnConfirm2?.setOnClickListener { click.invoke(this) } }

        btnCancel?.visibility = cancelText?.isNotEmpty().isShow()
        cancelClick?.let { click -> btnCancel?.setOnClickListener { click.invoke(this) } }

        btnConfirm?.text = confirmText
        btnConfirm2?.text = confirmText2
        btnCancel?.text = cancelText

        ivIcon?.visibility = showIcon().isShow()
        if (showIcon() && getResId() != 0) {
            ivIcon?.setImageResource(getResId())
        }
    }

    //    /**
//     * 弹窗的宽度，用来动态设定当前弹窗的宽度，受getMaxWidth()限制
//     *
//     * @return
//     */
//    override fun getPopupWidth(): Int {
//        return 0
//    }
//
//    /**
//     * 弹窗的高度，用来动态设定当前弹窗的高度，受getMaxHeight()限制
//     *
//     * @return
//     */
//    override fun getPopupHeight(): Int {
//        return 0
//    }
}
typealias onClick = (dialog: BasePopupView) -> Unit

