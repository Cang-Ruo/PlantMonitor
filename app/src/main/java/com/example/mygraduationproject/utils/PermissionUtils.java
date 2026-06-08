package com.example.mygraduationproject.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 权限管理工具类
 * 用于处理Android 10+的运行时权限申请
 */
public class PermissionUtils {

    private static final String TAG = "PermissionUtils";

    // 权限请求码
    public static final int REQUEST_CODE_STORAGE = 1001;
    public static final int REQUEST_CODE_CAMERA = 1002;
    public static final int REQUEST_CODE_ALL = 1003;

    /**
     * 存储权限数组（根据Android版本动态选择）
     */
    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 (API 30-32)
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } else {
            // Android 10及以下 (API 29及以下)
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * 相机权限数组
     */
    public static String[] getCameraPermissions() {
        return new String[]{
                Manifest.permission.CAMERA
        };
    }

    /**
     * 所有需要的权限数组
     */
    public static String[] getAllPermissions() {
        String[] storagePerms = getStoragePermissions();
        String[] cameraPerms = getCameraPermissions();
        
        String[] allPerms = new String[storagePerms.length + cameraPerms.length];
        System.arraycopy(storagePerms, 0, allPerms, 0, storagePerms.length);
        System.arraycopy(cameraPerms, 0, allPerms, storagePerms.length, cameraPerms.length);
        
        return allPerms;
    }

    /**
     * 检查存储权限是否已授予
     */
    public static boolean hasStoragePermission(Context context) {
        String[] permissions = getStoragePermissions();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查相机权限是否已授予
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查所有权限是否已授予
     */
    public static boolean hasAllPermissions(Context context) {
        return hasStoragePermission(context) && hasCameraPermission(context);
    }

    /**
     * 请求存储权限
     */
    public static void requestStoragePermission(Activity activity) {
        if (!hasStoragePermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    getStoragePermissions(),
                    REQUEST_CODE_STORAGE
            );
        }
    }

    /**
     * 请求相机权限
     */
    public static void requestCameraPermission(Activity activity) {
        if (!hasCameraPermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    getCameraPermissions(),
                    REQUEST_CODE_CAMERA
            );
        }
    }

    /**
     * 请求所有权限
     */
    public static void requestAllPermissions(Activity activity) {
        if (!hasAllPermissions(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    getAllPermissions(),
                    REQUEST_CODE_ALL
            );
        }
    }

    /**
     * 检查是否应该显示权限说明
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity) {
        String[] permissions = getAllPermissions();
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理权限请求结果
     * @param requestCode 请求码
     * @param permissions 权限数组
     * @param grantResults 授权结果
     * @param callback 回调
     */
    public static void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults,
            PermissionCallback callback) {
        
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            callback.onGranted(requestCode);
        } else {
            callback.onDenied(requestCode);
        }
    }

    /**
     * 显示权限说明对话框
     */
    public static void showPermissionRationale(Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 权限回调接口
     */
    public interface PermissionCallback {
        void onGranted(int requestCode);
        void onDenied(int requestCode);
    }
}
