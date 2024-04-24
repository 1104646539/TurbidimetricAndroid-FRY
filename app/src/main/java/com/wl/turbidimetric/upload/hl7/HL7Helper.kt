package com.wl.turbidimetric.upload.hl7

import android.os.Handler
import android.os.Message
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.upload.hl7.service.HL7UploadService
import com.wl.turbidimetric.upload.hl7.service.Hl7Log
import com.wl.turbidimetric.upload.hl7.util.*
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.Patient
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

    /**
     * 上传
     */
    val WHAT_UPLOAD_NEXT = 1000

    /**
     * 获取患者信息
     */
    val WHAT_GET_INFO_NEXT = 1100
    var uploadHelper: UploadHelper? = null
    var getPatientInfoHelper: GetPatientInfoHelper? = null
    val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (WHAT_UPLOAD_NEXT == msg.what) {
                removeMessages(WHAT_UPLOAD_NEXT)
                uploadHelper?.curTestResult()?.let { cur ->
                    uploadHelper?.uploadNextTestResult(cur, uploadHelper?.curCallback())
                }
            } else if (WHAT_GET_INFO_NEXT == msg.what) {
                removeMessages(WHAT_GET_INFO_NEXT)
                getPatientInfoHelper?.curCondition()?.let { cur ->
                    getPatientInfoHelper!!.getPatientInfoNext(cur, getPatientInfoHelper!!.curCallback())
                }

            }
        }
    }

    private fun createUploadService(): HL7UploadService {
        return HL7UploadService().apply {
            uploadHelper = UploadHelper(this)
            getPatientInfoHelper = GetPatientInfoHelper(this)
        }
    }

    fun uploadTestResult(
        datas: List<TestResultAndCurveModel>,
        onUploadCallbacks: List<OnUploadCallback> = mutableListOf(),
        callback: OnUploadTestResults
    ) {
        uploadHelper?.uploadTestResult(datas, onUploadCallbacks, callback)
    }

    override fun uploadTestResult(
        testResult: TestResultAndCurveModel, onUploadCallback: OnUploadCallback
    ) {
        uploadHelper?.uploadSingleTestResult(testResult, callback2 = onUploadCallback)
    }

    fun getPatientInfo(
        conditions: List<GetPatientCondition>, onGetPatientCallbacks: List<OnGetPatientCallback>
    ) {
        getPatientInfoHelper?.getPatientInfo(conditions, onGetPatientCallbacks)
    }

    override fun getPatientInfo(
        condition: GetPatientCondition, onGetPatientCallback: OnGetPatientCallback
    ) {
        getPatientInfoHelper?.getPatientInfoSingle(condition, onGetPatientCallback)
    }

    override fun connect(
        config: ConnectConfig, onConnectListener: OnConnectListener?
    ) {
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

    class UploadHelper(
        private val uploadService: HL7UploadService
    ) :
        UploadService {
        private var index = 0
        private var successCount = 0
        private var failedCount = 0
        private var lastIndex = 0
        private var testResults = mutableListOf<TestResultAndCurveModel>()
        private var callbacks = mutableListOf<OnUploadCallback?>()
        private var onUploadTestResults: OnUploadTestResults? = null


        fun uploadTestResult(
            datas: List<TestResultAndCurveModel>,
            onUploadCallbacks: List<OnUploadCallback> = mutableListOf(),
            callback: OnUploadTestResults
        ) {
            testResults.addAll(datas)
            callbacks.addAll(onUploadCallbacks)
            onUploadTestResults = callback
            lastIndex = testResults.lastIndex
            uploadNextTestResult(testResults.first(), onUploadCallbacks.firstOrNull())
        }

        fun uploadSingleTestResult(
            testResult: TestResultAndCurveModel,
            callback: OnUploadTestResults? = null,
            callback2: OnUploadCallback? = null
        ) {
            onUploadTestResults = callback
            testResults.add(testResult)
            callbacks.add(callback2)
            lastIndex = testResults.lastIndex
            if (testResults.size == 1) {
                handler.sendEmptyMessageDelayed(WHAT_UPLOAD_NEXT, 500)
            }
        }

        fun uploadNextTestResult(
            testResult: TestResultAndCurveModel,
            onUploadCallback: OnUploadCallback?
        ) {
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
                        handler.sendEmptyMessageDelayed(WHAT_UPLOAD_NEXT, 500)
                    }
                    onUploadCallback?.onUploadSuccess(msg)
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
                        handler.sendEmptyMessageDelayed(WHAT_UPLOAD_NEXT, 500)
                    }
                    onUploadCallback?.onUploadFailed(code, msg)
                }
            })
        }

        fun clearUploadInfo() {
            index = 0
            successCount = 0
            failedCount = 0
            lastIndex = -1
            testResults.clear()
            callbacks.clear()
        }

        override fun uploadTestResult(
            testResult: TestResultAndCurveModel, onUploadCallback: OnUploadCallback
        ) {
            uploadService.uploadTestResult(testResult, onUploadCallback)
        }

        override fun getPatientInfo(
            condition: GetPatientCondition, onGetPatientCallback: OnGetPatientCallback
        ) {

        }


        override fun connect(
            config: ConnectConfig, onConnectListener: OnConnectListener?
        ) {

        }

        override fun disconnect() {
            uploadService.disconnect()
        }

        override fun isConnected(): Boolean {
            return uploadService.isConnected()
        }

        fun curTestResult(): TestResultAndCurveModel? {
            if (index in testResults.indices)
                return testResults[index]
            else
                return null
        }

        fun curCallback(): OnUploadCallback? {
            if (index in callbacks.indices)
                return callbacks[index]
            else
                return null
        }
    }

    class GetPatientInfoHelper(
        private val uploadService: HL7UploadService
    ) :
        UploadService {
        private var index = 0
        private var lastIndex = 0
        private var conditions = mutableListOf<GetPatientCondition>()
        private var callbacks = mutableListOf<OnGetPatientCallback?>()


        fun clearUploadInfo() {
            index = 0
            conditions.clear()
            callbacks.clear()
            lastIndex = -1
        }

        override fun uploadTestResult(
            testResult: TestResultAndCurveModel, onUploadCallback: OnUploadCallback
        ) {

        }

        fun getPatientInfo(
            conditions: List<GetPatientCondition>, onGetPatientCallbacks: List<OnGetPatientCallback>
        ) {
            this.conditions.addAll(conditions)
            this.callbacks.addAll(onGetPatientCallbacks)
            lastIndex = conditions.lastIndex

            getPatientInfoNext(conditions.first(), onGetPatientCallbacks.first())
        }

        fun getPatientInfoSingle(
            condition: GetPatientCondition, onGetPatientCallback: OnGetPatientCallback
        ) {
            this.conditions.add(condition)
            this.callbacks.add(onGetPatientCallback)
            lastIndex = conditions.lastIndex
            if (conditions.size == 1) {
                handler.sendEmptyMessageDelayed(WHAT_GET_INFO_NEXT, 500)
            }
        }

        override fun getPatientInfo(
            condition: GetPatientCondition, onGetPatientCallback: OnGetPatientCallback
        ) {
            uploadService.getPatientInfo(condition,onGetPatientCallback)
        }
         fun getPatientInfoNext(
            condition: GetPatientCondition, onGetPatientCallback: OnGetPatientCallback?
        ) {
            uploadService.getPatientInfo(condition, object : OnGetPatientCallback {
                override fun onGetPatientSuccess(patients: List<Patient>?) {
                    onGetPatientCallback?.onGetPatientSuccess(patients)
                    index++
                    if (index > lastIndex) {
                        LogToFile.i("index=$index lastIndex=$lastIndex")
                        clearUploadInfo()
                    } else {
                        handler.sendEmptyMessageDelayed(WHAT_GET_INFO_NEXT, 500)
                    }
                }

                override fun onGetPatientFailed(code: Int, msg: String) {
                    onGetPatientCallback?.onGetPatientFailed(code, msg)
                    index++
                    if (index > lastIndex) {
                        LogToFile.i("index=$index lastIndex=$lastIndex")
                        clearUploadInfo()
                    } else {
                        handler.sendEmptyMessageDelayed(WHAT_GET_INFO_NEXT, 500)
                    }
                }
            })
        }

        override fun connect(
            config: ConnectConfig, onConnectListener: OnConnectListener?
        ) {

        }

        override fun disconnect() {
            uploadService.disconnect()
        }

        override fun isConnected(): Boolean {
            return uploadService.isConnected()
        }

        fun curCondition(): GetPatientCondition? {
            if (index in conditions.indices)
                return conditions[index]
            else
                return null
        }

        fun curCallback(): OnGetPatientCallback? {
            if (index in callbacks.indices)
                return callbacks[index]
            else
                return null
        }
    }


}
