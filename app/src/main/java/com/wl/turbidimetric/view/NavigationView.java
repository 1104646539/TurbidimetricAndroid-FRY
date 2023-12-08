package com.wl.turbidimetric.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.wl.turbidimetric.R;

import java.util.List;

import com.wl.wllib.LogToFile;
import com.wl.wllib.LogToFile.*;

/**
 * 导航栏
 */
public class NavigationView extends View {
    private final static String TAG = NavigationView.class.getSimpleName();
    private int width;
    private int height;
    private final int padding = 15;
    //logo下方与第一个导航的间隔
    private final int logoSpace = 80;

    //logo资源id
    private int icon_logo_id;
    //关机资源id
    private int icon_shutdown_id;
    //u盘是否有效的资源id
    private int icon_upan_id;
    //移动动画的持续时间
    private final int moveAnimDuration = 100;

    List<NavItem> navItems;
    //当前选中的下标
    public int selectIndex;
    //背景颜色
    private final int colorBg = getResources().getColor(R.color.white);
    //背景颜色
    private final int colorShutdownBg = getResources().getColor(R.color.shutdown_bg);
    //选中的背景颜色
    private final int colorSelectedBg = getResources().getColor(R.color.nav_select_bg);
    //文字颜色
    private final int colorName = getResources().getColor(R.color.textColor);

    //选中的区域
    RectF rectFSelect = new RectF(0, 0, 0, 0);
    //选中的区域的top，随动画更新
    float selectTop = 0;
    //icon的上下边距
    int iconPadding = 14;


    private Path bgPath;
    private Paint paintBg;
    private Paint paintShutdownBg;
    private Paint paintSelectedBg;
    private Paint paintName;

    //关机按钮背景
    private RectF rectShutdownBg;
    //背景的宽、高、圆角大小
    private final int shutdownBgWidth = 156;
    private final int shutdownBgHeight = 128;
    private final int round = 8;
    //按钮的文字高度，宽度
    private final int shutdownNameHeight = 15;
    private float shutdownTextWidth = 0;
    private final String shutdownText = getResources().getString(R.string.nav_shutdown);
    //关机按钮的bitmap,绘制区域等
    private Bitmap bitmapShutdown;
    private RectF rectShutdown;
    private Rect rectShutdownRange;

    int clickX;
    int clickY;

    //导航按钮的bitmap,绘制区域等
    Bitmap[] bitmaps;
    RectF[] rects;
    Rect[] rectBitmapRange;
    //被选中的导航背景区域
    RectF[] rectBgs;

    int navIconWidth = 80;
    int navIconHeight = 80;

    //logo的bitmap,绘制区域等
    Bitmap bitmapLogo;
    RectF rectLogo;
    Rect rectLogoRange;

    //标题的宽度
    private float[] nameWidths;

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
        Parcelable superData = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superData);
        savedState.selectedIndex = selectIndex;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        selectIndex = savedState.selectedIndex;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawSomething(canvas);
    }

    /**
     * 给绘制导航文字留下的高度
     */
    private final int nameHeight = 25;

    /**
     * 绘图所有元素
     *
     * @param canvas
     */
    private void drawSomething(Canvas canvas) {
        if (navItems == null) return;
        if (rects == null) return;
        drawBg(canvas);
        drawSelectedBg(canvas);
        drawShutdown(canvas);
        drawLogo(canvas);
        drawIcon(canvas);
        drawName(canvas);
        drawUpan(canvas);
    }

    private void drawUpan(Canvas canvas) {
        if (upanResIsNull()) return;
        if (rectUpan == null || rectUpan.bottom < 0) {
            initUPanIcon();
        }
        if (rectUpan != null) {
            canvas.drawBitmap(bitmapUpan, rectUpanRange, rectUpan, paintShutdownBg);
        }
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
    }


    public void setResIds(int shutdownId, List<NavItem> navItems, int logoId) {
        this.icon_shutdown_id = shutdownId;
        this.navItems = navItems;
        this.icon_logo_id = logoId;
//        selectIndex = -1;
    }

    public void setUpanResId(int icon_upan_id) {
        if (this.icon_upan_id == icon_upan_id) {
            return;
        }
        this.icon_upan_id = icon_upan_id;
        initUPanIcon();
        invalidate();
    }


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
        float top = rects[selectIndex].top;
        ValueAnimator animator = ValueAnimator.ofFloat(top, rects[index].top);
        animator.setDuration(moveAnimDuration);
        animator.addUpdateListener(animation -> {
            selectTop = (float) animation.getAnimatedValue();
            postInvalidate();
        });
        animator.start();

        selectIndex = index;
        if (navigationSelectedIndexChangeListener != null) {
            navigationSelectedIndexChangeListener.onSelectedIndexChange(selectIndex);
        }
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


    private boolean logoResIsNull() {
        return icon_logo_id == 0;
    }

    private boolean shutdownResIsNull() {
        return icon_shutdown_id == 0;
    }

    private boolean upanResIsNull() {
        return icon_upan_id == 0;
    }


    /**
     * 绘制被选中的背景
     *
     * @param canvas
     */
    private void drawSelectedBg(Canvas canvas) {
        rectFSelect.top = selectTop - iconPadding;
        rectFSelect.bottom = rectFSelect.top + rects[0].height() + iconPadding + iconPadding + nameHeight;
        canvas.drawRect(rectFSelect, paintSelectedBg);
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
        LogToFile.i("onMeasure height=" + height + " width=" + width + "selectIndex=" + selectIndex);
        rectFSelect = new RectF(0, 0, width, 0);

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
        initUPanIcon();//必须要在shutdown后面，因为要知道shutdown的高度，以便保持在shutdown按钮的上方
    }

    RectF rectUpan;
    Rect rectUpanRange;
    Bitmap bitmapUpan;

    /**
     * 初始化u盘状态的icon
     */
    private void initUPanIcon() {
        if (upanResIsNull()) {
            return;
        }
        if (height <= 0 || shutdownResIsNull() || rectShutdown == null) {
            return;
        }
        bitmapUpan = ((BitmapDrawable) getResources().getDrawable(icon_upan_id)).getBitmap();
        rectUpan = new RectF();
        rectUpan.bottom = rectShutdownBg.top - 10;
        rectUpan.top = rectUpan.bottom - bitmapUpan.getHeight();
        rectUpan.left = 40;
        rectUpan.right = rectUpan.left + bitmapUpan.getWidth();

        rectUpanRange = new Rect(0, 0, bitmapUpan.getWidth(), bitmapUpan.getHeight());
    }


    /**
     * 初始化图标的位置，根据图标自身的大小，左右居中
     */
    private void initBitmapRect() {
        if (navItems == null) return;
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
        if (selectIndex <= 0) {
            selectTop = rects[0].top;
        }
    }

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


    /**
     * 初始化绘制导航文字的信息
     */
    private void initNames() {
        if (navItems == null) return;
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
