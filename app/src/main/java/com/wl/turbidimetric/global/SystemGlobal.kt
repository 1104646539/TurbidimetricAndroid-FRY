package com.wl.turbidimetric.global

import com.wl.turbidimetric.model.MatchingArgState
import com.wl.turbidimetric.model.TestState
import com.wl.wllib.QRCodeUtil

object SystemGlobal {
    var uPath: String? = null
    lateinit var qrCode: QRCodeUtil;
    var testState = TestState.None
    var matchingTestState = MatchingArgState.None

    var isCodeDebug = false;

}
