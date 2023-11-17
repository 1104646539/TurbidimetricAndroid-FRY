package com.wl.turbidimetric.view.dialog

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.upload.model.Patient

class PatientInfoDialog(private val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_patient_info) {
    private var rv: RecyclerView? = null
    private var patients: MutableList<Patient> = mutableListOf()
    private var adapter: PatientInfoAdapter? = null
    override fun initDialogView() {
        rv = findViewById(R.id.rv)
        adapter = PatientInfoAdapter(patients)
        rv?.let {
            it.layoutManager = LinearLayoutManager(ct, LinearLayoutManager.VERTICAL, false)
            it.adapter = adapter
        }

    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

    fun showPatient(data: List<Patient>, onConfirm: (List<Patient>) -> Any?, onCancel: () -> Any?) {
        super.show()
        patients.clear()
        patients.addAll(data)
        adapter?.notifyDataSetChanged()

        this.confirmClick = {
            onConfirm.invoke(patients)
        }
        this.confirmText = "确定"
        this.cancelClick = {
            onCancel.invoke()
        }
        this.confirmText = "取消"
    }
}
