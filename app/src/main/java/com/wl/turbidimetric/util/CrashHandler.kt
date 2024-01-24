package com.wl.turbidimetric.util

import com.wl.turbidimetric.app.App
import com.wl.wllib.LogToFile.e
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

class CrashHandler: Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        saveCrashInfo2Str(e)
        App.instance?.exit()
    }

    private fun saveCrashInfo2Str(ex: Throwable) {
        val writer: Writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause: Throwable? = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        e("saveCrashInfo2Str=${result.replace("\n","    换行  ")}")
    }
}
