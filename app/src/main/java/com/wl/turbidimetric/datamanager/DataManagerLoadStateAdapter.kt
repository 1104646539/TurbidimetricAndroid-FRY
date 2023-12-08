package com.wl.turbidimetric.datamanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemDatamanagerFooterBinding
import com.wl.wllib.LogToFile.i

class DataManagerLoadStateAdapter :
    LoadStateAdapter<DataManagerLoadStateAdapter.DataManagerFooter>() {
    class DataManagerFooter(val bd: ItemDatamanagerFooterBinding) :
        RecyclerView.ViewHolder(bd.root)

    override fun onBindViewHolder(holder: DataManagerFooter, loadState: LoadState) {
        i("loadState=$loadState" )
        if (loadState is LoadState.NotLoading && loadState.endOfPaginationReached ) {
            //没有下一页
            holder.bd.tvState.text = "没有更多数据了"
            i("没有更多数据了")
        } else if (loadState is LoadState.Loading) {
            holder.bd.tvState.text = "加载中"
            i("加载中")
        } else if (loadState is LoadState.Error) {
            holder.bd.tvState.text = "加载错误，请重试"
            i("加载错误，请重试")
        } else {
            i("loadState=$loadState")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): DataManagerFooter {
        val bd: ItemDatamanagerFooterBinding =
            DataBindingUtil.inflate<ItemDatamanagerFooterBinding>(
                LayoutInflater.from(parent.context),
                R.layout.item_datamanager_footer,
                parent,
                false
            )
        return DataManagerFooter(bd)
    }

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading ||
                loadState is LoadState.Error
//                ||(loadState is LoadState.NotLoading && loadState.endOfPaginationReached);
    }
}
