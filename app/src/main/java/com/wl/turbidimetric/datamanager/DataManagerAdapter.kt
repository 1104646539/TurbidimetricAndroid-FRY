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
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.wllib.LogToFile.i
import com.wl.wllib.toLongTimeStr
import com.wl.wllib.toTimeStr
import java.math.RoundingMode

class DataManagerAdapter :
    PagingDataAdapter<TestResultAndCurveModel, DataManagerAdapter.DataManagerViewHolder>(
        diffCallback = MyDiff()
    ) {
    //局部刷新 选择改变
    val REFRESH_SELECT_CHANGE = 100

    class MyDiff : DiffUtil.ItemCallback<TestResultAndCurveModel>() {
        override fun areItemsTheSame(
            oldItem: TestResultAndCurveModel,
            newItem: TestResultAndCurveModel
        ): Boolean {
            return oldItem.result?.resultId == newItem.result?.resultId && oldItem.curve?.curveId == newItem.curve?.curveId
        }

        override fun areContentsTheSame(
            oldItem: TestResultAndCurveModel,
            newItem: TestResultAndCurveModel
        ): Boolean {
            return oldItem == newItem
        }
    }

    var onLongClick: ((pos: Long) -> Unit)? = null
    var onSelectChange: ((pos: Int, selected: Boolean) -> Unit)? = null


    class DataManagerViewHolder(val binding: ItemDatamanagerResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: TestResultAndCurveModel?, onLongClick: ((pos: Long) -> Unit)?) {
//            binding.setVariable(BR.item, item)
            binding.tvID.text = item?.result?.resultId.toString()
            binding.tvDetectionNum.text = item?.result?.detectionNum ?: "-"
            binding.tvName.text = item?.result?.name ?: "-"
            binding.tvGender.text = item?.result?.gender ?: "-"
            binding.tvAge.text = item?.result?.age ?: "-"
            binding.tvAbsorbances.text = item?.result?.absorbances?.setScale(6, RoundingMode.HALF_UP).toString() ?: "-"
            binding.tvResult.text = item?.result?.testResult ?: "-"
            binding.tvConcentration.text = item?.result?.concentration?.toString() ?: "-"
            binding.tvTestTime.text =
                if (item?.result?.testTime == 0L) "-" else item?.result?.testTime?.toTimeStr() ?: "-"
            binding.tvTestValue1.text = item?.result?.testValue1?.setScale(6, RoundingMode.HALF_UP).toString() ?: "-"
            binding.tvTestValue2.text = item?.result?.testValue2?.setScale(6, RoundingMode.HALF_UP).toString() ?: "-"
            binding.tvTestValue3.text = item?.result?.testValue3?.setScale(6, RoundingMode.HALF_UP).toString() ?: "-"
            binding.tvTestValue4.text = item?.result?.testValue4?.setScale(6, RoundingMode.HALF_UP).toString() ?: "-"
            binding.tvTestOriginalValue1.text = item?.result?.testOriginalValue1?.toString() ?: "-"
            binding.tvTestOriginalValue2.text = item?.result?.testOriginalValue2?.toString() ?: "-"
            binding.tvTestOriginalValue3.text = item?.result?.testOriginalValue3?.toString() ?: "-"
            binding.tvTestOriginalValue4.text = item?.result?.testOriginalValue4?.toString() ?: "-"


            binding.root.setOnLongClickListener {
                onLongClick?.invoke(item?.result?.resultId ?: 0)
                true
            }
        }
    }


    fun getSelectedItems(): List<TestResultAndCurveModel> {
//        val items = mutableListOf<TestResultAndCurveModel>().apply {
//            for (i in 0 until itemCount) {
//                getItem(i)?.let {
//                    if (it.isSelect)
//                        add(it)
//                }
//            }
//        }
        val items = mutableListOf<TestResultAndCurveModel>().apply {
            snapshot().items.forEach {
                if (it!!.result!!.isSelect)
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
                            if (it!!.result!!.isSelect) {
                                holder.binding.root.setBackgroundResource(R.drawable.bg_item_select)
                            } else {
                                holder.binding.root.setBackgroundColor(Color.WHITE)
                            }
                            holder.binding.ivSelect.isSelected = it.result!!.isSelect
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
                item?.result?.let { item ->
                    item.isSelect = !item.isSelect
                    snapshot()[holder.absoluteAdapterPosition]?.result?.isSelect = item.isSelect
                    notifyItemChanged(holder.absoluteAdapterPosition, REFRESH_SELECT_CHANGE)
                }
            }
            holder.binding.ivSelect.isSelected = item?.result?.isSelect ?: false
            item?.result?.let {
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
