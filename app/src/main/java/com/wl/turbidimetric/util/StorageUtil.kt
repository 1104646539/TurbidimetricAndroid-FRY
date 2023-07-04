package com.wl.turbidimetric.util

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.documentfile.provider.DocumentFile
import com.wl.turbidimetric.App
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.lang.reflect.Method

object StorageUtil {
    var curPath: String? = null
        private set

    fun startInit(context: Context, onPermission: (Boolean) -> Unit = {}) {
        curPath = getStoragePath(context, true)
        Timber.d("curPath=$curPath")
        if (curPath != null && curPath!!.isNotEmpty()) {
            val permission = isPermission()
            onPermission(permission)
            Timber.d("permission=$permission")
        }
    }

    fun isExist(): Boolean {
        if (curPath.isNullOrEmpty()) return false
        return File(curPath).exists()
    }

    /**
     * 判断当前U盘是否有读写权限
     * 返回true是没有权限
     * @return Boolean
     */
    fun isPermission(path: String? = curPath): Boolean {
        return DocumentsUtils.checkWritableRootPath(
            App.instance,
            path
        )
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
                Timber.d("path=$path removable=$removable")
                if (is_removable == removable) {
                    return path
                }
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return path
    }

    fun getOutputStream(documentFile: DocumentFile, context: Context): OutputStream? {
        return context?.contentResolver?.openOutputStream(documentFile.uri)
    }

    fun getDocumentDir(documentFile: DocumentFile, fileName: String): DocumentFile? {
        val file: List<DocumentFile> = documentFile.listFiles().filter { it.name == fileName }
        val documentFile = if (file.isNullOrEmpty() || !file.first().exists()) {
            null
        } else {
            file.first()
        }
        return documentFile
    }

    /**
     * 在一个目录下创建一个目录，如果已经存在就直接返回
     * @param root DocumentFile
     * @param dirName String
     * @return DocumentFile?
     */
    fun createDocumentDir(root: DocumentFile, dirName: String): DocumentFile? {
        if (dirName.isNullOrEmpty()) return null

        val file: List<DocumentFile> = root.listFiles().filter { it.name == dirName }
        val documentFile = if (file.isNullOrEmpty() || !file.first().exists()) {
            root.createDirectory(dirName)
        } else {
            file!!.first()
        }
        return documentFile
    }


    /**
     * 在一个目录下创建一个文件，如果已经存在就直接返回
     * @param root DocumentFile
     * @param dirName String
     * @return DocumentFile?
     */
    fun createDocumentFile(
        documentFile: DocumentFile,
        mimeType: String,
        name: String
    ): DocumentFile? {
        return documentFile.createFile(mimeType, name)
    }
}
