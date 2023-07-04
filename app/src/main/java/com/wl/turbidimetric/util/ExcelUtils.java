package com.wl.turbidimetric.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public class ExcelUtils {
    public static WritableFont arial14font = null;

    public static WritableCellFormat arial14format = null;
    public static WritableFont arial10font = null;
    public static WritableCellFormat arial10format = null;
    public static WritableFont arial12font = null;
    public static WritableCellFormat arial12format = null;

    public final static String UTF8_ENCODING = "UTF-8";
    public final static String GBK_ENCODING = "GBK";


    static {
        format();
    }

    /**
     * 单元格的格式设置 字体大小 颜色 对齐方式、背景颜色等...
     */
    public static void format() {
        try {
//            if (arial14font != null) {
//                return;
//            }
            arial14font = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
            arial14font.setColour(Colour.LIGHT_BLUE);
            arial14format = new WritableCellFormat(arial14font);
            arial14format.setAlignment(jxl.format.Alignment.CENTRE);
            arial14format.setBorder(jxl.format.Border.ALL, jxl.format.BorderLineStyle.THIN);
            arial14format.setBackground(Colour.VERY_LIGHT_YELLOW);

            arial10font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
            arial10format = new WritableCellFormat(arial10font);
            arial10format.setAlignment(jxl.format.Alignment.CENTRE);
            arial10format.setBorder(jxl.format.Border.ALL, jxl.format.BorderLineStyle.THIN);
            arial10format.setBackground(Colour.GRAY_25);

            arial12font = new WritableFont(WritableFont.ARIAL, 10);
            arial12format = new WritableCellFormat(arial12font);
            arial10format.setAlignment(jxl.format.Alignment.CENTRE);//对齐格式
            arial12format.setBorder(jxl.format.Border.ALL, jxl.format.BorderLineStyle.THIN); //设置边框

        } catch (WriteException e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> boolean writeObjListToExcel(List<String> titles, List<T> objList, OutputStream os) {
//        format();
        //标题
        WritableWorkbook workbook = null;
        WritableSheet sheet = null;
        try {
//                File file = new File(fileName);
//                if (!file.exists()) {
//                    file.createNewFile();
//                }
            workbook = Workbook.createWorkbook(os);
            sheet = workbook.createSheet("数据表", 0);
            if (!titles.isEmpty()) {
                //创建标题栏
//                sheet.addCell((WritableCell) new Label(0, 0, "数据表", arial14format));
                for (int col = 0; col < titles.size(); col++) {
                    sheet.addCell(new Label(col, 0, titles.get(col), arial10format));
                }
                sheet.setRowView(0, 340); //设置行高
//                workbook.write();
            }

            //内容
            int count = objList == null ? 0 : objList.size();
            if (count > 0) {
                for (int j = 0; j < count; j++) {
                    ArrayList<String> list = (ArrayList<String>) objList.get(j);
                    for (int i = 0; i < list.size(); i++) {
                        sheet.addCell(new Label(i, j + 1, list.get(i), arial12format));
                        if (list.get(i).length() <= 5) {
                            sheet.setColumnView(i, list.get(i).length() + 8); //设置列宽
                        } else {
                            sheet.setColumnView(i, list.get(i).length() + 5); //设置列宽
                        }
                    }
                    sheet.setRowView(j + 1, 350); //设置行高
                }

                workbook.write();
                os.flush();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    static ExcelUtils getInstance() {
        return ExcelUtilsHolder.instance;
    }

    static class ExcelUtilsHolder {
        static ExcelUtils instance = new ExcelUtils();
    }
}
