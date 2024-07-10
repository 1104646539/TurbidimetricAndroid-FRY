package com.wl.turbidimetric.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class FilesUtils {
    static String TAG = "FilesUtils";
    static final String FILE_PNG = "test_page.png";
    static final String FILE_DOC = "What is PrintHand.doc";
    static final String FILE_PDF = "What is PrintHand.pdf";
//    static final String FILE_PDF = "2024-05-15 6.pdf";

    static void extractFilesFromAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        File dir = getFilesDir(context);
        for (String filename : new String[]{FILE_PNG, FILE_DOC, FILE_PDF})
            extractFileFromAssets(assetManager, dir, filename);
    }

    private static void extractFileFromAssets(AssetManager assetManager, File dir, String filename) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                inputStream = assetManager.open(filename);
                File outFile = new File(dir, filename);
                if (outFile.exists())
                    outFile.delete();
                outFile.createNewFile();
                outputStream = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1)
                    outputStream.write(buffer, 0, read);
                outputStream.flush();
            } finally {
                if (inputStream != null)
                    inputStream.close();
                if (outputStream != null)
                    outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String getFilePath(Context context, String filename) {
        File file = new File(getFilesDir(context), filename);
        return file.exists() ? file.getAbsolutePath() : null;
    }

    static File getFilesDir(Context context) {
        return context.getExternalCacheDir();
    }

    public static ArrayList<Bitmap> pdfToBitmaps(Context context, File pdfFile) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));
                Bitmap bitmap;
                int pageCount = renderer.getPageCount();
                Log.d(TAG, "图片de 张数： " + pageCount);
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    Log.d(TAG, "page.getWidth()=" + page.getWidth()+" page.getHeight()=" + page.getHeight());
                    int width = context.getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                    int height = context.getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
                    Log.d(TAG, "width" + width+" height=" + height);
//                    int width = mRegionView.getRegionWidth();
//                    int height = mRegionView.getRegionHeight();
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE);
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    Rect r = new Rect(0, 0, width, height);
                    page.render(bitmap, r, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    bitmaps.add(bitmap);
                    // close the page
                    page.close();
                }
                // close the renderer
                renderer.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bitmaps;
    }

}
