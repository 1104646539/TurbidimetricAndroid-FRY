package com.wl.turbidimetric.upload.model

data class GetPatientCondition(
    val condition1: String,
    val condition2: String,
    val type: GetPatientType
)

enum class GetPatientType(val msg: String) {
    BC("BC"),//条码
    SN("SN"),//编号
    DT("DT"),//申请时间
}
