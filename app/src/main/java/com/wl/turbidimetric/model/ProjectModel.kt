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
    var f0: Double = 0.0,
    var f1: Double = 0.0,
    var f2: Double = 0.0,
    var f3: Double = 0.0,
    var fitGoodness: Double = 0.0,
    var createTime: String = "",
    var isSelect: Boolean = false,
    ) : BaseOBModel(0)
