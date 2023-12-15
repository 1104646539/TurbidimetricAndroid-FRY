package com.wl.turbidimetric.db.converters

import androidx.room.TypeConverter

class IntArrayConverters {
    @TypeConverter
    fun fromIntArray(value: IntArray?): String {
        if (value == null || value.isEmpty()) else ""
        return value!!.joinToString(",")
    }

    @TypeConverter
    fun stringToIntArray(str: String?): IntArray {
        return if (str.isNullOrEmpty()) intArrayOf()
        else str.split(",").map {
            it.toInt()
        }.toIntArray()
    }
}
