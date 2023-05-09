package com.wl.turbidimetric.db

import android.content.Context
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.model.MyObjectBox
import com.wl.turbidimetric.model.ProjectModel
import io.objectbox.BoxStore
import io.objectbox.DebugFlags

object DBManager {
    lateinit var boxStore: BoxStore
    val TestResultBox by lazy {
        boxStore.boxFor(TestResultModel::class.java)
    }
    val ProjectBox by lazy {
        boxStore.boxFor(ProjectModel::class.java)
    }

    fun init(context: Context) {
        boxStore = MyObjectBox.builder().androidContext(context)
            .debugFlags(DebugFlags.LOG_QUERIES or DebugFlags.LOG_QUERY_PARAMETERS).build()
    }
}
