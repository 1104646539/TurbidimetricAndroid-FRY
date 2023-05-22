package com.wl.turbidimetric.ex

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.wl.turbidimetric.App
import com.wl.turbidimetric.datastore.LocalDataGlobal


val Context.dataStore by preferencesDataStore("settings")

//inline fun <reified T> LocalDataGlobal.Key.put(data: T) {
//    when (T::class.java) {
//        java.lang.Integer::class.java -> {
//            println("int2")
//            LocalDataGlobal.datas.encode(key, data as Int)
//        }
//        java.lang.Float::class.java -> {
//            println("Float2")
//            LocalDataGlobal.datas.encode(key, data as Float)
//        }
//        java.lang.Double::class.java -> {
//            println("Double2")
//            LocalDataGlobal.datas.encode(key, data as Double)
//        }
//        java.lang.String::class.java -> {
//            println("String2")
//            LocalDataGlobal.datas.encode(key, data as String)
//        }
//        java.lang.Boolean::class.java -> {
//            println("Boolean3")
//            LocalDataGlobal.datas.encode(key, data as Boolean)
//        }
//        else -> {
//            throw Exception("不支持的类型")
//        }
//    }
//}
//
//inline fun <reified T> LocalDataGlobal.Key.get(defaultValue: T): T {
//    val data = when (defaultValue) {
//        is Int -> LocalDataGlobal.datas.decodeInt(key, defaultValue)
//        is Long -> LocalDataGlobal.datas.decodeLong(key, defaultValue)
//        is String -> LocalDataGlobal.datas.decodeString(key, defaultValue)
//        is Boolean -> LocalDataGlobal.datas.decodeBool(key, defaultValue)
//        is Float -> LocalDataGlobal.datas.decodeFloat(key, defaultValue)
//        is Double -> LocalDataGlobal.datas.decodeDouble(key, defaultValue)
//        else -> throw IllegalArgumentException("不支持取这个类型")
//    }
//    return data as T
//}


///**
// * 清空数据
// */
//fun clearData() {
//    LocalDataGlobal.datas.clearAll()
//}
