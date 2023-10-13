package com.wl.wllib

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object LogToFile {
    @JvmStatic
    var fileName1 = "/sdcard/MyLog10.txt"

    @JvmStatic
    var fileName2 = "/sdcard/MyLog11.txt"
    private var file1: File? = null;
    private var file2: File? = null;
    private var curFile: File? = null;
    var MaxSize = 300 * 1024 * 1024

    val DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS")
    var fos: FileOutputStream? = null

    @JvmStatic
    var format: Format? = null

    enum class Code {
        User,//用户操作
        Cmd, //与下位机交互
        Info,//运行日志
        Error//异常报错
    }

    @JvmStatic
    fun init() {
        file1 = File(fileName1)
        file2 = File(fileName2)
        if (!file1!!.exists()) {
            file1!!.getParentFile().mkdirs()
            file1!!.createNewFile()
        }
        if (!file2!!.exists()) {
            file2!!.getParentFile().mkdirs()
            file2!!.createNewFile()
        }
        curFile = getFile()
        fos = FileOutputStream(curFile, true)
    }

    fun getFile(): File {
        return if (file1!!.length() > MaxSize) {
            if (file1!!.length() > MaxSize / 3) {
                file2!!.delete()
            }
            file2!!
        } else {
            if (file2!!.length() > MaxSize / 3) {
                file1!!.delete()
            }
            file1!!
        }
    }

    @JvmStatic
    fun input(code: String, tag: String, msg: String) {
        format(code, tag, msg).let {
            Log.d(tag, "$code>>>[$msg]")
            write("$it\n")
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
        fos?.write(msg.toByteArray())
        fos?.flush();
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
}
