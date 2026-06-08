package com.example.mygraduationproject.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** 鍥剧墖澶勭悊宸ュ叿锛氬帇缂┿€佺缉鏀俱€佹棆杞€佹枃浠跺ぇ灏忔牸寮忓寲 */
public class ImageUtils {
    
    private static final int MAX_FILE_SIZE = 500 * 1024;
    private static final int DEFAULT_QUALITY = 85;
    private static final int MIN_QUALITY = 10;
    private static final int TARGET_WIDTH = 1920;
    private static final int TARGET_HEIGHT = 1080;
    
    public static File compressImage(File originalFile, File outputDir, String outputFileName) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath());
        if (bitmap == null) {
            throw new IOException("无法解码图片文件");
        }
        
        bitmap = resizeBitmap(bitmap, TARGET_WIDTH, TARGET_HEIGHT);
        
        int quality = DEFAULT_QUALITY;
        byte[] compressedBytes;
        
        do {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            compressedBytes = baos.toByteArray();
            
            if (compressedBytes.length <= MAX_FILE_SIZE || quality <= MIN_QUALITY) {
                break;
            }
            
            quality -= 5;
        } while (quality >= MIN_QUALITY);
        
        File outputFile = new File(outputDir, outputFileName);
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(compressedBytes);
        fos.flush();
        fos.close();
        
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        
        return outputFile;
    }
    
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }
        
        float scale;
        if (width > height) {
            scale = (float) maxWidth / width;
        } else {
            scale = (float) maxHeight / height;
        }
        
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        if (resizedBitmap != bitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        
        return resizedBitmap;
    }
    
    public static byte[] bitmapToByteArray(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }
    
    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) {
            return bitmap;
        }
        
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotatedBitmap != bitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        
        return rotatedBitmap;
    }
    
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }
    }
}
