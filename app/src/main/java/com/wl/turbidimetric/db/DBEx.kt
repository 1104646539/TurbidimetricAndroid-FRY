package com.wl.turbidimetric.db

import android.util.Log
import com.wl.turbidimetric.App
import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.model.TestResultAndCurveModel

fun MainDao.putTestResultAndCurve(model: TestResultAndCurveModel) {
    App.instance?.database?.runInTransaction {
        var curveId = 0L
        model.curve?.let {
            curveId = this.insertCurveModel(it)
        }
        if (curveId <= 0 && model.curve != null) throw Exception("curve插入失败")

        if (model.result == null) return@runInTransaction

        model.result?.curveOwnerId = curveId
        val resultId = insertTestResultModel(model.result)

        if (resultId <= 0 && model.result != null) throw Exception("result插入失败")
    }
}
