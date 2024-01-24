package com.wl.turbidimetric.ex

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore


val Context.dataStore by preferencesDataStore("settings")


///**
// * 清空数据
// */
fun clearData() {

}
