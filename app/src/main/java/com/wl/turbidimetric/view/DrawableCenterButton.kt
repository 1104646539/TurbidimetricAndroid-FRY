package com.wl.turbidimetric.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

open class DrawableCenterButton(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatButton(context, attrs) {

    override fun onDraw(canvas: Canvas) {
        var canvas = canvas
        canvas = getTopCanvas(canvas)
        super.onDraw(canvas)
    }

    open fun getTopCanvas(canvas: Canvas): Canvas {
        val drawables = compoundDrawables ?: return canvas
        var drawable = drawables[0] // 左面的drawable
        if (drawable == null) {
            drawable = drawables[2] // 右面的drawable
        }

        // float textSize = getPaint().getTextSize(); // 使用这个会导致文字竖向排下来
        val textSize = paint.measureText(text.toString())
        val drawWidth = drawable.intrinsicWidth
        val drawPadding = compoundDrawablePadding
        val contentWidth = textSize + drawWidth + drawPadding
        val leftPadding = (width - contentWidth).toInt()
        setPadding(0, 0, leftPadding, 0) // 直接贴到左边
        val dx = (width - contentWidth) / 2
        canvas.translate(dx, 0f) // 往右移动
        return canvas
    }
//    @Override
//    override fun onDraw(canvas: Canvas) {
//
//        val drawables = getCompoundDrawables();
//
//        if (drawables != null) {
//
//            val drawableLeft = drawables[0];
//
//            if (drawableLeft != null) {
//
//                val textWidth = getPaint().measureText(getText().toString());
//
//                val drawablePadding = getCompoundDrawablePadding();
//
//                val drawableWidth = drawableLeft.getIntrinsicWidth();
//
//                val bodyWidth = textWidth + drawableWidth + drawablePadding;
//
//                canvas.translate((getWidth() - bodyWidth) / 2, 0F);
//
//            }
//
//        }
//
//        super.onDraw(canvas);
//
//    }

}
