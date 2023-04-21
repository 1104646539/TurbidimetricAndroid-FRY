package com.wl.turbidimetric.model

import io.objectbox.annotation.*
import io.objectbox.relation.ToOne

@Entity
data class TestResultModel(
    @Id
    var id: Long = 0,
    var name: String = "",
    var gender: String = "",
    var age: String = "",

    var testResult: String = "",
    var concentration: String = "",
    var testValue1: String = "",
    var testValue2: String = "",
    var testValue3: String = "",
    var testValue4: String = ""
) : BaseOBModel(0) {

    lateinit var project: ToOne<ProjectModel>

    @Suppress("UNCHECKED_CAST")
    @Override
    fun copy(): TestResultModel {
        val tr = TestResultModel(
            id,
            name,
            gender,
            age,
            testResult,
            concentration
        )
        tr.project.target = if (project.target == null) null else project.target.copy()
        return tr
    }
}
