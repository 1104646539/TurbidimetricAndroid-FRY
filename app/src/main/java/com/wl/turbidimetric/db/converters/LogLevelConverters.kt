package com.wl.turbidimetric.db.converters

import androidx.room.TypeConverter
import com.wl.turbidimetric.log.LogLevel
import java.math.BigDecimal

class LogLevelConverters {
    @TypeConverter
    fun fromLogLevel(value: Int): LogLevel {
        val vs = LogLevel.values()
        for (i in vs.indices) {
            if (vs[i].value == value) {
                return vs[i];
            }
        }
        return vs[0]
    }

    @TypeConverter
    fun logLevelToInt(level: LogLevel): Int {
        return level.value
    }
}
