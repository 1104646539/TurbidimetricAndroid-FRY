package com.wl.wllib;

import static me.jahnen.libaums.core.fs.UsbFileStreamFactory.createBufferedOutputStream;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;


import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.FileSystem;
import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileInputStream;


/**
 * u盘文件操作
 */
public class UsbFlashDiskUtil {
    private final static String TAG = "UsbFlashDiskUtil";
    public static int requestCode = 555;
    private Context context;
    private UsbMassStorageDevice[] usbMassStorageDevices = null;
    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    public static FileSystem fileSystem;

    public static FileSystem getFileSystem() {
        return fileSystem;
    }

    public UsbFlashDiskUtil(Context context, int requestCode, String action) {
        this.context = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        permissionIntent =
                PendingIntent.getBroadcast(context, requestCode, new Intent(action), 0);
    }


    public void getAllUsbDisk() {
        usbMassStorageDevices = UsbMassStorageDevice.getMassStorageDevices(context);
    }

    /**
     * 获取u盘的权限
     *
     * @return
     */
    public void getUsbFilePermission() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < usbMassStorageDevices.length; i++) {
                    UsbDevice usbDevice = usbMassStorageDevices[i].getUsbDevice();
                    usbManager.requestPermission(usbDevice, permissionIntent);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 获取第一个有权限的u盘的第一个盘符的根目录
     *
     * @return
     */
    public UsbFile getFirstUsbFile() {
        UsbFile usbFileFirst = null;
        getAllUsbDisk();
        if (usbMassStorageDevices == null || usbMassStorageDevices.length == 0) {
            return null;
        }
        for (int i = 0; i < usbMassStorageDevices.length; i++) {
            try {
                UsbMassStorageDevice usbMassStorageDevice = usbMassStorageDevices[i];
                UsbDevice usbDevice = usbMassStorageDevices[i].getUsbDevice();
                if (!usbManager.hasPermission(usbDevice)) {
                    Log.d(TAG, "没有获取权限 device=" + usbDevice.toString());
                    continue;
                }
                usbMassStorageDevice.init();

                if (usbMassStorageDevice.partitions == null ||
                        usbMassStorageDevice.partitions.isEmpty()
                ) {
                    continue;
                }
                fileSystem = usbMassStorageDevice.partitions.get(0).getFileSystem();
                usbFileFirst = fileSystem.getRootDirectory();
            } catch (IOException e) {
                usbFileFirst = null;
                Log.d(TAG, "getFirstUsbFile 获取U盘失败" + e.toString());
            }

        }
        return usbFileFirst;
    }

    /**
     * 判断 parent 内是否有 file
     * @param parent 必须是文件夹
     * @param file
     * @return
     */
    public static boolean isExist(UsbFile parent, UsbFile file) {
        if (parent.isDirectory() ) {
            try {
                String[] names = parent.list();
                names = parent.list();
                String name = file.getName();
                for (int i = 0; i < names.length; i++) {
                    if (names[i].equals(name)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * usb设备是否是u盘
     *
     * @param usbDevice
     * @return
     */
    public static boolean isUSBFlashDisk(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return false;
        }
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            if (usbDevice.getInterface(i).getInterfaceClass() == 8) {
                return true;
            }
        }
        return false;
    }


//    /**
//     * 复制文件到u盘
//     *
//     * @param oldPath 必须是文件路径
//     * @param usbFile 必须是文件路径
//     * @throws IOException
//     */
//    public void copyFile(String oldPath, UsbFile usbFile) throws IOException {
//        UsbFileOutputStream fileOutputStream = new UsbFileOutputStream(usbFile);
//        int bytesum = 0;
//        int byteread = 0;
//        File oldfile = new File(oldPath);
//        if (oldfile.exists()) { //文件存在时
//            LogToFile.d(TAG,"copyFile oldfile.exists()");
//            FileInputStream inStream = new FileInputStream(oldPath); //读入原文件
////                inStream.available()
//            UsbFileOutputStream fs = fileOutputStream;
//            byte[] buffer = new byte[1024*2];
//            while ((byteread = inStream.read(buffer)) != -1) {
//                bytesum += byteread; //字节数 文件大小
////                    System.out.println(bytesum);
//                fs.write(buffer, 0, byteread);
//            }
//            fs.flush();
//            inStream.close();
//            fs.close();
//        }else{
//            LogToFile.d(TAG,"copyFile !oldfile.exists()");
//        }
//    }

    /**
     * 复制本地文件到u盘
     *
     * @param context  Application
     * @param filePath 必须是本地文件路径
     * @param usbFile  必须是U盘文件路径
     * @throws IOException
     */
    public static void copyFile(Application context, String filePath, UsbFile usbFile) {
        if (context == null) {
            LogToFile.d(TAG, "copyFile start filePath=" + filePath + " usbFile=" + usbFile.getName() + " context==null");
            return;
        }
        try {
            int chunkSize = UsbFlashDiskUtil.getFileSystem().getChunkSize();
            LogToFile.d(TAG, "copyFile start filePath=" + filePath + " usbFile=" + usbFile.getName() + " chunkSize=" + chunkSize);
            DocumentFile file = DocumentFile.fromFile(new File(filePath));
            long size = file.length();
            usbFile.setLength(size);
            InputStream inputStream = context.getContentResolver().openInputStream(file.getUri());
            OutputStream outputStream = createBufferedOutputStream(usbFile, UsbFlashDiskUtil.getFileSystem());
            byte[] bytes = new byte[chunkSize];
            int count;
            while ((count = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, count);
            }
            outputStream.close();
            inputStream.close();
            LogToFile.d(TAG, "copyFile finish=");
        } catch (IOException ioException) {
            LogToFile.d(TAG, "copyFile ioException=" + ioException.toString());
        }
    }

    /**
     * 复制u盘文件到本地
     *
     * @param newPath 必须是文件路径
     * @param usbFile 必须是文件路径
     * @throws IOException
     */
    public static void copyFile(UsbFile usbFile, String newPath) throws IOException {
        UsbFileInputStream ufi = new UsbFileInputStream(usbFile);
        File newFile = new File(newPath);
        if (newFile.exists()) {
            newFile.delete();
        }
        newFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(newPath);
        int len = 0;
        int chunkSize = UsbFlashDiskUtil.getFileSystem().getChunkSize();
        byte[] bdate = new byte[chunkSize];
        while ((len = ufi.read(bdate)) != -1) {
            fos.write(bdate, 0, len);
        }
        fos.flush();
        fos.close();
        ufi.close();
    }

    /**
     * 在u盘上创建这个目录，如果有就直接返回这个目录
     */
    public static UsbFile createFolder(UsbFile usbFile, String folderPath) throws IOException {
        UsbFile root = usbFile;
        //如果u盘已经存在这个目录就直接复制，如果不存在就新建这个目录
        UsbFile newDir = null;
        UsbFile[] files = root.listFiles();
        for (UsbFile file : files) {
            Log.d(TAG, file.getName());
            if (file.isDirectory()) {
                if (file.getName().equals(folderPath)) {
                    newDir = file;
                }
            }
        }
        if (newDir == null) {
            newDir = root.createDirectory(folderPath);
        }
        return newDir;
    }

    /**
     * 创建文件
     */
    public static UsbFile createFile(UsbFile usbFile, String fileName) throws IOException {
        UsbFile file = usbFile.createFile(fileName);
        return file;
    }

}
