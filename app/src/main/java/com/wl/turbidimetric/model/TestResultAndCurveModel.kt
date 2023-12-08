package com.wl.turbidimetric.model

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation

data class TestResultAndCurveModel(
    @Embedded
    val result: TestResultModel,
    @Relation(parentColumn = "curveOwnerId", entityColumn = "curveId")
    val curve: CurveModel? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is TestResultAndCurveModel) return false
        if (other!!.result == null && result == null && other!!.curve == null && curve == null) return true
        return other.result?.equals(result) == true && other?.curve?.equals(curve) == true
    }

    @Suppress("UNCHECKED_CAST")
    @Override
    fun copy(): TestResultAndCurveModel {
        return TestResultAndCurveModel(result?.copy(), curve?.copy())
    }
}
