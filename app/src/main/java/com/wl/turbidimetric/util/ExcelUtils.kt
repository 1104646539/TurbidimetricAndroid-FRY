package com.wl.turbidimetric.util

import jxl.Workbook
import jxl.format.Alignment
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import jxl.write.*
import java.io.OutputStream

object ExcelUtils {
    var headerFormat: WritableCellFormat? = null
    var contentFormat: WritableCellFormat? = null
    const val UTF8_ENCODING = "UTF-8"

    fun defaultContentFormat(): WritableCellFormat {
        val contentFont = WritableFont(WritableFont.ARIAL, 10)
        val contentFormat = WritableCellFormat(contentFont)
        contentFormat.alignment = Alignment.CENTRE //对齐格式
        contentFormat.setBorder(Border.ALL, BorderLineStyle.THIN) //设置边框
        return contentFormat
    }


    fun defaultHeaderFormat(): WritableCellFormat {
        val headerFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD)
        val headerFormat = WritableCellFormat(headerFont)
        headerFormat.alignment = Alignment.CENTRE
        headerFormat.setBorder(Border.ALL, BorderLineStyle.THIN)
        headerFormat.setBackground(Colour.GRAY_25)
        return headerFormat
    }

    init {
        try {
            format(defaultHeaderFormat(), defaultContentFormat())
        } catch (e: WriteException) {
            e.printStackTrace()
        }
    }

    /**
     * 单元格的格式设置 字体大小 颜色 对齐方式、背景颜色等...
     *
     * @param headerFormat  标题格式
     * @param contentFormat 内容格式
     */
    fun format(headerFormat: WritableCellFormat?, contentFormat: WritableCellFormat?) {
        ExcelUtils.headerFormat = headerFormat
        ExcelUtils.contentFormat = contentFormat
    }

    fun <T> writeObjListToExcel(
        titles: List<String?>,
        objList: List<T>?,
        os: OutputStream
    ): Boolean {
        //标题
        var workbook: WritableWorkbook? = null
        var sheet: WritableSheet? = null
        try {
            workbook = Workbook.createWorkbook(os)
            sheet = workbook.createSheet("数据表", 0)
            if (!titles.isEmpty()) {
                //创建标题栏
                for (col in titles.indices) {
                    sheet.addCell(Label(col, 0, titles[col], headerFormat))
                }
                sheet.setRowView(0, 340) //设置行高
            }

            //内容
            val count = objList?.size ?: 0
            if (count > 0) {
                for (j in 0 until count) {
                    val list = objList!![j] as ArrayList<String>
                    for (i in list.indices) {
                        sheet.addCell(Label(i, j + 1, list[i], contentFormat))
                        if (list[i].length <= 5) {
                            sheet.setColumnView(i, list[i].length + 8) //设置列宽
                        } else {
                            sheet.setColumnView(i, list[i].length + 5) //设置列宽
                        }
                    }
                    sheet.setRowView(j + 1, 350) //设置行高
                }
                workbook.write()
                os.flush()
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            if (workbook != null) {
                try {
                    workbook.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return false
    }

}
