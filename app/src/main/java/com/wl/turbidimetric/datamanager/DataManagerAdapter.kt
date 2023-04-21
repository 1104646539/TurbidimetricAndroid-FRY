package com.wl.turbidimetric.datamanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.DatamanagerItemResultBinding
import com.wl.turbidimetric.model.TestResultModel

class DataManagerAdapter :
    PagingDataAdapter<TestResultModel, DataManagerAdapter.DataManagerViewHolder>(diffCallback = MyDiff()) {
    class MyDiff : DiffUtil.ItemCallback<TestResultModel>() {
        override fun areItemsTheSame(
            oldItem: TestResultModel,
            newItem: TestResultModel
        ): Boolean {
            return oldItem == newItem;
        }

        override fun areContentsTheSame(
            oldItem: TestResultModel,
            newItem: TestResultModel
        ): Boolean {
            return oldItem == newItem;
        }
    }

    class DataManagerViewHolder(private val binding: DatamanagerItemResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: TestResultModel?) {
            binding.setVariable(BR.item, item)
        }
    }

    override fun onBindViewHolder(holder: DataManagerViewHolder, position: Int) {
        if (holder is DataManagerViewHolder) {
            holder.bindData(getItem(holder.absoluteAdapterPosition))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataManagerViewHolder {
        val binding = DataBindingUtil.inflate<DatamanagerItemResultBinding>(
            LayoutInflater.from(parent.context),
            R.layout.datamanager_item_result,
            parent,
            false
        )
        return DataManagerViewHolder(binding)
    }
}
