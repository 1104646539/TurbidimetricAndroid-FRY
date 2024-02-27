package com.wl.turbidimetric.upload.hl7

import android.os.Handler
import android.os.Message
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.upload.hl7.service.HL7UploadService
import com.wl.turbidimetric.upload.hl7.service.Hl7Log
import com.wl.turbidimetric.upload.hl7.util.*
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.upload.service.OnGetPatientCallback
import com.wl.turbidimetric.upload.service.OnUploadCallback
import com.wl.turbidimetric.upload.service.UploadService
import com.wl.wllib.LogToFile

typealias OnUploadTestResults = (count: Int, successCount: Int, failedCount: Int) -> Any?

object HL7Helper : UploadService {
    private var uploadService: HL7UploadService = createUploadService()
    var hl7Log: Hl7Log? = null
        set(value) {
            field = value
            uploadService.hl7Log = value
        }
    val WHAT_NEXT = 1000
    val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (WHAT_NEXT == msg.what) {
                removeMessages(WHAT_NEXT)
                uploadNextTestResult(testResults[index])
            }
        }
    }

    private fun createUploadService(): HL7UploadService {
        return HL7UploadService()
    }

    private var index = 0
    private var successCount = 0
    private var failedCount = 0
    private var lastIndex = 0
    private var testResults = mutableListOf<TestResultAndCurveModel>()
    private var onUploadTestResults: OnUploadTestResults? = null

    fun uploadTestResult(
        datas: List<TestResultAndCurveModel>,
        callback: OnUploadTestResults
    ) {
        testResults.addAll(datas)
        onUploadTestResults = callback
        lastIndex = testResults.lastIndex
        uploadNextTestResult(testResults.first())
    }

    fun uploadSingleTestResult(
        testResult: TestResultAndCurveModel,
        callback: OnUploadTestResults
    ) {
        onUploadTestResults = callback
        testResults.add(testResult)
        lastIndex = testResults.lastIndex
        if (testResults.size == 1) {
            handler.sendEmptyMessageDelayed(WHAT_NEXT, 1000)
        }
    }

    private fun uploadNextTestResult(testResult: TestResultAndCurveModel) {
        uploadTestResult(testResult, object : OnUploadCallback {
            override fun onUploadSuccess(msg: String) {
                LogToFile.i("onUploadSuccess msg=$msg index=$index")
                index++
                successCount++
                if (index > lastIndex) {
                    LogToFile.i("index=$index successCount=$successCount failedCount=$failedCount lastIndex=$lastIndex")
                    onUploadTestResults?.invoke(testResults.size, successCount, failedCount)
                    clearUploadInfo()
                } else {
                    handler.sendEmptyMessageDelayed(WHAT_NEXT, 1000)
                }
            }

            override fun onUploadFailed(code: Int, msg: String) {
                LogToFile.i("onUploadFailed msg=$msg index=$index")
                index++
                failedCount++
                if (index > lastIndex) {
                    LogToFile.i("index=$index successCount=$successCount failedCount=$failedCount lastIndex=$lastIndex")
                    onUploadTestResults?.invoke(testResults.size, successCount, failedCount)
                    clearUploadInfo()
                } else {
                    handler.sendEmptyMessageDelayed(WHAT_NEXT, 1000)
                }
            }
        })
    }

    fun clearUploadInfo() {
        index = 0
        successCount = 0
        failedCount = 0
        lastIndex = -1
        testResults.clear()
    }

    override fun uploadTestResult(
        testResult: TestResultAndCurveModel,
        onUploadCallback: OnUploadCallback
    ) {
        uploadService.uploadTestResult(testResult, onUploadCallback)
    }

    override fun getPatientInfo(
        condition: GetPatientCondition,
        onGetPatientCallback: OnGetPatientCallback
    ) {
        uploadService.getPatientInfo(condition, onGetPatientCallback)
    }


    override fun connect(
        config: ConnectConfig,
        onConnectListener: OnConnectListener?
    ) {
//        config.save()
//        SystemGlobal.uploadConfig = config
        if (!config.openUpload) return
        uploadService.connect(config, object : OnConnectListener {
            override fun onConnectResult(connectResult: ConnectResult) {
                onConnectListener?.onConnectResult(connectResult)
            }

            override fun onConnectStatusChange(connectStatus: ConnectStatus) {
                onConnectListener?.onConnectStatusChange(connectStatus)
                SystemGlobal.connectStatus = connectStatus
            }
        })
    }

    fun connect(onConnectListener: OnConnectListener?) {
        if (!isConnected()) {
            connect(getConfig(), onConnectListener)
        } else {
            onConnectListener?.onConnectResult(ConnectResult.AlreadyConnected())
        }
    }

    fun getConfig(): ConnectConfig {
        return SystemGlobal.uploadConfig
    }

    override fun disconnect() {
        uploadService.disconnect()
    }

    override fun isConnected(): Boolean {
        return uploadService.isConnected()
    }
}
