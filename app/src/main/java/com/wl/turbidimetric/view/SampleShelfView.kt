package com.wl.turbidimetric.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.wl.turbidimetric.R
import com.wl.turbidimetric.home.HomeViewModel
import com.wl.turbidimetric.model.SampleState
import kotlin.math.abs

/**
 * 首页样本框
 */
class SampleShelfView :
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

    var sampleStates: Array<HomeViewModel.SampleItem>? = null
        set(value) {
            field = value
            initSample()
        }
    val size = 10
    var label: String = "0"
        set(value) {
            field = value
            initText()
        }

    val paintBg by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = 1.0f
            style = Paint.Style.STROKE
        }
    }
    val rectBg by lazy {
        RectF(
            1.0f,
            labelSpace * 2 + labelHeight + 1,
            measuredWidth.toFloat() - 1,
            measuredHeight.toFloat() - 1
        )
    }
    val rectBgFrame by lazy {
        RectF(
            0.0f,
            labelSpace * 2 + labelHeight,
            measuredWidth.toFloat(),
            measuredHeight.toFloat()
        )
    }
    val paintLabel by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = 1.0f
            textSize = 14.0f
        }
    }
    val paintCircle by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = 1.0f
            style = Paint.Style.FILL

        }
    }

    private var labelWidth = 0.0f
    private var labelHeight = 0.0f
    private val labelSpace = 25.0f

    private val circleRadius = 25.0f
    private val circleTopSpace = 20.0f
    private val circleSpace = 31.0f
    private fun initConfig() {
        initText()

//        sampleStates = arrayOf(
//            SampleState.Sampling,
//            SampleState.Pierced,
//            SampleState.ScanSuccess,
//            SampleState.ScanFailed,
//            SampleState.Exist,
//            SampleState.None,
//            SampleState.None,
//            SampleState.None,
//            SampleState.None,
//            SampleState.None,
//        )
        initSample()
    }

    private val colorBgFrame = resources.getColor(R.color.frameColor)
    private val colorBg = resources.getColor(R.color.white)


    private val colorNone = resources.getColor(R.color.circle_none)
    private val colorWait = resources.getColor(R.color.circle_wait_sampling)
    private val colorSampling = resources.getColor(R.color.circle_sampling)
    private val colorFinish = resources.getColor(R.color.circle_sampling_finish)
    private val colorError = resources.getColor(R.color.circle_error)

    private var sampleRects: Array<RectF?> = arrayOfNulls(size)

    private fun initSample() {
        var top = labelSpace * 2 + labelHeight
        var y = top + circleTopSpace
        sampleRects = arrayOfNulls(size)

        sampleStates?.let { sts ->
            repeat(sts.size) {
                sampleRects[it] =
                    RectF(
                        0.0f,
                        y - circleSpace / 2,
                        measuredWidth.toFloat(),
                        y + circleRadius * 2 + circleSpace / 2
                    )
                y += circleSpace + (circleRadius * 2)
            }
        }
        postInvalidate()
    }

    private fun initText() {
        val rectfLabel = Rect()
        paintLabel.getTextBounds(label, 0, label.length, rectfLabel)
        labelWidth = rectfLabel.width().toFloat()
        labelHeight = rectfLabel.height().toFloat()

    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            drawLabel(canvas)
            drawBg(canvas)
            drawCircle(canvas)
        }
    }

    /**
     * 绘制样本
     * @param canvas Canvas
     */
    private fun drawCircle(canvas: Canvas) {
        if (sampleStates == null) return

        var top = labelSpace * 2 + labelHeight
        var x = measuredWidth / 2.0f
        var y = top + circleTopSpace + circleRadius
        sampleStates?.let { sts ->
            repeat(size) {
                paintCircle.color = getSampleColor(sts[it].state)
                paintCircle.style = Paint.Style.FILL
                canvas.drawCircle(x, y, circleRadius, paintCircle)
                if (colorNone == paintCircle.color) {
                    //如果是空的，需要加边框
                    paintCircle.color = colorBgFrame
                    paintCircle.style = Paint.Style.STROKE
                    canvas.drawCircle(x, y, circleRadius, paintCircle)
                }
                y += circleSpace + (circleRadius * 2)
            }
        }
    }

    /**
     * 状态不同，颜色不同
     * @param sampleState SampleState
     * @return Int
     */
    private fun getSampleColor(sampleState: SampleState): Int {
        return when (sampleState) {
            SampleState.Exist -> colorNone
            SampleState.ScanFailed -> colorNone
            SampleState.ScanSuccess -> colorSampling
            SampleState.Pierced -> colorSampling
            SampleState.Squeezing -> colorFinish
            SampleState.Sampling -> colorFinish
            SampleState.SamplingFailed -> colorError
            else -> colorNone
        }
    }

    /**
     * 绘制序号
     * @param canvas Canvas
     */
    private fun drawLabel(canvas: Canvas) {
        canvas.drawText(
            label,
            measuredWidth / 2.0f,
            labelSpace + labelHeight / 2,
            paintLabel
        )
    }

    /**
     * 绘制背景
     * @param canvas Canvas
     */
    private fun drawBg(canvas: Canvas) {
        paintBg.style = Paint.Style.STROKE
        paintBg.color = colorBgFrame
        paintBg.strokeWidth = 1.0f
        canvas.drawRoundRect(rectBgFrame, 5.0f, 5.0f, paintBg)

        paintBg.color = colorBg
        paintBg.style = Paint.Style.FILL
        paintBg.strokeWidth = 1.0f
        canvas.drawRoundRect(rectBg, 5.0f, 5.0f, paintBg)
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
                if (sampleRects == null || sampleRects.isNullOrEmpty()) return super.onTouchEvent(
                    event
                )
                //判断坐标在哪个icon里
                for (i in sampleRects.indices) {
                    val isClick: Boolean =
                        sampleRects[i]?.contains(clickX.toFloat(), clickY.toFloat()) ?: false
                    if (isClick) {
                        clickIndex?.invoke(i, sampleStates?.get(i))
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)
    }

    var clickIndex: ((Int, HomeViewModel.SampleItem?) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height =
            labelSpace * 2 + labelHeight + size * (circleRadius * 2) + ((size - 1) * circleSpace) + circleTopSpace * 2

        setMeasuredDimension(measuredWidth, height.toInt())

//        i("w=$measuredWidth h=$height")
        initConfig()
    }
}
