package com.wl.turbidimetric.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.Item
import com.wl.turbidimetric.model.ItemState
import kotlin.math.abs

class ShelfView :
    View {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initConfig()
    }

    val rectBg by lazy {
        RectF(
            1f,
            1f,
            measuredWidth.toFloat() - 1,
            measuredHeight.toFloat() - 1
        )
    }

    enum class Shape {
        Circle,
        Rectangle
    }

    private val colorBg = resources.getColor(R.color.shelf_bg_color)
    private val colorSelected = resources.getColor(R.color.shelf_selected_frame)

    var itemStates: Array<Item>? = null
        set(value) {
            field = value
            postInvalidate()
        }

    val size = 10
    var shape = Shape.Circle
    val paintBg by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = 1.0f
            style = Paint.Style.STROKE
        }
    }


    val paintItem by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = 1.0f
            style = Paint.Style.FILL

        }
    }
    val paintItemSolid by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = 1.0f
            style = Paint.Style.STROKE

        }
    }
    var curFocIndex = -1
        set(value) {
            if (field != value) {
                if (value < 10) {
                    field = value
                    postInvalidate()
                }
            }
        }

    fun clearFoc() {
        curFocIndex = -1
    }

    private fun initConfig() {
//        shape = Shape.Circle
//        itemStates = arrayOf(
//            Item(SampleState2.None),
//            Item(SampleState2.Exist),
//            Item(SampleState2.NONEXISTENT),
//            Item(SampleState2.ScanSuccess),
//            Item(SampleState2.ScanFailed),
//            Item(SampleState2.Pierced),
//            Item(SampleState2.Squeezing),
//            Item(SampleState2.SamplingFailed),
//            Item(SampleState2.Sampling),
//            Item(SampleState2.None),
//        )
//        shape = Shape.Rectangle
//        itemStates = arrayOf(
//            Item(CuvetteState2.None),
//            Item(CuvetteState2.Skip),
//            Item(CuvetteState2.CuvetteNotEmpty),
//            Item(CuvetteState2.DripSample),
//            Item(CuvetteState2.TakeReagentFailed),
//            Item(CuvetteState2.DripReagent),
//            Item(CuvetteState2.Stir),
//            Item(CuvetteState2.Test1),
//            Item(CuvetteState2.Test2),
//            Item(CuvetteState2.Test4),
//        )
//        curFocIndex = 1

        initItemRect()
    }

    var itemHeight = 0f
    var itemWidth = 0f
    var itemPadding = 6f
    private fun initItemRect() {
        itemWidth = measuredWidth - itemPadding * 2
        itemHeight = (measuredHeight - itemPadding * 11) / 10
        var top = itemPadding
        var y = top
        for (i in itemRects.indices) {
            itemRects[i] = RectF(
                itemPadding, y,
                itemPadding + itemWidth, y + itemHeight
            )
            y += itemHeight + itemPadding
        }
    }


    private var itemRects: Array<RectF?> = arrayOfNulls(size)


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas?.let {

            drawBg(canvas)
            drawItems(canvas)
        }
    }

    private fun drawItems(canvas: Canvas) {
        for (i in 0 until size) {
            if (itemStates?.getOrNull(i) != null) {
                drawItem(canvas, i, itemRects[i], itemStates!![i].state)
            }
        }
    }

    val colorMap = hashMapOf<Int, Int>()
    private fun drawItem(canvas: Canvas, index: Int, rectF: RectF?, itemState: ItemState) {
        rectF?.let { rect ->
            val color = getColorCache(itemState.color)
            paintItem.color = color
            if (shape == Shape.Circle) {
                canvas.drawCircle(rect.centerX(), rect.centerY(), itemWidth / 2, paintItem)


                if (itemState.soildWidth > 0 || curFocIndex == index) {//绘制边框
                    paintItemSolid.strokeWidth = itemState.soildWidth.toFloat()
                    if (curFocIndex == index) {
                        paintItemSolid.strokeWidth = 2f
                        paintItemSolid.color = (colorSelected)
                    } else {
                        paintItemSolid.color = getColorCache(itemState.soildColor)
                    }
                    canvas.drawCircle(rect.centerX(), rect.centerY(), itemWidth / 2, paintItemSolid)
                }

            } else {
                canvas.drawRoundRect(rect, 10f, 10f, paintItem)
                if (itemState.soildWidth > 0 || curFocIndex == index) {//绘制边框
                    paintItemSolid.strokeWidth = itemState.soildWidth.toFloat()
                    if (curFocIndex == index) {
                        paintItemSolid.strokeWidth = 2f
                        paintItemSolid.color = (colorSelected)
                    } else {
                        paintItemSolid.color = getColorCache(itemState.soildColor)
                    }
                    canvas.drawRoundRect(rectF, 10f, 10f, paintItemSolid)
                }
            }
        }
    }

    private fun getColorCache(res: Int): Int {
        var color: Int? = colorMap.get(res)
        if (color == null) {
            color = resources.getColor(res)
            colorMap[res] = color
        }
        return color
    }


    /**
     * 绘制背景
     * @param canvas Canvas
     */
    private fun drawBg(canvas: Canvas) {
        paintBg.color = colorBg
        paintBg.style = Paint.Style.FILL
        paintBg.strokeWidth = 1.0f
        canvas.drawRoundRect(rectBg, 10f, 10f, paintBg)
    }

    var clickX = 0
    var clickY = 0

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
                if (abs(event.x - clickX) > 10) {
                    return super.onTouchEvent(event)
                }
                if (abs(event.y - clickY) > 10) {
                    return super.onTouchEvent(event)
                }
                if (itemRects == null || itemRects.isNullOrEmpty())
                    return super.onTouchEvent(
                        event
                    )
                //判断坐标在哪个icon里
                for (i in itemRects.indices) {
                    val isClick: Boolean =
                        itemRects[i]?.contains(clickX.toFloat(), clickY.toFloat()) ?: false
                    if (isClick) {
                        clickIndex?.invoke(i, itemStates?.get(i))
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)
    }

    var clickIndex: ((Int, Item?) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        initConfig()
    }
}
