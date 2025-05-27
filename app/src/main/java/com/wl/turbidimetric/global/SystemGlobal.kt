package com.wl.turbidimetric.global

import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.hl7.util.defaultConfig
import com.wl.turbidimetric.view.dialog.LeftNavigationView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SystemGlobal {
//    /**
//     * 检测模式状态
//     */
//    var testState = TestState.None
//        set(value) {
//            field = value
//            _obTestState.value = value
//        }
//    private val _obTestState = MutableStateFlow(TestState.None)
//    val obTestState = _obTestState.asStateFlow()


    /**
     * 是否是在只使用安卓板时调试用的
     */
    var isCodeDebug = true


    /**
     * 是否是在升级中
     */
    var mcuUpdate = false

    /**
     * 上传配置
     */
    var uploadConfig = defaultConfig()
        set(value) {
            field = value
            _obUploadConfig.value = value
        }
    private val _obUploadConfig = MutableStateFlow(uploadConfig)

    //显示的曲线数量
    const val showCurveSize = 10;

    /**
     * 上传连接状态
     */
    var connectStatus: ConnectStatus = ConnectStatus.NONE
        set(value) {
            field = value
            _obConnectStatus.value = value
        }
    private val _obConnectStatus = MutableStateFlow(connectStatus)
    val obConnectStatus = _obConnectStatus.asStateFlow()

    /**
     * 下位机版本
     */
    var mcuVersion = ""

    /**
     * 上位机软件版本名
     */
    var versionName = ""

    /**
     * 上位机软件版本号
     */
    var versionCode = 0

    /**
     * 首页的导航资源
     */
    val navItems = mutableListOf(
        LeftNavigationView.NavItem(
            R.drawable.left_nav_analyse,
            R.drawable.left_nav_analyse_selected,
            getResource().getString(R.string.nav_analyse)
        ),
        LeftNavigationView.NavItem(
            R.drawable.left_nav_datamanager,
            R.drawable.left_nav_datamanager_selected,
            getResource().getString(R.string.nav_datamanager)
        ),
        LeftNavigationView.NavItem(
            R.drawable.left_nav_matching,
            R.drawable.left_nav_matching_selected,
            getResource().getString(R.string.nav_matching)
        ),
        LeftNavigationView.NavItem(
            R.drawable.left_nav_settings,
            R.drawable.left_nav_settings_selected,
            getResource().getString(R.string.nav_settings)
        ),
    )
}
