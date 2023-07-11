package com.wl.turbidimetric.ex

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.wl.turbidimetric.App
import com.wl.turbidimetric.datastore.LocalDataGlobal


val Context.dataStore by preferencesDataStore("settings")


///**
// * 清空数据
// */
fun clearData() {

}
