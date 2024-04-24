package com.wl.turbidimetric.upload.hl7.util

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Terser
import com.google.gson.Gson
import com.wl.turbidimetric.ex.getContent
import com.wl.turbidimetric.ex.saveFile
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.weiqianwllib.serialport.WQSerialGlobal
import com.wl.wllib.DateUtil
import com.wl.wllib.toTimeStr
import java.io.File
import java.util.Date

/**
 * 创建固定长度的数组，将map中的所有的key所对应的下标和值都填充到数组中，默认值为""
 * @param size Int 数组的长度
 * @param pr HashMap<Int, String>   key为要填充的数组下标 value为值
 * @return List<String>
 */
fun createArray(size: Int, pr: HashMap<Int, String>): List<String> {
    val arr = mutableListOf<String>()
    for (i in 0 until size) {
        arr.add(if (pr[i] == null) "" else pr[i]!!)
    }
    return arr
}

/**
 * 数组转换为string 每个元素中添加间隔符 sp
 * @param arr List<String> 数组列表
 * @param sp String 每个元素中添加的间隔符
 * @return String
 */
fun arrayToString(arr: List<String>, sp: String): String {
    val sb = StringBuffer()
    arr.forEachIndexed { index, s ->
        sb.append(s)
        if (index != arr.lastIndex) {
            sb.append(sp)
        }
    }
    return sb.toString()
}

/**
 * 获取Message 的msh中的第10个字段值 为msg-id
 * @param msg Message
 * @return String
 */
fun getMsgId(msg: Message): String {
    val t = Terser(msg)
    val messID = t["/MSH-10"]
    return messID
}

/**
 * 或者当前时间
 * @return String
 */
fun getCurDateTime(): String {
    return Date().time.toTimeStr(DateUtil.Time5Format)
}

/**
 * 保存配置到本地
 * @receiver Boolean
 */
fun ConnectConfig.save(): Boolean {
    val str = Gson().toJson(this)
    return File(UploadGlobal.UploadConfigFileName).saveFile(str)
}

/**
 * 保存配置到本地
 * @receiver ConnectConfig
 */
fun getLocalConfig(): ConnectConfig {
    val content = File(UploadGlobal.UploadConfigFileName).getContent()

    return if (content == null) {
        defaultConfig()
    } else {
        try {
            Gson().fromJson(content, ConnectConfig::class.java)
        } catch (e: Exception) {
            defaultConfig()
        }
    }
}

/**
 * 默认的上传配置信息
 * @return ConnectConfig
 */
fun defaultConfig(): ConnectConfig {
    return ConnectConfig(
        openUpload = false,
        autoUpload = false,
        "192.168.0.133",
        22222,
        "UTF-8",
        serialPort = false,
        serialPortBaudRate = 9600,
        serialPortName = WQSerialGlobal.COM4,
        isReconnection = false,
        retryCount = 1,
        reconnectionTimeout = 20000
    )
}

/**
 * 从hl7格式的性别转换成显示的
 */
fun sexHl7ToNormal(sex: String): String {
    return when (sex) {
        "M" -> {
            "男"
        }

        "F" -> {
            "女"
        }

        else -> {
            "未知"
        }

    }
}

/**
 * 从hl7格式的性别转换成显示的
 */
fun sexNormalToHl7(sex: String): String {
    return when (sex) {
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
}
