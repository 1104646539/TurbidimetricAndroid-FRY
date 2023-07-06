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
import com.wl.turbidimetric.ex.longToStr
import com.wl.turbidimetric.ex.toDate
import com.wl.turbidimetric.ex.toLongString
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

    public var onLongClick: ((pos: Long) -> Unit)? = null


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
            binding.tvTestTime.text = item?.testTime?.longToStr() ?: "-"
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

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: DataManagerViewHolder, position: Int) {
        if (holder is DataManagerViewHolder) {
            val item = getItem(holder.absoluteAdapterPosition)
            holder.bindData(item, onLongClick)

            holder.binding.root.setOnClickListener {
                item?.let { item ->
                    holder.binding.ivSelect?.let { view ->
                        item.isSelect = !item.isSelect
                        view.isSelected = !view.isSelected

                        if (item.isSelect) {
                            holder.binding.root.setBackgroundResource(R.drawable.bg_item_select)
                        } else if (holder.absoluteAdapterPosition % 2 == 0) {
                            holder.binding.root.setBackgroundColor(Color.WHITE)
                        } else {
                            holder.binding.root.setBackgroundResource(R.drawable.rip_item)
                        }


                    }
                }
            }
            holder.binding.ivSelect.isSelected = item?.isSelect ?: false
            item?.let {
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
