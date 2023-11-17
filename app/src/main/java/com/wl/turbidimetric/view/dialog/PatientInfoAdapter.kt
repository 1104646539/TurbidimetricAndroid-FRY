package com.wl.turbidimetric.view.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemPatientInfoBinding
import com.wl.turbidimetric.upload.model.Patient

class PatientInfoAdapter(private val patients: List<Patient>) :
    RecyclerView.Adapter<PatientInfoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientInfoViewHolder {
        val binding = DataBindingUtil.inflate<ItemPatientInfoBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_patient_info,
            parent,
            false
        )
        return PatientInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientInfoViewHolder, position: Int) {
        holder.binding.tvPatientName.text = patients[position].name
        holder.binding.tvPatientAge.text = patients[position].age
        holder.binding.tvPatientSex.text = patients[position].sex
        holder.binding.tvBc.text = patients[position].bc
        holder.binding.tvSn.text = patients[position].sn
        holder.binding.tvTdh.text = patients[position].tdh
        holder.binding.tvDeliveryDoctor.text = patients[position].deliveryDoctor
        holder.binding.tvDeliveryDepartments.text = patients[position].deliveryDepartments
        holder.binding.tvDeliveryTime.text = patients[position].deliveryTime
    }

    override fun getItemCount(): Int {
        return patients.size
    }

}

class PatientInfoViewHolder(var binding: ItemPatientInfoBinding) :
    RecyclerView.ViewHolder(binding.root) {
}
