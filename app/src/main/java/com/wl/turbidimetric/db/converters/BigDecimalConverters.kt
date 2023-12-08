package com.wl.turbidimetric.db.converters

import androidx.room.TypeConverter
import java.math.BigDecimal

class BigDecimalConverters {
    @TypeConverter
    fun fromDouble(value: Double):BigDecimal{
        return BigDecimal(value)
    }

    @TypeConverter
    fun bigDecimalToDouble(bigDecimal: BigDecimal):Double?{
        return bigDecimal.toDouble()
    }
}
