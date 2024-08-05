package com.wl.turbidimetric.datamanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemDatamanagerResultBinding
import com.wl.turbidimetric.model.ResultState
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.wllib.LogToFile.i
import com.wl.wllib.toTimeStr

class DataManagerAdapter :
    PagingDataAdapter<TestResultAndCurveModel, DataManagerAdapter.DataManagerViewHolder>(
        diffCallback = MyDiff()
    ) {
    //局部刷新 选择改变
    private val REFRESH_SELECT_CHANGE = 100

    class MyDiff : DiffUtil.ItemCallback<TestResultAndCurveModel>() {
        override fun areItemsTheSame(
            oldItem: TestResultAndCurveModel,
            newItem: TestResultAndCurveModel
        ): Boolean {
            return oldItem.result.resultId == newItem.result.resultId && oldItem.curve?.curveId == newItem.curve?.curveId
        }

        override fun areContentsTheSame(
            oldItem: TestResultAndCurveModel,
            newItem: TestResultAndCurveModel
        ): Boolean {
            return oldItem == newItem
        }
    }

    var onLongClick: ((pos: Long) -> Unit)? = null
    private val selectedIds = HashSet<Long>() // 存储选中的 item 的 ID

    /**
     * 清除选中的数据
     */
    fun clearSelected() {
        selectedIds.clear()
    }

    class DataManagerViewHolder(
        val binding: ItemDatamanagerResultBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: TestResultAndCurveModel?) {
//            binding.setVariable(BR.item, item)
            binding.tvId.text = item?.result?.resultId.toString()
            binding.tvDetectionNum.text = item?.result?.detectionNum ?: "-"
            binding.tvProjectName.text = item?.curve?.projectName ?: "-"
            binding.tvName.text = item?.result?.name ?: "-"
            binding.tvGender.text = item?.result?.gender ?: "-"
            binding.tvBarcode.text = item?.result?.sampleBarcode ?: "-"
            binding.tvAge.text = item?.result?.age ?: "-"
            binding.tvAbsorbances.text =
                item?.result?.absorbances?.toInt().toString()
//            binding.tvAbsorbances.text =
//                item?.result?.absorbances?.setScale(5, RoundingMode.HALF_UP).toString()
//            binding.tvResult.text = item?.result?.testResult ?: "-"
            binding.tvConcentration.text = item?.result?.concentration?.toString() ?: "-"
            binding.tvTestTime.text =
                if (item?.result?.testTime == 0L) "-" else item?.result?.testTime?.toTimeStr()
                    ?: "-"
            binding.tvTestValue1.text =
                item?.result?.testValue1?.toInt().toString()
            binding.tvTestValue2.text =
                item?.result?.testValue2?.toInt().toString()
            binding.tvTestValue3.text =
                item?.result?.testValue3?.toInt().toString()
            binding.tvTestValue4.text =
                item?.result?.testValue4?.toInt().toString()
            binding.tvTestOriginalValue1.text = item?.result?.testOriginalValue1?.toString() ?: "-"
            binding.tvTestOriginalValue2.text = item?.result?.testOriginalValue2?.toString() ?: "-"
            binding.tvTestOriginalValue3.text = item?.result?.testOriginalValue3?.toString() ?: "-"
            binding.tvTestOriginalValue4.text = item?.result?.testOriginalValue4?.toString() ?: "-"
            if (item?.result?.uploaded == true) {
                binding.ivStateUpload.setImageResource(R.drawable.icon_state_upload_finish)
            } else {
                binding.ivStateUpload.setImageResource(R.drawable.icon_state_upload_wait)
            }
            updateResult(binding, item)

        }

        private fun updateResult(
            binding: ItemDatamanagerResultBinding,
            item: TestResultAndCurveModel?
        ) {

            if (item?.result?.resultState == ResultState.SamplingFailed.ordinal || item?.result?.resultState == ResultState.TakeReagentFailed.ordinal) {//取样失败提示
                val ori: Int = item?.result?.resultState ?: 0
                binding.tvResult.text = ResultState.values()[ori].state
            } else {
                binding.tvResult.text = item?.result?.testResult ?: "-"
            }
        }
    }


    fun getSelectedItems(): List<TestResultAndCurveModel> {
        val items = mutableListOf<TestResultAndCurveModel>().apply {
            snapshot().items.forEach {
                if (selectedIds.contains(it.result.resultId)) {
                    add(it)
                }
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
                            val itemId = it.result.resultId
                            if (selectedIds.contains(itemId)) {
                                holder.binding.root.setBackgroundResource(R.drawable.bg_item_select)
                                holder.binding.ivSelect.isSelected = true
                            } else {
                                holder.binding.root.setBackgroundColor(Color.WHITE)
                                holder.binding.ivSelect.isSelected = false
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: DataManagerViewHolder, position: Int) {
        if (holder is DataManagerViewHolder) {
            val item = getItem(holder.absoluteAdapterPosition)
            holder.bindData(item)

            holder.binding.root.setOnClickListener {
                item?.result?.let { item ->
                    if (selectedIds.contains(item.resultId)) {
                        selectedIds.remove(item.resultId)
                    } else {
                        selectedIds.add(item.resultId)
                    }
                    notifyItemChanged(holder.absoluteAdapterPosition, REFRESH_SELECT_CHANGE)
                }
            }
            holder.binding.root.setOnLongClickListener {
                onLongClick?.invoke(item?.result?.resultId ?: 0)
                true
            }
            holder.binding.ivSelect.isSelected =
                selectedIds.contains(item?.result?.resultId ?: false)
            item?.result?.let {
                if (selectedIds.contains(it.resultId)) {
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
