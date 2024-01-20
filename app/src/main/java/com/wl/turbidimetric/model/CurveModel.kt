package com.wl.turbidimetric.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wl.turbidimetric.util.FitterType


@Entity
data class CurveModel(
    @PrimaryKey(autoGenerate = true)
    var curveId: Long = 0,
    var projectName: String = "",
    var projectCode: String = "",
    var projectLjz: Int = 100,
    var projectUnit: String = "ng/mL",
    var f0: Double = 0.0,
    var f1: Double = 0.0,
    var f2: Double = 0.0,
    var f3: Double = 0.0,
    /**
     * R方
     */
    var fitGoodness: Double = 0.0,
    var createTime: String = "",
    var isSelect: Boolean = false,
    /**
     * 曲线编号
     */
    var reagentNO: String = "",
    /**
     * 反应值 ，指拟合时各个梯度的反应值
     */
    var reactionValues: IntArray = intArrayOf(),
    /**
     * 拟合时的浓度目标值
     */
    var targets: DoubleArray = doubleArrayOf(),
    /**
     * 验证值 ，指拟合时使用的拟合参数反算出的值
     */
    var yzs: IntArray = intArrayOf(),
    /**
     * 曲线类型 ，指拟合时使用的曲线类型
     */
    var fitterType: Int = FitterType.Three.ordinal,
    /**
     * 梯度 ，指拟合时取的浓度梯度数量
     */
    var gradsNum: Int = 5,
    /**
     * 是否是有效的
     */
    var isValid: Boolean = true
) {
    override fun toString(): String {
        return "CurveModel(id=$curveId,projectName=$projectName,projectCode=$projectCode,projectLjz=$projectLjz,projectUnit=$projectUnit,f0=$f0,f1=$f1,f2=$f2,f3=$f3,fitGoodness=$fitGoodness,createTime=$createTime,reagentNO=$reagentNO,yzs=$yzs,fitterType=$fitterType,gradsNum=$gradsNum,reactionValues=${reactionValues.joinToString()},targets=${targets.joinToString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurveModel) return false

        if (curveId != other.curveId) return false
        if (projectName != other.projectName) return false
        if (projectCode != other.projectCode) return false
        if (projectLjz != other.projectLjz) return false
        if (projectUnit != other.projectUnit) return false
        if (f0 != other.f0) return false
        if (f1 != other.f1) return false
        if (f2 != other.f2) return false
        if (f3 != other.f3) return false
        if (fitGoodness != other.fitGoodness) return false
        if (createTime != other.createTime) return false
        if (isSelect != other.isSelect) return false
        if (reagentNO != other.reagentNO) return false
        if (reactionValues != null) {
            if (other.reactionValues == null) return false
            if (!reactionValues.contentEquals(other.reactionValues)) return false
        } else if (other.reactionValues != null) return false
        if (targets != null) {
            if (other.targets == null) return false
            if (!targets.contentEquals(other.targets)) return false
        } else if (other.targets != null) return false
        if (yzs != null) {
            if (other.yzs == null) return false
            if (!yzs.contentEquals(other.yzs)) return false
        } else if (other.yzs != null) return false
        if (fitterType != other.fitterType) return false
        return gradsNum == other.gradsNum
    }

    override fun hashCode(): Int {
        var result = curveId.hashCode()
        result = 31 * result + projectName.hashCode()
        result = 31 * result + projectCode.hashCode()
        result = 31 * result + projectLjz
        result = 31 * result + projectUnit.hashCode()
        result = 31 * result + f0.hashCode()
        result = 31 * result + f1.hashCode()
        result = 31 * result + f2.hashCode()
        result = 31 * result + f3.hashCode()
        result = 31 * result + fitGoodness.hashCode()
        result = 31 * result + createTime.hashCode()
        result = 31 * result + isSelect.hashCode()
        result = 31 * result + reagentNO.hashCode()
        result = 31 * result + reactionValues.contentHashCode()
        result = 31 * result + targets.contentHashCode()
        result = 31 * result + yzs.contentHashCode()
        result = 31 * result + fitterType.hashCode()
        result = 31 * result + gradsNum.hashCode()
        return result
    }
}
