package com.wl.turbidimetric.upload.test

import android.util.Log
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.concurrent.BlockingHashMap
import ca.uhn.hl7v2.llp.LLPException
import ca.uhn.hl7v2.llp.MinLowerLayerProtocol
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v231.message.ACK
import ca.uhn.hl7v2.model.v231.message.ORU_R01
import ca.uhn.hl7v2.model.v231.message.QRY_Q02
import ca.uhn.hl7v2.parser.PipeParser
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import com.wl.turbidimetric.upload.hl7.service.SerialPortInputStream
import com.wl.turbidimetric.upload.hl7.service.SerialPortOutputStream
import com.wl.turbidimetric.upload.hl7.util.*
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.msg.DsrMsg
import com.wl.turbidimetric.upload.msg.OruMsg
import com.wl.turbidimetric.upload.msg.QryMsg
import com.wl.weiqianwllib.serialport.BaseSerialPort
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 测试用的HL7 服务器端
 * @property config ConnectConfig
 * @property TAG String
 * @property serverSocket ServerSocket
 * @property hl7Write HL7Write?
 * @property hl7Reader HL7Reader?
 * @property serviceOs OutputStream?
 * @property serviceIs InputStream?
 * @property context HapiContext?
 * @property pipeParser PipeParser?
 * @constructor
 */
class TestHl7Thread(private val config: ConnectConfig) : Thread() {
    private val TAG = "TestHl7Service"
    private var serverSocket: ServerSocket? = null
    private var serialPort: BaseSerialPort? = null
    val responseMap = BlockingHashMap<String, String>()
    val hl7Write =
        HL7Write(Charset.forName(config.charset))
    val hl7Reader =
        HL7Reader(Charset.forName(config.charset))

    var ops: OutputStream? = null
    var ips: InputStream? = null

    var context: HapiContext? = null
    var pipeParser: PipeParser? = null
    private fun initContext() {
        context = DefaultHapiContext().apply {
            val mllp = MinLowerLayerProtocol();
            mllp.setCharset(config.charset);
            lowerLayerProtocol = mllp;
            validationContext = ValidationContextFactory.noValidation()
        }

        pipeParser = context!!.pipeParser
        if (!config.serialPort) {
            serverSocket = ServerSocket(config.port)
        } else {
            serverSocket = null
            serialPort = BaseSerialPort()
            serialPort!!.openSerial(config.serialPortName, config.serialPortBaudRate, 8)
            ips = SerialPortInputStream(serialPort!!)
            ops = SerialPortOutputStream(serialPort!!)
        }
    }

    init {
        initContext()
    }

    override fun run() {
        super.run()
        while (true) {
            if (serverSocket != null) {
                val socket = serverSocket?.accept()
                ops = socket!!.getOutputStream()
                ips = socket!!.getInputStream()

            }

            looperMsg()
        }
    }

    private fun looperMsg() {
        try {
            var msgStr: String? = ""
            while (true) {
                if (hl7Reader?.getMessage(ips).also { msgStr = it } != null) {
                    val msg = pipeParser!!.parse(msgStr)
//                    Log.d(TAG, "服务器接收到的 msg=$msg")

                    response(msg)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: HL7Exception) {
            e.printStackTrace()
        } catch (e: LLPException) {
            e.printStackTrace()
        }
    }


    /**
     * 根据接收到客户端的不同消息，回复不一样的响应码
     * 当收到的是ack消息时，不回复
     * @param msg Message
     */
    private fun response(msg: Message) {
        val ack = parse(msg)
        ack?.let {
            val resp1 = pipeParser!!.encode(it)
            Log.d(TAG, "服务器回复的响应码 sendAck: ack=$resp1")
            hl7Write?.putMessage(resp1, ops)
        }
    }

    fun encode() {

    }

    /**
     * 根据不同消息，返回不一样的响应码
     **/
    private fun parse(msg: Message): Message? {
        return if (msg is ORU_R01) {
            val oruMsg = msg
            parseOruMsg(oruMsg)
        } else if (msg is QRY_Q02) {
            val qryMsg = msg

//            parseQryMsg(qryMsg)
            parseQryMsgAck(qryMsg)
        } else if (msg is ACK) {
            val ackMsg = msg
            parseDSRAckMsg(ackMsg)
            null
        } else {
            msg!!.generateACK()
        }
    }

    private fun parseDSRAckMsg(ack: ACK) {
//        Log.d(TAG, "解析后的消息 parseAckMsg: ack=$ack")
        if (ack.msh.msh9_MessageType.msg1_MessageType.value == "ACK"
            && ack.msh.msh9_MessageType.msg2_TriggerEvent.value == "Q03"
        ) {
            responseMap.give(getMsgId(ack), ack.msa.msa1_AcknowledgementCode.value)
        }
    }

    private fun getPatients(): List<Patient> {
        return mutableListOf(
            Patient(
                "张三",
                "26",
                "男",
                "20230918125220",
                "张医生",
                "体检科",
                "123456",
                "1",
                "FOB"
            ),
            Patient(
                "李四",
                "28",
                "女",
                "20230918125220",
                "张医生",
                "体检科",
                "123457",
                "1",
                "FOB"
            ),
        )
    }

    private fun sendPatients(ps: List<Patient>, msgId: String) {
        ps.forEachIndexed { index, patient ->
            val last = ps.lastIndex == index
            val ret = sendPatient(patient, msgId, index + 1, last)

            if (ret > 0) {
                Log.d(TAG, "发送:ret=$ret patient=$patient ")
            } else if (ret == 0) {
                //拒绝访问
                Log.d(TAG, "拒绝访问:ret=$ret patient=$patient ")
                return
            } else {
                //超时
                Log.d(TAG, "超时:ret=$ret patient=$patient ")
                return
            }
        }

    }

    private fun sendPatient(p: Patient, msgId: String, rowId: Int, end: Boolean): Int {
        val msg = DsrMsg.create(config.charset,  p, msgId.toLong(), rowId, end)
//        val str = context!!.pipeParser.encode(msg)
        Log.d(TAG, "sendPatient: msg=$msg")
        val ret = sendWaitResponseRetry(msg, msgId) { re ->
            if (re == null) {
                return@sendWaitResponseRetry -1
            } else if (re == "AA") {
                return@sendWaitResponseRetry 1
            }
            return@sendWaitResponseRetry 0
        }
        return ret

    }

    /**
     * 接收 查询患者信息的消息
     * @param qryMsg QRY_Q02
     * @return Message
     */
    private fun parseQryMsgAck(qryMsg: QRY_Q02): Message {
        val qrd = qryMsg.qrd
        val queryTime = qrd.qrd1_QueryDateTime.timeOfAnEvent.value
        val queryType = qrd.qrd2_QueryFormatCode.value
        val qrd3 = qrd.qrd3_QueryPriority.value
        val queryId = qrd.qrd4_QueryID.value
        val condition1 =
            if (qrd.qrd8_WhoSubjectFilter.isEmpty()) "" else qrd.qrd8_WhoSubjectFilter.first().xcn1_IDNumber.value
        var condition2 =
            if (qrd.qrd9_WhatSubjectFilter.isEmpty()) "" else qrd.qrd9_WhatSubjectFilter.first().ce1_Identifier.value
        val queryLevel = qrd.qrd12_QueryResultsLevel.value

//        Log.d(
//            TAG,
//            "解析后的消息 queryTime=$queryTime queryType=$queryType qrd3=$qrd3 queryId=$queryId condition1=$condition1 condition2=$condition2 queryLevel=$queryLevel"
//        )

        val patients = mutableListOf<Patient>()

        val errorEnum: ErrorEnum = when (queryType.uppercase()) {
            GetPatientType.SN.msg -> {
                if (condition1.isNullOrEmpty()) {
                    ErrorEnum.NF
                } else {
                    if (condition2.isNullOrEmpty()) {
                        condition2 = condition1
                    }
                    var snStart = condition1.toIntOrNull() ?: -1
                    var snEnd = condition2.toIntOrNull() ?: -1

                    if (snEnd < snStart) {
                        snEnd = snStart
                    }
                    //snStart必须是大于等于0的数
                    if (snStart < 0) {
                        ErrorEnum.NF
                    } else {
                        for (i in snStart..snEnd) {
                            patients.add(
                                Patient(
                                    "李四",
                                    "${26 + i}",
                                    "M",
                                    "20230919125220",
                                    "张医生",
                                    "体检科",
                                    "ABCDEF$i",
                                    "$i",
                                    "FOB"
                                )
                            )
                        }
                        ErrorEnum.SUCCESS
                    }
                }
            }
            GetPatientType.BC.msg -> {
                if (condition1.isNullOrEmpty() ) {
                    ErrorEnum.NF
                } else {
                    patients.add(
                        Patient(
                            "张三",
                            "26",
                            "F",
                            "20230919125220",
                            "张医生",
                            "体检科",
                            "$condition1",
                            "10",
                            "FOB"
                        )
                    )
                    ErrorEnum.SUCCESS
                }
            }
            GetPatientType.DT.msg -> {
                if (condition1.isNullOrEmpty() || condition2.isNullOrEmpty()) {
                    ErrorEnum.NF
                } else {
                    patients.add(
                        Patient(
                            "张三",
                            "26",
                            "O",
                            condition1,
                            "张医生",
                            "体检科",
                            "ABCD1",
                            "10",
                            "FOB"
                        )
                    )
                    patients.add(
                        Patient(
                            "张三",
                            "27",
                            "O",
                            condition2,
                            "张医生",
                            "体检科",
                            "ABCD2",
                            "11",
                            "FOB"
                        )
                    )
                    ErrorEnum.SUCCESS
                }
            }
            else -> {
                ErrorEnum.AE
            }
        }

        val ack = QryMsg.createAck(
            config.charset,
            context!!,
            qryMsg.msh.msh10_MessageControlID.value,
            if (errorEnum == ErrorEnum.SUCCESS) Status.AA else Status.AE,
            errorEnum,
        )

//        Log.d(
//            TAG,
//            "解析后的消息 queryTime=$queryTime queryType=$queryType qrd3=$qrd3 queryId=$queryId condition1=$condition1 condition2=$condition2 queryLevel=$queryLevel"
//        )
        if (errorEnum == ErrorEnum.SUCCESS) {
            Log.d(TAG, "parseQryMsgAck: 开始发送患者信息")
            //发送查询的患者信息
            thread {
                sleep(1000)
                sendPatients(patients, getMsgId(qryMsg))
            }
        }
        return ack
    }

    /**
     * 接收 检测结果消息
     * @param oruMsg ORU_R01
     * @return Message
     */
    private fun parseOruMsg(oruMsg: ORU_R01): Message {
        //患者信息
        val pid = oruMsg.pidpD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTI.pidpD1NK1NTEPV1PV2.pid
        val xpns = pid.pid5_PatientName
        val patientName =
            if (xpns.isEmpty()) "" else xpns.first().xpn1_FamilyLastName.fn1_FamilyName.value
        val patientAge = pid.pid7_DateTimeOfBirth.timeOfAnEvent.value
        val patientSex = pid.pid8_Sex.value
        //检测信息
        val obr = oruMsg.pidpD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTI.orcobrnteobxntecti.obr
        val bc = obr.obr2_PlacerOrderNumber.ei1_EntityIdentifier.value
        val sn = obr.obr3_FillerOrderNumber.ei1_EntityIdentifier.value
        val samplePosition =
            if (obr.obr10_CollectorIdentifier.isEmpty()) "" else "${obr.obr10_CollectorIdentifier.first().xcn1_IDNumber.value}^${obr.obr10_CollectorIdentifier.first().xcn2_FamilyLastName.fn1_FamilyName.value}"
        val deliveryTime = obr.obr14_SpecimenReceivedDateTime.timeOfAnEvent.value
        val deliveryDoctor =
            if (obr.obr16_OrderingProvider.isEmpty()) "" else obr.obr16_OrderingProvider.first().xcn1_IDNumber.value
        val deliveryDepartments =
            if (obr.obr17_OrderCallbackPhoneNumber.isEmpty()) "" else obr.obr17_OrderCallbackPhoneNumber.first().xtn1_9999999X99999CAnyText.value
        val testDoctor = obr.obr20_FillerField1.value
        val testTime = obr.obr22_ResultsRptStatusChngDateTime.timeOfAnEvent.value

        //检测结果
        val obxs = oruMsg.pidpD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTI.orcobrnteobxntecti.obxnteAll

//        Log.d(TAG, "解析后的消息 patientName=$patientName patientAge=$patientAge patientSex=$patientSex")
//        Log.d(
//            TAG,
//            "解析后的消息 bc=$bc sn=$sn samplePosition=$samplePosition deliveryTime=$deliveryTime deliveryDoctor=$deliveryDoctor deliveryDepartments=$deliveryDepartments testDoctor=$testDoctor testTime=$testTime"
//        )
        obxs.forEach {
            val resultType = it.obx.obx2_ValueType.value
            val tdh = it.obx.obx3_ObservationIdentifier.ce1_Identifier.value
            val projectName = it.obx.obx4_ObservationSubID.value
            val con =
                if (it.obx.obx5_ObservationValue.isEmpty()) "" else it.obx.obx5_ObservationValue.first().data.encode()
            val unit = it.obx.obx6_Units.ce1_Identifier.value
            val range = it.obx.obx7_ReferencesRange.value
            val result =
                if (it.obx.obx9_Probability.isEmpty()) "" else it.obx.obx9_Probability.first().value
            val oriValue = it.obx.obx13_UserDefinedAccessChecks.value
            val testTime = it.obx.obx14_DateTimeOfTheObservation.timeOfAnEvent.value
//            Log.d(
//                TAG,
//                "解析后的消息 resultType=$resultType tdh=$tdh projectName=$projectName con=$con unit=$unit range=$range result=$result oriValue=$oriValue testTime=$testTime"
//            )
        }

        val ack = OruMsg.createAck(
            config.charset,
            context!!,
            msgId = getMsgId(oruMsg),
            Status.AA,
            ErrorEnum.SUCCESS
        )
        return ack
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

    fun sendData(resp1: String): Int {
        hl7Write?.putMessage(resp1, ops)
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

    fun waitResponse(
        key: String,
        timeout: Long,
        timeUnit: TimeUnit,
        condition: (String?) -> Int
    ): Int {
        val future2 = responseMap.asyncPoll(key, timeout, timeUnit)
        val ret = future2.get()
        return condition.invoke(ret)
    }

    fun exit() {
        serverSocket?.close()
        serverSocket = null
        interrupt()
    }
}
