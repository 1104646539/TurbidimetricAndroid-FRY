package com.wl.turbidimetric.report

import android.graphics.Bitmap
import android.os.Environment
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.view.ReportChartView
import com.wl.wllib.DateUtil
import com.wl.wllib.LogToFile
import com.wl.wllib.toTimeStr
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object PdfCreateUtil {
    init {

    }

    private val TAG: String = PdfCreateUtil::class.java.simpleName

    /**
     * 医院名
     */
    var hospitalName: String = ""

    /**
     * 检测医生
     */
    var detectionDoctor: String = ""

    /**
     * 姓名
     */
    var patientName: String = ""

    /**
     * 性别
     */
    var patientSex: String = ""

    /**
     * 年龄
     */
    var patientAge: String = ""

    /**
     * 检测日期
     */
    var detectionDate: String = ""

    /**
     * 检测结果
     */
    var detectionResult: String = ""

    /**
     * 浓度
     */
    var concentration: String = ""

    /**
     * 项目名
     */
    var projectName: String = ""

    /**
     * 浓度单位
     */
    var projecteUnit: String = ""

    /**
     * 浓度临界值
     */
    var projectLjz: String = ""

    /**
     * 送检医生
     */
    var deliveryDoctor: String = ""

    /**
     * 送检时间
     */
    var deliveryTime: String = ""
    lateinit var pdfFont: PdfFont

    /**
     * 中文字体
     */
    private var pdfFile: String? = null
    private var resultModel: TestResultAndCurveModel? = null
    private val rootFile =
        File(Environment.getExternalStorageDirectory().absolutePath + "/" + "检测报告")


    /**
     * @param drm
     * @param filePath 文件目录
     * @param copy     是否需要拷贝到u盘
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun create(
        drm: TestResultAndCurveModel,
        filePath: File,
        hospitalName: String,
        detectionDoctor: String
    ): String? {
        pdfFile = filePath.absolutePath
        resultModel = drm
        PdfCreateUtil.hospitalName = hospitalName
        PdfCreateUtil.detectionDoctor = detectionDoctor
        init()

        LogToFile.i(TAG, "pdfFile=$pdfFile drm=$drm")
        try {
            //创建pdf
            createPdf()
            LogToFile.i(TAG, "pdf生成成功")
        } catch (e: IOException) {
            LogToFile.i(TAG, "pdf生成失败 IOException $e")
            return ""
        }
        return pdfFile
    }


    private fun init() {
        initData()
        initFont()

    }

    /**
     * 删除缓存
     */
    @Throws(IOException::class)
    fun deleteCacheFolder(rootFile: File) {
        if (!rootFile.exists()) {
            rootFile.mkdirs()
        } else {
            val childFiles = rootFile.listFiles()
            for (i in childFiles.indices) {
                childFiles[i].delete()
            }
        }
    }

    private fun initFont() {
        pdfFont = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H")
    }


    private fun initData() {
        if (resultModel == null) return
        val pm: CurveModel? = resultModel?.curve

        if (pm != null) {
            patientName = resultModel?.result?.name ?: ""
            patientSex = resultModel?.result?.gender ?: ""
            patientAge = resultModel?.result?.age ?: ""
            detectionDate =
                resultModel?.result?.testTime?.toTimeStr(DateUtil.Time4Format) ?: ""

            projectLjz = pm.projectLjz.toString()
            projectName = pm.projectName
        }

        detectionResult = resultModel?.result?.testResult ?: ""
        concentration = if (resultModel?.result?.concentration === -1) {
            "无效"
        } else {
            (resultModel?.result?.concentration as Int).toString()
        }
        deliveryDoctor = resultModel?.result?.deliveryDoctor ?: ""
        deliveryTime = (resultModel?.result?.deliveryTime ?: "")
        projecteUnit =
            if (resultModel?.curve == null) " " else " " + resultModel?.curve?.projectUnit
    }

    private fun createPdf() {
        //创建一个 PdfWriter 对象，用于将文档写入到 PDF 文件中
        var writer =
            PdfWriter(pdfFile)
        //创建一个 PdfDocument 对象，表示 PDF 文档
        val pdfDoc = PdfDocument(writer)
        //创建一个 Document 对象，表示 PDF 文档的页面  A4 大小
        val document = Document(pdfDoc, PageSize.A4)
        document.setFont(pdfFont)
        document.setFontSize(12f)

        drawTitle(document)
        drawPatientInfoTable(document)
        drawResultImg(document)
        drawResultTable(document)
        drawReferenceInfo(document)
        drawDoctor(document)
        document.close()
        document.flush()
    }

    /**
     * 绘制参考信息
     * @param document Document
     */
    private fun drawReferenceInfo(document: Document) {
        val paragraphTitle = Paragraph("检测意义")
        paragraphTitle.setTextAlignment(TextAlignment.CENTER)
        paragraphTitle.setFontSize(14.0f)
        paragraphTitle.setBold()
        paragraphTitle.setMultipliedLeading(2f)
        document.add(paragraphTitle)

        val paragraphReference1 =
            Paragraph("\t便潜血试验定量检测，能够早期发现肠道问题、较早期发现结直肠癌和癌前病变，使疾病在可治愈阶段得到根治，从而减少结直肠癌的发病率和死亡率。")
        paragraphReference1.setTextAlignment(TextAlignment.LEFT)
        paragraphReference1.setFontSize(12.0f)
        document.add(paragraphReference1)

        val paragraphReference2 = Paragraph("温馨提示")
        paragraphReference2.setTextAlignment(TextAlignment.CENTER)
        paragraphReference2.setFontSize(14.0f)
        paragraphReference2.setMultipliedLeading(2f)
        document.add(paragraphReference2)

        val paragraphReference3 =
            Paragraph("若检测结果为阴性：建议每年做一次便潜血检查，预防肠道疾病发生\n若检测结果为阳性：表示患有结直肠癌的可能性比较高，建议到正规医院做肠镜检查")
        paragraphReference3.setTextAlignment(TextAlignment.LEFT)
        paragraphReference3.setFontSize(12.0f)
        document.add(paragraphReference3)


    }

    /**
     * 绘制结果图标
     * @param document Document
     */
    private fun drawResultImg(document: Document) {
        val paragraph1 = Paragraph("")
        paragraph1.setPaddingTop(10f)
        document.add(paragraph1)

//        val chartView = ReportChartView(resultModel!!, (500).toInt(), (250).toInt());
        val chartView = ReportChartView(resultModel!!, (1000).toInt(), (500).toInt());
        val bitmap = chartView.convertToBitmap();
        val file = saveBitmapToInternalStorage(bitmap!!, "temp")
        val img = Image(ImageDataFactory.create(file.toURL()))
        img.scale(0.5f, 0.5f)
        document.add(img)

        val paragraph2 = Paragraph("")
        paragraph2.setPaddingTop(10f)
        document.add(paragraph2)
    }


    fun saveBitmapToInternalStorage(bitmap: Bitmap, imageName: String): File {
        val file = File("/sdcard", "$imageName.png")
        try {
            file.createNewFile()
            FileOutputStream(file).use { out ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    out
                )
                out.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    /**
     * 绘制医生签名
     * @param document Document
     */
    private fun drawDoctor(document: Document) {
        //这个数组用于存储后续用于创建表格的列宽度信息
        val resultArray = arrayOfNulls<UnitValue>(4)
        //宽度将占据表格可用宽度的 x%。
        resultArray[0] = UnitValue.createPercentValue(70f)
        resultArray[1] = UnitValue.createPercentValue(20f)
        //方法用于使表格使用所有可用的宽度，确保表格填满水平空间。
        val table = Table(resultArray).useAllAvailableWidth().setBorder(Border.NO_BORDER)
        //表格的总宽度为 390 点
        table.setWidth(500f)
        table.setHorizontalAlignment(HorizontalAlignment.CENTER)
        table.setVerticalAlignment(VerticalAlignment.MIDDLE)
        table.setMarginTop(10f)

        val paragraphPeople1_1 = Paragraph("检测医生:")
        paragraphPeople1_1.setTextAlignment(TextAlignment.RIGHT)
        val cellPeople1_1: Cell = Cell(1, 1).add(paragraphPeople1_1)
            .setBorder(Border.NO_BORDER)
        val paragraphPeople1_2 = Paragraph("$detectionDoctor")
        paragraphPeople1_2.setTextAlignment(TextAlignment.LEFT)
        val cellPeople1_2: Cell = Cell(1, 1).add(paragraphPeople1_2).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))

        table.addCell(cellPeople1_1)
        table.addCell(cellPeople1_2)
        document.add(table)
    }

    /**
     * 绘制结果信息
     * @param document Document
     */
    private fun drawResultTable(document: Document) {
        //这个数组用于存储后续用于创建表格的列宽度信息
        val resultArray = arrayOfNulls<UnitValue>(4)
        //宽度将占据表格可用宽度的 x%。
        resultArray[0] = UnitValue.createPercentValue(20f)
        resultArray[1] = UnitValue.createPercentValue(20f)
        resultArray[2] = UnitValue.createPercentValue(20f)
        resultArray[3] = UnitValue.createPercentValue(40f)
        //方法用于使表格使用所有可用的宽度，确保表格填满水平空间。
        val table = Table(resultArray).useAllAvailableWidth().setBorder(Border.NO_BORDER)
        //表格的总宽度为 390 点
        table.setWidth(500f)
        table.setHorizontalAlignment(HorizontalAlignment.CENTER)
        table.setFontSize(10f)
        table.setVerticalAlignment(VerticalAlignment.MIDDLE)
        table.setMarginTop(5f)
        val paragraphPeople1_1 = Paragraph("${projectName}浓度:")
        paragraphPeople1_1.setTextAlignment(TextAlignment.RIGHT)
        val cellPeople1_1: Cell = Cell(1, 1).add(paragraphPeople1_1).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))
        val paragraphPeople1_2 = Paragraph(">=")
        paragraphPeople1_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_2: Cell = Cell(1, 1).add(paragraphPeople1_2).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))
        val paragraphPeople1_3 = Paragraph("$projectLjz")
        paragraphPeople1_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_3: Cell = Cell(1, 1).add(paragraphPeople1_3).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))
        val paragraphPeople1_4 = Paragraph("阳性")
        paragraphPeople1_4.setTextAlignment(TextAlignment.LEFT)
        val cellPeople1_4: Cell = Cell(1, 1).add(paragraphPeople1_4).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))

        table.addCell(cellPeople1_1)
        table.addCell(cellPeople1_2)
        table.addCell(cellPeople1_3)
        table.addCell(cellPeople1_4)

        val paragraphPeople2_1 = Paragraph("")
        paragraphPeople2_1.setTextAlignment(TextAlignment.RIGHT)
        val cellPeople2_1: Cell = Cell(1, 1).add(paragraphPeople2_1).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))
        val paragraphPeople2_2 = Paragraph("<")
        paragraphPeople2_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_2: Cell = Cell(1, 1).add(paragraphPeople2_2).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))
        val paragraphPeople2_3 = Paragraph("$projectLjz")
        paragraphPeople2_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_3: Cell = Cell(1, 1).add(paragraphPeople2_3).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))
        val paragraphPeople2_4 = Paragraph("正常")
        paragraphPeople2_4.setTextAlignment(TextAlignment.LEFT)
        val cellPeople2_4: Cell = Cell(1, 1).add(paragraphPeople2_4).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))

        table.addCell(cellPeople2_1)
        table.addCell(cellPeople2_2)
        table.addCell(cellPeople2_3)
        table.addCell(cellPeople2_4)

        document.add(table)
        val paragraphPeopleResult =
            Paragraph("检测结果:        $concentration        $detectionResult")
        paragraphPeopleResult.setFontSize(14f)
        paragraphPeopleResult.setBold()
        paragraphPeopleResult.setTextAlignment(TextAlignment.LEFT)
        document.add(paragraphPeopleResult)
    }

    /**
     * 绘制患者信息表
     * @param document Document
     */
    private fun drawPatientInfoTable(document: Document) {
        //这个数组用于存储后续用于创建表格的列宽度信息
        val resultArray = arrayOfNulls<UnitValue>(6)
        //宽度将占据表格可用宽度的 x%。
        resultArray[0] = UnitValue.createPercentValue(14f)
        resultArray[1] = UnitValue.createPercentValue(18f)
        resultArray[2] = UnitValue.createPercentValue(14f)
        resultArray[3] = UnitValue.createPercentValue(18f)
        resultArray[4] = UnitValue.createPercentValue(14f)
        resultArray[5] = UnitValue.createPercentValue(22f)
        //方法用于使表格使用所有可用的宽度，确保表格填满水平空间。
        val table = Table(resultArray).useAllAvailableWidth()
        //表格的总宽度为 390 点
        table.setWidth(500f)
        table.setHorizontalAlignment(HorizontalAlignment.CENTER)
        table.setVerticalAlignment(VerticalAlignment.MIDDLE)
        table.setBorder(SolidBorder(1f))

        val paragraphPeople1_1 = Paragraph("姓名")
        paragraphPeople1_1.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_1: Cell = Cell(1, 1).add(paragraphPeople1_1)
        val paragraphPeople1_2 = Paragraph(patientName)
        paragraphPeople1_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_2: Cell = Cell(1, 1).add(paragraphPeople1_2)

        val paragraphPeople1_3 = Paragraph("性别")
        paragraphPeople1_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_3: Cell = Cell(1, 1).add(paragraphPeople1_3)
        val paragraphPeople1_4 = Paragraph(patientSex)
        paragraphPeople1_4.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_4: Cell = Cell(1, 1).add(paragraphPeople1_4)

        val paragraphPeople1_5 = Paragraph("检测时间")
        paragraphPeople1_5.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_5: Cell = Cell(1, 1).add(paragraphPeople1_5)
        val paragraphPeople1_6 = Paragraph(detectionDate).setFontSize(12f)
        paragraphPeople1_6.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_6: Cell = Cell(1, 1).add(paragraphPeople1_6)

        table.addCell(cellPeople1_1)
        table.addCell(cellPeople1_2)
        table.addCell(cellPeople1_3)
        table.addCell(cellPeople1_4)
        table.addCell(cellPeople1_5)
        table.addCell(cellPeople1_6)

        val paragraphPeople2_1 = Paragraph("年龄")
        paragraphPeople2_1.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_1: Cell = Cell(1, 1).add(paragraphPeople2_1)
        val paragraphPeople2_2 = Paragraph(patientAge)
        paragraphPeople2_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_2: Cell = Cell(1, 1).add(paragraphPeople2_2)

        val paragraphPeople2_3 = Paragraph("送检医生")
        paragraphPeople2_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_3: Cell = Cell(1, 1).add(paragraphPeople2_3)
        val paragraphPeople2_4 = Paragraph(deliveryDoctor)
        paragraphPeople2_4.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_4: Cell = Cell(1, 1).add(paragraphPeople2_4)

        val paragraphPeople2_5 = Paragraph("送检时间")
        paragraphPeople2_5.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_5: Cell = Cell(1, 1).add(paragraphPeople2_5)
        val paragraphPeople2_6 = Paragraph(deliveryTime).setFontSize(12f)
        paragraphPeople2_6.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_6: Cell = Cell(1, 1).add(paragraphPeople2_6)

        table.addCell(cellPeople2_1)
        table.addCell(cellPeople2_2)
        table.addCell(cellPeople2_3)
        table.addCell(cellPeople2_4)
        table.addCell(cellPeople2_5)
        table.addCell(cellPeople2_6)


        val paragraphPeople3_1 = Paragraph("编号")
        paragraphPeople3_1.setTextAlignment(TextAlignment.CENTER)
        val cellPeople3_1: Cell = Cell(1, 1).add(paragraphPeople3_1)
        val paragraphPeople3_2 = Paragraph(resultModel?.result?.detectionNum ?: "")
        paragraphPeople3_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople3_2: Cell = Cell(1, 1).add(paragraphPeople3_2)

        val paragraphPeople3_3 = Paragraph("条码")
        paragraphPeople3_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople3_3: Cell = Cell(1, 1).add(paragraphPeople3_3)
        val paragraphPeople3_4 = Paragraph(resultModel?.result?.sampleBarcode ?: "")
        paragraphPeople3_4.setTextAlignment(TextAlignment.CENTER)
        val cellPeople3_4: Cell = Cell(1, 1).add(paragraphPeople3_4)

//        val paragraphPeople3_5 = Paragraph("")
//        paragraphPeople3_5.setTextAlignment(TextAlignment.CENTER)
//        val cellPeople3_5: Cell = Cell(1, 1).add(paragraphPeople3_5)
//        val paragraphPeople3_6 = Paragraph("")
//        paragraphPeople3_6.setTextAlignment(TextAlignment.CENTER)
//        val cellPeople3_6: Cell = Cell(1, 1).add(paragraphPeople3_6)

        table.addCell(cellPeople3_1)
        table.addCell(cellPeople3_2)
        table.addCell(cellPeople3_3)
        table.addCell(cellPeople3_4)
//        table.addCell(cellPeople3_5)
//        table.addCell(cellPeople3_6)

        document.add(table)
    }

    /**
     * 绘制标题
     * @param document Document
     */
    private fun drawTitle(document: Document) {
        val paraTitle = hospitalName
        val paragraphTitle = Paragraph(paraTitle)
        paragraphTitle.setTextAlignment(TextAlignment.CENTER)
        paragraphTitle.setFontSize(18.0f)
        paragraphTitle.setBold()
        document.add(paragraphTitle)

        val title2 = if (projectName.contains("血红蛋白")) {
            "便潜血试验定量检测报告"
        } else {
            "${projectName}试验定量检测报告"
        }
        val paragraphTitle2 = Paragraph(title2)
        paragraphTitle2.setTextAlignment(TextAlignment.CENTER)
        paragraphTitle2.setFontSize(14.0f)
        document.add(paragraphTitle2)
    }
}
