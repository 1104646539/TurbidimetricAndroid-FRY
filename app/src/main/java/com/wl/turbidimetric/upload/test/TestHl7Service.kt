package com.wl.turbidimetric.upload.test

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.wl.turbidimetric.upload.model.ConnectConfig
import kotlin.concurrent.thread


class TestHl7Service : Service() {
    val TAG = "TestService"

    inner class LocalBinder : Binder() {
        val service: TestHl7Service
            get() = this@TestHl7Service
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: TestService start")

        thread {
            val testHl7Service =
                TestHl7Thread(
                    ConnectConfig(
                        openUpload = false,
                        autoUpload = false,
                        "192.168.0.1",
                        22222,
//                        Charset.forName("UTF-8"),
                        "GBK",
                        serialPort = true,
                        serialPortBaudRate = 9600,
                        serialPortName = "COM3"
                    )
                )
            testHl7Service.start()
            Log.d(TAG, "onCreate: hl7Service start")
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onCreate: TestService stop")
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    private val mBinder: IBinder = LocalBinder()


}

