package com.wl.turbidimetric.upload.hl7.service


import android.os.Handler
import android.os.Message
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.llp.MinLowerLayerProtocol
import ca.uhn.hl7v2.parser.PipeParser
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.hl7.util.HL7Reader
import com.wl.turbidimetric.upload.hl7.util.HL7Write
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.service.ConnectService
import com.wl.turbidimetric.upload.service.OnConnectListener
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * 对ConnectService添加了发送接收消息和断开重连的方法
 * @property successListener Function0<Unit>
 * @property TAG String
 * @property output OutputStream?
 * @property input InputStream?
 * @property config ConnectConfig?
 * @property onConnectListener OnConnectListener?
 * @property WHAT_RECONNECTION Int
 * @property hl7Reader HL7Reader?
 * @property hl7Write HL7Write?
 * @property pipeParser PipeParser?
 * @property context HapiContext?
 * @property handler Handler
 * @property isConnect Boolean
 * @constructor
 */
abstract class AbstractConnectService(private val successListener: () -> Unit) : ConnectService {
    val TAG = "AbstractConnectService"
    var output: OutputStream? = null
    var input: InputStream? = null

    var config: ConnectConfig? = null
    var onConnectListener: OnConnectListener? = null

    /**
     * 重新连接的消息
     */
    val WHAT_RECONNECTION = 1000
    var hl7Reader: HL7Reader? = null
    var hl7Write: HL7Write? = null

    var pipeParser: PipeParser? = null
    var context: HapiContext? = null
    val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (WHAT_RECONNECTION == msg.what) {
                cancelConnectionMsg()
                config?.let {
                    onConnectListener?.onConnectStatusChange(ConnectStatus.RECONNECTION)
                    connect(it, onConnectListener)
                }
            }
        }
    }

    var isConnect: Boolean = false

    open fun initSuccess() {
        successListener.invoke()
    }

    fun initContext(charset: Charset) {
        context = DefaultHapiContext().apply {
            val mllp = MinLowerLayerProtocol()
            mllp.setCharset(charset)
            lowerLayerProtocol = mllp
            validationContext = ValidationContextFactory.noValidation()
        }

        pipeParser = context!!.pipeParser
    }

    fun getMessage(): String? {
        return hl7Reader?.getMessage(input)
    }

    fun putMessage(msg: String?) {
        hl7Write?.putMessage(msg, output!!)
    }

    /**
     * 发起重新连接的消息
     */
    fun reconnection() {
        if (config!!.isReconnection) {
            handler.sendEmptyMessageDelayed(
                WHAT_RECONNECTION,
                config!!.reconnectionTimeout
            )
        }
    }

    /**
     * 取消重新连接的消息
     */
    fun cancelConnectionMsg() {
        handler.removeMessages(WHAT_RECONNECTION)
    }
}
