package com.wl.turbidimetric.model

import com.wl.turbidimetric.ob.BigDecimalConverter
import io.objectbox.annotation.*
import io.objectbox.relation.ToOne
import java.math.BigDecimal

@Entity
data class TestResultModel(
    @Id
    var id: Long = 0,
    var isSelect: Boolean = false,
    var name: String = "",
    var gender: String = "",
    var age: String = "",
    /**
     * 样本码
     */
    var sampleBarcode: String = "",

    /**
     * 样本类型
     * @see SampleType
     */
    var sampleType: Int = SampleType.NONEXISTENT.ordinal,

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
    @Convert(dbType = String::class, converter = BigDecimalConverter::class)
    var absorbances: BigDecimal = BigDecimal("0.0"),
    /**
     * 浓度
     */
    var concentration: Int = 0,
    /**
     * 第一次检测值
     */
    @Convert(dbType = String::class, converter = BigDecimalConverter::class)
    var testValue1: BigDecimal = BigDecimal("0.0"),
    /**
     * 第二次检测值
     */
    @Convert(dbType = String::class, converter = BigDecimalConverter::class)
    var testValue2: BigDecimal = BigDecimal("0.0"),
    /**
     * 第三次检测值
     */
    @Convert(dbType = String::class, converter = BigDecimalConverter::class)
    var testValue3: BigDecimal = BigDecimal("0.0"),
    /**
     * 第四次检测值
     */
    @Convert(dbType = String::class, converter = BigDecimalConverter::class)
    var testValue4: BigDecimal = BigDecimal("0.0"),
    /**
     * 第一次检测值
     */
    var testOriginalValue1: Int = 0,
    /**
     * 第二次检测值
     */
    var testOriginalValue2: Int = 0,
    /**
     * 第三次检测值
     */
    var testOriginalValue3: Int = 0,
    /**
     * 第四次检测值
     */
    var testOriginalValue4: Int = 0,
    /**
     * 创建时间
     */
    var createTime: Long = 0,
    /**
     * 检测时间 第四次
     */
    var testTime: Long = 0,

    var deliveryTime: String = "", //送检时间
    var deliveryDepartment: String = "",//送检科室
    var deliveryDoctor: String = "",//送检医生

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
        if (sampleBarcode != other.sampleBarcode) return false
        if (detectionNum != other.detectionNum) return false
        if (testState != other.testState) return false
        if (testResult != other.testResult) return false
        if (absorbances != other.absorbances) return false
        if (concentration != other.concentration) return false
        if (testValue1 != other.testValue1) return false
        if (testValue2 != other.testValue2) return false
        if (testValue3 != other.testValue3) return false
        if (testValue4 != other.testValue4) return false
        if (testOriginalValue1 != other.testOriginalValue1) return false
        if (testOriginalValue2 != other.testOriginalValue2) return false
        if (testOriginalValue3 != other.testOriginalValue3) return false
        if (testOriginalValue4 != other.testOriginalValue4) return false
        if (createTime != other.createTime) return false
        if (testTime != other.testTime) return false
        if (project != other.project) return false
        if (sampleType != other.sampleType) return false
        if (deliveryTime != other.deliveryTime) return false
        if (deliveryDepartment != other.deliveryDepartment) return false
        if (deliveryDoctor != other.deliveryDoctor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isSelect.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + age.hashCode()
        result = 31 * result + sampleBarcode.hashCode()
        result = 31 * result + detectionNum.hashCode()
        result = 31 * result + testState
        result = 31 * result + testResult.hashCode()
        result = 31 * result + absorbances.hashCode()
        result = 31 * result + concentration.hashCode()
        result = 31 * result + testValue1.hashCode()
        result = 31 * result + testValue2.hashCode()
        result = 31 * result + testValue3.hashCode()
        result = 31 * result + testValue4.hashCode()
        result = 31 * result + testOriginalValue1
        result = 31 * result + testOriginalValue2
        result = 31 * result + testOriginalValue3
        result = 31 * result + testOriginalValue4
        result = 31 * result + createTime.hashCode()
        result = 31 * result + testTime.hashCode()
        result = 31 * result + project.hashCode()
        result = 31 * result + sampleType.hashCode()
        result = 31 * result + deliveryTime.hashCode()
        result = 31 * result + deliveryDepartment.hashCode()
        result = 31 * result + deliveryDoctor.hashCode()
        return result
    }

    override fun toString(): String {
        return "TestResultModel(id=$id, isSelect=$isSelect, name='$name', sampleType='$sampleType',gender='$gender', age='$age', sampleBarcode='$sampleBarcode', detectionNum='$detectionNum', testState=$testState, testResult='$testResult', absorbances=$absorbances, concentration=$concentration, testValue1=$testValue1, testValue2=$testValue2, testValue3=$testValue3, testValue4=$testValue4, testOriginalValue1=$testOriginalValue1, testOriginalValue2=$testOriginalValue2, testOriginalValue3=$testOriginalValue3, testOriginalValue4=$testOriginalValue4, createTime='$createTime', testTime='$testTime', project=$project)"
    }

}
