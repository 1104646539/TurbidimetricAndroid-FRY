package com.wl.turbidimetric.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

import com.wl.turbidimetric.R;
import com.wl.wllib.DateExKt;
import com.wl.wllib.DateUtil;
import com.wl.wllib.LogToFile;


/**
 * 标准的对话框
 */
public class DialogUtil {
    private Context context;
    private Dialog dialog;
    private View root;
    private int width = 1200;
    private int height = WindowManager.LayoutParams.WRAP_CONTENT;

    public DialogUtil(Context context) {
        this.context = context;
        dialog = new Dialog(context, R.style.Dialog_NoTitle2);
    }

    public boolean isShowing() {
        if (dialog == null) return false;
        return dialog.isShowing();
    }

    public Dialog getDialog() {
        return dialog;
    }

    public DialogUtil setCancelable(boolean isCancel) {
        dialog.setCancelable(isCancel);
        dialog.setCanceledOnTouchOutside(isCancel);
        return this;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        dialog.setOnDismissListener(listener);
    }

    public void setOnShowListener(DialogInterface.OnShowListener listener) {
        dialog.setOnShowListener(listener);
    }

    public DialogUtil setView(@LayoutRes int layoutId) {
        root = LayoutInflater.from(context).inflate(layoutId, null, false);
        dialog.setContentView(root);
        return this;
    }

    public View getView(@IdRes int viewId) {
        return root.findViewById(viewId);
    }

    public DialogUtil setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public DialogUtil addViewOnClick(@IdRes int viewId, View.OnClickListener onClickListener) {
        View view = root.findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(onClickListener);
        }
        return this;
    }

    public View getRootView() {
        return root;
    }

    public DialogUtil show() {
        return show(width, height);
    }

    public DialogUtil show(int width, int height) {
        dialog.show();
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = width;
        params.height = height;
        dialog.getWindow().setAttributes(params);
//        dialog.setCanceledOnTouchOutside(false);
//        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return this;
    }

    public void dismiss() {
        dialog.dismiss();
    }

}
