package com.example.mygraduationproject.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/** 绯荤粺鐩稿唽宸ュ叿锛氫繚瀛樺浘鐗囧埌鐩稿唽锛屽吋瀹笰ndroid Q鍒嗗尯瀛樺偍 */
public class GalleryUtils {
    
    private static final String TAG = "GalleryUtils";
    private static final String ALBUM_NAME = "PlantMonitor";
    
    public static boolean saveToGallery(Context context, File imageFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image file");
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.getName());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM_NAME);
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }
            
            android.net.Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Log.e(TAG, "Failed to insert MediaStore");
                return false;
            }
            
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Log.e(TAG, "Failed to open output stream");
                return false;
            }
            
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
            }
            
            Log.d(TAG, "Image saved to gallery: " + uri.toString());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image to gallery", e);
            return false;
        }
    }
    
    public static boolean saveToGallery(Context context, String imagePath) {
        return saveToGallery(context, new File(imagePath));
    }
}
