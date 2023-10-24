package com.wl.turbidimetric.global

import androidx.lifecycle.MutableLiveData
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.view.NavigationView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SystemGlobal {
    //检测模式状态
    var testState = TestState.None
        set(value) {
            field = value
            _obTestState.value = value
        }
    //检测类型
    var testType = TestType.None


//    //拟合状态
//    var matchingTestState = MatchingArgState.None
//        set(value) {
//            field = value
//            obMatchingTestState.postValue(value)
//        }
//
//    //重复性检测状态
//    var repeatabilityState = RepeatabilityState.None
//
//    //仪器当前状态,是否自检等
//    var machineArgState = MachineState.None

    //仪器检测模式
//    var machineTestModel = MachineTestModel.Auto

    var isCodeDebug = false;

    //可监听的检测状态
    private val _obTestState = MutableStateFlow(TestState.None)
    val obTestState = _obTestState.asStateFlow()
//    //可监听的拟合状态
//    val obMatchingTestState = MutableLiveData(MatchingArgState.None)

    //首页的导航资源
    val navItems = mutableListOf(
        NavigationView.NavItem(
            R.drawable.icon_analyse,
            getResource().getString(R.string.nav_analyse)
        ),
        NavigationView.NavItem(
            R.drawable.icon_datamanager,
            getResource().getString(R.string.nav_datamanager)
        ),
        NavigationView.NavItem(
            R.drawable.icon_matching,
            getResource().getString(R.string.nav_matching)
        ),
        NavigationView.NavItem(
            R.drawable.icon_settings,
            getResource().getString(R.string.nav_settings)
        ),
    )
}
