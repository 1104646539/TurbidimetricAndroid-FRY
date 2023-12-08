package com.wl.turbidimetric.matchingargs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemMatchingargsBinding
import com.wl.turbidimetric.ex.scaleStr
import com.wl.turbidimetric.model.CurveModel
import com.wl.wllib.LogToFile.i
class MatchingArgsAdapter :
    RecyclerView.Adapter<MatchingArgsAdapter.MatchingArgsViewHolder>() {
    var onSelectChange: ((CurveModel) -> Unit)? = null


    val items: MutableList<CurveModel> = mutableListOf()
    fun submit(items: MutableList<CurveModel>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    class MatchingArgsViewHolder(
        val binding: ItemMatchingargsBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: CurveModel?) {
            binding.setVariable(BR.item, item)
            binding.tvID.text = (item?.reagentNO ?: "-").toString()
            binding.tvA1.text = (item?.f0 ?: 0.0).scaleStr(6).toString()
            binding.tvA2.text = (item?.f1 ?: 0.0).scaleStr(6).toString()
            binding.tvX0.text = (item?.f2 ?: 0.0).scaleStr(6).toString()
            binding.tvP.text = (item?.f3 ?: 0.0).scaleStr(6).toString()
            binding.tvTestTime.text = item?.createTime ?: "-"
            binding.tvFitGoodness.text = (item?.fitGoodness ?: 0.0).scaleStr(6).toString()
        }
    }


    override fun onBindViewHolder(holder: MatchingArgsViewHolder, position: Int) {
        if (holder is MatchingArgsViewHolder) {
            val item = items[holder.absoluteAdapterPosition]
//            i("holder.absoluteAdapterPosition=${holder.absoluteAdapterPosition} item=$item")
            holder.bindData(item)

            holder.binding.root.setOnClickListener {
                val item = items[holder.absoluteAdapterPosition]
                i("holder.absoluteAdapterPosition2=${holder.absoluteAdapterPosition} bindingAdapterPosition=${holder.bindingAdapterPosition} oldPosition=${holder.oldPosition} adapterPosition=${holder.adapterPosition} layoutPosition=${holder.layoutPosition}")
                item.let { item ->
                    it?.let { view ->
                        if (item.isSelect) {
                            return@setOnClickListener
                        }
                        item.isSelect = true
                        view.isSelected = true

                        holder.binding.ivSelect.isSelected = true
                        holder.binding.root.setBackgroundResource(R.drawable.bg_item_select)
                        setSelectIndex(holder.absoluteAdapterPosition)
                    }
                }
            }
            holder.binding.ivSelect.isSelected = item.isSelect
            item.let {
                if (it.isSelect) {
                    holder.binding.root.setBackgroundResource(R.drawable.bg_item_select)
                } else if (holder.absoluteAdapterPosition % 2 == 0) {
                    holder.binding.root.setBackgroundColor(Color.WHITE)
                } else {
                    holder.binding.root.setBackgroundResource(R.drawable.rip_item)
                }
            }
        }
    }

    var selectPos = -1
    var oldSelectPos = -1
    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchingArgsViewHolder {
        val binding = DataBindingUtil.inflate<ItemMatchingargsBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_matchingargs,
            parent,
            false
        )
        return MatchingArgsViewHolder(binding)
    }

    fun setSelectIndex(pos: Int) {
        if (pos >= items.size) {
            return
        }
        oldSelectPos = selectPos
        selectPos = pos

        if (oldSelectPos in items.indices) {
            items[oldSelectPos].isSelect = false
            notifyItemChanged(oldSelectPos, "change")
        }

        if (selectPos in items.indices) {
            onSelectChange?.invoke((items[selectPos]))
            items[selectPos].isSelect = true
        }
    }

    override fun getItemCount(): Int {
        return if (items.isNullOrEmpty()) {
            0
        } else if (items.size > 10) {
            10
        } else {
            items.size
        }
    }
}
