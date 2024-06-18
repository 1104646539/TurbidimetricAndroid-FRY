package com.wl.turbidimetric.upload.hl7.service

import android.util.Log
import ca.uhn.hl7v2.model.v231.message.ACK
import ca.uhn.hl7v2.model.v231.message.QCK_Q02
import ca.uhn.hl7v2.parser.Parser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.upload.hl7.util.ErrorEnum
import com.wl.turbidimetric.upload.hl7.util.MsaStatus
import com.wl.turbidimetric.upload.hl7.util.Status
import com.wl.turbidimetric.upload.hl7.util.UploadGlobal
import com.wl.turbidimetric.upload.hl7.util.errorEnumForCode
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.msg.OruMsg
import com.wl.turbidimetric.upload.msg.QryMsg
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.upload.service.OnGetPatientCallback
import com.wl.turbidimetric.upload.service.OnUploadCallback
import com.wl.turbidimetric.upload.service.UploadService
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

typealias Hl7Log = (String) -> Any?

/**
 * 实现了基于hl7协议的上传和获取或者信息的功能
 * @property TAG String
 * @property connectService HL7ConnectService
 * @property parser Parser?
 */
class HL7UploadService : UploadService {
    val TAG = "HL7UploadService"
    var connectService: HL7ConnectService = HL7ConnectService()
    var parser: Parser? = null
        get() {
            return connectService.getContext()?.pipeParser
        }

    /**
     * 同时只能发送一条数据，上一条没回只能等待
     */
    var canSend = true
        set(value) {
            field = value
            i("canSend=$value")
        }
    var hl7Log: Hl7Log? = null
        set(value) {
            field = value
            connectService.hl7Log = value
        }
    /**
     * 上传检测数据
     * @param testResult TestResult
     * @param onUploadCallback OnUploadCallback
     */
    override fun uploadTestResult(testResult: TestResultAndCurveModel, onUploadCallback: OnUploadCallback) {
        if (!canSend) {
            onUploadCallback.onUploadFailed(
                ErrorEnum.BE_COMMUNICATION.code,
                ErrorEnum.BE_COMMUNICATION.msg
            )
            return
        }
        canSend = false
        connectService.submitThread {
            val msgId = UploadGlobal.MSG_ID++
            val msgStr =
                OruMsg.create(
                    connectService.connectService!!.config!!.charset,
                    testResult,
                    msgId
                )
            i("uploadTestResult=$msgStr")
            try {
                if (connectService.isConnected()) {
                    if (msgStr.isNullOrEmpty()) {
                        canSend = true
                        onUploadCallback.onUploadFailed(ErrorEnum.ORDER.code, "上传数据为空")
                    } else if(connectService.connectService!!.config!!.twoWay){//双向通讯
                        val response =
                            connectService.sendWaitResponseRetry(
                                msgStr,
                                msgId.toString()
                            ) { str ->
                                val m = parser?.parse(str)
                                if (m is ACK) {
                                    val ackCode = m.msa.msa1_AcknowledgementCode.value
                                    var errCode: Int? = try {
                                        m.msa.msa6_ErrorCondition.ce1_Identifier.value.toIntOrNull()
                                            ?: 0
                                    } catch (e: Exception) {
                                        0
                                    }

                                    val status = getStatus(ackCode, errCode, ErrorEnum.SUCCESS.msg)
//                                    Log.d(
//                                        TAG,
//                                        "上传:接收 ackCode=$ackCode errCode=$errCode status=$status"
//                                    )
                                    return@sendWaitResponseRetry when (status) {
                                        is MsaStatus.Error -> {
                                            canSend = true
                                            onUploadCallback.onUploadFailed(status.code, status.msg)
                                            0
                                        }
                                        is MsaStatus.Success -> {
                                            canSend = true
                                            onUploadCallback.onUploadSuccess("上传成功")
                                            1
                                        }
                                    }
                                } else {
                                    canSend = true
                                    onUploadCallback.onUploadFailed(ErrorEnum.ORDER.code, "未响应")
                                    0
                                }
                            }
                        if (response == -1) {
                            canSend = true
                            onUploadCallback.onUploadFailed(ErrorEnum.ORDER.code, "响应超时")
                        }
                    }else{//单向通讯
                        connectService.sendData(msgStr)
                        GlobalScope.launch {
                            delay(UploadGlobal.UpoadInterval)
                            canSend = true
                            onUploadCallback.onUploadSuccess("上传成功")
                        }
                    }
                } else {
                    canSend = true
                    onUploadCallback.onUploadFailed(
                        ErrorEnum.NOT_CONNECTED.code,
                        ErrorEnum.NOT_CONNECTED.msg
                    )
                }
            } catch (e: Exception) {
                canSend = true
                onUploadCallback.onUploadFailed(ErrorEnum.ORDER.code, "错误 ${e.message}")
            }
        }
    }

    /**
     * 根据返回的状态报错
     * @param ackCode String?
     * @param errCode Int?
     * @param qckCode String 只有在QAK中才有
     * @return MsaStatus
     */
    private fun getStatus(ackCode: String?, errCode: Int?, qckCode: String): MsaStatus {
        val status: MsaStatus = if (ackCode == Status.AA.msg) {
            when (qckCode) {
                ErrorEnum.NF.msg -> {
                    MsaStatus.Error(ErrorEnum.NF.code, ErrorEnum.NF.msg)
                }
                ErrorEnum.SUCCESS.msg -> {
                    MsaStatus.Success("")
                }
                ErrorEnum.AE.msg -> {
                    MsaStatus.Error(ErrorEnum.AE.code, ErrorEnum.AE.msg)
                }
                else -> {
                    MsaStatus.Error(ErrorEnum.AR.code, ErrorEnum.AR.msg)
                }
            }
        } else {
            val errorEnum = errorEnumForCode(errCode ?: -1)
            if (errorEnum == null) {
                MsaStatus.Error(errCode ?: -1, "未知错误")
            } else {
                MsaStatus.Error(errorEnum.code, errorEnum.msg)
            }
        }
        return status
    }

    /**
     * 获取待检信息 根据条码
     * @param condition String
     * @param onGetPatientCallback OnGetPatientCallback
     */
    override fun getPatientInfo(
        condition: GetPatientCondition,
        onGetPatientCallback: OnGetPatientCallback
    ) {
        val msgId = UploadGlobal.MSG_ID++
        val queryId = UploadGlobal.QUERY_ID++
        val msg = QryMsg.create(
            connectService.connectService!!.config!!.charset,
            condition,
            msgId,
            queryId
        )
        getPatientInfo(msg, msgId, onGetPatientCallback)
    }


    private fun getPatientInfo(
        msgStr: String,
        msgId: Long,
        onGetPatientCallback: OnGetPatientCallback
    ) {
        if (!canSend) {
            onGetPatientCallback.onGetPatientFailed(
                ErrorEnum.BE_COMMUNICATION.code,
                ErrorEnum.BE_COMMUNICATION.msg
            )
            return
        }
        canSend = false
        connectService.submitThread {
            if (connectService.isConnected()) {
                if (msgStr.isNullOrEmpty()) {
                    onGetPatientCallback.onGetPatientFailed(1, "数据为空")
                } else if(connectService.connectService!!.config!!.twoWay){//双向通讯
                    Log.d(TAG, "获取信息:发送 $msgStr")
                    val response =
                        connectService.sendWaitResponseRetry(
                            msgStr,
                            msgId.toString()
                        ) { str ->
                            Log.d(TAG, "获取信息:接收 $str")
                            if (str?.contains("QCK^Q02") == true) {
                                try {
                                    val m = parser?.parse(str)
                                    if (m is QCK_Q02) {//是响应信息，说明出错了
                                        val ackCode = m.msa.msa1_AcknowledgementCode.value
                                        val errCode =
                                            m.msa.msa6_ErrorCondition.ce1_Identifier.value.toIntOrNull()
                                        val qckCode =
                                            m.qak.qak2_QueryResponseStatus.value
                                        val status = getStatus(ackCode, errCode, qckCode)
                                        canSend = true
                                        return@sendWaitResponseRetry when (status) {
                                            is MsaStatus.Error -> {
                                                canSend = true
                                                onGetPatientCallback.onGetPatientFailed(
                                                    status.code,
                                                    status.msg
                                                )
                                                0
                                            }
                                            is MsaStatus.Success -> {
                                                1
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    canSend = true
                                    onGetPatientCallback.onGetPatientFailed(
                                        ErrorEnum.ORDER.code,
                                        ErrorEnum.ORDER.msg
                                    )
                                }
                                0
                            } else {
                                try {
                                    val type = object : TypeToken<List<Patient>>() {}.type
                                    val patients =
                                        Gson().fromJson<List<Patient>>(str, type)
                                    canSend = true
                                    onGetPatientCallback.onGetPatientSuccess(patients)
                                    1
                                } catch (e: Exception) {
                                    canSend = true
                                    onGetPatientCallback.onGetPatientFailed(
                                        ErrorEnum.ORDER.code,
                                        ErrorEnum.ORDER.msg
                                    )
                                    0
                                }
                            }
                        }
                    if (response == -1) {
                        canSend = true
                        onGetPatientCallback.onGetPatientFailed(ErrorEnum.ORDER.code, "响应超时")
                    }
                }else{
                    canSend = true
                    onGetPatientCallback.onGetPatientFailed(ErrorEnum.ORDER.code, "非双向通讯")
                }
            } else {
                canSend = true
                onGetPatientCallback.onGetPatientFailed(
                    ErrorEnum.NOT_CONNECTED.code,
                    ErrorEnum.NOT_CONNECTED.msg
                )
            }
        }
    }

    override fun connect(config: ConnectConfig, onConnectListener: OnConnectListener?) {
        connectService.connect(config, onConnectListener)
    }


    override fun disconnect() {
        connectService.disconnect()
    }

    override fun isConnected(): Boolean {
        return connectService.isConnected()
    }

//    override fun receiverMsg(): String {
//        return connectService.receiverMsg()
//    }
}
