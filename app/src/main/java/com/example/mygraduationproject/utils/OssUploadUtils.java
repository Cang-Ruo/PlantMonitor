package com.example.mygraduationproject.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 阿里云OSS图片上传工具类
 * 用于将拍摄的照片上传到阿里云OSS存储
 * 
 * 重要提示：请替换为你的阿里云 OSS 配置信息
 */
public class OssUploadUtils {

    private static final String TAG = "OssUploadUtils";

    // ==================== OSS 配置参数 ====================
    // Bucket名称
    private static final String BUCKET_NAME = "YOUR_BUCKET_NAME";
    
    // OSS Endpoint（请替换为你的 Endpoint）
    private static final String ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com";
    
    // AccessKey ID（请替换为你的 AccessKey ID）
    private static final String ACCESS_KEY_ID = "YOUR_ACCESS_KEY_ID";
    
    // AccessKey Secret（请替换为你的 AccessKey Secret）
    private static final String ACCESS_KEY_SECRET = "YOUR_ACCESS_KEY_SECRET";
    
    // OSS存储目录前缀
    private static final String UPLOAD_DIR = "plant_photos/";

    // ==================== 单例模式 ====================
    private static OssUploadUtils instance;
    private OSS ossClient;
    private ExecutorService executorService;
    private Context context;

    private OssUploadUtils(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        initOssClient();
    }

    public static synchronized OssUploadUtils getInstance(Context context) {
        if (instance == null) {
            instance = new OssUploadUtils(context);
        }
        return instance;
    }

    /**
     * 初始化OSS客户端
     */
    private void initOssClient() {
        try {
            // 配置认证信息
            OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(
                    ACCESS_KEY_ID, ACCESS_KEY_SECRET);

            // 配置客户端
            ClientConfiguration conf = new ClientConfiguration();
            conf.setConnectionTimeout(15 * 1000); // 连接超时时间，默认15秒
            conf.setSocketTimeout(15 * 1000);     // Socket超时时间，默认15秒
            conf.setMaxConcurrentRequest(5);      // 最大并发请求数，默认5个
            conf.setMaxErrorRetry(2);             // 失败后最大重试次数，默认2次

            // 开启日志（调试时使用，发布时关闭）
            OSSLog.enableLog();

            // 创建OSS客户端实例
            ossClient = new OSSClient(context, ENDPOINT, credentialProvider, conf);

            Log.i(TAG, "OSS客户端初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "OSS客户端初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传照片到OSS
     * @param photoFile 要上传的照片文件
     * @param callback 上传回调
     */
    public void uploadPhoto(File photoFile, UploadCallback callback) {
        if (ossClient == null) {
            Log.e(TAG, "OSS客户端未初始化");
            if (callback != null) {
                callback.onFailed("OSS客户端未初始化");
            }
            return;
        }

        if (photoFile == null || !photoFile.exists()) {
            Log.e(TAG, "照片文件不存在");
            if (callback != null) {
                callback.onFailed("照片文件不存在");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                // 生成OSS对象名称（使用时间戳命名）
                String objectKey = generateObjectKey();

                // 创建上传请求
                PutObjectRequest putRequest = new PutObjectRequest(
                        BUCKET_NAME,
                        objectKey,
                        photoFile.getAbsolutePath()
                );
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("image/jpeg");
                metadata.setContentLength(photoFile.length());
                putRequest.setMetadata(metadata);

                // 设置上传进度回调
                putRequest.setProgressCallback((request, currentSize, totalSize) -> {
                    int progress = (int) (currentSize * 100 / totalSize);
                    Log.i(TAG, "上传进度: " + progress + "%");
                    if (callback != null) {
                        callback.onProgress(progress);
                    }
                });

                // 执行上传
                PutObjectResult result = ossClient.putObject(putRequest);

                // 上传成功
                String url = generateUrl(objectKey);
                Log.i(TAG, "上传成功，URL: " + url);

                if (callback != null) {
                    callback.onSuccess(url, objectKey);
                }

            } catch (Exception e) {
                Log.e(TAG, "上传失败: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onFailed("上传失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 上传照片（通过Uri）
     * @param uri 照片Uri
     * @param callback 上传回调
     */
    public void uploadPhotoFromUri(Uri uri, UploadCallback callback) {
        if (ossClient == null) {
            Log.e(TAG, "OSS客户端未初始化");
            if (callback != null) {
                callback.onFailed("OSS客户端未初始化");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                // 生成OSS对象名称
                String objectKey = generateObjectKey();

                // 从Uri获取输入流
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new Exception("无法打开文件流");
                }

                // 将InputStream转换为byte数组
                byte[] data = readStreamToBytes(inputStream);
                inputStream.close();

                // 创建上传请求
                PutObjectRequest putRequest = new PutObjectRequest(
                        BUCKET_NAME,
                        objectKey,
                        data
                );

                // 设置对象元数据
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("image/jpeg");
                putRequest.setMetadata(metadata);

                // 执行上传
                PutObjectResult result = ossClient.putObject(putRequest);

                // 上传成功
                String url = generateUrl(objectKey);
                Log.d(TAG, "上传成功，URL: " + url);

                if (callback != null) {
                    callback.onSuccess(url, objectKey);
                }

            } catch (Exception e) {
                Log.e(TAG, "上传失败: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onFailed("上传失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 将InputStream转换为byte数组
     */
    private byte[] readStreamToBytes(InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * 生成OSS对象名称
     * 格式：plant_photos/plant_yyyyMMdd_HHmmss.jpg
     */
    private String generateObjectKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        return UPLOAD_DIR + "plant_" + timestamp + ".jpg";
    }

    /**
     * 生成访问URL（带签名，有效期7天）
     */
    private String generateUrl(String objectKey) {
        try {
            String signedUrl = ossClient.presignConstrainedObjectURL(
                    BUCKET_NAME, 
                    objectKey, 
                    7 * 24 * 60 * 60
            );
            Log.d(TAG, "生成签名URL: " + signedUrl);
            return signedUrl;
        } catch (Exception e) {
            Log.e(TAG, "生成签名URL失败: " + e.getMessage());
            return "https://" + BUCKET_NAME + ".oss-cn-hangzhou.aliyuncs.com/" + objectKey;
        }
    }

    /**
     * 获取OSS访问URL（公开读）
     */
    public String getPublicUrl(String objectKey) {
        return "https://" + BUCKET_NAME + ".oss-cn-hangzhou.aliyuncs.com/" + objectKey;
    }

    /**
     * 删除OSS上的文件
     * @param objectKey 对象名称
     * @param callback 删除回调
     */
    public void deletePhoto(String objectKey, DeleteCallback callback) {
        if (ossClient == null) {
            if (callback != null) {
                callback.onFailed("OSS客户端未初始化");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                com.alibaba.sdk.android.oss.model.DeleteObjectRequest deleteRequest = 
                    new com.alibaba.sdk.android.oss.model.DeleteObjectRequest(BUCKET_NAME, objectKey);
                ossClient.deleteObject(deleteRequest);
                Log.i(TAG, "删除成功: " + objectKey);
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "删除失败: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onFailed("删除失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 释放资源
     */
    public void release() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        instance = null;
    }

    // ==================== 回调接口 ====================

    /**
     * 上传回调接口
     */
    public interface UploadCallback {
        void onSuccess(String url, String objectKey);
        void onFailed(String errorMsg);
        default void onProgress(int progress) {}
    }

    /**
     * 删除回调接口
     */
    public interface DeleteCallback {
        void onSuccess();
        void onFailed(String errorMsg);
    }
}
