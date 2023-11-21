package com.wl.turbidimetric.global

import androidx.lifecycle.MutableLiveData
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.hl7.util.UploadGlobal
import com.wl.turbidimetric.upload.hl7.util.defaultConfig
import com.wl.turbidimetric.view.NavigationView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SystemGlobal {
    /**
     * 检测模式状态
     */
    var testState = TestState.None
        set(value) {
            field = value
            _obTestState.value = value
        }
    private val _obTestState = MutableStateFlow(TestState.None)
    val obTestState = _obTestState.asStateFlow()

    /**
     * 检测类型
     */
    var testType = TestType.None

    /**
     * 是否是在只使用安卓板时调试用的
     */
    var isCodeDebug = true


    /**
     * 是否是调试模式
     */
    var isDebugMode = false
        set(value) {
            field = value
            _obDebugMode.value = value
        }
    private val _obDebugMode = MutableStateFlow(false)
    val obDebugMode = _obDebugMode.asStateFlow()
    /**
     * 上传配置
     */
    var uploadConfig = defaultConfig()
        set(value) {
            field = value
            _obUploadConfig.value = value
        }
    private val _obUploadConfig = MutableStateFlow(uploadConfig)
    val obUploadConfig = _obUploadConfig.asStateFlow()

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
     * 首页的导航资源
     */
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
