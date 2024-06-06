package com.wl.turbidimetric.matchingargs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemMatchingStateResultBinding
import com.wl.turbidimetric.ex.getIndexOrNullDefault
import com.wl.turbidimetric.view.dialog.isShow

class MatchingStateAdapter(
    private val matchingNum: Int,
    private val items: List<List<Double>>,
    private val isQuality: Boolean
) :
    RecyclerView.Adapter<MatchingStateAdapter.MatchingStateViewHolder>() {
    class MatchingStateViewHolder(
        private val matchingNum: Int,
        private val isQuality: Boolean,
        val binding: ItemMatchingStateResultBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: List<Double>) {
            binding.tvResult1.text = getIndexOrNullDefault(item, 0, "-")
            binding.tvResult2.text = getIndexOrNullDefault(item, 1, "-")
            binding.tvResult3.text = getIndexOrNullDefault(item, 2, "-")
            binding.tvResult4.text = getIndexOrNullDefault(item, 3, "-")
            binding.tvResult5.text = getIndexOrNullDefault(item, 4, "-")
            binding.tvResult6.text = getIndexOrNullDefault(item, 5, "-")
            binding.tvResult7.text = getIndexOrNullDefault(item, 6, "-")
            binding.tvResult8.text = getIndexOrNullDefault(item, 7, "-")
            if (isQuality) {
                binding.tvQualityL.text = getIndexOrNullDefault(item, matchingNum, "-")
                binding.tvQualityH.text = getIndexOrNullDefault(item, matchingNum + 1, "-")
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchingStateViewHolder {
        return MatchingStateViewHolder(
            matchingNum,
            isQuality,
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_matching_state_result, parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return if (items.size < 3) 3 else items.size
    }

    override fun onBindViewHolder(holder: MatchingStateViewHolder, position: Int) {
        holder.binding.tvResult6.visibility = (matchingNum > 5).isShow()
        holder.binding.tvResult7.visibility = (matchingNum > 6).isShow()
        holder.binding.tvResult8.visibility = (matchingNum > 7).isShow()
        holder.binding.tvQualityL.visibility = isQuality.isShow()
        holder.binding.tvQualityH.visibility = isQuality.isShow()
        holder.binding.tvResultHeader.text = "${position + 1}"
        items.getOrNull(position)?.let {
            holder.bind(it)
        }
    }
}
