package com.wl.turbidimetric.upload.hl7.util

/**
 * MSA 第2个字段 请求结果状态
 * @property msg String
 * @constructor
 */
enum class Status(val msg: String) {
    AA("AA"),//正常响应
    AE("AE"),//程序出错
    AR("AR"),//拒绝访问
}

/**
 * 根据返回的错误码和错误信息包装为统一的响应结果信息
 */
sealed class MsaStatus {
    data class Success(val msg: String) : MsaStatus()
    data class Error(val code: Int, val msg: String) : MsaStatus()
}

/**
 * 结果MSA
 * @property code Int
 * @property msg String
 * @constructor
 */
enum class ErrorEnum(val code: Int, val msg: String) {
    SUCCESS(0, "OK"),//正常响应
    BE_COMMUNICATION(1, "正在通讯，等勿重复提交"),//正在通讯，请勿重复提交
    NF(10, "NF"),//无匹配的数据，无错误
    AE(100, "AE"),//程序出错
    AR(200, "AR"),//拒绝访问
    NOT_CONNECTED(500, "AR"),//未连接
    ORDER(501, "AE");//其他错误
}

/**
 * 上传结果
 */
enum class UploadResultEnum(val code: Int,val msg: String){
    SUCCESS(0, "OK"),//正常响应

}
/**
 * 根据 ErrorEnum的code，返回ErrorEnum，找不到则返回null
 * @param code Int
 * @return ErrorEnum?
 */
fun errorEnumForCode(code: Int): ErrorEnum? {
    ErrorEnum.values().forEach {
        if (it.code == code) {
            return it
        }
    }
    return null
}

/**
 * 连接状态
 * @property msg String
 * @constructor
 */
enum class ConnectStatus(val msg: String) {
    NONE("未连接"),//未连接
    RECONNECTION("重新连接中"),//重新连接中
    CONNECTED("已连接"),//已连接
    DISCONNECTED("已断开"),//已断开
}

/**
 * 根据返回的错误码和错误信息包装为统一的响应结果信息
 */
sealed class ConnectResult {
    data class Success(val msg: String = "连接成功") : ConnectResult()
    data class AlreadyConnected(val msg: String = "已经连接") : ConnectResult()
    data class OrderError(val code: Int, val msg: String) : ConnectResult()
}
