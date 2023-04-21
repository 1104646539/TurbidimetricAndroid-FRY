package com.wl.turbidimetric.ex

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.wl.turbidimetric.App
import com.wl.turbidimetric.datastore.LocalDataGlobal
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val Data: Preferences by lazy { LocalDataGlobal.data }


public inline fun <reified T> LocalDataGlobal.Key.put(data: T) {
    when (T::class.java) {
        java.lang.Integer::class.java -> {
            println("int2")
            putIntData(data as Int)
        }
        java.lang.Float::class.java -> {
            println("Float2")
            putFloatData(data as Float)
        }
        java.lang.Double::class.java -> {
            println("Double2")
            putDoubleData(data as Double)
        }
        java.lang.String::class.java -> {
            println("String2")
            putStringData(data as String)
        }
        java.lang.Boolean::class.java -> {
            println("Boolean3")
            putBooleanData(data as Boolean)
        }
        else -> {
            throw Exception("不支持的类型")
        }
    }
}


/**
 * 取出Int数据
 */
fun LocalDataGlobal.Key.getIntData(default: Int = 0): Int = runBlocking {
    return@runBlocking App.instance!!.dataStore.data.map {
        it[intPreferencesKey(key)] ?: default
    }.first()
}

fun LocalDataGlobal.Key.getLongData(default: Long = 1L): Long = runBlocking {
    return@runBlocking App.instance!!.dataStore.data.map {
        it[longPreferencesKey(key)] ?: default
    }.first()
}

fun LocalDataGlobal.Key.getFloatData(default: Float = 0.0f): Float = runBlocking {
    return@runBlocking App.instance!!.dataStore.data.map {
        it[floatPreferencesKey(key)] ?: default
    }.first()
}

fun LocalDataGlobal.Key.getDoubleData(default: Double = 0.0): Double = runBlocking {
    return@runBlocking App.instance!!.dataStore.data.map {
        it[doublePreferencesKey(key)] ?: default
    }.first()
}

fun LocalDataGlobal.Key.getStringData(default: String = ""): String = runBlocking {
    return@runBlocking App.instance!!.dataStore.data.map {
        it[stringPreferencesKey(key)] ?: default
    }.first()
}

fun LocalDataGlobal.Key.getBooleanData(default: Boolean = false): Boolean = runBlocking {
    App.instance!!.dataStore.data
    return@runBlocking App.instance!!.dataStore.data.map {
        it[booleanPreferencesKey(key)] ?: default
    }.first()
}

fun <T> LocalDataGlobal.Key.getData(defaultValue: T): T {
    val data = when (defaultValue) {
        is Int -> getIntData(defaultValue)
        is Long -> getLongData(defaultValue)
        is String -> getStringData(defaultValue)
        is Boolean -> getBooleanData(defaultValue)
        is Float -> getFloatData(defaultValue)
        is Double -> getDoubleData(defaultValue)
        else -> throw IllegalArgumentException("不支持取这个类型")
    }
    return data as T
}

fun LocalDataGlobal.Key.putBooleanData(value: Boolean) = runBlocking {
    App.instance!!.dataStore!!.edit {
        it[booleanPreferencesKey(key)] = value
    }
}

fun LocalDataGlobal.Key.putStringData(value: String) = runBlocking {
    App.instance!!.dataStore!!.edit {
        it[stringPreferencesKey(key)] = value
    }
}

fun LocalDataGlobal.Key.putFloatData(value: Float) = runBlocking {
    App.instance!!.dataStore!!.edit {
        it[floatPreferencesKey(key)] = value
    }
}

fun LocalDataGlobal.Key.putDoubleData(value: Double) = runBlocking {
    App.instance!!.dataStore!!.edit {
        it[doublePreferencesKey(key)] = value
    }
}

fun LocalDataGlobal.Key.putIntData(value: Int) = runBlocking {
    App.instance!!.dataStore!!.edit {
        it[intPreferencesKey(key)] = value
    }
}

fun LocalDataGlobal.Key.putLongData(value: Long) = runBlocking {
    App.instance!!.dataStore!!.edit {
        it[longPreferencesKey(key)] = value
    }
}

fun <T> LocalDataGlobal.Key.putData(value: T) {
    when (value) {
        is Int -> putIntData(value)
        is Long -> putLongData(value)
        is String -> putStringData(value)
        is Boolean -> putBooleanData(value)
        is Float -> putFloatData(value)
        is Double -> putDoubleData(value)
        else -> throw IllegalArgumentException("不支持插入这个类型")
    }
}

/**
 * 清空数据
 */
fun clearData() = runBlocking {
    App.instance!!.dataStore!!.edit {
        it.clear()
    }
}
