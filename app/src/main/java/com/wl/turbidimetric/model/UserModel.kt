package com.wl.turbidimetric.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
@SuppressWarnings("")
data class UserModel(
    @PrimaryKey(autoGenerate = true)
    var userId: Long = 0,
    var userName: String = "",
    var password: String = "",
    var level: Int = 2,
    var createTime: String = ""
) {
    fun isAdmin(): Boolean {
        return level == 0 || level == 1
    }

    fun showLevel(): String {
        return when (level) {
            0 -> "生产厂家"
            1 -> "管理员"
            2 -> "普通用户"
            else -> "未知"
        }
    }
}
