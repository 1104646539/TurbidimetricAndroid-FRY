package com.wl.turbidimetric.settings.log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemLogListBinding
import com.wl.turbidimetric.log.LogModel
import com.wl.wllib.toLongTimeStr
import com.wl.wllib.toTimeStr

class LogListItemCallback : DiffUtil.ItemCallback<LogModel>() {
    override fun areItemsTheSame(oldItem: LogModel, newItem: LogModel): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: LogModel, newItem: LogModel): Boolean {
        return oldItem == newItem
    }
}

class LogListAdapter : PagingDataAdapter<LogModel, LogListViewHolder>(LogListItemCallback()) {

    override fun onBindViewHolder(holder: LogListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogListViewHolder {
        return LogListViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context), R.layout.item_log_list, parent, false
            )
        )

    }
}

class LogListViewHolder(val binding: ItemLogListBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: LogModel?) {
        item?.let {
            binding.tvTime.text = it.time.toTimeStr()
            binding.tvLevel.text = it.level.state
            binding.tvContent.text = it.content
            binding.tvTag.text = it.tag
        }
    }

}
