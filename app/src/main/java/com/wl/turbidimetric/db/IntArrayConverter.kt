package com.wl.turbidimetric.db

import android.text.TextUtils
import io.objectbox.converter.PropertyConverter

class IntArrayConverter : PropertyConverter<IntArray?, String?> {
    override fun convertToEntityProperty(databaseValue: String?): IntArray? {
        if (TextUtils.isEmpty(databaseValue)) {
            return null
        }
        val newValue = databaseValue?.replace(" ","")
        val st = newValue!!.split(",").toTypedArray()
        return if (st.isEmpty()) {
            null
        } else {
            val ins = IntArray(st.size)
            for (i in st.indices) {
                ins[i] = st[i].toInt()
            }
            ins
        }
    }

    override fun convertToDatabaseValue(entityProperty: IntArray?): String? {
        return if (entityProperty == null || entityProperty.isEmpty()) {
            ""
        } else entityProperty.joinToString()
    }
}
