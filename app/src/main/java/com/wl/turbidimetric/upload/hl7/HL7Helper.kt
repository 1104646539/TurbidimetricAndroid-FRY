package com.wl.turbidimetric.upload.hl7

import com.wl.turbidimetric.global.SystemGlobal
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
import java.util.concurrent.CountDownLatch

typealias OnUploadTestResults = (count: Int, successCount: Int, failedCount: Int) -> Any?

object HL7Helper : UploadService {
    private var uploadService: HL7UploadService = createUploadService()
    var hl7Log: Hl7Log? = null
        set(value) {
            field = value
            uploadService?.hl7Log = value
        }

    private fun createUploadService(): HL7UploadService {
        return HL7UploadService()
    }

    private var index = 0;
    private var successCount = 0;
    private var failedCount = 0;
    private var lastIndex = 0;
    private var testResults: List<TestResultModel> = mutableListOf()
    private var onUploadTestResults: OnUploadTestResults? = null
    fun uploadTestResult(
        testResults: List<TestResultModel>,
        onUploadTestResults: OnUploadTestResults
    ) {
        this.testResults = testResults
        this.onUploadTestResults = onUploadTestResults
        index = 0;
        successCount = 0;
        failedCount = 0;
        lastIndex = testResults.lastIndex;
        uploadNextTestResult(testResults.first())
    }

    private fun uploadNextTestResult(testResult: TestResultModel) {
        uploadTestResult(testResult, object : OnUploadCallback {
            override fun onUploadSuccess(msg: String) {
                LogToFile.i("onUploadSuccess msg=$msg index=$index")
                index++
                successCount++
                if (index > lastIndex) {
                    LogToFile.i("index=$index successCount=$successCount failedCount=$failedCount lastIndex=$lastIndex")
                    onUploadTestResults?.invoke(testResults.size, successCount, failedCount)
                } else {
                    Thread.sleep(500)
                    uploadNextTestResult(testResults[index])
                }
            }

            override fun onUploadFailed(code: Int, msg: String) {
                LogToFile.i("onUploadFailed msg=$msg index=$index")
                index++
                failedCount++
                if (index > lastIndex) {
                    LogToFile.i("index=$index successCount=$successCount failedCount=$failedCount lastIndex=$lastIndex")
                    onUploadTestResults?.invoke(testResults.size, successCount, failedCount)
                } else {
                    Thread.sleep(500)
                    uploadNextTestResult(testResults[index])
                }
            }
        })
    }

    override fun uploadTestResult(testResult: TestResultModel, onUploadCallback: OnUploadCallback) {
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
        config.save()
        SystemGlobal.uploadConfig = config
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
        return getLocalConfig()
    }

    override fun disconnect() {
        uploadService.disconnect()
    }

    override fun isConnected(): Boolean {
        return uploadService.isConnected()
    }
}
