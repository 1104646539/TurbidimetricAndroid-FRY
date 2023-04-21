package com.wl.wllib;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class AdbUtil {
    private String TAG = "AdbUtil";
    String fileName = "resetUsbHub.sh";
    private File shFile;

    public static AdbUtil getInstance() {
        return AdbUtilHolder.adbUtil;
    }

    private static class AdbUtilHolder {
        static AdbUtil adbUtil = new AdbUtil();
    }


    private AdbUtil() {
        shFile = new File("/mnt/sdcard", fileName);
    }

    /**
     * 重启usb hub
     * su
     * cd /sys/class/gpio
     * echo 64 > export
     * cd gpio64
     * echo out > direction
     * echo 0 > value
     * echo 1 > value
     * cat value
     */
    public void resetUsbHost(Runnable finishRunnable) {
        Log.e(TAG, "resetUsbHost");

        if (!shFileIsExist()) {
            createShFile();
        }

        //方式1：.sh脚本命令
        ThreadUtil.cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                do_exec("/system/bin/sh /mnt/sdcard/resetUsbHub.sh");
            }
        });
//        do_exec("/system/bin/sh /mnt/sdcard/resetUsbHub.sh");
        handler.postDelayed(finishRunnable, 1000 * 20);
    }

    /**
     * 生成脚本文件
     */
    private void createShFile() {
        LogToFile.d(TAG, "shFile=" + shFile.toString());
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(shFile);
            fileOutputStream.write("adb shell su".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("cd /sys/class/gpio".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("echo 64 > export".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("cd gpio64".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("echo out > direction".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("echo 0 > value".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("cat value".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("echo 1 > value".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("cat value".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean shFileIsExist() {
        return shFile.isFile();
    }

    Handler handler = new Handler() {
    };


    String do_exec(String cmd) {
        String s = "/n";
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                s += line + "/n";
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d(TAG, "do_exec e=" + e.toString());
        }
        return cmd;
    }

    public static void doCmds(String cmd) throws Exception {
        Runtime.getRuntime().exec(cmd);

//
//        Process process = Runtime.getRuntime().exec(cmd);
//        DataOutputStream os = new DataOutputStream(process.getOutputStream());
//        os.flush();
//        os.close();
//        process.waitFor();
    }
}
