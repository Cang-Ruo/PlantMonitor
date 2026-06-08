package com.example.mygraduationproject.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** CameraX鐩告満绠＄悊锛氶瑙堝拰鎷嶇収灏佽 */
public class CameraManager {
    
    private static final String TAG = "CameraManager";
    private static final int TARGET_WIDTH = 1920;
    private static final int TARGET_HEIGHT = 1080;
    
    private final Context context;
    private final ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private PreviewView previewView;
    private boolean isCameraStarted = false;
    
    public CameraManager(Context context) {
        this.context = context;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }
    
    public void startCamera(PreviewView previewView, LifecycleOwner lifecycleOwner) {
        this.previewView = previewView;
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(context);
        
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(lifecycleOwner);
                isCameraStarted = true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to start camera", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }
    
    private void bindCameraUseCases(LifecycleOwner lifecycleOwner) {
        if (cameraProvider == null) {
            return;
        }
        
        cameraProvider.unbindAll();
        
        Preview preview = new Preview.Builder()
                .build();
        
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(new android.util.Size(TARGET_WIDTH, TARGET_HEIGHT))
                .build();
        
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
        );
    }
    
    public void capturePhoto(File outputDir, CaptureCallback callback) {
        if (imageCapture == null) {
            callback.onError("Camera not initialized");
            return;
        }
        
        String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".jpg";
        File outputFile = new File(outputDir, fileName);
        
        imageCapture.takePicture(
                cameraExecutor,
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        try {
                            Bitmap bitmap = imageProxyToBitmap(image);
                            image.close();
                            
                            if (bitmap != null) {
                                saveBitmapToFile(bitmap, outputFile);
                                callback.onSuccess(outputFile);
                            } else {
                                callback.onError("Failed to process image");
                            }
                        } catch (Exception e) {
                            callback.onError("Failed to save image: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        callback.onError("Capture failed: " + exception.getMessage());
                    }
                }
        );
    }
    
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
            return null;
        }
        
        int rotationDegrees = image.getImageInfo().getRotationDegrees();
        if (rotationDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            return rotatedBitmap;
        }
        
        return bitmap;
    }
    
    private void saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        fos.flush();
        fos.close();
        bitmap.recycle();
    }
    
    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            isCameraStarted = false;
        }
    }
    
    public void shutdown() {
        stopCamera();
        cameraExecutor.shutdown();
    }
    
    public boolean isCameraStarted() {
        return isCameraStarted;
    }
    
    public interface CaptureCallback {
        void onSuccess(File imageFile);
        void onError(String errorMessage);
    }
}
