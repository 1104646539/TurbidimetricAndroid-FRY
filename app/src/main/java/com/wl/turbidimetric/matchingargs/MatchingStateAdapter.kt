package com.wl.turbidimetric.matchingargs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemMatchingStateResultBinding
import com.wl.turbidimetric.ex.getIndexOrNullDefault
import com.wl.turbidimetric.view.dialog.isShow

class MatchingStateAdapter(private val matchingNum: Int, val items: List<List<Double>>) :
    RecyclerView.Adapter<MatchingStateAdapter.MatchingStateViewHolder>() {
    class MatchingStateViewHolder(
        private val matchingNum: Int,
        val binding: ItemMatchingStateResultBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: List<Double>) {
            binding.tvResult1.text = getIndexOrNullDefault(item, 0, "-")
            binding.tvResult2.text = getIndexOrNullDefault(item, 1, "-")
            binding.tvResult3.text = getIndexOrNullDefault(item, 2, "-")
            binding.tvResult4.text = getIndexOrNullDefault(item, 3, "-")
            binding.tvResult5.text = getIndexOrNullDefault(item, 4, "-")
            binding.tvResult6.visibility = (item.size > 5).isShow()
            if (matchingNum > 5) {
                binding.tvResult6.text = getIndexOrNullDefault(item, 5, "-")
            }
            binding.tvResult7.visibility = (item.size > 6).isShow()
            if (matchingNum > 6) {
                binding.tvResult7.text = getIndexOrNullDefault(item, 6, "-")
            }
            binding.tvResult8.visibility = (item.size > 7).isShow()
            if (matchingNum > 7) {
                binding.tvResult8.text = getIndexOrNullDefault(item, 7, "-")
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchingStateViewHolder {
        return MatchingStateViewHolder(
            matchingNum,
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_matching_state_result, parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MatchingStateViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
