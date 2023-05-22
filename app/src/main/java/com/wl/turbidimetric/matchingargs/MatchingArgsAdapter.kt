package com.wl.turbidimetric.matchingargs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemMatchingargsBinding
import com.wl.turbidimetric.datamanager.DataManagerAdapter
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.ProjectModel

class MatchingArgsAdapter :
    PagingDataAdapter<ProjectModel, MatchingArgsAdapter.MatchingArgsViewHolder>(diffCallback = MyDiff()) {
    class MyDiff : DiffUtil.ItemCallback<ProjectModel>() {
        override fun areItemsTheSame(
            oldItem: ProjectModel,
            newItem: ProjectModel
        ): Boolean {
            return oldItem == newItem;
        }

        override fun areContentsTheSame(
            oldItem: ProjectModel,
            newItem: ProjectModel
        ): Boolean {
            return oldItem == newItem;
        }
    }

    class MatchingArgsViewHolder(private val binding: ItemMatchingargsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: ProjectModel?) {
            binding.setVariable(BR.item, item)
            binding.tvID.text = item?.projectId.toString()
            binding.tvProjectName.text = item?.projectName ?: "-"
            binding.tvA1.text = (item?.f0 ?: 0.0).scale(10).toString()
            binding.tvA2.text = (item?.f1 ?: 0.0).scale(10).toString()
            binding.tvX0.text = (item?.f2 ?: 0.0).scale(10).toString()
            binding.tvP.text = (item?.f3 ?: 0.0).scale(10).toString()
            binding.tvTestTime.text = item?.createTime ?: "-"
            binding.tvFitGoodness.text = (item?.fitGoodness ?: 0.0).scale(10).toString()
            binding.tvLjz.text = (item?.projectLjz ?: "-").toString()
            binding.tvUnit.text = (item?.projectUnit ?: "-").toString()
        }
    }

    override fun onBindViewHolder(holder: MatchingArgsViewHolder, position: Int) {
        if (holder is MatchingArgsViewHolder) {
            holder.bindData(getItem(holder.absoluteAdapterPosition))
        }
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
}
