package com.wl.turbidimetric.matchingargs

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.powerspinner.DefaultSpinnerAdapter
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.skydoves.powerspinner.PowerSpinnerInterface
import com.skydoves.powerspinner.PowerSpinnerView
import com.skydoves.powerspinner.databinding.PowerspinnerItemDefaultPowerBinding
import com.wl.turbidimetric.databinding.ItemSelectProjectBinding
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.ProjectModel

internal val NO_INT_VALUE: Int = Int.MIN_VALUE
internal val NO_SELECTED_INDEX: Int = -1

class SelectCoverAdapter(
    powerSpinnerView: PowerSpinnerView
) : RecyclerView.Adapter<SelectCoverAdapter.SelectCoverViewHolder>(),
    PowerSpinnerInterface<ProjectModel> {


    override var index: Int = powerSpinnerView.selectedIndex
    override val spinnerView: PowerSpinnerView = powerSpinnerView
    override var onSpinnerItemSelectedListener: OnSpinnerItemSelectedListener<ProjectModel>? = null

    private val spinnerItems: MutableList<ProjectModel> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectCoverViewHolder {
        val binding =
            ItemSelectProjectBinding.inflate(
                LayoutInflater.from(parent.context), parent,
                false
            )

        return SelectCoverViewHolder(binding).apply {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?: return@setOnClickListener
                notifyItemSelected(position)
            }
        }

    }

    override fun onBindViewHolder(holder: SelectCoverViewHolder, position: Int) {
        holder.bind(spinnerItems[position], spinnerView)
    }

    override fun setItems(itemList: List<ProjectModel>) {
        this.spinnerItems.clear()
        this.spinnerItems.addAll(itemList)
        this.index = NO_SELECTED_INDEX
        notifyDataSetChanged()
    }

    fun projectStr(projectModel: ProjectModel): String {
        return "序号: ${if (projectModel.reagentNO.isNullOrEmpty()) "-" else projectModel.reagentNO} 检测时间:${projectModel.createTime}\n" +
                "f0:${projectModel.f0.scale(4).toString()} f1:${
                    projectModel.f1.scale(4).toString()
                } f2:${projectModel.f2.scale(4).toString()} f3:${
                    projectModel.f3.scale(4).toString()
                } "
    }

    override fun notifyItemSelected(index: Int) {
        if (index == NO_SELECTED_INDEX) return
        val oldIndex = this.index
        this.index = index
        this.spinnerView.notifyItemSelected(index, projectStr(spinnerItems[index]))
        this.onSpinnerItemSelectedListener?.onItemSelected(
            oldIndex = oldIndex,
            oldItem = oldIndex.takeIf { it != NO_SELECTED_INDEX }?.let { spinnerItems[oldIndex] },
            newIndex = index,
            newItem = spinnerItems[index]
        )
    }

    override fun getItemCount(): Int = spinnerItems.size

    public class SelectCoverViewHolder(private val binding: ItemSelectProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        internal fun bind(item: ProjectModel, spinnerView: PowerSpinnerView) {
            binding.root.setPadding(
                spinnerView.paddingLeft,
                spinnerView.paddingTop,
                spinnerView.paddingRight,
                spinnerView.paddingBottom
            )
            if (spinnerView.spinnerItemHeight != NO_INT_VALUE) {
//                binding.root.height = spinnerView.spinnerItemHeight
//                binding.root.updateLayoutParams {
//                    height = spinnerView.spinnerItemHeight
//                }
//                binding.root.minimumHeight
            }
            binding.apply {
                tvA1.text = "f0:" + item.f0?.scale(6).toString()
                tvA2.text = "f1:" + item.f1?.scale(6).toString()
                tvX0.text = "f2:" + item.f2?.scale(6).toString()
                tvP.text = "f3:" + item.f3?.scale(6).toString()
                tvTime.text = "时间:" + item.createTime
                tvNO.text =
                    "序号:" + if (item.reagentNO.isNullOrEmpty()) "-" else item.reagentNO
            }

        }
    }
}
