package com.wl.turbidimetric.upload.msg

import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.Message
import com.wl.turbidimetric.upload.hl7.util.*
import com.wl.turbidimetric.upload.model.GetPatientCondition

object QryMsg {

    @JvmStatic
    fun create(
        charset: String,
        t: GetPatientCondition,
        msgId: Long,
        qId: Long
    ): String {
        val sb = StringBuffer()
        sb.append(createMSH(charset, "QRY^Q02", msgId))
        sb.append(createQRD(t, qId))
        return sb.toString()
    }

    private fun createQRD(t: GetPatientCondition, qId: Long): String {
        return arrayToString(
            createArray(
                13,
                hashMapOf(
                    0 to "QRD",
                    1 to getCurDateTime(),
                    2 to t.type.msg,
                    3 to "D",
                    4 to "$qId",
                    8 to t.condition1,
                    9 to t.condition2,
                    12 to "1",
                )
            ), "|"
        )
    }

    /**
     * @param oruR01 ORU_R01
     * @param t TestResult
     * @return MSH
     */
    private fun createMSH(charset: String, msgType: String, msgId: Long): String {
        val arr = createArray(
            18,
            hashMapOf(
                0 to "MSH",
                1 to "^~\\&",
                2 to "BZ",
                4 to "BZ",
                5 to "LIS System",
                6 to getCurDateTime(),
                8 to msgType,
                9 to "$msgId",
                10 to "P",
                11 to "2.3.1",
                17 to charset
            )
        )
        return arrayToString(arr, "|").plus("\r")
    }

    fun createAck(
        charset: String,
        hapiContext: HapiContext, msgId: String,
        msaStatus: Status,
        msaErrorStatus: ErrorEnum
    ): Message {
        val mshArr = createArray(
            18, hashMapOf(
                0 to "MSH",
                1 to "^~\\&",
                2 to "Lis",
                4 to "Lis",
                5 to "BZ",
                6 to getCurDateTime(),
                8 to "QCK^Q02",
                9 to "$msgId",
                10 to "P",
                11 to "2.3.1",
                17 to charset
            )
        )


        val msaArr = createArray(
            7, hashMapOf(
                0 to "MSA",
                1 to "${msaStatus.msg}",
                2 to "$msgId",
                3 to "${msaErrorStatus.msg}",
                6 to "${msaErrorStatus.code}",
            )
        )

        val qakArr = createArray(
            3, hashMapOf(
                0 to "QAK",
                1 to "SR",
                2 to "${msaErrorStatus.msg}",
            )
        )
        val msgStr = arrayToString(mshArr, "|").plus("\r")
            .plus(arrayToString(msaArr, "|").plus("\r"))
            .plus(arrayToString(qakArr, "|").plus(""))

        val msg = hapiContext.pipeParser.parse(msgStr)
        return msg
    }
}
