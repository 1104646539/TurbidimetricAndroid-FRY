package com.wl.turbidimetric.settings.log

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentLogListBinding
import com.wl.turbidimetric.log.LogLevel
import com.wl.turbidimetric.matchingargs.SpnSampleAdapter
import com.wl.turbidimetric.repository.if2.LogCondition
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.log

class LogListFragment :
    BaseFragment<LogListViewModel, FragmentLogListBinding>(R.layout.fragment_log_list) {
    override val vm: LogListViewModel by viewModels { LogListViewModelFactory() }

    override fun initViewModel() {
    }

    override fun init(savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    private var logListAdapter: LogListAdapter? = null
    private var logLevels = mutableListOf<String>().apply {
        add("全部")
        addAll(LogLevel.values().map { it.state })
    }

    private var logTime = mutableListOf("今天", "最近30天", "全部")

    private var logLevelAdapter: SpnSampleAdapter? = null
    private var logTimeAdapter: SpnSampleAdapter? = null

    private var selectLogLevel = "";
    private var selectLogTime = "";
    private fun initView() {
        logLevelAdapter = SpnSampleAdapter(requireContext(), logLevels)
        vd.spnLevel.adapter = logLevelAdapter

        logTimeAdapter = SpnSampleAdapter(requireContext(), logTime)
        vd.spnTime.adapter = logTimeAdapter

        selectLogLevel = logLevels.first()
        selectLogTime = logTime.first()

        vd.rvLogList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        logListAdapter = LogListAdapter()
        vd.rvLogList.adapter = logListAdapter

        vd.spnLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectLogLevel = logLevels[position]
                conditionChange()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectLogLevel = logLevels[0]
            }
        }
        vd.spnTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectLogTime = logTime[position]
                conditionChange()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectLogTime = logTime[0]
            }
        }

        lifecycleScope.launch {
            vm.conditionModel.collectLatest {
                queryLogList(it)
            }
        }

        vd.llHeader.llRoot.setBackgroundResource(R.drawable.bg_item)
    }

    /**
     * 获取当天的零点时间戳
     *
     * @return 当天的零点时间戳
     */
    open fun getTodayStartTime(): Long {
        //设置时区
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        return calendar.timeInMillis
    }

    private fun initData() {
    }

    var job: Job? = null
    private fun conditionChange() {
        val logTimeIndex = logTime.indexOf(selectLogTime)
        var startTime = 0L
        val oneDay = 86400000L

        if (logTimeIndex <= 0) {
            startTime = getTodayStartTime()
        } else if (logTimeIndex == 1) {
            startTime = getTodayStartTime() - oneDay * 30
        }

        val logLevelIndex = logLevels.indexOf(selectLogLevel).let {
            if (it == -1) {
                0
            } else it
        }
        val levels = mutableListOf<Int>()
        if (logLevelIndex == 0) {
            levels.add(0)
            levels.add(1)
        } else {
            levels.add(logLevelIndex - 1)
        }
        vm.changeCondition(LogCondition(levels.toList(), startTime))
    }

    private suspend fun queryLogList(logCondition: LogCondition) {
        job?.cancelAndJoin()
        job = lifecycleScope.launch {
            vm.listenerLogList(logCondition)
                .collectLatest {
                    logListAdapter?.submitData(lifecycle,it)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (job != null && job?.isActive == true) {
            job?.cancel()
        }
    }
}
