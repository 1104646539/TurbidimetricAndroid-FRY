package com.wl.turbidimetric.upload.msg

import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.Message
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.upload.hl7.util.*
import com.wl.wllib.DateUtil
import com.wl.wllib.toTimeStr

/**
 * 检测结果
 */
object OruMsg {

    @JvmStatic
    fun create(charset: String, t: TestResultAndCurveModel, msgId: Long): String {
        val sb = StringBuffer()

        sb.append(createMSH(charset, "ORU^R01", msgId))
        sb.append(createPID(t))
        sb.append(createOBR(t))
        sb.append(createOBX(t))
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
                3 to "BZ",
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

    fun createPID(t: TestResultAndCurveModel): String {
        val sex = when (t.result.gender) {
            "男" -> {
                "M"
            }
            "女" -> {
                "F"
            }
            else -> {
                "O"
            }
        }
        val arr = createArray(
            31, hashMapOf(
                0 to "PID", 5 to t.result.name, 7 to t.result.age, 8 to sex
            )
        )
        return arrayToString(arr, "|").plus("\r")
    }

    fun createOBR(t: TestResultAndCurveModel): String {
        val arr = createArray(
            46, hashMapOf(
                0 to "OBR",
                2 to t.result.sampleBarcode,
                3 to t.result.detectionNum,
                14 to t.result.deliveryTime,
                16 to t.result.deliveryDepartment,
                20 to t.result.deliveryDoctor
            )
        )
        return arrayToString(arr, "|").plus("\r")
    }

    fun createOBX(t: TestResultAndCurveModel): String {
        val range = if (t.curve?.projectLjz == null) {
            return "0-100"
        } else {
            "0-${t.curve.projectLjz}"
        }
//        val arr = createArray(
//            18, hashMapOf(
//                0 to "OBX",
//                2 to "NM",
//                3 to t.project?.target?.projectCode ?: "",
//                4 to t.project?.target?.projectName ?: "",
//                5 to t.concentration,
//                6 to t.project?.target?.projectUnit ?: "",
//                7 to range,
//                9 to t.testResult,
//                13 to t.absorbances.toString(),
//                14 to t.testTime.toTimeStr(DateUtil.Time5Format),
//            )
//        )
        val arr = createArray(
            18, hashMapOf(
                0 to "OBX",
                2 to "NM",
                3 to t.curve.projectCode,
                4 to t.curve.projectName,
                5 to t.result.concentration.toString(),
                6 to t.curve.projectUnit,
                7 to range,
                9 to t.result.testResult,
                13 to t.result.absorbances.setScale(5).toString(),
                14 to t.result.testTime.toTimeStr(DateUtil.Time5Format),
            )
        )
        return arrayToString(arr, "|")
    }

    fun createAck(
        charset: String,
        hapiContext: HapiContext,
        msgId: String,
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
                2 to "Lis",
                4 to "Lis",
                5 to "BZ",
                6 to getCurDateTime(),
                8 to "ACK^R01",
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
