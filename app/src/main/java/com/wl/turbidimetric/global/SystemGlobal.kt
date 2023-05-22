package com.wl.turbidimetric.global

import androidx.lifecycle.MutableLiveData
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.MachineState
import com.wl.turbidimetric.model.MatchingArgState
import com.wl.turbidimetric.model.TestState
import com.wl.wllib.QRCodeUtil

object SystemGlobal {
    var uPath: String? = null
    lateinit var qrCode: QRCodeUtil;
    var testState = TestState.None
    var matchingTestState = MatchingArgState.None
    var machineArgState = MachineState.None

    var isCodeDebug = false;

    val shitTubeDoorIsOpen = MutableLiveData(false)
    val cuvetteDoorIsOpen = MutableLiveData(false)



    val icons = mutableListOf(
        R.drawable.icon_navigation_home,
        R.drawable.icon_navigation_datamanager,
        R.drawable.icon_navigation_parameterlist,
        R.drawable.icon_navigation_settings,
        R.drawable.icon_navigation_settings,
    )
}
