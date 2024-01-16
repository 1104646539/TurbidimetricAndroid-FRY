package com.wl.turbidimetric.db.converters

import androidx.room.TypeConverter

class DoubleArrayConverters {
    @TypeConverter
    fun fromDoubleArray(value: DoubleArray?): String {
        if (value == null || value.isEmpty()) else ""
        return value!!.joinToString(",")
    }

    @TypeConverter
    fun stringToDoubleArray(str: String?): DoubleArray {
        return if (str.isNullOrEmpty()) doubleArrayOf()
        else str.split(",").map {
            it.toDouble()
        }.toDoubleArray()
    }
}
