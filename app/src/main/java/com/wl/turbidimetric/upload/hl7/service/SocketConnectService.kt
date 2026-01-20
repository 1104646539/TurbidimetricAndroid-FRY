package com.wl.turbidimetric.upload.hl7.service

import android.util.Log
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.hl7.util.ErrorEnum
import com.wl.turbidimetric.upload.hl7.util.HL7Reader
import com.wl.turbidimetric.upload.hl7.util.HL7Write
import com.wl.turbidimetric.upload.model.ConnectConfig
import com.wl.turbidimetric.upload.service.OnConnectListener
import java.net.Socket
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread


/**
 * 实现基础的连接、断开 网络连接
 * @property socket Socket?
 * @constructor
 */
open class SocketConnectService(successListener: () -> Unit) : AbstractConnectService(
    successListener
) {
    private var socket: Socket? = null

    /**
     * 建立连接
     * @param config ConnectConfig
     * @param onConnectListener OnConnectListener?
     */
    override fun connect(
        config: ConnectConfig, onConnectListener: OnConnectListener?
    ) {
        cancelConnectionMsg()
        this.config = config
        this.onConnectListener = onConnectListener
        initContext(Charset.forName(config.charset))
        hl7Write = HL7Write(Charset.forName(config.charset))
        hl7Reader = HL7Reader(Charset.forName(config.charset))
        thread {
            try {
                socket?.close()
                socket = if (config.tlsEnabled) {
                    createTlsSocket(config.ip, config.port)
                } else {
                    Socket(config.ip, config.port)
                }
                output = socket!!.getOutputStream()
                input = socket!!.getInputStream()
                initSuccess()
                onConnectListener?.onConnectStatusChange(ConnectStatus.CONNECTED)
                onConnectListener?.onConnectResult(ConnectResult.Success())
                cancelConnectionMsg()
                isConnect = true
            } catch (e: Exception) {
                Log.d(TAG, "e${e}")
                isConnect = false
                onConnectListener?.onConnectStatusChange(ConnectStatus.DISCONNECTED)
                onConnectListener?.onConnectResult(
                    ConnectResult.OrderError(
                        ErrorEnum.NOT_CONNECTED.code, "连接失败 ${e.message}"
                    )
                )
                cancelConnectionMsg()
                reconnection()
            }
        }
    }

    private fun createTlsSocket(ip: String, port: Int): SSLSocket {
        val nowMillis = System.currentTimeMillis()
        val minReasonableMillis = 1577808000000L
        if (nowMillis < minReasonableMillis) {
            throw IllegalStateException("系统时间异常：${Date(nowMillis)}，请校准系统时间后再使用TLS")
        }
        val sslSocketFactory = createPinnedSSLSocketFactory()
        val sslSocket = sslSocketFactory.createSocket(ip, port) as SSLSocket
        sslSocket.useClientMode = true
        Log.d(TAG, "TLS deviceTime=${Date(nowMillis)}")
        Log.d(TAG, "TLS supportedProtocols=${sslSocket.supportedProtocols?.joinToString()}")
        Log.d(TAG, "TLS enabledProtocols=${sslSocket.enabledProtocols?.joinToString()}")
        Log.d(TAG, "TLS enabledCipherSuites=${sslSocket.enabledCipherSuites?.joinToString()}")
        try {
            sslSocket.startHandshake()
            val session = sslSocket.session
            val peer = runCatching { session.peerPrincipal?.name }.getOrNull()
            Log.d(TAG, "TLS handshake ok protocol=${session.protocol} cipher=${session.cipherSuite} peer=$peer")
        } catch (e: Exception) {
            Log.d(TAG, "TLS handshake failed: $e")
            throw e
        }
        return sslSocket
    }

    private fun createPinnedSSLSocketFactory(): SSLSocketFactory {
        val app = App.instance ?: error("App.instance == null")
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val pinnedCertificates: List<X509Certificate> = app.assets.open("certificate.crt").use { inputStream ->
            certificateFactory.generateCertificates(inputStream).map { it as X509Certificate }
        }

        if (pinnedCertificates.isEmpty()) {
            throw CertificateException("No certificates found in assets/certificate.crt")
        }

        val pinnedPublicKeyHashes = pinnedCertificates
            .map { sha256Hex(it.publicKey.encoded) }
            .toSet()
        pinnedCertificates.forEach { c ->
            Log.d(TAG, "Pinned cert subject=${c.subjectDN.name} notBefore=${c.notBefore} notAfter=${c.notAfter}")
        }
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain == null || chain.isEmpty()) throw CertificateException("Empty server certificate chain")
                val serverPublicKeyHashes = chain.map { sha256Hex(it.publicKey.encoded) }
                if (serverPublicKeyHashes.any { it in pinnedPublicKeyHashes }) {
                    try {
                        chain.first().checkValidity()
                    } catch (e: CertificateException) {
                        throw CertificateException("服务器证书时间有效期校验失败，deviceTime=${Date(System.currentTimeMillis())}", e)
                    }
                    return
                }
                throw CertificateException(
                    "Pinned certificate mismatch. serverPublicKeySha256=$serverPublicKeyHashes pinnedPublicKeySha256=$pinnedPublicKeyHashes"
                )
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }

        return sslContext.socketFactory
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return buildString(digest.size * 2) {
            for (b in digest) append(((b.toInt() and 0xFF) + 0x100).toString(16).substring(1))
        }
    }
    override fun setOnConnectListener2(onConnectListener: OnConnectListener?) {
        this.onConnectListener = onConnectListener
    }


    override fun disconnect() {
        isConnect = false
        socket?.close()
        cancelConnectionMsg()
        onConnectListener?.onConnectStatusChange(ConnectStatus.DISCONNECTED)
    }

    override fun isConnected(): Boolean {
        Log.d(TAG, "isConnected:${socket?.isBound} ${socket?.isClosed} $isConnect")
        return (socket != null) && socket!!.isConnected && isConnect
    }

}
