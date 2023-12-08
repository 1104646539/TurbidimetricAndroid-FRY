package com.wl.turbidimetric.upload.hl7.service

import android.util.Log
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.concurrent.BlockingHashMap
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v231.message.ACK
import ca.uhn.hl7v2.model.v231.message.DSR_Q03
import ca.uhn.hl7v2.model.v231.message.QCK_Q02
import com.google.gson.Gson
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.msg.DsrMsg
import com.wl.turbidimetric.upload.service.ConnectService
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.upload.hl7.util.ErrorEnum
import com.wl.turbidimetric.upload.hl7.util.Status
import com.wl.turbidimetric.upload.hl7.util.getMsgId
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 实现了hl7协议的基本响应、连接、解析接收的数据、发送数据、重发、超时等功能
 */
class HL7ConnectService : ConnectService {
    private val TAG = "HL7ConnectService"

    private var responseMap: BlockingHashMap<String, String>? =
        BlockingHashMap(Executors.newCachedThreadPool())
    var connectService: AbstractConnectService? = null

    private val threads = Executors.newCachedThreadPool()
    var hl7Log: Hl7Log? = null

    private val gson = Gson()

    private fun startListener() {
        submitThread {
            while (true) {
                try {
                    val response = connectService?.getMessage()
                    if (response == null) {
                        Log.d(TAG, "startListener 服务器断开连接")
                        connectService?.disconnect()
                        connectService?.reconnection()
                        return@submitThread
                    }
                    response.let {
                        val responseMsg = connectService?.pipeParser?.parse(response)
                        responseMsg?.let {
                            val ackID = getMsgId(responseMsg)
                //                            Log.d(TAG, "run: ackID==$ackID response=$response")
                            if (ackID == null) {
                                Log.d(TAG, "ackID == null")
                            } else {
                                if (responseMsg == null) {
                                    Log.d(TAG, "responseMsg == null")
                                } else {
                                    if (responseMsg is QCK_Q02) {
                                        log("接收:$response")
                //                                        Log.d(TAG, "上传:接收 $response")
                                        if (responseMsg.qak.qak2_QueryResponseStatus.value == ErrorEnum.NF.msg) {
                                            responseMap?.give(ackID, response)
                                        } else {
                //                                            Log.d(TAG, "收到QCK^Q02:responseMsg=$responseMsg")
                                        }
                                    } else if (responseMsg is DSR_Q03) {
                                        log("接收:$response")
                                        parse(responseMsg)
                                    } else if (responseMsg is ACK) {
                                        log("接收:$responseMsg")
                                        responseMap?.give(ackID, response)
                                    } else {
                                        log("接收:意外的消息 responseMsg == $response")
                //                                        Log.d(TAG, "意外的消息 responseMsg == $response")
                                    }
                                }
                            }
                        }
                    }
                } catch (e: SocketException) {
                    Log.d(TAG, "startListener 断开连接")
                    connectService?.disconnect()
                    connectService?.reconnection()
                    return@submitThread
                } catch (e: Exception) {
                    Log.d(TAG, "startListener e=${e.message}")
                }
            }
        }
    }

    fun log(msg: String) {
        hl7Log?.invoke(msg)
    }

    val patients = mutableListOf<Patient>()

    /**
     * 解析收到的信息
     * 当收到的是样本申请信息时，需要恢复响应码并保存收到的信息
     * @param responseMsg Message
     */
    private fun parse(responseMsg: Message) {

        if (responseMsg is DSR_Q03) {
            val ack = DsrMsg.createAck(
                connectService!!.config!!.charset,
                connectService?.context!!,
                responseMsg.msh.msh10_MessageControlID.value,
                Status.AA,
                ErrorEnum.SUCCESS
            )
            sendAck(ack)

            val dsc = responseMsg.dsc.dsc1_ContinuationPointer.value
            if (dsc == "1") {//传输开始
                patients.clear()
            }
            val patient = convertDsrToPatient(responseMsg)
            patients.add(patient)
            if (dsc.isNullOrEmpty() || dsc == "-1") {//传输结束了
                responseMap?.give(getMsgId(responseMsg), convertPatientsToStr(patients))
                patients.clear()
            }
        }
    }

    /**
     * 解析为json字符串
     * @param patients MutableList<Patient>
     * @return String?
     */
    private fun convertPatientsToStr(patients: MutableList<Patient>): String? {
        return gson.toJson(patients)
    }

    private fun convertDsrToPatient(responseMsg: DSR_Q03): Patient {
        val name = if (responseMsg.dspAll.size > 0) {
            responseMsg.dspAll[0].dsp3_DataLine.value
        } else {
            ""
        }
        val sex = if (responseMsg.dspAll.size > 1) {
            responseMsg.dspAll[1].dsp3_DataLine.value
        } else {
            ""
        }
        val age = if (responseMsg.dspAll.size > 2) {
            responseMsg.dspAll[2].dsp3_DataLine.value
        } else {
            ""
        }
        val bc = if (responseMsg.dspAll.size > 3) {
            responseMsg.dspAll[3].dsp3_DataLine.value
        } else {
            ""
        }
        val sn = if (responseMsg.dspAll.size > 4) {
            responseMsg.dspAll[4].dsp3_DataLine.value
        } else {
            ""
        }
        val deliveryTime = if (responseMsg.dspAll.size > 5) {
            responseMsg.dspAll[5].dsp3_DataLine.value
        } else {
            ""
        }
        val tdh = if (responseMsg.dspAll.size > 6) {
            responseMsg.dspAll[6].dsp3_DataLine.value
        } else {
            ""
        }
        val deliveryDoctor = if (responseMsg.dspAll.size > 7) {
            responseMsg.dspAll[7].dsp3_DataLine.value
        } else {
            ""
        }
        val deliveryDepartments = if (responseMsg.dspAll.size > 8) {
            responseMsg.dspAll[8].dsp3_DataLine.value
        } else {
            ""
        }


        return Patient(
            name, age, sex, deliveryTime, deliveryDoctor, deliveryDepartments, bc, sn, tdh
        )
    }


    fun submitThread(task: Runnable) {
        threads.execute(task)
    }

    /**
     * 发送响应消息
     * @param msg Message
     */
    private fun sendAck(msg: Message) {
        threads.execute {
            connectService?.pipeParser?.encode(msg)?.let {
                sendData(it)
            }

//            hl7Write?.putMessage(msgStr, output);
//            Log.d(TAG, "上传:发送 sendAck:msgStr=$msgStr ")

        }
    }

    /**
     * 发送消息等待响应，当超时未响应时尝试重发
     * 不管回复的是ACK还是NCK都算成功了，只有超时未响应时才会尝试重发
     * @param ba ByteArray
     * @param key String
     * @param condition (String?) -> Int
     * @return Int 超时返回-1(超时不由调用者返回)，成功返回1，失败返回0
     */
    fun sendWaitResponseRetry(
        ba: String,
        key: String,
        condition: (String?) -> Int
    ): Int {

        return sendWaitResponseRetry(
            ba,
            key,
            connectService?.config?.timeout ?: 3000,
            TimeUnit.MILLISECONDS,
            connectService?.config?.retryCount ?: 3,
            condition
        )
    }

    /**
     * 发送消息等待响应，当超时未响应时尝试重发
     * 不管回复的是ACK还是NCK都算成功了，只有超时未响应时才会尝试重发
     * @param ba ByteArray
     * @param timeout Long
     * @param timeUnit TimeUnit
     * @param retryCount Int
     * @return Int
     */
    private fun sendWaitResponseRetry(
        ba: String,
        key: String,
        timeout: Long = 200000,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        retryCount: Int = 3,
        condition: (String?) -> Int
    ): Int {
        var count = 0
        do {
            if (count > 0) {
                Log.i(TAG, "正在重发 $count 最大重发数 $retryCount")
            }
            val ret = sendWaitResponse(ba, key, timeout, timeUnit) { str ->
                if (str == null) {//如果无响应，就返回-1
                    -1
                } else {//其他情况执行回调，相当于只拦截超时重发的情况
                    condition.invoke(str)
                }
            }
            if (ret >= 0) {//不管回复的是ACK还是NCK都算成功了
                return ret
            }
            count++
        } while (count < retryCount)

        return -1
    }

    private fun sendData(resp1: String): Int {
        log("发送：$resp1")
        connectService?.putMessage(resp1)
        return 1
    }

    /**
     * 发送数据等待返回码
     * ack 1
     * nck 0
     * noResponse -1
     */
    private fun sendWaitResponse(
        ba: String,
        key: String,
        timeout: Long = 3000,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        condition: (String?) -> Int
    ): Int {
        sendData(ba)
        return waitResponse(key, timeout, timeUnit, condition)
    }

    /**
     * 等待某个key的值
     * @param key String
     * @param timeout Long
     * @param timeUnit TimeUnit
     * @param condition Function1<String?, Int>
     * @return Int
     */
    private fun waitResponse(
        key: String,
        timeout: Long,
        timeUnit: TimeUnit,
        condition: (String?) -> Int
    ): Int {
        val future2 = responseMap?.asyncPoll(key, timeout, timeUnit)
        val ret = future2?.get()
        return condition.invoke(ret)
    }

    override fun connect(config: ConnectConfig, onConnectListener: OnConnectListener?) {
        disconnect()//连接前先断开以前的连接
        connectService = ConnectServiceFactory.create(config) { startListener() }
        connectService?.connect(config, onConnectListener)
    }

    override fun disconnect() {
        connectService?.disconnect()
    }

    override fun isConnected(): Boolean {
        if (connectService == null) return false
        return connectService!!.isConnected()
    }


    fun getContext(): HapiContext? {
        return connectService?.context
    }
}
