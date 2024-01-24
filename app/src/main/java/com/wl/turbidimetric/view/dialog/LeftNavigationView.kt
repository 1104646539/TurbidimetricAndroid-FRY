package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import ca.uhn.hl7v2.util.MessageIterator.Index
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.wllib.LogToFile.i

class LeftNavigationView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributeSet) {
    private val colorWhite = getResource().getColor(R.color.white)
    private val colorTextSelected = getResource().getColor(R.color.left_nav_text_selected)
    private val paintBg by lazy {
        Paint().apply {
            color = colorWhite
        }
    }
    private val paintItemBg by lazy {
        Paint().apply {
            color = colorTextSelected
        }
    }
    private val paintText by lazy {
        Paint().apply {
            color = colorWhite
            textSize = this@LeftNavigationView.textSize

        }
    }

    //内部的间隔 左
    private val leftPadding = 36

    //内部的间隔 上 下
    private val topPadding = 32
    private val textSize = 35f
    private var textHeight = 60

    //文字离上面图标的间隔
    private val textMarginTop = 20

    //当前选择的index
    private var curIndex = 0

    //每个item的底部间隔
    private val itemMargin = 8
    private val itemHeight = 186

    private val srcs = mutableListOf<Int>()
    private val selectSrcs = mutableListOf<Int>()
    private val texts = mutableListOf<String>()
    private var bg: Int = 0
    fun setItem(srcs: List<Int>, selectSrcs: List<Int>, texts: List<String>, bg: Int) {
        if (srcs.size != texts.size) throw Exception("item数量不一致")
        this.srcs.clear()
        this.srcs.addAll(srcs)
        this.selectSrcs.clear()
        this.selectSrcs.addAll(selectSrcs)
        this.texts.clear()
        this.texts.addAll(texts)
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

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            srcs.forEachIndexed { index, item ->
                drawItem(canvas, index)
            }
        }
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

    private fun selectIndexChange(index: Int) {
        curIndex = index
        postInvalidate()
        onItemChangeListener?.invoke(index)
    }

    private fun drawItem(canvas: Canvas, index: Int) {
        if (srcs.isEmpty()) return
        drawBg(canvas, index)
        drawIcon(canvas, index)
        drawText(canvas, index)
    }

    private fun drawText(canvas: Canvas, index: Int) {
        paintText.apply {
            if (index == curIndex) {
                color = colorTextSelected
            } else {
                color = colorWhite
            }
        }
        val x = leftPadding.toFloat()
        val y = (itemSrcRect[index].bottom + textHeight / 2 + 10 + textMarginTop).toFloat()
        canvas.drawText(
            texts[index], x, y,
            paintText
        )
    }

    private fun initParams() {
        srcs?.forEachIndexed { index, src ->
            //背景区域
            val tempH =
                if (index == 0) {
                    0
                } else {
                    (itemHeight + itemMargin) * index
                }
            itemRect.add(Rect(0, tempH, measuredWidth, tempH + itemHeight))
            itemBgBitmap = (resources.getDrawable(bg) as BitmapDrawable).bitmap
            itemBgSrcRangeRect =
                Rect(
                    0,
                    0,
                    itemBgBitmap.width,
                    itemBgBitmap.height
                )


            //主图标
            val item = itemRect[index]
            itemBitmap.add((resources.getDrawable(src) as BitmapDrawable).bitmap)
            selectItemBitmap.add((resources.getDrawable(selectSrcs[index]) as BitmapDrawable).bitmap)
            itemSrcRangeRect.add(Rect(0, 0, itemBitmap[index].width, itemBitmap[index].height))
            itemSrcRect.add(
                Rect(
                    item.left + leftPadding,
                    item.top + topPadding,
                    item.left + leftPadding + itemSrcRangeRect[index].width(),
                    item.top + topPadding + itemSrcRangeRect[index].height()
                )
            )
            //文字

        }
    }

    private fun drawIcon(canvas: Canvas, index: Int) {
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

    private fun drawBg(canvas: Canvas, index: Int) {
        if (index == curIndex) {
            canvas.drawRect(itemRect[index], paintBg)
        } else {
            canvas.drawBitmap(
                itemBgBitmap,
                itemBgSrcRangeRect,
                itemRect[index],
                paintBg
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        initParams()
    }
}
typealias OnItemChangeListener = (index: Int) -> Unit

