package com.wl.turbidimetric.db.converters

import androidx.room.TypeConverter

class StringToStringListConverters {
        @TypeConverter
        fun fromStringArray(value: Array<String>?): String {
            return value?.joinToString(",") ?: ""
        }

        @TypeConverter
        fun toStringArray(value: String): Array<String> {
            return if (value.isEmpty()) arrayOf() else value.split(",").toTypedArray()
        }
}
