package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeProjectAdapter
import com.wl.turbidimetric.model.CurveModel
import java.util.*
import android.animation.ObjectAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

class GetMachineStateDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_getmachine_state) {

    var iv: ImageView? = null
    var animator: ObjectAnimator? = null
    fun rotateImageContinuously(imageView: ImageView) {
        // 创建旋转动画
        if (animator == null) {
            animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f);
            animator?.setDuration(1000); // 持续时间，单位毫秒
            animator?.setInterpolator(LinearInterpolator()); // 线性插值器，保持匀速旋转
            animator?.setRepeatCount(ObjectAnimator.INFINITE); // 无限重复
            animator?.start(); // 开始动画
        }
    }

    override fun setContent() {
        super.setContent()
        iv?.let {
            rotateImageContinuously(it)
        }
    }

    fun show2() {
        if (isCreated) {
            setContent()
        }
        super.show()
    }

    override fun onDismiss() {
        super.onDismiss()
        if (animator != null) {
            animator?.cancel()
            animator = null
        }
    }

    override fun initDialogView() {
        iv = findViewById(R.id.iv)
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
