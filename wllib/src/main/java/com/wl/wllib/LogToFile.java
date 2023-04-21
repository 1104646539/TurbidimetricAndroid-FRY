package com.wl.wllib;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志模块，
 * 会将日志缓存到根目录下的MyLog1.txt和MyLog2.txt和
 * 每个文件默认最大为200M
 */
public class LogToFile extends HandlerThread {
    private static Handler mHandler;
    private static LogToFile mLogToFile;
    private static final String LOG_THREAD = "LOG_THREAD";
    private static final String TAG = "LogToFile";

    private static boolean isDebug = true;

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    private LogToFile(String name) {
        super(name);
    }


    //在application初始化 LogToFile.init();
    public static void init(boolean debug) {
        isDebug = debug;
        if (mLogToFile == null) {
            mLogToFile = new LogToFile(LOG_THREAD);
            mLogToFile.start();
            mHandler = new Handler(mLogToFile.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg != null) {//子线程执行保存文件操作
                        Bundle data = msg.getData();
                        saveLogFile(data.getString("TAG", "tag is null"), data.getString("MESSAGE", "message is null"));
                    }
                }
            };
        }
    }

    //在程序退出时调用
    public static void deInit() {
        if (mLogToFile != null) {
            mHandler.removeCallbacksAndMessages(null);
            mLogToFile.quit();
            mLogToFile = null;
            mHandler = null;
        }
    }

    public static void v(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
        toChildThread(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
        toChildThread(tag, msg);
    }

    public static void d(String msg) {
        if (isDebug) {
            Log.d(TAG, msg);
        }
        toChildThread(TAG, msg);
    }

    public static void i(String tag, String msg) {
//        if (isDebug) {
//            Log.i(tag, msg);
//        }
        toChildThread(tag, msg);
    }

    public static void i(String msg) {
        if (isDebug) {
            Log.i(TAG, msg);
        }
        toChildThread(TAG, msg);
    }


    public static void w(String tag, String msg) {
        if (isDebug) {
            Log.w(tag, msg);
        }
        toChildThread(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
        toChildThread(tag, msg);
    }

    public static void e(String msg) {
        if (isDebug) {
            Log.e(TAG, msg);
        }
        toChildThread(TAG, msg);
    }

    private static void toChildThread(String tag, String message) {

        if (!isDebug) {//release版本不保存
            return;
        }

        if (mHandler != null && !TextUtils.isEmpty(message)) {
            Message obtain = Message.obtain();
            Bundle bundle = new Bundle();
            if (TextUtils.isEmpty(tag)) {
                tag = TAG;
            }
            bundle.putString("TAG", tag);
            bundle.putString("MESSAGE", message);
            obtain.setData(bundle);
            mHandler.sendMessage(obtain);
        }
    }

    private static final int LOG_FILE_SIZE_MAX = 200 * 1024 * 1024;
    private static final String PATH = "/sdcard/";
    private static final String NAME1 = "MyLog.txt";
    private static final String NAME2 = "MyLog2.txt";
    private static SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

    static StringBuilder sb = new StringBuilder();

    private static void saveLogFile(String tag, String message) {//

        if (TextUtils.isEmpty(tag)) {
            tag = "--Tag null--";
        }

        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(PATH, getLogFileName());
            if (file == null || !file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file, true);

//            long size = fileOutputStream.getChannel().size();
//            if (size > LOG_FILE_SIZE_MAX) {//超过20m，删除
//                fileOutputStream.close();
//                file.delete();
//                return;
//            }
            sb.delete(0, sb.length());
            sb.append(DateFormat.format(new Date()));
            sb.append("/");
            sb.append(tag);
            sb.append("=====>>[");
            sb.append(message);
            sb.append("]\n");
            String result = sb.toString();
            fileOutputStream.write(result.getBytes());
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 建立两个日志文件 Log1 Log2 轮流存储日志，当当前的文件存储到一半并且另一个文件满了时候，删除另一个
     *
     * @return
     */
    private static String getLogFileName() throws IOException {
        File file1 = new File(PATH, NAME1);
        File file2 = new File(PATH, NAME2);
        FileOutputStream fileOutputStream1 = null;
        FileOutputStream fileOutputStream2 = null;
        try {
            if (file1 == null || !file1.exists()) {
                file1.getParentFile().mkdirs();
                file1.createNewFile();
            }

            fileOutputStream1 = new FileOutputStream(file1, true);
            long size1 = fileOutputStream1.getChannel().size();

            if (file2 == null || !file2.exists()) {
                file2.getParentFile().mkdirs();
                file2.createNewFile();
            }
            fileOutputStream2 = new FileOutputStream(file2, true);
            long size2 = fileOutputStream2.getChannel().size();


            if (size1 == 0) {//file1 0
                if (size2 == 0 || size2 >= LOG_FILE_SIZE_MAX) {
                    return NAME1;
                } else {
                    return NAME2;
                }
            } else if (size1 > 0) {
                if (size1 >= LOG_FILE_SIZE_MAX) {
                    if ((size2 >= LOG_FILE_SIZE_MAX / 1.2)) {
                        file1.delete();
                        LogToFile.e(TAG, "file1.delete() size1=" + size1 + " size2=" + size2);
                    }
                    return NAME2;
                } else {
                    if ((size1 >= LOG_FILE_SIZE_MAX / 1.2) && (size2 >= LOG_FILE_SIZE_MAX)) {
                        file2.delete();
                        LogToFile.e(TAG, "file2.delete() size1=" + size1 + " size2=" + size2);
                    }
                    return NAME1;
                }
            }
//            if (size > LOG_FILE_SIZE_MAX) {//超过20m，删除
//                fileOutputStream1.close();
//                file1.delete();
//            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LogToFile.e(TAG, "_______________________________________________________FileNotFoundException_______________________________________________________");
        } catch (IOException e) {
            e.printStackTrace();
            LogToFile.e(TAG, "_______________________________________________________IOException_______________________________________________________");
        } finally {
            if (fileOutputStream1 != null) {
                fileOutputStream1.close();
            }
            if (fileOutputStream2 != null) {
                fileOutputStream2.close();
            }
        }
        return NAME1;
    }

    public static void test() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    LogToFile.d(TAG, "_asdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdfjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdljasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdljasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdljasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlljasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlljasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlljasdlasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdlasdfkl;ajsdfl;jasdl;fjkasdlkvmck,.zxjcklisuafodjasklfdjasldfjasdl" +
                            "_______________________________________________________");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
