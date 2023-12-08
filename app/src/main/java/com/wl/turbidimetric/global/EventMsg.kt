package com.wl.turbidimetric.global

 class  EventMsg<T>( val what: Int,) {
    var data: T? = null
}


object EventGlobal {
    /**
     * 扫码模块初始化
     */
    const val WHAT_INIT_QRCODE = 100
}
