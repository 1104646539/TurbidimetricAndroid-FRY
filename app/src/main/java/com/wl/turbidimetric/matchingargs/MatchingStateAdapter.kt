package com.wl.turbidimetric.matchingargs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemMatchingStateType1Binding
import com.wl.turbidimetric.databinding.ItemMatchingStateType2Binding

class MatchingStateAdapter(
    private val items: MutableList<Data>,
    private var result: String
) :
    RecyclerView.Adapter<ViewHolder>() {
    class MatchingStateViewHolderType1(
        val binding: ItemMatchingStateType1Binding
    ) :
        ViewHolder(binding.root) {
        fun bind(data: Data?, index: Int) {
            if (index == 0) {
                binding.tvTitle.text = "序号"
                binding.tvTargetCon.text = "理论浓度"
                binding.tvResultCon.text = "测量浓度"
                binding.tvReactionValue.text = "反应度"
            } else {
                binding.tvTitle.text = "$index"
                binding.tvTargetCon.text = "${data?.targetCon}"
                binding.tvResultCon.text = "${data?.testCon}"
                binding.tvReactionValue.text = "${data?.reactionValue}"
            }
        }
    }

    class MatchingStateViewHolderType2(
        val binding: ItemMatchingStateType2Binding
    ) :
        ViewHolder(binding.root) {
        fun bind(result: String) {
            binding.tvTitle.text = "结论"
            binding.tvResult.text = result
        }
    }

    companion object {
        const val Type_Value = 1
        const val Type_Result = 2
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        if (viewType == Type_Value) {
            return MatchingStateViewHolderType1(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_matching_state_type_1, parent, false
                )
            )
        } else {
            return MatchingStateViewHolderType2(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_matching_state_type_2, parent, false
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) {
            Type_Result
        } else {
            Type_Value
        }
    }

    override fun getItemCount(): Int {
        return items.size + 2
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MatchingStateViewHolderType1) {
            holder.bind(items.getOrNull(position - 1), position)
        } else if (holder is MatchingStateViewHolderType2) {
            holder.bind(result)
        }
    }

    fun update(data: List<Data>, result: String) {
        this.result = result
        this.items.clear()
        this.items.addAll(data)
        notifyDataSetChanged()
    }

    data class Data(val targetCon: String, val testCon: String, val reactionValue: String)
}
