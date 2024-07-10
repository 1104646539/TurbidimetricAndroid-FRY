package com.wl.turbidimetric.report

import android.os.Environment
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.wllib.LogToFile
import com.wl.wllib.toTimeStr
import java.io.File
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
    fun create(drm: TestResultAndCurveModel, filePath: File, hospitalName: String): String? {
        pdfFile = filePath.absolutePath
        resultModel = drm
        PdfCreateUtil.hospitalName = hospitalName
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
                resultModel?.result?.testTime?.toTimeStr() ?: ""
        }

        detectionResult = resultModel?.result?.testResult ?: ""
        concentration = if (resultModel?.result?.concentration === -1) {
            "无效"
        } else {
            (resultModel?.result?.concentration as Int).toString()
        }
        projecteUnit =
            if (resultModel?.curve == null) " " else " " + resultModel?.curve?.projectUnit
        projectLjz = resultModel?.curve?.projectLjz.toString()
        deliveryDoctor = resultModel?.result?.deliveryDoctor ?: ""
    }

    private fun createPdf() {
        //创建一个 PdfWriter 对象，用于将文档写入到 PDF 文件中
        var writer: PdfWriter =
            PdfWriter(pdfFile)
        //创建一个 PdfDocument 对象，表示 PDF 文档
        val pdfDoc = PdfDocument(writer)
        //创建一个 Document 对象，表示 PDF 文档的页面  A4 大小
        val document = Document(pdfDoc, PageSize.A4)
        document.setFont(pdfFont)
        document.setFontSize(12f)

        drawTitle(document)
        drawPatientInfoTable(document)
        drawResultTable(document)
        drawDoctor(document)
        document.close()
        document.flush()
    }

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
        val paragraphPeople1_2 = Paragraph("$deliveryDoctor")
        paragraphPeople1_2.setTextAlignment(TextAlignment.LEFT)
        val cellPeople1_2: Cell = Cell(1, 1).add(paragraphPeople1_2).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))

        table.addCell(cellPeople1_1)
        table.addCell(cellPeople1_2)
        document.add(table)
    }

    private fun drawResultTable(document: Document) {
        //这个数组用于存储后续用于创建表格的列宽度信息
        val resultArray = arrayOfNulls<UnitValue>(4)
        //宽度将占据表格可用宽度的 x%。
        resultArray[0] = UnitValue.createPercentValue(30f)
        resultArray[1] = UnitValue.createPercentValue(15f)
        resultArray[2] = UnitValue.createPercentValue(25f)
        resultArray[3] = UnitValue.createPercentValue(30f)
        //方法用于使表格使用所有可用的宽度，确保表格填满水平空间。
        val table = Table(resultArray).useAllAvailableWidth().setBorder(Border.NO_BORDER)
        //表格的总宽度为 390 点
        table.setWidth(500f)
        table.setHorizontalAlignment(HorizontalAlignment.CENTER)
        table.setVerticalAlignment(VerticalAlignment.MIDDLE)
        table.setMarginTop(10f)
        val paragraphPeople1_1 = Paragraph("测定项目名")
        paragraphPeople1_1.setTextAlignment(TextAlignment.RIGHT)
        val cellPeople1_1: Cell = Cell(1, 1).add(paragraphPeople1_1).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))
        val paragraphPeople1_2 = Paragraph("结果")
        paragraphPeople1_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_2: Cell = Cell(1, 1).add(paragraphPeople1_2).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))
        val paragraphPeople1_3 = Paragraph("正常参考值")
        paragraphPeople1_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople1_3: Cell = Cell(1, 1).add(paragraphPeople1_3).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))
        val paragraphPeople1_4 = Paragraph("单位")
        paragraphPeople1_4.setTextAlignment(TextAlignment.LEFT)
        val cellPeople1_4: Cell = Cell(1, 1).add(paragraphPeople1_4).setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(1f))

        table.addCell(cellPeople1_1)
        table.addCell(cellPeople1_2)
        table.addCell(cellPeople1_3)
        table.addCell(cellPeople1_4)

        val paragraphPeople2_1 = Paragraph("${projectName}浓度")
        paragraphPeople2_1.setTextAlignment(TextAlignment.RIGHT)
        val cellPeople2_1: Cell = Cell(1, 1).add(paragraphPeople2_1).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))
        val paragraphPeople2_2 = Paragraph(concentration)
        paragraphPeople2_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_2: Cell = Cell(1, 1).add(paragraphPeople2_2).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))
        val paragraphPeople2_3 = Paragraph("<$projectLjz")
        paragraphPeople2_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_3: Cell = Cell(1, 1).add(paragraphPeople2_3).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))
        val paragraphPeople2_4 = Paragraph(projecteUnit)
        paragraphPeople2_4.setTextAlignment(TextAlignment.LEFT)
        val cellPeople2_4: Cell = Cell(1, 1).add(paragraphPeople2_4).setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(1f))

        table.addCell(cellPeople2_1)
        table.addCell(cellPeople2_2)
        table.addCell(cellPeople2_3)
        table.addCell(cellPeople2_4)

        document.add(table)

        val paragraphPeopleResult = Paragraph("检测结果:$detectionResult")
        paragraphPeopleResult.setTextAlignment(TextAlignment.LEFT)
        paragraphPeopleResult.setMarginLeft(20f)
        document.add(paragraphPeopleResult)
    }

    /**
     * 绘制患者信息表
     * @param document Document
     */
    private fun drawPatientInfoTable(document: Document) {
        //这个数组用于存储后续用于创建表格的列宽度信息
        val resultArray = arrayOfNulls<UnitValue>(4)
        //宽度将占据表格可用宽度的 x%。
        resultArray[0] = UnitValue.createPercentValue(25f)
        resultArray[1] = UnitValue.createPercentValue(25f)
        resultArray[2] = UnitValue.createPercentValue(25f)
        resultArray[3] = UnitValue.createPercentValue(25f)
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

        table.addCell(cellPeople1_1)
        table.addCell(cellPeople1_2)
        table.addCell(cellPeople1_3)
        table.addCell(cellPeople1_4)

        val paragraphPeople2_1 = Paragraph("年龄")
        paragraphPeople2_1.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_1: Cell = Cell(1, 1).add(paragraphPeople2_1)
        val paragraphPeople2_2 = Paragraph(patientAge)
        paragraphPeople2_2.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_2: Cell = Cell(1, 1).add(paragraphPeople2_2)
        val paragraphPeople2_3 = Paragraph("检测时间")
        paragraphPeople2_3.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_3: Cell = Cell(1, 1).add(paragraphPeople2_3)
        val paragraphPeople2_4 = Paragraph(detectionDate)
        paragraphPeople2_4.setTextAlignment(TextAlignment.CENTER)
        val cellPeople2_4: Cell = Cell(1, 1).add(paragraphPeople2_4)

        table.addCell(cellPeople2_1)
        table.addCell(cellPeople2_2)
        table.addCell(cellPeople2_3)
        table.addCell(cellPeople2_4)

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
        paragraphTitle.setMultipliedLeading(2f)
        document.add(paragraphTitle)
    }
}
