package com.wl.turbidimetric.upload.model

data class Patient(
    val name: String,
    val age: String,
    val sex: String,
    val deliveryTime: String,//送检时间
    val deliveryDoctor: String,//送检医生
    val deliveryDepartments: String,//送检科室
    val bc: String,//条码
    val sn: String,//编号
    val tdh: String//通道号，lis传过来的
)
