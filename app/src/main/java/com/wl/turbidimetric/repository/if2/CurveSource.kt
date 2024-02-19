package com.wl.turbidimetric.repository.if2

import com.wl.turbidimetric.model.CurveModel
import kotlinx.coroutines.flow.Flow

interface CurveSource {
    /**
     * 监听所有曲线参数
     */
    fun listenerCurve(valid: Boolean = true): Flow<List<CurveModel>>

    /**
     * 直接获取曲线参数
     */
    suspend fun getCurveModels(valid: Boolean = true): List<CurveModel>

    /**
     * 更新曲线参数
     * @param curve CurveModel
     * @return Long
     */
    suspend fun updateCurve(curve: CurveModel): Int

    /**
     * 添加曲线参数
     * @param curve CurveModel
     */
    suspend fun addCurve(curve: CurveModel): Long

    /**
     * 删除曲线参数
     * @param curve CurveModel
     * @return Boolean
     */
    suspend fun removeCurve(curve: CurveModel): Boolean
}
