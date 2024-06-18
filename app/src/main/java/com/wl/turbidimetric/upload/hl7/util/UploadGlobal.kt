package com.wl.turbidimetric.upload.hl7.util

object UploadGlobal {
    /**
     * 消息Id，由仪器方生成，并非每条消息+1，而是完整的一套流程+1
     */
    var MSG_ID: Long = 1

    /**
     * 查询id，每次查询+1
     */
    var QUERY_ID: Long = 1

    /**
     * 上传文件名
     */
    val UploadConfigFileName = "/sdcard/UploadConfig.txt"

    /**
     * 上传间隔
     */
    val UpoadInterval = 500L

}
