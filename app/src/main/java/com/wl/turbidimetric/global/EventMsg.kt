package com.wl.turbidimetric.global

class EventMsg<T>(val what: Int, val data: T? = null)


object EventGlobal {
    /**
     * 扫码模块初始化
     */
    const val WHAT_INIT_QRCODE = 100


    /**
     * 首页是否获取温度
     */
    const val WHAT_GET_TEMP_CHANGE = 110

    /**
     * 拟合增加了曲线
     */
    const val WHAT_PROJECT_ADD = 120

    /**
     * 自检结束
     */
    const val WHAT_HIDE_SPLASH = 130

    /**
     * 上传配置发生变化
     */
    const val WHAT_UPLOAD_CHANGE = 140
    /**
     * 编号发生变化
     */
    const val WHAT_DETECTION_NUM_CHANGE = 150
    /**
     * HomeFragment首屏加载完毕
     */
    const val WHAT_HOME_INIT_FINISH = 160
}
