package com.wl.turbidimetric.view;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lxj.xpopup.core.BubbleAttachPopupView;
import com.lxj.xpopup.util.XPopupUtils;
import com.wl.turbidimetric.R;

/**
 * 自定义气泡Attach弹窗
 */
public class CustomBubbleAttachPopup extends BubbleAttachPopupView {
    private int bgColor = 0xee666666;
    public CustomBubbleAttachPopup(@NonNull Context context) {
        super(context);
    }

    public CustomBubbleAttachPopup(@NonNull Context context, String content) {
        super(context);
        this.content = content;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.custom_bubble_attach_popup;
    }

    TextView tv;
    String content;

    @Override
    protected void onCreate() {
        super.onCreate();
        setBubbleBgColor(bgColor);
        setArrowWidth(XPopupUtils.dp2px(getContext(), 10));
        setArrowHeight(XPopupUtils.dp2px(getContext(), 10));
        setArrowRadius(XPopupUtils.dp2px(getContext(), 3));
        tv = findViewById(R.id.tv);
        setContent(content);
        tv.setOnClickListener(v -> dismiss());
    }

    public void setContent(String content) {
        if (tv != null) {
            tv.setText(content);
        }
    }
}
