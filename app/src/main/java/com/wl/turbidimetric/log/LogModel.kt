package com.wl.turbidimetric.log

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogLevel(val value: Int, val state: String) {
    WARRING(0, "警告"), ERROR(1, "错误")
}

@Entity
data class LogModel(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var tag: String = "",
    var content: String = "",
    var time: Long = 0,
    var level: LogLevel = LogLevel.WARRING
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is LogModel) return false

        if (other.id != id) return false
        if (other.tag != tag) return false
        if (other.content != content) return false
        if (other.time != time) return false
        if (other.level != level) return false
        return super.equals(other)
    }
}

