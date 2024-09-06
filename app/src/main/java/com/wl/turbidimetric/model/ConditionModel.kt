package com.wl.turbidimetric.model

data class ConditionModel(
    val name: String = "",
    val qrcode: String = "",
    val conMin: Int = 0,
    val conMax: Int = 0,
    val results: Array<String> = arrayOf(),
    val selectProjects: Array<String> = arrayOf(),
    val testTimeMin: Long = 0,
    val testTimeMax: Long = 0,
)
