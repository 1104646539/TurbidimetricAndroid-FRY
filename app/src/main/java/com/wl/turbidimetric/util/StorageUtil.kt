package com.wl.turbidimetric.util

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import timber.log.Timber
import java.io.File
import java.lang.reflect.Method

object StorageUtil {
    private var curPath: String? = null

    fun startInit(context: Context) {
        curPath = getStoragePath(context, true)
        Timber.d("curPath=$curPath")
    }

    fun isExist(): Boolean {
        if (curPath.isNullOrEmpty()) return false

        return File(curPath).exists()
    }

    fun setCurPath(path: String?) {
        this.curPath = path
    }

    /**
     * 通过反射调用获取内置存储和外置sd卡根路径(通用)
     *
     * @param mContext    上下文
     * @param is_removable 是否可移除，false返回内部存储路径，true返回外置SD卡路径
     * @return
     */
    private fun getStoragePath(mContext: Context, is_removable: Boolean): String? {
        var path: String? = null
        //使用getSystemService(String)检索一个StorageManager用于访问系统存储功能。
        val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var storageVolumeClazz: Class<*>? = null
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList: Method = mStorageManager.javaClass.getMethod("getVolumeList")
            val getPath: Method = storageVolumeClazz.getMethod("getPath")
            val isRemovable: Method = storageVolumeClazz.getMethod("isRemovable")
            val result: Array<StorageVolume> =
                getVolumeList.invoke(mStorageManager)!! as Array<StorageVolume>
            for (i in result.indices) {
                val storageVolumeElement: Any = result[i]
                path = getPath.invoke(storageVolumeElement) as String
                val removable = isRemovable.invoke(storageVolumeElement) as Boolean
                Timber.d("path=$path")
                if (is_removable == removable) {
                    return path
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return path
    }
}
