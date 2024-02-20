package com.wl.turbidimetric.mcuupdate

import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.weiqianwllib.upan.StorageUtil
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


/**
 * mcu升级帮助类
 * 1、发送命令到下位机询问是否准备好升级
 * 2、得到同意才将U盘内的升级文件和计算得到的MD5发送到下位机
 * 3、下位机验证升级文件传输完毕，验证成功后回复成功
 * 4、上位机提示升级完毕，重启仪器生效
 */
class McuUpdateHelper(private val appViewModel: AppViewModel) {
    val updateFileName = "bzapp.bin"

    private var mcuUpdate: Boolean = false
        set(value) {
            field = value
            SystemGlobal.mcuUpdate = value
        }

    /**
     * 更新mcu
     *
     * @param taskDispatcher 更新时用的协程
     * @param resultDispatcher 返回结果时用的协程
     * @param coroutineScope Scope
     * @param onResult 更新结果
     */
    fun update(
        taskDispatcher: CoroutineDispatcher,
        resultDispatcher: CoroutineDispatcher,
        coroutineScope: CoroutineScope,
        onResult: OnUpdateResult
    ) {
        try {
            //step1 发送升级命令
            if (!StorageUtil.isExist() || StorageUtil.curPath?.isEmpty() == true) {
                onResult.invoke(UpdateResult.Failed("U盘不存在，请插入U盘"))
                return
            }
            val rootFile = File(StorageUtil.curPath)
            val curMcuFile = File(rootFile, updateFileName)
            if (!curMcuFile.exists()) {
                onResult.invoke(UpdateResult.Failed("${updateFileName}升级文件不存在，请检测U盘文件"))
                return
            }
            val fileSize = curMcuFile.length()
            appViewModel.serialPort.mcuUpdate(fileSize)
            //step2 收到结果，发送文件和md5
            appViewModel.serialPort.setMcuUpdateCallBack {
                coroutineScope.launch(taskDispatcher) {
                    delay(1000)
                    mcuUpdate = true
                    val tempBytes = ByteArray(256)
                    val input = curMcuFile.inputStream()
                    var readLen: Int
                    //发送文件
                    while (input.read(tempBytes).also { readLen = it } > 0) {
                        appViewModel.serialPort.updateWrite(
                            tempBytes.copyOf(readLen).toUByteArray()
                        )
                        delay(100)
                    }
                    delay(1000)
                    //发送md5
                    val md5 = getMD5(curMcuFile)
                    md5?.let {
                        appViewModel.serialPort.updateWrite(it.toByteArray().toUByteArray())
                    }
                    //step3 收到发送文件和md5的结果
                    appViewModel.serialPort.setOnResult { ret ->
                        coroutineScope.launch(resultDispatcher) {
                            //step4 提示升级完毕，重启仪器生效
                            onResult.invoke(ret)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            mcuUpdate = false
            onResult.invoke(UpdateResult.Failed("异常停止 ${e.message}"))
        }
    }

    /**
     * 获取本地文件的md5值
     *
     * @param f 需要计算的文件
     * @return
     */
    private fun getMD5(f: File): String? {
        var bi: BigInteger? = null
        val md5 = StringBuilder()
        try {
            val buffer = ByteArray(8192)
            var len = 0
            val md = MessageDigest.getInstance("MD5")
            val fis = FileInputStream(f)
            while (fis.read(buffer).also { len = it } != -1) {
                md.update(buffer, 0, len)
            }
            fis.close()
            val bs = md.digest()
            bi = BigInteger(1, bs)
            //自动补0，不然会出现问题
            for (offset in bs.indices) {
                var i = bs[offset].toInt()
                if (i < 0) i += 256
                if (i < 16) md5.append("0")
                md5.append(Integer.toHexString(i))
            }
            i("getMD5 kk=$md5")
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return md5.toString()
    }
}

sealed class UpdateResult {
    class Failed(val msg: String) : UpdateResult()
    class Success(val msg: String) : UpdateResult()
}
typealias OnUpdateResult = ((UpdateResult) -> Unit)
