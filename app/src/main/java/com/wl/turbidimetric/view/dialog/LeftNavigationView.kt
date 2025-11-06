package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.wllib.LogToFile.i

class LeftNavigationView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributeSet) {
    private val colorText = getResource().getColor(R.color.left_nav_text_selected)
    private val colorTextSelected = getResource().getColor(R.color.white)
    private val colorBg = getResource().getColor(R.color.bg_nav_item_gray)
    private val paintBg by lazy {
        Paint().apply {
            color = colorText
        }
    }
    private val paintItemMarin by lazy {
        Paint().apply {
            color = colorTextSelected
            style = Paint.Style.FILL
            strokeWidth = 2f
        }
    }
    private val paintItemBg by lazy {
        Paint().apply {
            color = colorBg
        }
    }
    private val paintText by lazy {
        Paint().apply {
            color = colorText
            textSize = this@LeftNavigationView.textSize

        }
    }

    //内部的间隔 左
    private val leftPadding = 36

    //内部的间隔 上 下
    private val topPadding = 45
    private val textSize = 32f
    private var textHeight = 60

    //文字离上面图标的间隔
    private val textMarginTop = 24

    //当前选择的index
    private var curIndex = 0

    //每个item的底部间隔
    private val itemMargin = 8
    private var itemHeight = 0
    private var itemWidth = 0
    private var bgItemMargin = 4

    private val navItems = mutableListOf<NavItem>()
    private var bg: Int = 0
    fun setItem(navItems: List<NavItem>, bg: Int) {
        this.navItems.clear()
        this.navItems.addAll(navItems)
        this.bg = bg
    }

    init {
        paintText.isAntiAlias = true
        textHeight = paintText.fontSpacing.toInt()
        i("textHeight=$textHeight")
    }

    //背景区域
    private val itemRect = mutableListOf<Rect>()

    //图标区域
    private val itemSrcRect = mutableListOf<Rect>()

    //图标需要绘制区域
    private val itemSrcRangeRect = mutableListOf<Rect>()

    //图标bitmap
    private val itemBitmap = mutableListOf<Bitmap>()

    //图标bitmap
    private val selectItemBitmap = mutableListOf<Bitmap>()


    //背景bitmap
    private lateinit var itemBgBitmap: Bitmap

    //背景需要绘制区域
    private var itemBgSrcRangeRect = Rect()

    var onItemChangeListener: OnItemChangeListener? = null

    //背景和底色的间隔
    var bgMargin: Float = 1f

    override fun onDraw(canvas: Canvas) {
        canvas?.let {
            drawBg(canvas)
            navItems.forEachIndexed { index, item ->
                drawItem(canvas, index)
            }

        }
    }

    private fun drawBg(canvas: Canvas) {
        paintItemBg.color = colorTextSelected
        canvas.drawRoundRect(
            0f,
            0f,
            measuredWidth - 0f * 2,
            measuredHeight - 0f * 2,
            16F,
            16F,
            paintItemBg
        )
        paintItemBg.color = colorBg
        canvas.drawRoundRect(
            bgMargin,
            bgMargin,
            measuredWidth - bgMargin * 2,
            measuredHeight - bgMargin * 2,
            16F,
            16F,
            paintItemBg
        )
    }

    var clickX: Int = 0
    var clickY: Int = 0
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                //获取屏幕上点击的坐标
                clickX = event.x.toInt()
                clickY = event.y.toInt()
                return true
            }

            MotionEvent.ACTION_UP -> {
                //点击抬起后，回复初始位置。
                if (Math.abs(event.x - clickX) > 10) {
                    return super.onTouchEvent(event)
                }
                if (Math.abs(event.y - clickY) > 10) {
                    return super.onTouchEvent(event)
                }
                //判断坐标在哪个icon里
                itemRect.forEachIndexed { index, item ->
                    val isClick: Boolean =
                        item.contains(clickX, clickY)
                    if (isClick) {
                        return if (index == curIndex) {
                            super.onTouchEvent(event)
                        } else {
                            selectIndexChange(index)
                            true
                        }
                    }
                }

                invalidate() //更新视图
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun selectIndexChange(index: Int) {
        if (curIndex == index) return
        curIndex = index
        postInvalidate()
        onItemChangeListener?.invoke(index)
    }

    private fun drawItem(canvas: Canvas, index: Int) {
        if (navItems.isEmpty()) return
        drawItemBg(canvas, index)
        drawItemIcon(canvas, index)
        drawItemText(canvas, index)
    }

    private fun drawItemText(canvas: Canvas, index: Int) {
        paintText.apply {
            if (index == curIndex) {
                color = colorTextSelected
            } else {
                color = colorText
            }
        }
        val textWidth = paintText.measureText(navItems[index].title)
        val x = measuredWidth / 2 - textWidth / 2
        val y = (itemSrcRect[index].bottom + textHeight / 2 + 10 + textMarginTop).toFloat()
        canvas.drawText(
            navItems[index].title, x, y,
            paintText
        )
    }

    private fun initParams() {
        if (itemRect.isNotEmpty()) {
            itemRect.clear()
            itemBitmap.clear()
            selectItemBitmap.clear()
            itemSrcRangeRect.clear()
            itemSrcRect.clear()
        }
        navItems?.forEachIndexed { index, src ->
            //背景区域
            val tempH =
                if (index == 0) {
                    bgItemMargin
                } else {
                    (itemHeight * index) + bgItemMargin
                }
            itemBgBitmap = (resources.getDrawable(bg) as GradientDrawable).toBitmap(
                width = itemWidth,
                height = itemHeight,
            )
            i("tempH=$tempH tempH + itemHeight=${tempH + itemHeight}")
            itemRect.add(Rect(bgItemMargin, tempH, itemWidth + bgItemMargin, tempH + itemHeight))
            itemBgSrcRangeRect =
                Rect(
                    0,
                    0,
                    itemBgBitmap.width,
                    itemBgBitmap.height
                )


            //主图标
            val item = itemRect[index]
            itemBitmap.add((resources.getDrawable(navItems[index].icon) as BitmapDrawable).bitmap)
            selectItemBitmap.add((resources.getDrawable(navItems[index].selectIcon) as BitmapDrawable).bitmap)
            itemSrcRangeRect.add(Rect(0, 0, itemBitmap[index].width, itemBitmap[index].height))
            val left =
                (itemWidth / 2 - itemSrcRangeRect[index].width() / 2 - bgMargin - bgItemMargin).toInt()
            i("left=$left")
            itemSrcRect.add(
                Rect(
                    left,
                    item.top + topPadding,
                    left + itemSrcRangeRect[index].width(),
                    item.top + topPadding + itemSrcRangeRect[index].height()
                )
            )
        }

    }

    private fun drawItemIcon(canvas: Canvas, index: Int) {
        if (index == curIndex) {
            selectItemBitmap[index]
        } else {
            itemBitmap[index]
        }.let {
            canvas.drawBitmap(
                it,
                itemSrcRangeRect[index],
                itemSrcRect[index],
                paintBg
            )
        }

    }

    private fun drawItemBg(canvas: Canvas, index: Int) {
        if (index == curIndex) {
            canvas.drawBitmap(
                itemBgBitmap,
                itemBgSrcRangeRect,
                itemRect[index],
                paintBg
            )
        } else {
            canvas.drawRect(itemRect[index], paintItemBg)
            if (index < itemRect.lastIndex) {
                canvas.drawLine(
                    10f,
                    itemRect[index].bottom.toFloat(),
                    itemRect[index].right.toFloat() - 10 * 2,
                    itemRect[index].bottom.toFloat(),
                    paintItemMarin
                )
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        itemHeight = (measuredHeight - bgItemMargin * 2) / 4
        itemWidth = measuredWidth - bgItemMargin * 2

        initParams()
        i("measuredWidth=$measuredWidth measuredHeight=$measuredHeight itemHeight=$itemHeight itemWidth=$itemWidth")

    }


    data class NavItem(val icon: Int, val selectIcon: Int, val title: String)
}
typealias OnItemChangeListener = (index: Int) -> Unit

