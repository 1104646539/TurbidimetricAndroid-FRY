package com.wl.turbidimetric.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import ca.uhn.hl7v2.model.v231.datatype.ST
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.getResource
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.wllib.LogToFile.i

/**
 * 报告需要的图表
 */
class ReportChartView(
    private val resultAndCurveModel: TestResultAndCurveModel,
    val width: Int,
    val height: Int,
    private val amplificationFactor: Float = 2.0f
) {
    private var bitmap: Bitmap? = null

    private val gradsNum = 6

    //左右的空白
    private val innerHoriPadding = 30 * amplificationFactor

    //中间的图表左右的空白
    private val chartHoriPadding = 110 * amplificationFactor

    //上下的空白
    private val innerVerPadding = 5 * amplificationFactor

    //中间的图表上下的空白
    private val chartVerPadding = 20 * amplificationFactor

    //浓度柱子的宽度
    private val valueWidth = 50 * amplificationFactor

    //右上角示例中 矩形的宽高
    private val hiltRectWidth = 20 * amplificationFactor
    private val hiltRectHeight = 10 * amplificationFactor

    private val innerWidth = width - innerHoriPadding * 2
    private val chartWidth = innerWidth - chartHoriPadding * 2

    private val innerHeight = height - innerVerPadding * 2
    private val chartHeight = innerHeight - chartVerPadding * 2

    private val strokeWidth = 1f * amplificationFactor
    private val textSize = 12f * amplificationFactor
    private val textSizeTitle = 12f * amplificationFactor

    private val grads = mutableListOf(0)

    private val bgColor = getResource().getColor(R.color.white)
    private val frameColor = getResource().getColor(R.color.black)
    private val ljzColor = getResource().getColor(R.color.ljz_red)
    private val valueColor = getResource().getColor(R.color.themeNegativeColor)
    private val textColor = getResource().getColor(R.color.black)
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    init {
        initView()
    }

    private fun initView() {
        initData()
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap!!)
        onDraw(canvas)
    }

    private fun initData() {
        initGrads()
    }

    private fun onDraw(canvas: Canvas) {
        drawFrame(canvas)
        drawInnerFrame(canvas)
        drawChart(canvas)
    }

    private fun drawFrame(canvas: Canvas) {
        canvas.drawRect(Rect(0, 0, width, height), paint.apply {
            color = bgColor
            style = Paint.Style.FILL
        })
    }


    private fun drawChart(canvas: Canvas) {
        val startX = innerHoriPadding + chartHoriPadding * 1f
        val endX = innerHoriPadding + chartHoriPadding + chartWidth * 1f
        val startTop = height - innerVerPadding - chartVerPadding * 1f
        for (i in 0..gradsNum) {
            val top = startTop - (chartHeight / gradsNum * i) * 1f
            //1、x轴
            canvas.drawLine(startX, top, endX, top, paint.apply {
                color = frameColor
                strokeWidth = this@ReportChartView.strokeWidth
                style = Paint.Style.FILL
            })

            //2、y轴浓度
            val textX = startX * 1f
            val textY = top * 1f
            val textW = paint.apply {
                color = textColor
                strokeWidth = this@ReportChartView.strokeWidth
                style = Paint.Style.FILL
                textSize = this@ReportChartView.textSize
            }.measureText("${grads[i]}")
            canvas.drawText("${grads[i]}", textX - textW - 5, textY, paint)

        }
        //3、y轴
        canvas.drawLine(startX, startTop, startX, startTop - chartHeight, paint.apply {
            color = frameColor
            strokeWidth = this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
        })

        //4、临界值
        val ljz = resultAndCurveModel.curve?.projectLjz ?: 100
        val lineH = chartHeight / grads.last().toFloat()
        val ljzY = startTop - (lineH * ljz) * 1f
        canvas.drawLine(startX, ljzY, endX, ljzY, paint.apply {
            color = ljzColor
            strokeWidth = 5f
            style = Paint.Style.FILL
        })

        //5、浓度值柱子
        val con = resultAndCurveModel.result.concentration
        val left = startX + chartWidth / 2 - valueWidth / 2
        val top = startTop
        val bottom = top - (lineH * con)
        val rect = RectF(left, top, left + valueWidth, bottom)

        canvas.drawRect(rect, paint.apply {
            color = valueColor
            strokeWidth = this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
        });

        //6、浓度值
        paint.apply {
            color = textColor
            strokeWidth = this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
            textSize = this@ReportChartView.textSize
        }
        val textRect = getTextRect(paint, "$con");
        canvas.drawText(
            "$con",
            rect.left + rect.width() / 2 - textRect.width() / 2,
            rect.bottom - textRect.height() - innerVerPadding,
            paint.apply {
                color = textColor
                strokeWidth = this@ReportChartView.strokeWidth
                style = Paint.Style.FILL
                textSize = this@ReportChartView.textSizeTitle
            });

        //7、右上角的示例
        //7.1 最外边的框框
        val hiltTextRect = getTextRect(paint.apply {
            color = textColor
            strokeWidth = this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
            textSize = this@ReportChartView.textSize
        }, "123");

        val hiltFrameLeft = endX * 1f
        val hiltFrameSpc = innerVerPadding * 2 * 1f
        val hiltFrameRight = width - innerHoriPadding * 1f
        val hiltFrameTop = innerVerPadding * 2 * 1f
        val hiltFrameBottom = hiltFrameTop + hiltTextRect.height() * 2 + hiltFrameSpc * 3 * 1f
        val hiltFrameRect = RectF(hiltFrameLeft, hiltFrameTop, hiltFrameRight, hiltFrameBottom)

        canvas.drawRect(hiltFrameRect, paint.apply {
            color = frameColor
            strokeWidth = this@ReportChartView.strokeWidth
            style = Paint.Style.STROKE
        })
        //7.2 框里左边的矩形
        val hiltRectLeft = hiltFrameLeft + innerVerPadding
        val hiltRectRight = hiltRectLeft + hiltRectWidth
        val hiltRectTop = hiltFrameTop + hiltFrameSpc
        val hiltRectBottom = hiltRectTop + hiltRectHeight
        val hiltRect1 = RectF(
            hiltRectLeft,
            hiltRectTop,
            hiltRectRight,
            hiltRectBottom
        )
        canvas.drawRect(hiltRect1, paint.apply {
            color = valueColor
            strokeWidth = this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
        })
        val hiltRectTop2 = hiltRectTop + hiltRectHeight + hiltFrameSpc
        val hiltRectBottom2 = hiltRectBottom + hiltFrameSpc + hiltRectHeight
        val hiltRect2 = RectF(
            hiltRectLeft,
            hiltRectTop2,
            hiltRectRight,
            hiltRectBottom2
        )
        canvas.drawRect(hiltRect2, paint.apply {
            color = ljzColor
            strokeWidth = this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
        })
        //7.3 框里右边的提示文字
        canvas.drawText(
            "检测结果",
            hiltRectRight + innerVerPadding,
            hiltRectTop  + hiltFrameSpc,
            paint.apply {
                color = textColor
                strokeWidth = this@ReportChartView.strokeWidth
                style = Paint.Style.FILL
                textSize = this@ReportChartView.textSize
            });
        canvas.drawText(
            "临界值",
            hiltRectRight + innerVerPadding,
            hiltRectTop2  + hiltFrameSpc,
            paint.apply {
                color = textColor
                strokeWidth = this@ReportChartView.strokeWidth
                style = Paint.Style.FILL
                textSize = this@ReportChartView.textSize
            });
    }

    fun getTextRect(textPaint: Paint, str: String): Rect {
        val bounds = Rect()
        textPaint.getTextBounds(str, 0, str.length, bounds)
        return bounds
    }

    private fun initGrads() {
        val ljz = resultAndCurveModel.curve?.projectLjz ?: 100
        val con = resultAndCurveModel.result.concentration

        var temp = ljz
        if (con > ljz) {
            temp = con
        }
        var temp2 = (temp / 10) * 10
        if (temp2 < (temp / 10) * 10) {
            temp2 = ((temp / 10) + 1) * 10
        }
        i("temp=$temp temp2=$temp2")
        grads.clear()
        for (i in 0..gradsNum) {
            grads.add(temp2 / (gradsNum - 1) * i)
        }
    }

    private fun drawInnerFrame(canvas: Canvas) {
        paint.apply {
            color = frameColor
            this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            RectF(
                innerHoriPadding,
                innerVerPadding,
                innerHoriPadding + innerWidth,
                innerVerPadding + innerHeight
            ), paint
        )

        paint.apply {
            color = bgColor
            this@ReportChartView.strokeWidth
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            RectF(
                innerHoriPadding + this@ReportChartView.strokeWidth,
                innerVerPadding + this@ReportChartView.strokeWidth,
                innerHoriPadding + innerWidth - this@ReportChartView.strokeWidth,
                innerVerPadding + innerHeight - this@ReportChartView.strokeWidth
            ), paint
        )
    }

    fun convertToBitmap(): Bitmap? {
        return bitmap;
    }
}
