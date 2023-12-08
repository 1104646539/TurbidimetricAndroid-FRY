package com.wl.turbidimetric.home

import com.wl.turbidimetric.App
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel

class CurveRepository {
    val dao = App.instance!!.mainDao

    /**
     * 所有项目
     */
    val allDatas =
//        DBManager.CurveBox.query().orderDesc(CurveModel_.curveId).build()
        dao.listenerCurveModels()


    /**
     * 更新项目参数
     * @param curve CurveModel
     * @return Long
     */
    fun updateCurve(curve: CurveModel): Int {
        return dao.updateCurveModel(curve)
    }

    /**
     * 添加项目参数
     * @param curve CurveModel
     */
    fun addCurve(curve: CurveModel): Long {
        return dao.insertCurveModel(curve)
    }

    /**
     * 删除项目参数
     * @param curve CurveModel
     * @return Boolean
     */
    fun removeCurve(curve: CurveModel): Boolean {
        return dao.removeCurveModel(curve) > 0
    }
}
