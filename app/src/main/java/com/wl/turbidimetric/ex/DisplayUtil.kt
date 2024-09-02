package com.wl.turbidimetric.ex

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.WindowManager


object DisplayUtil {
    @JvmStatic
    fun pxToDp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
    @JvmStatic
    fun dpToPx(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
    @JvmStatic
    fun pxToSp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }
    @JvmStatic
    fun spToPx(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }
    @JvmStatic
    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
    @JvmStatic
    fun spToPx(context: Context, sp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
    @JvmStatic
    fun screenWidth(context: Context): Int {
        return (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.width
    }
    @JvmStatic
    fun screenHeight(context: Context): Int {
        return (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.height
    }
    @JvmStatic
    fun getViewHeight(views: View): Int {
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        views.measure(spec, spec)
        val measuredWidthTicketNum = views.measuredHeight
        return measuredWidthTicketNum
    }
}
