package com.wl.wllib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.MainThread;

/**
 * Toast模块，全部都提交到主线程
 */
public class ToastUtil {
    private static Toast toast;
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static Context mContext;

    public static void init(Context context) {
        mContext = context;
    }

    public static void showToast(Context context, String msg, boolean isShort) {
        if (context == null) return;
        if (msg == null) return;
        handler.post(() -> {
            if (toast == null) {
                toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            }
            toast.setText(msg);
            toast.setDuration(isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
            toast.show();
        });
    }

    public static void showToast(Context context, String msg) {
        showToast(context, msg, true);
    }

    public static void showToast(String msg) {
        showToast(mContext, msg, true);
    }

    public static void showToastLong(String msg) {
        showToast(mContext, msg, false);
    }
}
