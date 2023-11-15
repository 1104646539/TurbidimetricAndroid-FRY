package com.wl.turbidimetric.upload.msg

import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.Message
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.hl7.util.*

object DsrMsg {
    @JvmStatic
    fun create(
        charset: String,
        patient: Patient,
        msgId: Long,
        rowId: Int,
        end: Boolean
    ): String {


        val sb = StringBuffer()
        sb.append(createMSH(charset, "DSR^Q03", msgId))
        sb.append(createMSA())
//        sb.append(createERR())
        sb.append(createQAK())
        sb.append(createDSPs(patient, rowId, end))
//        val msg = hapiContext.pipeParser.parse(sb.toString())

        return sb.toString()
    }

    fun createMSA(): String {
        val arr = createArray(
            6, hashMapOf(
                0 to "MSA",
                1 to "AA",
                2 to "2",
            )
        )
        return arrayToString(arr, "|").plus("\r")
    }

    fun createERR(): String {
        val arr = createArray(
            2, hashMapOf(
                0 to "ERR",
                1 to "0",
            )
        )
        return arrayToString(arr, "|").plus("\r")
    }

    fun createQAK(): String {
        val arr = createArray(
            3, hashMapOf(
                0 to "QAK",
                1 to "SR",
                2 to "OK",
            )
        )
        return arrayToString(arr, "|").plus("\r")
    }

    private fun createDSPs(patient: Patient, rowId: Int, end: Boolean): String {


        val dsp1 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "1",
                    3 to patient.name,
                )
            ), "|"
        ).plus("\r")


        val dsp2 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "2",
                    3 to patient.sex,
                )
            ), "|"
        ).plus("\r")


        val dsp3 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "3",
                    3 to patient.age,
                )
            ), "|"
        ).plus("\r")


        val dsp4 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "4",
                    3 to patient.bc,
                )
            ), "|"
        ).plus("\r")


        val dsp5 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "5",
                    3 to patient.sn,
                )
            ), "|"
        ).plus("\r")


        val dsp6 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "6",
                    3 to patient.deliveryTime,
                )
            ), "|"
        ).plus("\r")

        val dsp7 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "7",
                    3 to patient.tdh,
                )
            ), "|"
        ).plus("\r")

        val dsp8 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "8",
                    3 to patient.deliveryDoctor,
                )
            ), "|"
        ).plus("\r")

        val dsp9 = arrayToString(
            createArray(
                6, hashMapOf(
                    0 to "DSP",
                    1 to "9",
                    3 to patient.deliveryDepartments,
                )
            ), "|"
        ).plus("\r")

        val dsc = arrayToString(
            createArray(
                2, hashMapOf(
                    0 to "DSC",
                    1 to if (end) "-1" else "$rowId",
                )
            ), "|"
        )


        val sb = StringBuffer()
        sb.append(dsp1)
        sb.append(dsp2)
        sb.append(dsp3)
        sb.append(dsp4)
        sb.append(dsp5)
        sb.append(dsp6)
        sb.append(dsp7)
        sb.append(dsp8)
        sb.append(dsp9)
        sb.append(dsc)
        return sb.toString()
    }

    /**
     * @param oruR01 ORU_R01
     * @param t TestResult
     * @return MSH
     */
    fun createMSH(charset: String, msgType: String, msgId: Long): String {
        val arr = createArray(
            18, hashMapOf(
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
//                17 to "UNICODE-UTF8"
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
        /**
         * MSH|^~\&|BZ|LIS System|BZ||20231102100639.974+0800||ACK^R01|4|P|2.3.1MSA|AA|2
         */
        /**
         * <SB>MSH|^~\&|||||20120508110131||ACK^Q03|4|P|2.3.1||||||ASCII|||<CR>
         * MSA|AA|4|Message accepted|||0|<CR>
         * <EB><CR>
         */
        val mshArr = createArray(
            18, hashMapOf(
                0 to "MSH",
                1 to "^~\\&",
                2 to "BZ",
                4 to "BZ",
                5 to "LIS",
                6 to getCurDateTime(),
                8 to "ACK^Q03",
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
        val msgStr = arrayToString(mshArr, "|").plus("\r")
            .plus(arrayToString(msaArr, "|").plus(""))

        val msg = hapiContext.pipeParser.parse(msgStr)
        return msg
    }
}
