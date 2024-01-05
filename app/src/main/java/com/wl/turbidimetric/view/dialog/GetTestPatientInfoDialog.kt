package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.widget.Button
import android.widget.EditText
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.upload.model.GetPatientType

class GetTestPatientInfoDialog(val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_get_test_patient_info) {
    var etCondition1: EditText? = null
    var etCondition2: EditText? = null
    var tb: TabLayout? = null
    var selectIndex = 0
    override fun initDialogView() {
        etCondition1 = findViewById(R.id.et_condition1)
        etCondition2 = findViewById(R.id.et_condition2)
        tb = findViewById(R.id.tb)

        tb?.apply {
            addTab(newTab().setText("编号"))
            addTab(newTab().setText("条码"))
            addTab(newTab().setText("时间"))

            addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    selectIndex = tab?.position ?: 0

                    if (selectIndex == 0) {
                        etCondition1?.setText("1")
                        etCondition2?.setText("3")
                    } else if (selectIndex == 1) {
                        etCondition1?.setText("ABCD")
                        etCondition2?.setText("")
                    } else {
                        etCondition1?.setText("20220202020202")
                        etCondition2?.setText("20220203020202")
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {

                }
            })
            selectTab(getTabAt(0))
            etCondition1?.setText("1")
            etCondition2?.setText("3")
        }
    }

    fun show(onConfirm: (String, String, GetPatientType) -> Any?): BasePopupView {
        super.show()

        this.confirmClick = {
            val condition1 = etCondition1?.text.toString()
            val condition2 = etCondition2?.text.toString()
            val type = when (selectIndex) {
                0 -> {
                    GetPatientType.SN
                }
                1 -> {
                    GetPatientType.BC
                }
                else -> {
                    GetPatientType.DT
                }
            }
            onConfirm.invoke(
                condition1, condition2, type
            )
        }
        this.confirmText = "获取"
        this.cancelClick = {
            it.dismiss()
        }
        this.cancelText = "取消"
        if (isCreated) {
            setContent()
        }
        return this
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }
}
