package com.wl.turbidimetric.ob

import io.objectbox.converter.PropertyConverter
import java.math.BigDecimal

class BigDecimalConverter : PropertyConverter<BigDecimal, String> {
    override fun convertToEntityProperty(databaseValue: String?): BigDecimal {
        return BigDecimal(databaseValue)
    }

    override fun convertToDatabaseValue(entityProperty: BigDecimal?): String {
        return entityProperty.toString()
    }
}
