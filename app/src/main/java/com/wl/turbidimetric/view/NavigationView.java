package com.wl.turbidimetric.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.wl.turbidimetric.R;

import java.util.List;

import timber.log.Timber;

/**
 * 导航栏
 */
public class NavigationView extends View {
    private final static String TAG = NavigationView.class.getSimpleName();
    private int width;
    private int height;
    private int padding = 15;
    //logo下方与第一个导航的间隔
    private int logoSpace = 80;

    //    //导航资源id
//    private int resIds[];
//    //导航名
//    private String names[];
    //logo资源id
    private int icon_logo_id;
    //关机资源id
    private int icon_shutdown_id;


    List<NavItem> navItems;
    //当前选中的下标
    public int selectIndex ;
    //背景颜色
    private int colorBg = getResources().getColor(R.color.white);
    //背景颜色
    private int colorShutdownBg = getResources().getColor(R.color.shutdown_bg);
    //选中的背景颜色
    private int colorSelectedBg = getResources().getColor(R.color.nav_select_bg);
    //文字颜色
    private int colorName = getResources().getColor(R.color.textColor);

    private Path bgPath;
    private Path selectedPath;
    private Paint paintBg;
    private Paint paintShutdownBg;
    private Paint paintSelectedBg;
    private Paint paintName;

    public NavigationView(Context context) {
        this(context, null);
    }

    public NavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Timber.d("onSaveInstanceState selectIndex=" + selectIndex);
        Parcelable superData = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superData);
        savedState.selectedIndex = selectIndex;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        Timber.d("onRestoreInstanceState selectIndex=" + selectIndex);
        super.onRestoreInstanceState(savedState.getSuperState());
        selectIndex = savedState.selectedIndex;
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        super.onScreenStateChanged(screenState);
        Timber.d("onScreenStateChanged selectIndex=" + selectIndex);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Timber.d("onConfigurationChanged selectIndex=" + selectIndex + " newConfig=" + newConfig);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawSomething(canvas);
    }

    /**
     * 给绘制导航文字留下的高度
     */
    private int nameHeight = 25;

    /**
     * 绘图所有元素
     *
     * @param canvas
     */
    private void drawSomething(Canvas canvas) {
        drawBg(canvas);
        drawSelectedBg(canvas);
        drawShutdown(canvas);
        drawLogo(canvas);
        drawIcon(canvas);
        drawName(canvas);
    }

    /**
     * 绘制关机按钮
     *
     * @param canvas
     */
    private void drawShutdown(Canvas canvas) {
        if (shutdownResIsNull()) return;
        canvas.drawRoundRect(rectShutdownBg, round, round, paintShutdownBg);
        canvas.drawBitmap(bitmapShutdown, rectShutdownRange, rectShutdown, paintShutdownBg);
        canvas.drawText(shutdownText, width / 2 - shutdownTextWidth / 2, height - padding - 20, paintName);
    }

    /**
     * 绘制logo
     *
     * @param canvas
     */
    private void drawLogo(Canvas canvas) {
        if (logoResIsNull()) return;
        canvas.drawBitmap(bitmapLogo, rectLogoRange, rectLogo, paintSelectedBg);
    }

    /**
     * 绘制导航标题
     *
     * @param canvas
     */
    private void drawName(Canvas canvas) {

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


    public void setResIds(int shutdownId, List<NavItem> navItems, int logoId) {
        this.icon_shutdown_id = shutdownId;
        this.navItems = navItems;
        this.icon_logo_id = logoId;
//        selectIndex = -1;
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
//                if (navResIsNull()) {
//                    return super.onTouchEvent(event);
//                }
                //判断坐标在哪个icon里
                for (int i = 0; i < rectBgs.length; i++) {
                    boolean isClick = rectBgs[i].contains(clickX, clickY);
                    if (isClick) {
                        if (i == selectIndex) {
                            return super.onTouchEvent(event);
                        } else {
                            selectIndexChange(i);
                            return true;
                        }
                    }
                }
                //判断是否点击关机
                boolean clickShutdown = rectShutdownBg.contains(clickX, clickY);
                if (clickShutdown) {
                    navigationShutdownListener.onShutdown();
                }
                invalidate();//更新视图
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void selectIndexChange(int index) {
        selectIndex = index;
        if (navigationSelectedIndexChangeListener != null) {
            navigationSelectedIndexChangeListener.onSelectedIndexChange(selectIndex);
        }
        postInvalidate();
    }

    NavigationSelectedIndexChangeListener navigationSelectedIndexChangeListener;

    public void setNavigationSelectedIndexChangeListener(NavigationSelectedIndexChangeListener NavigationSelectedIndexChangeListener) {
        this.navigationSelectedIndexChangeListener = NavigationSelectedIndexChangeListener;
    }

    public interface NavigationSelectedIndexChangeListener {

        void onSelectedIndexChange(int index);
    }

    NavigationShutdownListener navigationShutdownListener;

    public void setNavigationShutdownListener(NavigationShutdownListener navigationShutdownListener) {
        this.navigationShutdownListener = navigationShutdownListener;
    }

    public interface NavigationShutdownListener {
        void onShutdown();
    }

    /**
     * 初始化画笔等
     */
    private void init() {
        paintBg = new Paint();
        paintBg.setAntiAlias(true);
        paintBg.setColor(colorBg);
        paintShutdownBg = new Paint();
        paintShutdownBg.setAntiAlias(true);
        paintShutdownBg.setColor(colorShutdownBg);
        paintSelectedBg = new Paint();
        paintSelectedBg.setAntiAlias(true);
        paintSelectedBg.setColor(colorSelectedBg);
        paintName = new Paint();
        paintName.setAntiAlias(true);
        paintName.setTextSize(getResources().getDimension(R.dimen.nav_name_size));
        paintName.setColor(colorName);

        initView();
    }

    /**
     * 绘制导航的图标
     *
     * @param canvas
     */
    private void drawIcon(Canvas canvas) {
        for (int i = 0; i < rects.length; i++) {
            canvas.drawBitmap(bitmaps[i], rectBitmapRange[i],
                    rects[i], null);
        }
        for (int i = 0; i < navItems.size(); i++) {
            canvas.drawText(navItems.get(i).navName, rects[i].left + (rects[i].width() - nameWidths[i]) / 2, rects[i].bottom + nameHeight, paintName);
        }
    }

//    private boolean navResIsNull() {
//        return resIds == null || resIds.length == 0;
//    }

    private boolean logoResIsNull() {
        return icon_logo_id == 0;
    }

    private boolean shutdownResIsNull() {
        return icon_shutdown_id == 0;
    }

//    private boolean namesIsNull() {
//        return names == null || names.length == 0;
//    }

    /**
     * 绘制被选中的背景
     *
     * @param canvas
     */
    private void drawSelectedBg(Canvas canvas) {
        int iconPadding = 14;
        selectedPath.reset();
        selectedPath.moveTo(0, rects[selectIndex].top - iconPadding);

        selectedPath.lineTo(width, rects[selectIndex].top - iconPadding);

        selectedPath.lineTo(width, rects[selectIndex].bottom + iconPadding + nameHeight);

        selectedPath.lineTo(0, rects[selectIndex].bottom + iconPadding + nameHeight);

        selectedPath.close();

        canvas.drawPath(selectedPath, paintSelectedBg);
    }

    /**
     * 绘制整个控件的背景
     *
     * @param canvas
     */
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
        Timber.d("onMeasure height=" + height + " width=" + width + "selectIndex=" + selectIndex);

        initRect();
    }

    /**
     * 初始化所有绘制前需要的参数
     */
    private void initRect() {
        initNames();
        initLogo();
        initBitmapRect();//必须要在logo后面，因为要知道logo的高度
        initShutdown();
    }

    //导航按钮的bitmap,绘制区域等
    Bitmap[] bitmaps;
    RectF[] rects;
    Rect[] rectBitmapRange;
    //被选中的导航背景区域
    RectF[] rectBgs;

    int navIconWidth = 80;
    int navIconHeight = 80;

    /**
     * 初始化图标的位置，根据图标自身的大小，左右居中
     */
    private void initBitmapRect() {
        rects = new RectF[navItems.size()];
        bitmaps = new Bitmap[navItems.size()];
        rectBitmapRange = new Rect[navItems.size()];
        rectBgs = new RectF[navItems.size()];
        float top = padding * 2 + (logoResIsNull() ? 0 : bitmapLogo.getHeight()) + logoSpace;
        for (int i = 0; i < navItems.size(); i++) {
            bitmaps[i] = ((BitmapDrawable) getResources().getDrawable(navItems.get(i).navIcon)).getBitmap();
            rects[i] = new RectF();
            rects[i].left = (width - navIconWidth) / 2;
            rects[i].right = rects[i].left + navIconWidth;
            rects[i].top = top;
            rects[i].bottom = rects[i].top + navIconHeight;

            rectBgs[i] = new RectF(0, rects[i].top, width, rects[i].bottom + nameHeight + padding);

            top = rects[i].bottom + padding * 2 + nameHeight + padding;

            rectBitmapRange[i] = new Rect(0, 0, bitmaps[i].getWidth(), bitmaps[i].getHeight());
        }

//        selectIndex = 0;
    }

    //关机按钮背景
    RectF rectShutdownBg;
    //背景的宽、高、圆角大小
    int shutdownBgWidth = 156;
    int shutdownBgHeight = 128;
    int round = 8;
    //按钮的文字高度，宽度
    int shutdownNameHeight = 15;
    float shutdownTextWidth = 0;
    String shutdownText = getResources().getString(R.string.nav_shutdown);
    //关机按钮的bitmap,绘制区域等
    Bitmap bitmapShutdown;
    RectF rectShutdown;
    Rect rectShutdownRange;

    /**
     * 初始化绘制关机按钮需要的信息
     */
    private void initShutdown() {
        if (shutdownResIsNull()) {
            return;
        }
        shutdownTextWidth = paintName.measureText(shutdownText, 0, shutdownText.length());

        bitmapShutdown = ((BitmapDrawable) getResources().getDrawable(icon_shutdown_id)).getBitmap();
        rectShutdown = new RectF();
        rectShutdown.left = (width - bitmapShutdown.getWidth()) / 2;
        rectShutdown.right = rectShutdown.left + bitmapShutdown.getWidth();
        rectShutdown.top = height - shutdownBgHeight / 2 - bitmapShutdown.getHeight() / 2 - padding - shutdownNameHeight;
        rectShutdown.bottom = rectShutdown.top + bitmapShutdown.getHeight();
        rectShutdownRange = new Rect(0, 0, bitmapShutdown.getWidth(), bitmapShutdown.getHeight());

        rectShutdownBg = new RectF();
        rectShutdownBg.left = (width - shutdownBgWidth) / 2;
        rectShutdownBg.right = rectShutdownBg.left + shutdownBgWidth;
        rectShutdownBg.top = height - shutdownBgHeight - padding;
        rectShutdownBg.bottom = rectShutdownBg.top + shutdownBgHeight;
    }

    //logo的bitmap,绘制区域等
    Bitmap bitmapLogo;
    RectF rectLogo;
    Rect rectLogoRange;

    /**
     * 初始化绘制logo需要的图片等信息
     */
    private void initLogo() {
        if (logoResIsNull()) {
            return;
        }
        bitmapLogo = ((BitmapDrawable) getResources().getDrawable(icon_logo_id)).getBitmap();
        rectLogo = new RectF();
        rectLogo.left = (width - bitmapLogo.getWidth()) / 2;
        rectLogo.right = rectLogo.left + bitmapLogo.getWidth();
        rectLogo.top = padding * 2;
        rectLogo.bottom = rectLogo.top + bitmapLogo.getHeight();
        rectLogoRange = new Rect(0, 0, bitmapLogo.getWidth(), bitmapLogo.getHeight());
    }

    private float[] nameWidths;

    /**
     * 初始化绘制导航文字的信息
     */
    private void initNames() {
        nameWidths = new float[navItems.size()];
        for (int i = 0; i < navItems.size(); i++) {
            nameWidths[i] = paintName.measureText(navItems.get(i).navName);
        }
    }

    public static class NavItem {
        int navIcon;
        String navName;

        public NavItem(int navIcon, String navName) {
            this.navIcon = navIcon;
            this.navName = navName;
        }

        public int getNavIcon() {
            return navIcon;
        }

        public void setNavIcon(int navIcon) {
            this.navIcon = navIcon;
        }

        public String getNavName() {
            return navName;
        }

        public void setNavName(String navName) {
            this.navName = navName;
        }
    }

    static class SavedState extends BaseSavedState {
        int selectedIndex;

        public SavedState(Parcel source) {
            super(source);
            selectedIndex = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(selectedIndex);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
