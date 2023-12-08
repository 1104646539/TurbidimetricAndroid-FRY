package com.wl.turbidimetric.upload.service

import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.Patient

interface UploadService : ConnectService {
    fun uploadTestResult(testResult: TestResultAndCurveModel, onUploadCallback: OnUploadCallback)
    fun getPatientInfo(condition: GetPatientCondition, onGetPatientCallback: OnGetPatientCallback)
}

interface OnUploadCallback {
    fun onUploadSuccess(msg: String)
    fun onUploadFailed(code: Int, msg: String)
}


interface OnGetPatientCallback {
    fun onGetPatientSuccess(patients: List<Patient>?)
    fun onGetPatientFailed(code: Int, msg: String)
}

interface OnConnectListener {
    fun onConnectResult(connectResult: ConnectResult)
    fun onConnectStatusChange(connectStatus: ConnectStatus)
}
