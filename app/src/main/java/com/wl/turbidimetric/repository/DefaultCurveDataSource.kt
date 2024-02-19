package com.wl.turbidimetric.repository

import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.repository.if2.CurveSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class DefaultCurveDataSource constructor(
    private val dao: MainDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CurveSource {
    override fun listenerCurve(valid: Boolean ): Flow<List<CurveModel>> {
        return dao.listenerCurveModels(valid)
    }

    /**
     * 直接获取曲线参数
     */
    override suspend fun getCurveModels(valid: Boolean ): List<CurveModel> {
        return dao.getCurveModels(valid)
    }

    /**
     * 更新曲线参数
     * @param curve CurveModel
     * @return Long
     */
    override suspend fun updateCurve(curve: CurveModel): Int {
        return dao.updateCurveModel(curve)
    }

    /**
     * 添加曲线参数
     * @param curve CurveModel
     */
    override suspend fun addCurve(curve: CurveModel): Long {
        return dao.insertCurveModel(curve)
    }

    /**
     * 删除曲线参数
     * @param curve CurveModel
     * @return Boolean
     */
    override suspend fun removeCurve(curve: CurveModel): Boolean {
        return dao.removeCurveModel(curve) > 0
    }
}
