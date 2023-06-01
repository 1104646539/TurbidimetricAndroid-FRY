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
import com.wl.turbidimetric.databinding.DatamanagerItemResultBinding
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.TestResultModel
import timber.log.Timber

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


    class DataManagerViewHolder(val binding: DatamanagerItemResultBinding) :
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
            binding.tvTestTime.text = item?.testTime ?: "-"
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

            holder.binding.ivSelect.setOnClickListener {
                item?.let { item ->
                    it?.let { view ->
                        item.isSelect = !item.isSelect
                        view.isSelected = !view.isSelected

                        if (holder.binding.ivSelect.isSelected) {
                            holder.binding.root.setBackgroundColor(Color.RED)
                        } else if (holder.absoluteAdapterPosition % 2 == 0) {
                            holder.binding.root.setBackgroundColor(Color.BLUE)
                        } else {
                            holder.binding.root.setBackgroundColor(Color.WHITE)
                        }
                    }
                }
            }
            holder.binding.ivSelect.isSelected = item?.isSelect ?: false
            if (holder.binding.ivSelect.isSelected) {
                holder.binding.root.setBackgroundColor(Color.RED)
            } else if (holder.absoluteAdapterPosition % 2 == 0) {
                holder.binding.root.setBackgroundColor(Color.BLUE)
            } else {
                holder.binding.root.setBackgroundColor(Color.WHITE)
            }
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
