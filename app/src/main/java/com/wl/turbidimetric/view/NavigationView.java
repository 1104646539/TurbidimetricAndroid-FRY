package com.wl.turbidimetric.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.wl.turbidimetric.R;


public class NavigationView extends View {
    private final static String TAG = NavigationView.class.getSimpleName();
    private int width;
    private int height;
    private int padding = 15;

    //导航资源id
    private int resIds[];
    //logo资源id
    private int icon_logo_id;

    //当前选中的下标
    public int selectIndex = 0;
    //背景颜色
    private int colorBg = getResources().getColor(R.color.white);
    //选中的背景颜色
    private int colorSelectedBg = getResources().getColor(R.color.nav_select_bg);

    private float iconRadius = 10;
    private Path bgPath;
    private Path selectedPath;
    private Paint paintBg;
    private Paint paintSelectedBg;

    public NavigationView(Context context) {
        this(context, null);
    }

    public NavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawSomething(canvas);
    }

    //绘图逻辑
    private void drawSomething(Canvas canvas) {
        //绘图
        drawBg(canvas);
        drawSelectedBg(canvas);
        drawIcon(canvas);
    }

    /**
     * 初始化View
     */
    private void initView() {
        setFocusable(true);
        setKeepScreenOn(true);
        setFocusableInTouchMode(true);
        bgPath = new Path();
        selectedPath = new Path();
    }


    public void setResIds(int logoId, int[] resIds) {
        this.resIds = resIds;
        this.icon_logo_id = logoId;
        selectIndex = -1;
        initBitmapRect();
    }

    RectF[] rects;
    Bitmap[] bitmaps;
    Bitmap logoBitmap;
    Rect[] rectBitmapRange;

    /**
     * 初始化图标的位置，根据图标自身的大小，只固定左边距
     */
    private void initBitmapRect() {
        if (resIsNull()) {
            return;
        }
        rects = new RectF[resIds.length];
        bitmaps = new Bitmap[resIds.length];
        rectBitmapRange = new Rect[resIds.length];
        logoBitmap = ((BitmapDrawable) getResources().getDrawable(icon_logo_id)).getBitmap();
        float top = padding * 2 + logoBitmap.getHeight();
        for (int i = 0; i < resIds.length; i++) {
            bitmaps[i] = ((BitmapDrawable) getResources().getDrawable(resIds[i])).getBitmap();
            rects[i] = new RectF();
//            rects[i].left = padding;
//            if (i == 0) {
//                top = padding * 4;
//            }
//            rects[i].top = top;
//            rects[i].right = rects[i].left + bitmaps[i].getWidth();
//            rects[i].bottom = rects[i].top + bitmaps[i].getHeight();
//            top = rects[i].bottom + padding * 2;
            int bW = bitmaps[i].getWidth();
            int bH = bitmaps[i].getHeight();
            rects[i].left = (width - bW) / 2;
            rects[i].right = rects[i].left + bW;
            rects[i].top = top;
            rects[i].bottom = rects[i].top + bH;

            top += rects[i].bottom + padding * 2;

            rectBitmapRange[i] = new Rect(0, 0, bitmaps[i].getWidth(), bitmaps[i].getHeight());
        }

        selectIndex = 0;
    }

    int clickX;
    int clickY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                //获取屏幕上点击的坐标
                clickX = (int) event.getX();
                clickY = (int) event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                //点击抬起后，回复初始位置。
                if (Math.abs(event.getX() - clickX) > 10) {
                    return super.onTouchEvent(event);
                }
                if (Math.abs(event.getY() - clickY) > 10) {
                    return super.onTouchEvent(event);
                }
                if (resIsNull()) {
                    return super.onTouchEvent(event);
                }
                //判断坐标在哪个icon里
                for (int i = 0; i < rects.length; i++) {
                    boolean isClick = rects[i].contains(clickX, clickY);
                    if (isClick) {
                        if (i == selectIndex) {
                            return super.onTouchEvent(event);
                        } else {
                            selectIndexChange(i);
                        }
                    }
                }

                invalidate();//更新视图
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void selectIndexChange(int index) {
        selectIndex = index;
        if (rightNavigationSelectedIndexChangeListener != null) {
            rightNavigationSelectedIndexChangeListener.onSelectedIndexChange(selectIndex);
        }
        postInvalidate();
    }

    RightNavigationSelectedIndexChangeListener rightNavigationSelectedIndexChangeListener;

    public void setRightNavigationSelectedIndexChangeListener(RightNavigationSelectedIndexChangeListener rightNavigationSelectedIndexChangeListener) {
        this.rightNavigationSelectedIndexChangeListener = rightNavigationSelectedIndexChangeListener;
    }

    public interface RightNavigationSelectedIndexChangeListener {
        void onSelectedIndexChange(int index);
    }

    private void init(Context context, AttributeSet attrs) {
        paintBg = new Paint();
        paintBg.setAntiAlias(true);
        paintBg.setColor(colorBg);
        paintSelectedBg = new Paint();
        paintSelectedBg.setAntiAlias(true);
        paintSelectedBg.setColor(colorSelectedBg);

        initView();
    }


    private void drawIcon(Canvas canvas) {
        if (resIsNull()) {
            return;
        }
        for (int i = 0; i < rects.length; i++) {
            canvas.drawBitmap(bitmaps[i], rectBitmapRange[i],
                    rects[i], null);
        }
    }

    private boolean resIsNull() {
        return resIds == null || resIds.length == 0;
    }

    private void drawSelectedBg(Canvas canvas) {
        if (resIsNull()) {
            return;
        }
//        int iconPadding = 6;
//        selectedPath.reset();
//        selectedPath.moveTo(0, rects[selectIndex].top - iconPadding - iconRadius);
//        selectedPath.quadTo(0, rects[selectIndex].top - iconPadding, iconRadius, rects[selectIndex].top - iconPadding);
//
//        selectedPath.lineTo(rects[selectIndex].right + iconPadding - iconRadius, rects[selectIndex].top - iconPadding);
//        selectedPath.quadTo(rects[selectIndex].right + iconPadding, rects[selectIndex].top - iconPadding, rects[selectIndex].right + iconPadding, rects[selectIndex].top - iconPadding + iconRadius);
//
//        selectedPath.lineTo(rects[selectIndex].right + iconPadding, rects[selectIndex].bottom + iconPadding - iconRadius);
//        selectedPath.quadTo(rects[selectIndex].right + iconPadding, rects[selectIndex].bottom + iconPadding, rects[selectIndex].right + iconPadding - iconRadius, rects[selectIndex].bottom + iconPadding);
//
//        selectedPath.lineTo(0 + iconRadius, rects[selectIndex].bottom + iconPadding);
//        selectedPath.quadTo(0, rects[selectIndex].bottom + iconPadding, 0, rects[selectIndex].bottom + iconPadding + iconRadius);
//
//        selectedPath.close();

        int iconPadding = 6;
        selectedPath.reset();
        selectedPath.moveTo(0, rects[selectIndex].top - iconPadding - iconRadius);

        selectedPath.lineTo(width, rects[selectIndex].top - iconPadding);

        selectedPath.lineTo(width, rects[selectIndex].bottom + iconPadding - iconRadius);

        selectedPath.lineTo(0 + iconRadius, rects[selectIndex].bottom + iconPadding);

        selectedPath.close();


        canvas.drawPath(selectedPath, paintSelectedBg);
    }


    private void drawBg(Canvas canvas) {
        bgPath.reset();
        bgPath.lineTo(height, 0);
        bgPath.lineTo(height, height);
        bgPath.lineTo(0, height);
        bgPath.close();
        canvas.drawPath(bgPath, paintBg);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = getMeasuredWidth();

        height = getMeasuredHeight();
        setMeasuredDimension(width, height);
        Log.d(TAG, "onMeasure height=" + height + " width=" + width);
    }
}
