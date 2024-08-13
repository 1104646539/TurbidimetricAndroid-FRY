package com.wl.weiqianwllib.upan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.preference.PreferenceManager
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import kotlin.concurrent.thread

object StorageUtil {
    const val TAG = "StorageUtil"

    @JvmStatic
    var curPath: String? = null
        private set

    /**
     * 权限请求code
     */
    @JvmStatic
    val OPEN_DOCUMENT_TREE_CODE = 8000

    /**
     * usb权限广播
     */
    @JvmStatic
    val ACTION_USB_PERMISSION = "com.wl.USB_PERMISSION"

    @JvmStatic
    var state: StorageState = StorageState.NONE

    /**
     * 初始化U盘，如果没有权限将会弹框请求权限
     * @param context Context
     * @param onPermission Function1<Boolean, Unit>
     */
    @JvmStatic
    fun startInit(context: Context, onPermission: (allow: Boolean) -> Unit = {}) {
        curPath = getStoragePath(context, true)
        Log.d(TAG, "curPath=$curPath")
        if (curPath != null && curPath!!.isNotEmpty()) {
            DocumentsUtils.root = getRootUri(context, curPath!!)
            val permission = !isPermission(context = context)
            onPermission(permission && DocumentsUtils.root != null)
            Log.d(TAG, "permission=$permission")
        }
    }

    @JvmStatic
    fun remove() {
//        curPath = null
//        DocumentsUtils.root = null
        DocumentsUtils.cleanCache()
    }

    @JvmStatic
    fun getRootUri(context: Context, path: String): String? {
        val perf = PreferenceManager.getDefaultSharedPreferences(context)
        return perf.getString(path, null)
    }

    /**
     * 是否存在U盘
     * @return Boolean
     */
    @JvmStatic
    fun isExist(): Boolean {
        if (!state.isExist()) return false
        if (curPath.isNullOrEmpty()) return false
        return File(curPath).exists()
    }

    /**
     * 判断当前U盘是否有读写权限
     * 返回true是没有权限
     * @return Boolean
     */
    @JvmStatic
    fun isPermission(path: String? = curPath, context: Context): Boolean {
        return DocumentsUtils.checkWritableRootPath(
            context,
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
                Log.d(TAG, "path=$path removable=$removable")
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

    /**
     * 获取文件输出流
     * @param file File
     * @param context Context
     * @return OutputStream?
     */
    @JvmStatic
    fun getOutputStream(file: DocumentFile, context: Context): OutputStream? {
        return context.contentResolver.openOutputStream(file.uri)
    }

    /**
     * 获取文件输入流
     * @param file File
     * @param context Context
     * @return InputStream?
     */
    @JvmStatic
    fun getInputStream(file: DocumentFile, context: Context): InputStream? {
        return context.contentResolver.openInputStream(file.uri)
    }

    @JvmStatic
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
    @JvmStatic
    fun createDocumentDir(root: DocumentFile, dirName: String): DocumentFile? {
        if (dirName.isNullOrEmpty()) return null

        val file: List<DocumentFile> = root.listFiles().filter { it.name == dirName }
        val documentFile = if (file.isNullOrEmpty() || !file.first().exists()) {
            root.createDirectory(dirName)
        } else {
            file.first()
        }
        return documentFile
    }


    /**
     * 在一个目录下创建一个文件，如果已经存在就直接返回
     * @param root DocumentFile
     * @param dirName String
     * @return DocumentFile?
     */
    @JvmStatic
    fun createDocumentFile(
        documentFile: DocumentFile,
        mimeType: String,
        name: String
    ): DocumentFile? {
        return documentFile.createFile("*", name)
    }

    fun File.toDocumentFile(context: Context): DocumentFile {
        val file = DocumentsUtils.getDocumentFile(this, this.isDirectory, context)
        return file
    }


    /**
     * 在主路径下创建文件/文件夹,如果已经有此文件就返回此目录(此方法只能创建带后缀的文件，否则会认为是文件夹)
     * @param path String   x/y/z.后缀 例：test/test.txt
     * @param context Context
     * @param cover Boolean 当文件已存在时，是否覆盖（文件夹不生效）
     */
    @JvmStatic
    fun createFile(path: String, context: Context, cover: Boolean = false): DocumentFile? {
        if (!isExist()) {
            Log.d(TAG, "U盘不存在")
            return null
        }
        val names = path.split("/")
        var df = DocumentsUtils.getDocumentFile(File(curPath), true, context)
        if (df == null) return null
        for (i in names.indices) {
            var tempDf = df.findFile(names[i])
            if (tempDf == null) {
                if (names[i].contains(".")) {
                    df = df.createFile("", names[i])
                } else {
                    df = df.createDirectory(names[i])
                }
            } else {
                if (names[i].contains(".") && cover) {
                    tempDf.delete()
                    df = df.createFile("", names[i])
                } else {
                    df = tempDf
                }
            }
        }

        return df
    }

    /**
     * 在主路径下查找文件
     * @param path String
     * @param context Context
     */
    @JvmStatic
    fun getFile(path: String, context: Context): DocumentFile? {
        if (!isExist()) {
            Log.d(TAG, "U盘不存在")
            return null
        }
        val names = path.split("/")
        var df = DocumentsUtils.getDocumentFile(File(curPath), true, context)

        for (i in names.indices) {
            df = df.findFile(names[i])
            if (df == null) {
                return null
            }
        }
        return df
    }

    /**
     * 显示获取权限的对话框
     * @param context Activity
     * @param requestCode Int
     */
    @JvmStatic
    fun showOpenDocumentTree(context: Activity, requestCode: Int) {
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm: StorageManager = context.getSystemService(StorageManager::class.java)
            val volume = sm.getStorageVolume(File(curPath))
            if (volume != null) {
                intent = volume.createAccessIntent(null)
            }
        }
        Log.d(TAG, "showOpenDocumentTree intent=\$intent")
        if (intent == null) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        context.startActivityForResult(intent, requestCode)
    }

    /**
     * 保存已经获取权限的路径到系统
     * @param context Context
     * @param path String
     * @param uri Uri
     * @return Boolean
     */
    @JvmStatic
    fun saveTreeUri(context: Context, path: String, uri: Uri): Boolean {
        DocumentsUtils.root = uri.toString()
        return DocumentsUtils.saveTreeUri(context, path, uri)
    }

    /**
     *
     * 复制存储内的文件到u盘
     * @param context Context
     * @param file File 源文件
     * @param targetPath String 目标文件
     * @param onSuccess Function0<Unit>
     * @param onFailed Function1<[@kotlin.ParameterName] String, Unit>
     */
    @JvmStatic
    fun copyStorageToUpan(
        context: Context,
        file: File,
        targetPath: String,
        onSuccess: () -> Unit,
        onFailed: (err: String) -> Unit
    ) {
        Log.d(TAG, "copyStorageToUpan file=$file targetPath=$targetPath")
        if (!isExist()) {
            onFailed("U盘不存在")
            return
        }
        if (!file.exists()) {
            onFailed("文件不存在")
            return
        }
        if (!file.isFile) {
            onFailed("不是一个文件")
            return
        }
        val targetDoc = createFile(targetPath, context, false)
        if (targetDoc == null) {
            onFailed("文件创建失败")
            return
        }
        val sourceIs = FileInputStream(file)
        val targetOs = getOutputStream(targetDoc, context)

        try {
            val copySize = sourceIs.copyTo(targetOs!!)
            targetOs.flush()
            if (copySize != file.length()) {
                onFailed("文件复制失败,并非所有字节都已复制，请重新尝试")
                return
            }
        } catch (e: Exception) {
            onFailed("文件复制失败")
        } finally {
            targetOs?.close()
            sourceIs.close()
        }
        val c = (file.length() / 1024 / 1024 / 10).let { if (it < 5) 5 else it }
        val m = c * 3000
        //如果不等待这个时间，那有几率拷贝下一个文件时会失败,每10M的文件等待3s,最小为15s
        Thread.sleep(m)
        onSuccess()
    }
}
