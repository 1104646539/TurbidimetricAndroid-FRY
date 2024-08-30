package com.wl.turbidimetric.matchingargs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemParamsType1Binding
import com.wl.turbidimetric.databinding.ItemParamsType2Binding
import com.wl.turbidimetric.ex.getEquation2
import com.wl.turbidimetric.ex.getFitGoodness
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.util.FitterType

class MatchingArgsInfoAdapter(private var curveModel: CurveModel? = null) :
    RecyclerView.Adapter<ViewHolder>() {

    companion object {
        const val ViewType_Title = 1;
        const val ViewType_Params = 2;
    }

    fun update(curveModel: CurveModel?) {
        this.curveModel = curveModel
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == ViewType_Params) {
            return MatchingArgsType2ViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_params_type_2,
                    parent,
                    false
                )
            );
        } else {
            return MatchingArgsType1ViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_params_type_1,
                    parent,
                    false
                )
            );
        }
    }

    override fun getItemCount(): Int {
        return 6
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0)
            ViewType_Title
        else
            ViewType_Params
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MatchingArgsType1ViewHolder) {
            holder.bind(curveModel)
        } else if (holder is MatchingArgsType2ViewHolder) {
            holder.bind(curveModel, position)
        }
    }
}

class MatchingArgsType1ViewHolder(private val viewBinding: ItemParamsType1Binding) :
    ViewHolder(viewBinding.root) {
    fun bind(curveModel: CurveModel?) {
        if (curveModel == null) {
            viewBinding.tvValue.text = ""
        } else {
            viewBinding.tvValue.text =
                "${getEquation2(FitterType.toValue(curveModel.fitterType))}"
        }
    }
}

class MatchingArgsType2ViewHolder(private val viewBinding: ItemParamsType2Binding) :
    ViewHolder(viewBinding.root) {
    fun bind(curveModel: CurveModel?, pos: Int) {
        if (pos == 1) {
            viewBinding.tvTitle.text = "a"
            viewBinding.tvValue.text = "${curveModel?.f0?.scale(8) ?: ""}"
        } else if (pos == 2) {
            viewBinding.tvTitle.text = "b"
            viewBinding.tvValue.text = "${curveModel?.f1?.scale(8) ?: ""}"
        } else if (pos == 3) {
            viewBinding.tvTitle.text = "c"
            viewBinding.tvValue.text = "${curveModel?.f2?.scale(8) ?: ""}"
        } else if (pos == 4) {
            viewBinding.tvTitle.text = "d"
            viewBinding.tvValue.text = "${curveModel?.f3?.scale(8) ?: ""}"
        } else if (pos == 5) {
            viewBinding.tvTitle.text = "R²"
            if (curveModel == null) {
                viewBinding.tvValue.text = ""
            } else {
                viewBinding.tvValue.text =
                    "${
                        getFitGoodness(
                            FitterType.toValue(curveModel.fitterType),
                            curveModel.fitGoodness
                        )
                    }"
            }
        }
    }
}
