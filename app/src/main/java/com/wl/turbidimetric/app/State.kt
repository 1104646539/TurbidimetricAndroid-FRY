package com.wl.turbidimetric.app

import com.wl.turbidimetric.R

/**
 * 仪器状态
 */
enum class MachineState(val id: Int, val str: String) {
    None(R.drawable.state_machine_none, "未自检"),
    MachineError(R.drawable.state_machine_none, "自检失败"),
    MachineNormal(R.drawable.state_machine_none, "正常的"),
    MachineRunningError(R.drawable.state_machine_none, "运行错误"),
}

/**
 * 上传状态
 */
enum class UploadState(val id: Int, val str: String) {
    None(R.drawable.state_upload_none, "未初始化"),
    ReConnection(R.drawable.state_upload_none, "重新连接中"),
    Connected(R.drawable.state_upload_connected, "已连接"),
    Disconnected(R.drawable.state_upload_none, "已断开"),
}

/**
 * u盘状态
 */
enum class StorageState(val id: Int, val str: String) {
    None(R.drawable.state_storage_none, "未插入"),
    Inserted(R.drawable.state_storage_insert, "已插入，请等待"),
    Exist(R.drawable.state_storage_normal, "已授权，可使用"),
    Unauthorized(R.drawable.state_storage_unauthorized, "U盘未授权");

    /**
     * U盘已可使用
     * @return Boolean
     */
    fun isExist(): Boolean {
        return this == Exist
    }
}
