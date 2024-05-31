package com.wl.turbidimetric.app

import com.wl.turbidimetric.R

/**
 * 仪器状态
 */
enum class MachineState(val id: Int, val str: String) {
    None(R.drawable.state_machine_none, "仪器未自检"),
    MachineError(R.drawable.state_machine_error, "仪器自检失败"),
    MachineNormal(R.drawable.state_machine_normal, "仪器正常运行"),
    MachineRunningError(R.drawable.state_machine_error, "仪器运行错误"),
}

/**
 * 上传状态
 */
enum class UploadState(val id: Int, val str: String) {
    None(R.drawable.state_upload_none, "上传未初始化"),
    ReConnection(R.drawable.state_upload_none, "上传重新连接中"),
    Connected(R.drawable.state_upload_connected, "上传已连接"),
    Disconnected(R.drawable.state_upload_none, "上传已断开"),
}

/**
 * u盘状态
 */
enum class StorageState(val id: Int, val str: String) {
    None(R.drawable.state_storage_none, "U盘未插入"),
    Inserted(R.drawable.state_storage_insert, "U盘已插入，请等待授权"),
    Exist(R.drawable.state_storage_normal, "U盘已授权，可使用"),
    Unauthorized(R.drawable.state_storage_unauthorized, "U盘未授权");

    /**
     * U盘已可使用
     * @return Boolean
     */
    fun isExist(): Boolean {
        return this == Exist
    }
}

/**
 * 打印机状态
 */
enum class PrinterState(val id: Int, val str: String) {
    None(R.drawable.state_printer_none, "打印机未使用"),
    NotInstallApk(R.drawable.state_printer_err, "打印程序未安装"),
    InitSdkFailed(R.drawable.state_printer_err, "打印驱动初始化失败"),
    Success(R.drawable.state_printer_normal, "打印机正在使用"),
    NotPrinter(R.drawable.state_printer_err, "未选择打印机");

}
