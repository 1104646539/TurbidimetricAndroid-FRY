package com.wl.turbidimetric.db

import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.model.TestResultAndCurveModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

suspend fun MainDao.putTestResultAndCurve(model: TestResultAndCurveModel) {
    val dao = this
    App.instance?.database?.runInTransaction {
        var curveId = 0L
        model.curve?.let {
            GlobalScope.launch {
                curveId = dao.insertCurveModel(it)
            }
        }
        if (curveId <= 0 && model.curve != null) throw Exception("curve插入失败")

        if (model.result == null) return@runInTransaction

        model.result.curveOwnerId = curveId
        GlobalScope.launch {
            val resultId = dao.insertTestResultModel(model.result)
            if (resultId <= 0 && model.result != null) throw Exception("result插入失败")
        }

    }
}
