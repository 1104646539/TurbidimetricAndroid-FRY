package com.wl.turbidimetric.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import java.math.BigDecimal

@Entity
data class ProjectModel(
    @Id
    var projectId: Long = 0,
    var projectName: String = "",
    var projectCode: String = "",
    var projectLjz: Int = 0,
    var projectUnit: String = "",
    var a1: Double = 0.0,
    var a2: Double = 0.0,
    var x0: Double = 0.0,
    var p: Double = 0.0,
    var fitGoodness: Double = 0.0,
    var createTime: String = ""
) : BaseOBModel(0)
