package com.wl.turbidimetric.view.dialog


import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.model.CuvetteState
import com.wl.turbidimetric.model.SampleState
import com.wl.turbidimetric.model.SampleType
import com.wl.turbidimetric.model.TestResultModel

class MatchingConfigDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_matching_config) {


//    override fun show(
//
//    ) {
//        if (isCreated) {
//            setContent()
//        }
//        super.show()
//    }

    override fun initDialogView() {

    }

    override fun setContent() {
        super.setContent()

    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
