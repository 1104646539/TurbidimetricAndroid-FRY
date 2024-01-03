package com.wl.turbidimetric.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ProjectModel(
    @PrimaryKey(autoGenerate = true)
    var projectId: Long = 0,
    var projectName: String = "",
    var projectCode: String = "",
    var projectLjz: Int = 100,
    var projectUnit: String = "ng/mL",
    var createTime: String = "",
    var isSelect: Boolean = false,
)  {
    override fun toString(): String {
        return "ProjectModel(id=$projectId,projectName=$projectName,projectCode=$projectCode,projectLjz=$projectLjz,projectUnit=$projectUnit,createTime=$createTime)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProjectModel) return false

        if (projectId != other.projectId) return false
        if (projectName != other.projectName) return false
        if (projectCode != other.projectCode) return false
        if (projectLjz != other.projectLjz) return false
        if (projectUnit != other.projectUnit) return false
        if (createTime != other.createTime) return false
        if (isSelect != other.isSelect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = projectId.hashCode()
        result = 31 * result + projectName.hashCode()
        result = 31 * result + projectCode.hashCode()
        result = 31 * result + projectLjz
        result = 31 * result + projectUnit.hashCode()
        result = 31 * result + createTime.hashCode()
        result = 31 * result + isSelect.hashCode()
        return result
    }
}
