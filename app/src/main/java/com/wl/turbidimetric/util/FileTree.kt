package com.wl.turbidimetric.util

import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileTree {
    lateinit var fos: FileOutputStream
    val fileName1 = "/sdcard/MyLog10.txt"
    val fileName2 = "/sdcard/MyLog11.txt"
    var file1: File;
    var file2: File;
    var curFile: File;
    val MaxSize = 300 * 1024 * 1024
    private val DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS")

    val instance = object : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
            super.log(priority, tag, message, t)
            fos.write("${DateFormat.format(Date())}/${tag ?: (tag ?: "")}>>>[${message}]\n".toByteArray())
            fos.flush();
        }
    }

    init {
        file1 = File(fileName1)
        file2 = File(fileName2)
        if (!file1.exists()) {
            file1.getParentFile().mkdirs()
            file1.createNewFile()
        }
        if (!file2.exists()) {
            file2.getParentFile().mkdirs()
            file2.createNewFile()
        }
        curFile = getFile()
        fos = FileOutputStream(curFile, true)


    }


    fun getFile(): File {
        return if (file1.length() > MaxSize) {
            if (file1.length() > MaxSize / 3) {
                file2.delete()
            }
            file2
        } else {
            if (file2.length() > MaxSize / 3) {
                file1.delete()
            }
            file1
        }
    }

}
