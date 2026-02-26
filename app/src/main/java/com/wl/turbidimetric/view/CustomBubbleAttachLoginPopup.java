package com.wl.turbidimetric.view;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lxj.xpopup.core.BubbleAttachPopupView;
import com.lxj.xpopup.util.XPopupUtils;
import com.wl.turbidimetric.R;

/**
 * 自定义弹窗
 * 登录信息
 */
public class CustomBubbleAttachLoginPopup extends BubbleAttachPopupView {
    private int bgColor = 0xee666666;

    public CustomBubbleAttachLoginPopup(@NonNull Context context) {
        super(context);
    }

    public CustomBubbleAttachLoginPopup(@NonNull Context context, String content, View.OnClickListener listener) {
        super(context);
        this.content = content;
        this.listener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.custom_bubble_attach_login_popup;
    }

    TextView tv;
    Button btnChange;
    String content;
    View.OnClickListener listener;

    @Override
    protected void onCreate() {
        super.onCreate();
        setBubbleBgColor(bgColor);
        setArrowWidth(XPopupUtils.dp2px(getContext(), 10));
        setArrowHeight(XPopupUtils.dp2px(getContext(), 10));
        setArrowRadius(XPopupUtils.dp2px(getContext(), 3));
        tv = findViewById(R.id.tv);
        btnChange = findViewById(R.id.btn_change);
        setContent(content);
        setBtnChange(listener);
        tv.setOnClickListener(v -> dismiss());
    }

    public void setContent(String content) {
        if (tv != null) {
            tv.setText(content);
        }
    }

    public void setBtnChange(View.OnClickListener listener) {
        if (btnChange != null) {
            btnChange.setOnClickListener(v -> {
                dismiss();
                listener.onClick(v);
            });
        }
    }
}
