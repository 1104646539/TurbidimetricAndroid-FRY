package com.wl.turbidimetric.view.dialog

import android.content.Context
import com.wl.turbidimetric.R

class MatchingStateDialog(val context: Context) :
    CustomBtn3Popup(context, R.layout.dialog_matching_state) {
    override fun initDialogView() {

    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }
}
