package com.wl.turbidimetric.ex

import androidx.datastore.preferences.core.*
import com.wl.turbidimetric.datastore.LocalDataGlobal.cache
import com.wl.turbidimetric.datastore.LocalDataGlobal.datas
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class IntDataStoreProperty(defaultValue: Int = 0) :
    DataStoreProperty<Int>(defaultValue) {
    override fun getKey(keyName: String): Preferences.Key<Int> {
        return intPreferencesKey(keyName)
    }
}

class FloatDataStoreProperty(defaultValue: Float = 0f) :
    DataStoreProperty<Float>(defaultValue) {
    override fun getKey(keyName: String): Preferences.Key<Float> {
        return floatPreferencesKey(keyName)
    }
}

class DoubleDataStoreProperty(defaultValue: Double = 0.0) :
    DataStoreProperty<Double>(defaultValue) {
    override fun getKey(keyName: String): Preferences.Key<Double> {
        return doublePreferencesKey(keyName)
    }
}

class BooleanDataStoreProperty(defaultValue: Boolean = false) :
    DataStoreProperty<Boolean>(defaultValue) {
    override fun getKey(keyName: String): Preferences.Key<Boolean> {
        return booleanPreferencesKey(keyName)
    }
}

class StringDataStoreProperty(defaultValue: String = "") :
    DataStoreProperty<String>(defaultValue) {
    override fun getKey(keyName: String): Preferences.Key<String> {
        return stringPreferencesKey(keyName)
    }
}

class LongDataStoreProperty(defaultValue: Long = 0) :
    DataStoreProperty<Long>(defaultValue) {
    override fun getKey(keyName: String): Preferences.Key<Long> {
        return longPreferencesKey(keyName)
    }
}

abstract class DataStoreProperty<T>(
    private val defaultValue: T
) :
    ReadWriteProperty<Any, T> {
    var c: T? = null
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (c == null) {
            c = cache?.get(getKey(property.name)) ?: defaultValue
        }
        return c!!
    }

    abstract fun getKey(keyName: String): Preferences.Key<T>

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T)= runBlocking {
            datas.edit {
                if (value != null) {
                    it[getKey(property.name)] = value
                } else {
                    it.remove(getKey(property.name))
                }
            }
            c = value

    }
}
