package com.wl.turbidimetric.test

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseDifferAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.DatamanagerItemResultBinding
import com.wl.turbidimetric.datamanager.DataManagerAdapter
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.TestResultModel

class TestDataAdapter :
    BaseDifferAdapter<TestResultModel, TestDataAdapter.TestDataViewHolder>(MyDiff()) {
    public var onLongClick: ((pos: Long) -> Unit)? = null

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

    class TestDataViewHolder(private val binding: DatamanagerItemResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: TestResultModel?, onLongClick: ((pos: Long) -> Unit)?) {
            binding.setVariable(BR.item, item)
            binding.tvID.text = item?.id.toString()
            binding.tvDetectionNum.text = item?.detectionNum ?: "-"
            binding.tvName.text = item?.name ?: "-"
            binding.tvGender.text = item?.gender ?: "-"
            binding.tvAge.text = item?.age ?: "-"
            binding.tvAbsorbances.text = item?.absorbances?.scale(3).toString()
            binding.tvResult.text = item?.testResult ?: "-"
            binding.tvConcentration.text = item?.concentration.toString() ?: "-"
            binding.tvTestTime.text = item?.testTime ?: "-"
            binding.tvTestValue1.text = item?.testValue1.toString()
            binding.tvTestValue2.text = item?.testValue2.toString()
            binding.tvTestValue3.text = item?.testValue3.toString()
            binding.tvTestValue4.text = item?.testValue4.toString()

            binding.root.setOnLongClickListener {
                onLongClick?.invoke(item?.id ?: 0)
                true
            }
        }
    }

    override fun onBindViewHolder(
        holder: TestDataViewHolder,
        position: Int,
        item: TestResultModel?
    ) {
        if (holder is TestDataAdapter.TestDataViewHolder) {
            holder.bindData(getItem(holder.absoluteAdapterPosition), onLongClick)
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): TestDataViewHolder {
        val binding = DataBindingUtil.inflate<DatamanagerItemResultBinding>(
            LayoutInflater.from(parent.context),
            R.layout.datamanager_item_result,
            parent,
            false
        )
        return TestDataAdapter.TestDataViewHolder(binding)
    }
}

