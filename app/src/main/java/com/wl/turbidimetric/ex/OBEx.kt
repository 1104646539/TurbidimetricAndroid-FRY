package com.wl.turbidimetric.ex

import com.wl.turbidimetric.db.DBManager
import com.wl.turbidimetric.model.BaseOBModel
import com.wl.turbidimetric.model.ProjectModel
import io.objectbox.Box

inline fun <reified T : BaseOBModel> T.put() {
    DBManager.boxStore.boxFor(T::class.java).put(this)
}

inline fun <reified T : BaseOBModel> getOB(): Box<T> {
    return DBManager.boxStore.boxFor(T::class.java)
}
