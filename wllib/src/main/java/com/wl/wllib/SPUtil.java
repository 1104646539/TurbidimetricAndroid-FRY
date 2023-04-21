package com.wl.wllib;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences 模块
 */
public class SPUtil {
    private static SharedPreferences sharedPreferences;
    private static final String FILE_NAME = "config";
    private static String name = FILE_NAME;

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences(name, context.MODE_PRIVATE);
    }

}
