package com.wl.turbidimetric.datamanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemDatamanagerResultBinding
import com.wl.turbidimetric.model.TestResultModel
import com.wl.wllib.LogToFile.i
import com.wl.wllib.toLongTimeStr
import com.wl.wllib.toTimeStr

class DataManagerAdapter :
    PagingDataAdapter<TestResultModel, DataManagerAdapter.DataManagerViewHolder>(diffCallback = MyDiff()) {
    //局部刷新 选择改变
    val REFRESH_SELECT_CHANGE = 100

    class MyDiff : DiffUtil.ItemCallback<TestResultModel>() {
        override fun areItemsTheSame(
            oldItem: TestResultModel,
            newItem: TestResultModel
        ): Boolean {
            return oldItem.id == newItem.id;
        }

        override fun areContentsTheSame(
            oldItem: TestResultModel,
            newItem: TestResultModel
        ): Boolean {
            return oldItem == newItem;
        }
    }

    public var onLongClick: ((pos: Long) -> Unit)? = null
    public var onSelectChange: ((pos: Int, selected: Boolean) -> Unit)? = null


    class DataManagerViewHolder(val binding: ItemDatamanagerResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: TestResultModel?, onLongClick: ((pos: Long) -> Unit)?) {
            binding.setVariable(BR.item, item)
            binding.tvID.text = item?.id.toString()
            binding.tvDetectionNum.text = item?.detectionNum ?: "-"
            binding.tvName.text = item?.name ?: "-"
            binding.tvGender.text = item?.gender ?: "-"
            binding.tvAge.text = item?.age ?: "-"
            binding.tvAbsorbances.text = item?.absorbances?.toString() ?: "-"
            binding.tvResult.text = item?.testResult ?: "-"
            binding.tvConcentration.text = item?.concentration?.toString() ?: "-"
            binding.tvTestTime.text =
                if (item?.testTime == 0L) "-" else item?.testTime?.toTimeStr() ?: "-"
            binding.tvTestValue1.text = item?.testValue1?.toString() ?: "-"
            binding.tvTestValue2.text = item?.testValue2?.toString() ?: "-"
            binding.tvTestValue3.text = item?.testValue3?.toString() ?: "-"
            binding.tvTestValue4.text = item?.testValue4?.toString() ?: "-"
            binding.tvTestOriginalValue1.text = item?.testOriginalValue1?.toString() ?: "-"
            binding.tvTestOriginalValue2.text = item?.testOriginalValue2?.toString() ?: "-"
            binding.tvTestOriginalValue3.text = item?.testOriginalValue3?.toString() ?: "-"
            binding.tvTestOriginalValue4.text = item?.testOriginalValue4?.toString() ?: "-"


            binding.root.setOnLongClickListener {
                onLongClick?.invoke(item?.id ?: 0)
                true
            }
        }
    }


    fun getSelectedItems(): List<TestResultModel> {
//        val items = mutableListOf<TestResultModel>().apply {
//            for (i in 0 until itemCount) {
//                getItem(i)?.let {
//                    if (it.isSelect)
//                        add(it)
//                }
//            }
//        }
        val items = mutableListOf<TestResultModel>().apply {
            snapshot().items.forEach {
                if (it.isSelect)
                    add(it)
            }
        }
        return items
    }

    override fun onBindViewHolder(
        holder: DataManagerViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNullOrEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val tem = payloads[0]
            if (tem is Int) {
                when (tem) {
                    REFRESH_SELECT_CHANGE -> {
                        getItem(position)?.let {
                            if (it.isSelect) {
                                holder.binding.root.setBackgroundResource(R.drawable.bg_item_select)
                            } else {
                                holder.binding.root.setBackgroundColor(Color.WHITE)
                            }
                            holder.binding.ivSelect.isSelected = it?.isSelect ?: false
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: DataManagerViewHolder, position: Int) {
        if (holder is DataManagerViewHolder) {
            val item = getItem(holder.absoluteAdapterPosition)
            holder.bindData(item, onLongClick)

            holder.binding.root.setOnClickListener {
                item?.let { item ->
                    item.isSelect = !item.isSelect
                    snapshot()[holder.absoluteAdapterPosition]?.isSelect = item.isSelect
                    notifyItemChanged(holder.absoluteAdapterPosition, REFRESH_SELECT_CHANGE)
                }
            }
            holder.binding.ivSelect.isSelected = item?.isSelect ?: false
            item?.let {
                if (it.isSelect) {
                    holder.binding.root.setBackgroundResource(R.drawable.bg_item_select)
                } else {
                    holder.binding.root.setBackgroundColor(Color.WHITE)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataManagerViewHolder {
        val binding = DataBindingUtil.inflate<ItemDatamanagerResultBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_datamanager_result,
            parent,
            false
        )
        return DataManagerViewHolder(binding)
    }


}
