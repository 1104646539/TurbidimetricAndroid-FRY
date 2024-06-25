package com.wl.turbidimetric.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.RemoteException
import com.dynamixsoftware.printingsdk.IPage
import com.dynamixsoftware.printingsdk.IPrintListener
import com.dynamixsoftware.printingsdk.Result
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.wllib.LogToFile.i
import java.io.File

object PrintHelper {

    private val queue = WorkQueue<PrintReport>(35000L)
    var context: Context? = null
    var onSizeChange: ((num: Int) -> Unit)? = null
    var size = queue.queue.size
        private set(value) {
            field = value
            onSizeChange?.invoke(value)
        }

    init {
        queue.onWorkStart = { result ->
            printReport(result.result, result.hospitalName, result.barcode)
            size = queue.queue.size
        }
    }

    /**
     * 单个结果打印，用于检测完成后的自动打印
     */
    fun printReport(
        result: TestResultAndCurveModel,
        hospitalName: String,
        barcode: Boolean
    ) {
        val ret = ExportReportHelper.create(result, hospitalName, barcode)
        if (ret != null) {
            print(ret)
        }
    }

    @JvmStatic
    fun addPrintWork(
        result: TestResultAndCurveModel,
        hospitalName: String,
        barcode: Boolean
    ) {
        queue.addWork(PrintReport(result, hospitalName, barcode))
        size = queue.queue.size
    }

    @JvmStatic
    fun addPrintWork(
        result: List<TestResultAndCurveModel>,
        hospitalName: String,
        barcode: Boolean
    ) {
        result?.forEach {
            addPrintWork(it, hospitalName, barcode)
        }
    }

    @JvmStatic
    private fun print(path: String) {
        try {
            PrintSDKHelper.printImage(createPages(path), object : IPrintListener.Stub() {
                override fun startingPrintJob() {
                }

                override fun start() {
                }

                override fun sendingPage(arg0: Int, arg1: Int) {

                }

                override fun preparePage(arg0: Int) {

                }

                override fun needCancel(): Boolean {


                    return false
                }

                override fun finishingPrintJob() {

                }

                override fun finish(arg0: Result, arg1: Int, arg2: Int) {
                    i("printImage finish arg0=$arg0 arg1=$arg1 arg2=$arg2")
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun createPages(path: String): List<IPage> {
        if (PrintSDKHelper.getCurPrinter() != null) {
            val hResolution = PrintSDKHelper.getCurPrinter()!!.context.hResolution
            val vResolution = PrintSDKHelper.getCurPrinter()!!.context.vResolution
            val imageArea = PrintSDKHelper.getCurPrinter()!!.context.imageArea
            val paperHeight = PrintSDKHelper.getCurPrinter()!!.context.paperHeight
            val paperWidth = PrintSDKHelper.getCurPrinter()!!.context.paperWidth
            i("getBitmapFragment hResolution=$hResolution vResolution=$vResolution imageArea=$imageArea paperHeight=$paperHeight paperWidth=$paperWidth")

            val pages: MutableList<IPage> = ArrayList()
            pages.add(IPage { fragment ->
//            val height = 11788
                val height = vResolution.toFloat() / 72 * paperHeight
                val fWidth = hResolution.toFloat() / 72 * paperWidth
                val fHeight = fragment.height()
                i("getBitmapFragment: fWidth=$fWidth fHeight=$fHeight height=$height")
                val bitmap =
                    Bitmap.createBitmap(fWidth.toInt(), fHeight.toInt(), Bitmap.Config.ARGB_8888)
                val imageBMP: Bitmap = FilesUtils.pdfToBitmaps(
                    context,
                    File(path)
                )[0]
                val p = Paint()
                p.isAntiAlias = true
                p.isDither = true
                var imageWidth = 0
                var imageHeight = 0
                if (imageBMP != null) {
                    imageWidth = imageBMP.width
                    imageHeight = imageBMP.height
                }
                val aspectH = (height / imageHeight).toFloat()
                val dst = RectF(0f, 0f, fWidth.toFloat(), height.toFloat())
                i("getBitmapFragment:  aspectH=$aspectH")
                i(
                    "getBitmapFragment: imageWidth=$imageWidth imageHeight=$imageHeight"
                )
                val sLeft = 0f
                val sTop = fragment.top / aspectH
                val sRight = imageWidth.toFloat()
                val sBottom = imageHeight.toFloat()
                val source = RectF(sLeft, sTop, sRight, sBottom)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                // move image to actual printing area
                i("getBitmapFragment: source=$source dst=$dst")
                dst.offsetTo(0f, 0f)
                val matrix = Matrix()
                matrix.setRectToRect(source, dst, Matrix.ScaleToFit.START)
                canvas.drawBitmap(imageBMP!!, matrix, p)
                bitmap
            })

            return pages
        }
        return mutableListOf()
    }
// 7180 正常打印
//    private fun createPages(path: String): List<IPage> {
//        if(PrintSDKHelper.getCurPrinter()!=null){
//            val hResolution = PrintSDKHelper.getCurPrinter()!!.context.hResolution
//            val vResolution = PrintSDKHelper.getCurPrinter()!!.context.vResolution
//            val imageArea = PrintSDKHelper.getCurPrinter()!!.context.imageArea
//            val paperHeight = PrintSDKHelper.getCurPrinter()!!.context.paperHeight
//            val paperWidth = PrintSDKHelper.getCurPrinter()!!.context.paperWidth
//            i("getBitmapFragment hResolution=$hResolution vResolution=$vResolution imageArea=$imageArea paperHeight=$paperHeight paperWidth=$paperWidth")
//        }
//        val pages: MutableList<IPage> = ArrayList()
//        pages.add(IPage { fragment ->
//            val height = 3392
//            val fWidth = fragment.width()
//            val fHeight = fragment.height()
//            i("getBitmapFragment: fWidth=$fWidth fHeight=$fHeight")
//            val bitmap = Bitmap.createBitmap(fWidth, fHeight, Bitmap.Config.ARGB_8888)
//            val imageBMP: Bitmap = FilesUtils.pdfToBitmaps(
//                context,
//                File(path)
//            )[0]
//            val p = Paint()
//            p.isAntiAlias = true
//            p.isDither = true
//            var imageWidth = 0
//            var imageHeight = 0
//            if (imageBMP != null) {
//                imageWidth = imageBMP.width
//                imageHeight = imageBMP.height
//            }
//            val aspectH = (height / imageHeight).toFloat()
//            val dst = RectF(0f, 0f, fWidth.toFloat(), height.toFloat())
//            i("getBitmapFragment:  aspectH=$aspectH")
//            i(
//                "getBitmapFragment: imageWidth=$imageWidth imageHeight=$imageHeight"
//            )
//            val sLeft = 0f
//            val sTop = fragment.top / aspectH
//            val sRight = imageWidth.toFloat()
//            val sBottom = imageHeight.toFloat()
//            val source = RectF(sLeft, sTop, sRight, sBottom)
//            val canvas = Canvas(bitmap)
//            canvas.drawColor(Color.WHITE)
//            // move image to actual printing area
//            i("getBitmapFragment: source=$source dst=$dst")
//            dst.offsetTo(0f, 0f)
//            val matrix = Matrix()
//            matrix.setRectToRect(source, dst, Matrix.ScaleToFit.START)
//            canvas.drawBitmap(imageBMP!!, matrix, p)
//            bitmap
//        })
//        return pages
//    }

    data class PrintReport(
        val result: TestResultAndCurveModel,
        val hospitalName: String,
        val barcode: Boolean
    )

}
