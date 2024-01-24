package com.wl.turbidimetric.home

import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.model.CurveModel

class CurveRepository {
    val dao = App.instance!!.mainDao

    /**
     * 所有项目
     */
    val allDatas =
//        DBManager.CurveBox.query().orderDesc(CurveModel_.curveId).build()
        dao.listenerCurveModels()

    /**
     * 直接获取曲线参数
     */
    suspend fun getCurveModels(): List<CurveModel> {
        return dao.getCurveModels()
    }

    /**
     * 更新曲线参数
     * @param curve CurveModel
     * @return Long
     */
    suspend

    fun updateCurve(curve: CurveModel): Int {
        return dao.updateCurveModel(curve)
    }

    /**
     * 添加曲线参数
     * @param curve CurveModel
     */
    suspend fun addCurve(curve: CurveModel): Long {
        return dao.insertCurveModel(curve)
    }

    /**
     * 删除曲线参数
     * @param curve CurveModel
     * @return Boolean
     */
    suspend fun removeCurve(curve: CurveModel): Boolean {
        return dao.removeCurveModel(curve) > 0
    }
}
