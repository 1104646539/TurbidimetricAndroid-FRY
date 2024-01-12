package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getIndexOrNullDefault
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.matchingargs.MatchingStateAdapter
import com.wl.wllib.LogToFile.i

/**
 * 显示拟合中的状态
 *
 * 比如已经拟合过的结果
 */
class MatchingStateDialog(val ct: Context) :
    CustomBtn3Popup(ct, R.layout.dialog_matching_state) {
    var vHeader: View? = null
    var vFooter: View? = null
    var tvHeaderTitle: TextView? = null
    var tvFooterTitle: TextView? = null
    var tvHeader1: TextView? = null
    var tvHeader2: TextView? = null
    var tvHeader3: TextView? = null
    var tvHeader4: TextView? = null
    var tvHeader5: TextView? = null
    var tvHeader6: TextView? = null
    var tvHeader7: TextView? = null
    var tvHeader8: TextView? = null
    var tvFooter1: TextView? = null
    var tvFooter2: TextView? = null
    var tvFooter3: TextView? = null
    var tvFooter4: TextView? = null
    var tvFooter5: TextView? = null
    var tvFooter6: TextView? = null
    var tvFooter7: TextView? = null
    var tvFooter8: TextView? = null
    var rv: RecyclerView? = null

    var abss: MutableList<MutableList<Double>> = mutableListOf()
    var matchingNum: Int = 5
    var targets: MutableList<Double> = mutableListOf()
    var muans: MutableList<Double> = mutableListOf()

    var adapter: MatchingStateAdapter? = null
    override fun initDialogView() {
        vHeader = findViewById(R.id.incHeader)
        vFooter = findViewById(R.id.incFooter)
        rv = findViewById(R.id.rv)
        vHeader?.let { it ->
            tvHeaderTitle = it.findViewById(R.id.tv_result_header)
            tvHeader1 = it.findViewById(R.id.tv_result_1)
            tvHeader2 = it.findViewById(R.id.tv_result_2)
            tvHeader3 = it.findViewById(R.id.tv_result_3)
            tvHeader4 = it.findViewById(R.id.tv_result_4)
            tvHeader5 = it.findViewById(R.id.tv_result_5)
            tvHeader6 = it.findViewById(R.id.tv_result_6)
            tvHeader7 = it.findViewById(R.id.tv_result_7)
            tvHeader8 = it.findViewById(R.id.tv_result_8)
        }
        vFooter?.let { it ->
            tvFooterTitle = it.findViewById(R.id.tv_result_header)
            tvFooter1 = it.findViewById(R.id.tv_result_1)
            tvFooter2 = it.findViewById(R.id.tv_result_2)
            tvFooter3 = it.findViewById(R.id.tv_result_3)
            tvFooter4 = it.findViewById(R.id.tv_result_4)
            tvFooter5 = it.findViewById(R.id.tv_result_5)
            tvFooter6 = it.findViewById(R.id.tv_result_6)
            tvFooter7 = it.findViewById(R.id.tv_result_7)
            tvFooter8 = it.findViewById(R.id.tv_result_8)
        }


        i("$tvHeaderTitle $tvFooterTitle")
    }

    override fun setContent() {
        super.setContent()

        tvHeaderTitle?.text = "目标值"
        tvFooterTitle?.text = "平均值"

        tvHeader6?.visibility = (matchingNum > 5).isShow()
        tvHeader7?.visibility = (matchingNum > 6).isShow()
        tvHeader8?.visibility = (matchingNum > 7).isShow()
        tvFooter6?.visibility = (matchingNum > 5).isShow()
        tvFooter7?.visibility = (matchingNum > 6).isShow()
        tvFooter8?.visibility = (matchingNum > 7).isShow()

        adapter = MatchingStateAdapter(matchingNum, abss)
        rv?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rv?.adapter = adapter

        tvHeader6?.text = getIndexOrNullDefault(targets, 5, "-")
        tvHeader7?.text = getIndexOrNullDefault(targets, 6, "-")
        tvHeader8?.text = getIndexOrNullDefault(targets, 7, "-")

    }

    fun showDialog(
        matchingNum: Int,
        abss: MutableList<MutableList<Double>>,
        targets: List<Double>,
        muans: List<Double>
    ) {
        this.matchingNum = matchingNum
        this.abss.clear()
        this.abss.addAll(abss)
        this.targets.clear()
        this.targets.addAll(targets)
        this.muans.clear()
        this.muans.addAll(muans)

        this.confirmText = "添加拟合数据"
        this.confirmClick = {
            toast("click confirm")
        }
        this.confirmText2 = "拟合"
        this.confirmClick2 = {
            toast("click matching")
        }
        this.cancelText = "取消"
        this.cancelClick = {
            toast("click cancel")
        }

        if (isCreated) {
            setContent()
        }
        super.show()
    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }
}
