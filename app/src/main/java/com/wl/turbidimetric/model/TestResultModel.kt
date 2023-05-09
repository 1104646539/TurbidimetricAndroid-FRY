package com.wl.turbidimetric.model

import io.objectbox.annotation.*
import io.objectbox.relation.ToOne

@Entity
data class TestResultModel(
    @Id
    var id: Long = 0,
    var isSelect: Boolean = false,
    var name: String = "",
    var gender: String = "",
    var age: String = "",
    /**
     * 采便管码
     */
    var sampleQRCode: String = "",

    /**
     * 编号
     */
    var detectionNum: String = "",
    /**
     * 检测状态
     */

    var testState: Int = 0,
    /**
     * 判定结果
     */
    var testResult: String = "",
    /**
     * 吸光度
     */
    var absorbances: Double = 0.0,
    /**
     * 浓度
     */
    var concentration: Double = 0.0,
    /**
     * 第一次检测值
     */
    var testValue1: Double = 0.0,
    /**
     * 第二次检测值
     */
    var testValue2: Double = 0.0,
    /**
     * 第三次检测值
     */
    var testValue3: Double = 0.0,
    /**
     * 第四次检测值
     */
    var testValue4: Double = 0.0,
    /**
     * 创建时间
     */
    var createTime: String = "",
    /**
     * 检测时间 第四次
     */
    var testTime: String = "",
) : BaseOBModel(0) {

    lateinit var project: ToOne<ProjectModel>

    @Suppress("UNCHECKED_CAST")
    @Override
    fun copy(): TestResultModel {
        val tr = TestResultModel(
            id,
            false,
            name,
            gender,
            age,
        )
        tr.project.target = if (project.target == null) null else project.target.copy()
        return tr
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestResultModel) return false

        if (id != other.id) return false
        if (isSelect != other.isSelect) return false
        if (name != other.name) return false
        if (gender != other.gender) return false
        if (age != other.age) return false
        if (sampleQRCode != other.sampleQRCode) return false
        if (detectionNum != other.detectionNum) return false
        if (testState != other.testState) return false
        if (testResult != other.testResult) return false
        if (absorbances != other.absorbances) return false
        if (concentration != other.concentration) return false
        if (testValue1 != other.testValue1) return false
        if (testValue2 != other.testValue2) return false
        if (testValue3 != other.testValue3) return false
        if (testValue4 != other.testValue4) return false
        if (createTime != other.createTime) return false
        if (testTime != other.testTime) return false
        if (project != other.project) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isSelect.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + age.hashCode()
        result = 31 * result + sampleQRCode.hashCode()
        result = 31 * result + detectionNum.hashCode()
        result = 31 * result + testState
        result = 31 * result + testResult.hashCode()
        result = 31 * result + absorbances.hashCode()
        result = 31 * result + concentration.hashCode()
        result = 31 * result + testValue1.hashCode()
        result = 31 * result + testValue2.hashCode()
        result = 31 * result + testValue3.hashCode()
        result = 31 * result + testValue4.hashCode()
        result = 31 * result + createTime.hashCode()
        result = 31 * result + testTime.hashCode()
        result = 31 * result + project.hashCode()
        return result
    }

}
