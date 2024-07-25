package com.wl.wllib

import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

object LogToFile {
    @JvmStatic
    var fileName1 = "/sdcard/MyLog10.txt"

    @JvmStatic
    var fileName2 = "/sdcard/MyLog11.txt"
    private var file1: File? = null
    private var file2: File? = null
    private var curFile: File? = null
    var MaxSize = 50 * 1024 * 1024

    val DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS")
    private var fos: FileOutputStream? = null

    @JvmStatic
    var format: Format? = null

    enum class Code {
        User,//用户操作
        Cmd, //与下位机交互
        Info,//运行日志
        Error//异常报错
    }

    private var isAllowInput = true
    private var tsHashMap = hashMapOf<String, TSModel>()

    @JvmStatic
    fun stopInput() {
        isAllowInput = false
        fos?.let {
            synchronized(it) {
                fos?.close()
            }
        }
    }

    @JvmStatic
    fun startInput() {
        init()
        isAllowInput = true
        putMsgTs()
    }

    private fun putMsgTs() {
        for ((k, v) in tsHashMap) {
            input(v.code, v.tag, v.msg)
        }
        tsHashMap.clear()
    }

    @JvmStatic
    fun init() {
        file1 = File(fileName1)
        file2 = File(fileName2)
        if (!file1!!.exists()) {
            file1!!.parentFile.mkdirs()
            file1!!.createNewFile()
        }
        if (!file2!!.exists()) {
            file2!!.parentFile.mkdirs()
            file2!!.createNewFile()
        }
        curFile = getFile()
        fos = FileOutputStream(curFile, true)
    }

    /**
     * 获取这次输出的文件
     * 如果文件大于最大值并且另一个文件大于1/3最大值，就删除这个文件
     * 1、如果两个文件有大于最大值的直接删除
     * 2、返回输出的文件
     * 2.1、file1Size == 0 && file2Size 不为空 return file2
     * 2.2、file2Size == 0 && file1Size 不为空 return file1
     * 2.3、两个都不为空 return 最小的文件
     * @return File
     */
    fun getFile(): File {
        var file1Size = file1!!.length()
        var file2Size = file2!!.length()

        if (file1Size > MaxSize && file2Size > MaxSize / 3) {
            file1!!.delete()
            file1!!.createNewFile()
            file1Size = 0
        } else if (file2Size > MaxSize && file1Size > MaxSize / 3) {
            file2!!.delete()
            file2!!.createNewFile()
            file2Size = 0
        }
        return if (file1Size == 0L && file2Size in 1 until MaxSize) {
            file2!!
        } else if (file2Size == 0L && file1Size in 1 until MaxSize) {
            file1!!
        } else {
            min(file1Size, file2Size).let {
                if (it == file2Size) {
                    return file2!!
                } else {
                    return file1!!
                }
            }
        }
    }

    @JvmStatic
    fun input(code: String, tag: String, msg: String) {
        format(code, tag, msg).let {
            Log.d(tag, "$code>>>[$msg]")
            if (isAllowInput) {
                write("$it\n")
            } else {
                tsHashMap.put(code, TSModel(code, tag, msg))
            }
        }
    }

    @JvmStatic
    fun u(msg: String) {
        u(getTag(), msg)
    }

    @JvmStatic
    fun u(tag: String, msg: String) {
        input(Code.User.name, tag, msg)
    }

    @JvmStatic
    fun i(msg: String) {
        i(getTag(), msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        input(Code.Info.name, tag, msg)
    }

    @JvmStatic
    fun c(msg: String) {
        c(getTag(), msg)
    }

    @JvmStatic
    fun c(tag: String, msg: String) {
        input(Code.Cmd.name, tag, msg)
    }

    @JvmStatic
    fun e(msg: String) {
        e(getTag(), msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        input(Code.Error.name, tag, msg)
    }

    private fun format(code: String, tag: String, msg: String): String {
        if (format == null) format = defaultFormat
        return format!!.onFormat(code, tag, msg)
//        return "${DateFormat.format(Date())}>${tag}/${code}>>>[${msg}]"
    }

    @JvmStatic
    fun write(msg: String) {
        fos?.let {
            synchronized(it) {
                it.write(msg.toByteArray())
                //强制内存缓冲区内的数据写入到系统。不保证写入到磁盘
                it.flush()
                //强制写入此流的数据到磁盘
                it.fd.sync()
            }
        }
    }

    private fun getTag(): String {
        val sts = Throwable().stackTrace
        val jk = sts.find {
            it.className.split(".").last() != this.javaClass.simpleName
        }?.className?.split(".")?.last() ?: this.javaClass.simpleName
        return jk
    }

    interface Format {
        fun onFormat(code: String, tag: String, msg: String): String
    }

    private val defaultFormat = object : Format {
        override fun onFormat(code: String, tag: String, msg: String): String {
            return "${DateFormat.format(Date())}>${tag}/${code}>>>[${msg}]"
        }
    }

    data class TSModel(val code: String, val tag: String, val msg: String)
}
