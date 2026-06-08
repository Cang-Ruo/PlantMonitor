package com.example.mygraduationproject.ui.monitor;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mygraduationproject.MainActivity;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.camera.CameraManager;
import com.example.mygraduationproject.data.Repository;
import com.example.mygraduationproject.databinding.FragmentMonitorBinding;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.GrowthRecord;
import com.example.mygraduationproject.model.PlantImage;
import com.example.mygraduationproject.model.PlantRecord;
import com.example.mygraduationproject.mqtt.MqttManager;
import com.example.mygraduationproject.network.ApiService;
import com.example.mygraduationproject.service.CameraService;
import com.example.mygraduationproject.utils.DateUtils;
import com.example.mygraduationproject.utils.GalleryUtils;
import com.example.mygraduationproject.utils.HistoryRecordsManager;
import com.example.mygraduationproject.utils.ImageUtils;
import com.example.mygraduationproject.utils.OssUploadUtils;
import com.example.mygraduationproject.utils.PreferenceManager;
import com.example.mygraduationproject.utils.HealthEvaluator;
import com.example.mygraduationproject.worker.AutoCaptureWorker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/** 监控端主界面：相机预览、拍照、AI识别、OSS上传、MQTT远程指令，支持花烛/白掌/绿萝健康评估 */
public class MonitorFragment extends Fragment {

    private static final String TAG = "MonitorFragment";

    private FragmentMonitorBinding binding; // 视图绑定实例
    private PreferenceManager preferenceManager; // 偏好设置管理器
    private CameraManager cameraManager; // CameraX相机管理器
    private ApiService apiService; // API服务（百度识别+通义千问）
    private Repository repository; // 数据仓库
    private HistoryRecordsManager historyRecordsManager; // 云端同步管理器
    private ActivityResultLauncher<String[]> requestPermissionLauncher; // 权限请求启动器
    private ActivityResultLauncher<String> pickImageLauncher; // 图片选择启动器
    private boolean isCameraReady = false; // 相机是否就绪
    private CountDownTimer countDownTimer; // 拍照倒计时器
    private boolean isCountingDown = false; // 是否正在倒计时
    private long remainingTimeMillis = 0; // 剩余倒计时毫秒数
    private String currentOssUrl = null; // 当前照片的OSS签名URL
    private boolean isRemoteCapture = false; // 是否为远程拍照
    private float capturedWaterTemp = 0f; // 拍照时缓存的水温
    private float capturedAirTemp = 0f; // 拍照时缓存的气温
    private float capturedAirHumidity = 0f; // 拍照时缓存的湿度
    private String currentPlantType = "其他"; // 当前植物类型（花烛/白掌/绿萝）
    private long lastGrowthRecordId = -1; // 最近一条生长记录的ID
    private long currentCaptureTimestamp = 0; // 当前拍照的统一时间戳（原子化绑定）
    private MqttManager mqttManager; // MQTT通信管理器
    private boolean remoteListenerSetup = false; // 远程指令监听是否已设置
    private MqttManager.ConnectionStateListener mqttConnectionListener; // MQTT连接状态监听器
    private HealthEvaluator healthEvaluator = new HealthEvaluator(); // 健康评估器

    /** 接收 AutoCaptureWorker 发来的本地广播，在前台时自动执行拍照 */
    private final BroadcastReceiver autoCaptureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AutoCaptureWorker.ACTION_AUTO_CAPTURE.equals(intent.getAction())) {
                Log.d(TAG, "Auto capture broadcast received");
                
                // 如果正在使用 CountDownTimer 倒计时，忽略广播（避免重复拍照）
                if (isCountingDown) {
                    Log.d(TAG, "CountDownTimer is running, ignoring broadcast");
                    return;
                }
                
                // 只有在不使用 CountDownTimer 时才响应广播（App 在后台时）
                if (hasCameraPermission()) {
                    Log.d(TAG, "Triggering capture from broadcast...");
                    capturePhoto();
                } else {
                    Log.w(TAG, "Auto capture skipped: camera permission not granted");
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean cameraGranted = result.get(Manifest.permission.CAMERA);
                    if (cameraGranted != null && cameraGranted) {
                        startCamera();
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), R.string.permission_camera_required, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedImage(uri);
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMonitorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initServices();
        setupViews();
        
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void initServices() {
        if (getContext() == null) return;
        
        preferenceManager = new PreferenceManager(getContext());
        repository = Repository.getInstance(getContext());
        apiService = new ApiService(getContext());
        cameraManager = new CameraManager(getContext());
        historyRecordsManager = HistoryRecordsManager.getInstance(getContext());
    }

    private void setupRemoteCommandListener() {
        if (remoteListenerSetup) return;
        
        if (preferenceManager != null && !"monitor".equals(preferenceManager.getDeviceRole())) {
            Log.e(TAG, "[远程指令] 非监控端角色，跳过远程指令监听");
            return;
        }
        
        if (getActivity() instanceof MainActivity) {
            mqttManager = ((MainActivity) getActivity()).getMqttManager();
            
            if (mqttManager != null) {
                Log.e(TAG, "[远程指令] mqttManager 已就绪，设置监听器");
                mqttManager.setRemoteCommandReceivedListener(new MqttManager.RemoteCommandReceivedListener() {
                    @Override
                    public void onCaptureNowCommand() {
                        Log.d(TAG, "收到立即拍照指令");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "收到远程拍照指令", Toast.LENGTH_SHORT).show();
                                isRemoteCapture = true;
                                capturePhoto();
                            });
                        }
                    }

                    @Override
                    public void onSetIntervalCommand(int intervalMinutes) {
                        Log.d(TAG, "收到设置间隔指令: " + intervalMinutes + " 分钟");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (preferenceManager != null) {
                                    preferenceManager.setCaptureInterval(intervalMinutes);
                                    updateIntervalDisplay();
                                    cancelTimerCountdown();
                                    if (preferenceManager.isAutoCaptureEnabled()) {
                                        startTimerCountdown();
                                    }
                                    cancelAutoCapture();
                                    scheduleAutoCapture();
                                    Toast.makeText(getContext(), "拍照间隔已设置为 " + intervalMinutes + " 分钟", Toast.LENGTH_SHORT).show();
                                    mqttManager.sendRemoteResponse("success", "间隔设置已更新为 " + intervalMinutes + " 分钟");
                                }
                            });
                        }
                    }

                    @Override
                    public void onToggleTimerCommand(boolean enabled) {
                        Log.d(TAG, "收到定时开关指令: " + (enabled ? "开启" : "关闭"));
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (preferenceManager != null) {
                                    preferenceManager.setAutoCapture(enabled);
                                    binding.switchAutoCapture.setChecked(enabled);
                                    if (enabled) {
                                        startTimerCountdown();
                                        if (getContext() != null) {
                                            CameraService.start(getContext());
                                        }
                                        Toast.makeText(getContext(), "远程开启定时拍照", Toast.LENGTH_SHORT).show();
                                    } else {
                                        cancelTimerCountdown();
                                        if (getContext() != null) {
                                            CameraService.stop(getContext());
                                        }
                                        Toast.makeText(getContext(), "远程关闭定时拍照", Toast.LENGTH_SHORT).show();
                                    }
                                    mqttManager.sendRemoteResponse("success", enabled ? "定时拍照已开启" : "定时拍照已关闭");
                                }
                            });
                        }
                    }
                });
                mqttManager.subscribeToRemoteCommands();
                remoteListenerSetup = true;
                Log.e(TAG, "[远程指令] 监听器设置完成，轮询已启动");
            } else {
                Log.e(TAG, "[远程指令] mqttManager 为 null，注册连接监听等待重试");
                registerMqttConnectionListener();
            }
        }
    }
    
    private void registerMqttConnectionListener() {
        if (mqttConnectionListener != null) return;
        
        mqttConnectionListener = new MqttManager.ConnectionStateListener() {
            @Override
            public void onConnected() {
                Log.e(TAG, "[远程指令] MQTT已连接，重新尝试设置监听器");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> setupRemoteCommandListener());
                }
            }

            @Override
            public void onDisconnected() {}

            @Override
            public void onConnectionFailed(String error) {}
        };
        
        if (getActivity() instanceof MainActivity) {
            MqttManager mm = ((MainActivity) getActivity()).getMqttManager();
            if (mm != null) {
                mm.addConnectionListener(mqttConnectionListener);
                Log.e(TAG, "[远程指令] 已注册连接监听器");
            } else {
                Log.e(TAG, "[远程指令] mqttManager 仍为 null，使用延迟重试");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (!remoteListenerSetup && getActivity() != null) {
                        MqttManager mm2 = ((MainActivity) getActivity()).getMqttManager();
                        if (mm2 != null) {
                            if (mm2.isConnected()) {
                                Log.e(TAG, "[远程指令] mqttManager 已连接，直接设置监听器");
                                getActivity().runOnUiThread(() -> setupRemoteCommandListener());
                            } else {
                                mm2.addConnectionListener(mqttConnectionListener);
                                Log.e(TAG, "[远程指令] 延迟注册连接监听器成功");
                            }
                        } else {
                            Log.e(TAG, "[远程指令] 延迟后 mqttManager 仍为 null");
                        }
                    }
                }, 2000);
            }
        }
    }
    
    private void unregisterMqttConnectionListener() {
        if (mqttConnectionListener != null && getActivity() instanceof MainActivity) {
            MqttManager mm = ((MainActivity) getActivity()).getMqttManager();
            if (mm != null) {
                mm.removeConnectionListener(mqttConnectionListener);
            }
            mqttConnectionListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isCameraReady && cameraManager != null && binding != null) {
            cameraManager.startCamera(binding.previewView, getViewLifecycleOwner());
        }
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter(AutoCaptureWorker.ACTION_AUTO_CAPTURE);
            LocalBroadcastManager.getInstance(getContext())
                    .registerReceiver(autoCaptureReceiver, filter);
        }
        if (!remoteListenerSetup) {
            setupRemoteCommandListener();
        }
    }

    private void setupViews() {
        if (binding == null || preferenceManager == null) return;
        
        updateIntervalDisplay();
        updateLastCaptureTime();
        
        binding.switchAutoCapture.setChecked(preferenceManager.isAutoCaptureEnabled());
        binding.switchAutoCapture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (preferenceManager != null) {
                preferenceManager.setAutoCapture(isChecked);
                if (isChecked) {
                    // 启动定时拍摄（显示倒计时）
                    startTimerCountdown();
                    // 启动后台服务（用于 App 在后台时继续拍照）
                    if (getContext() != null) {
                        CameraService.start(getContext());
                    }
                    Toast.makeText(getContext(), "自动拍摄已开启", Toast.LENGTH_SHORT).show();
                } else {
                    // 取消定时拍摄
                    cancelTimerCountdown();
                    // 停止后台服务
                    if (getContext() != null) {
                        CameraService.stop(getContext());
                    }
                    Toast.makeText(getContext(), "自动拍摄已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 取消倒计时按钮
        binding.btnCancelCountdown.setOnClickListener(v -> {
            cancelTimerCountdown();
            binding.switchAutoCapture.setChecked(false);
            Toast.makeText(getContext(), "已取消定时拍照", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnCapture.setOnClickListener(v -> {
            capturePhoto();
        });
        
        binding.btnUpload.setOnClickListener(v -> {
            pickImageFromGallery();
        });
    }

    private boolean hasCameraPermission() {
        if (getContext() == null) return false;
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
    }

    private void startCamera() {
        if (cameraManager != null && binding != null && getViewLifecycleOwner() != null) {
            cameraManager.startCamera(binding.previewView, getViewLifecycleOwner());
            isCameraReady = true;
        }
    }

    private void capturePhoto() {
        if (getContext() == null || binding == null) return;
        
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }
        
        if (cameraManager == null || !cameraManager.isCameraStarted()) {
            Toast.makeText(getContext(), "相机正在初始化，请稍候...", Toast.LENGTH_SHORT).show();
            startCamera();
            return;
        }
        
        showProgress(true);
        capturedWaterTemp = 0f;
        capturedAirTemp = 0f;
        capturedAirHumidity = 0f;
        currentCaptureTimestamp = 0;

        if (getActivity() instanceof MainActivity) {
            MqttManager mm = ((MainActivity) getActivity()).getMqttManager();
            if (mm != null && mm.isConnected()) {
                mm.querySensorData(new MqttManager.SensorDataCallback() {
                    @Override
                    public void onResult(float waterTemp, float airTemp, float airHumidity) {
                        capturedWaterTemp = waterTemp;
                        capturedAirTemp = airTemp;
                        capturedAirHumidity = airHumidity;
                        Log.d(TAG, "拍照时捕获传感器数据: 水温=" + waterTemp + " 气温=" + airTemp + " 湿度=" + airHumidity);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "拍照时查询传感器数据失败: " + error);
                    }
                });
            }
        }
        
        File outputDir = getContext().getFilesDir();
        
        cameraManager.capturePhoto(outputDir, new CameraManager.CaptureCallback() {
            @Override
            public void onSuccess(File imageFile) {
                currentOssUrl = null; // 重置OSS URL
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            String fileName = "plant_" + DateUtils.formatForFileName(System.currentTimeMillis()) + ".jpg";
                            File compressedFile = ImageUtils.compressImage(imageFile, outputDir, fileName);
                            
                            GalleryUtils.saveToGallery(getContext(), compressedFile);
                            
                            OssUploadUtils.getInstance(getContext()).uploadPhoto(compressedFile, new OssUploadUtils.UploadCallback() {
                                @Override
                                public void onSuccess(String url, String objectKey) {
                                    Log.d(TAG, "OSS上传成功: " + url);
                                    currentOssUrl = url;
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "照片已上传到云端", Toast.LENGTH_SHORT).show();
                                            
                                            if (getActivity() instanceof MainActivity) {
                                                MqttManager mm = ((MainActivity) getActivity()).getMqttManager();
                                                if (mm != null) {
                                                    mm.reportPhotoUrl(url);
                                                    Log.d(TAG, "已上报照片 URL 到 OneNET");
                                                }
                                            }

                                            saveGrowthRecord(url, capturedWaterTemp, capturedAirTemp, capturedAirHumidity);
                                        });
                                    }
                                }
                                
                                @Override
                                public void onFailed(String errorMsg) {
                                    Log.e(TAG, "OSS上传失败: " + errorMsg);
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "云端上传失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                                            saveGrowthRecord(null, capturedWaterTemp, capturedAirTemp, capturedAirHumidity);
                                        });
                                    }
                                }
                            });
                            
                            PlantImage plantImage = new PlantImage(compressedFile.getAbsolutePath(), compressedFile.length());
                            if (repository != null) {
                                repository.insertPlantImage(plantImage, id -> {
                                    plantImage.setId(id);
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            if (apiService != null && apiService.isApiKeyConfigured()) {
                                                analyzeImage(compressedFile, plantImage);
                                            } else {
                                                showProgress(false);
                                                Toast.makeText(getContext(), "提示：配置API Key后可启用AI识别功能", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                });
                            } else {
                                if (apiService != null && apiService.isApiKeyConfigured()) {
                                    analyzeImage(compressedFile, plantImage);
                                } else {
                                    showProgress(false);
                                }
                            }
                            
                            if (preferenceManager != null) {
                                preferenceManager.setLastCaptureTime(System.currentTimeMillis());
                            }
                            updateLastCaptureTime();
                            
                            Toast.makeText(getContext(), "照片已保存到相册", Toast.LENGTH_SHORT).show();
                            
                        } catch (Exception e) {
                            showProgress(false);
                            Toast.makeText(getContext(), "图片处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), "拍照失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void pickImageFromGallery() {
        pickImageLauncher.launch("image/*");
    }

    private void handleSelectedImage(Uri imageUri) {
        if (getContext() == null || binding == null) return;
        
        showProgress(true);
        capturedWaterTemp = 0f;
        capturedAirTemp = 0f;
        capturedAirHumidity = 0f;
        currentCaptureTimestamp = 0;

        if (getActivity() instanceof MainActivity) {
            MqttManager mm = ((MainActivity) getActivity()).getMqttManager();
            if (mm != null && mm.isConnected()) {
                mm.querySensorData(new MqttManager.SensorDataCallback() {
                    @Override
                    public void onResult(float waterTemp, float airTemp, float airHumidity) {
                        capturedWaterTemp = waterTemp;
                        capturedAirTemp = airTemp;
                        capturedAirHumidity = airHumidity;
                        Log.d(TAG, "上传时捕获传感器数据: 水温=" + waterTemp + " 气温=" + airTemp + " 湿度=" + airHumidity);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "上传时查询传感器数据失败: " + error);
                    }
                });
            }
        }
        
        new Thread(() -> {
            try {
                ContentResolver resolver = getContext().getContentResolver();
                InputStream inputStream = resolver.openInputStream(imageUri);
                if (inputStream == null) {
                    showError("无法读取图片");
                    return;
                }
                
                String fileName = "upload_" + DateUtils.formatForFileName(System.currentTimeMillis()) + ".jpg";
                File outputFile = new File(getContext().getFilesDir(), fileName);
                
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                
                File compressedFile = ImageUtils.compressImage(outputFile, getContext().getFilesDir(), "compressed_" + fileName);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentOssUrl = null;

                        OssUploadUtils.getInstance(getContext()).uploadPhoto(compressedFile, new OssUploadUtils.UploadCallback() {
                            @Override
                            public void onSuccess(String url, String objectKey) {
                                Log.d(TAG, "上传图片OSS成功: " + url);
                                currentOssUrl = url;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "图片已上传到云端", Toast.LENGTH_SHORT).show();

                                        if (getActivity() instanceof MainActivity) {
                                            MqttManager mm = ((MainActivity) getActivity()).getMqttManager();
                                            if (mm != null) {
                                                mm.reportPhotoUrl(url);
                                            }
                                        }

                                        saveGrowthRecord(url, capturedWaterTemp, capturedAirTemp, capturedAirHumidity);
                                    });
                                }
                            }

                            @Override
                            public void onFailed(String errorMsg) {
                                Log.e(TAG, "上传图片OSS失败: " + errorMsg);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "云端上传失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                                        saveGrowthRecord(null, capturedWaterTemp, capturedAirTemp, capturedAirHumidity);
                                    });
                                }
                            }
                        });

                        PlantImage plantImage = new PlantImage(compressedFile.getAbsolutePath(), compressedFile.length());
                        if (repository != null) {
                            repository.insertPlantImage(plantImage, id -> {
                                plantImage.setId(id);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (apiService != null && apiService.isApiKeyConfigured()) {
                                            analyzeImage(compressedFile, plantImage);
                                        } else {
                                            showProgress(false);
                                            Toast.makeText(getContext(), "提示：配置API Key后可启用AI识别功能", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            });
                        } else {
                            if (apiService != null && apiService.isApiKeyConfigured()) {
                                analyzeImage(compressedFile, plantImage);
                            } else {
                                showProgress(false);
                            }
                        }
                        
                        if (preferenceManager != null) {
                            preferenceManager.setLastCaptureTime(System.currentTimeMillis());
                        }
                        updateLastCaptureTime();
                        
                        Toast.makeText(getContext(), "图片已保存", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                showError("图片处理失败: " + e.getMessage());
            }
        }).start();
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showProgress(boolean show) {
        if (binding == null) return;
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnCapture.setEnabled(!show);
        binding.btnUpload.setEnabled(!show);
    }

    private void analyzeImage(File imageFile, PlantImage plantImage) {
        if (apiService == null || plantImage == null) return;
        
        apiService.identifyPlant(imageFile, new ApiService.PlantIdentifyCallback() {
            @Override
            public void onSuccess(List<AIResult> results) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        
                        if (results == null || results.isEmpty()) {
                            Toast.makeText(getContext(), "识别失败，请重试", Toast.LENGTH_SHORT).show();
                            // 远程拍照失败时发送失败响应
                            if (isRemoteCapture) {
                                sendRemoteCaptureResponse(false, "识别失败");
                                isRemoteCapture = false;
                            }
                            return;
                        }
                        
                        AIResult result = results.get(0);
                        result.setImageId(plantImage.getId());
                        result.setImagePath(imageFile.getAbsolutePath());
                        result.setWaterTemp(capturedWaterTemp);
                        result.setAirTemp(capturedAirTemp);
                        result.setAirHumidity(capturedAirHumidity);
                        result.setPlantType(normalizePlantType(result.getPlantName()));
                        if (currentCaptureTimestamp > 0) {
                            result.setTimestamp(currentCaptureTimestamp);
                        }
                        currentPlantType = result.getPlantType();

                        HealthEvaluator.EnvironmentData envData = new HealthEvaluator.EnvironmentData(
                                capturedWaterTemp, capturedAirHumidity, capturedAirTemp, (float) result.getConfidence());
                        float healthScore = healthEvaluator.calculateHealthScore(envData, result.getPlantType());
                        String healthDesc = healthEvaluator.getStatusDescription(healthScore);
                        result.setHealthScore(healthScore);
                        result.setHealthStatus(healthDesc);
                        Log.d(TAG, "健康评分计算: 水温=" + capturedWaterTemp + " 湿度=" + capturedAirHumidity + " 气温=" + capturedAirTemp + " AI置信度=" + result.getConfidence() + " → 评分=" + healthScore + " 状态=" + healthDesc);
                        
                        if (repository != null) {
                            repository.insertAIResult(result, id -> {});
                            if (lastGrowthRecordId > 0) {
                                repository.updateGrowthRecordTypeAndScore(lastGrowthRecordId, result.getPlantType(), healthScore, healthDesc);
                                Log.d(TAG, "更新生长记录plantType: ID=" + lastGrowthRecordId + " plantType=" + result.getPlantType() + " healthScore=" + healthScore);
                            }
                        }
                        
                        saveToHistoryRecords(result, imageFile);
                        
                        StringBuilder msg = new StringBuilder();
                        msg.append("识别完成: ").append(result.getPlantName());
                        msg.append(" (").append(String.format("%.0f%%", result.getConfidence() * 100)).append(")");
                        
                        if (results.size() > 1) {
                            msg.append("\n还有 ").append(results.size() - 1).append(" 个候选结果");
                        }
                        
                        Toast.makeText(getContext(), msg.toString(), Toast.LENGTH_SHORT).show();
                        
                        // 远程拍照成功时发送响应和同步结果
                        if (isRemoteCapture) {
                            sendRemoteCaptureResponse(true, "拍照识别完成");
                            // 同步结果到photo/sync主题
                            syncPhotoResult(result, currentOssUrl);
                            isRemoteCapture = false;
                        }
                    });
                }
            }
            
            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        // 远程拍照失败时发送失败响应
                        if (isRemoteCapture) {
                            sendRemoteCaptureResponse(false, message);
                            isRemoteCapture = false;
                        }
                    });
                }
            }
        });
    }
    
    private void sendRemoteCaptureResponse(boolean success, String message) {
        if (mqttManager != null) {
            String status = success ? "success" : "failed";
            mqttManager.sendRemoteResponse(status, message);
            Log.d(TAG, "发送远程拍照响应: " + status + " - " + message);
        }
    }
    
    private void syncPhotoResult(AIResult result, String imageUrl) {
        if (mqttManager == null || result == null) return;
        
        try {
            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("plantName", result.getPlantName());
            payload.put("confidence", result.getConfidence());
            payload.put("healthStatus", result.getHealthStatus());
            payload.put("suggestion", result.getSuggestion());
            payload.put("detailedAnalysis", result.getDetailedAnalysis());
            payload.put("imageUrl", imageUrl != null ? imageUrl : "");
            payload.put("timestamp", System.currentTimeMillis());
            
            mqttManager.syncPhotoResult(payload.toString());
            Log.d(TAG, "同步照片结果到photo/sync: " + payload.toString());
        } catch (Exception e) {
            Log.e(TAG, "同步照片结果失败: " + e.getMessage());
        }
    }
    
    private void saveToHistoryRecords(AIResult result, File imageFile) {
        if (historyRecordsManager == null || result == null) return;
        
        String imageUrl = currentOssUrl; // 优先使用OSS上传成功的URL
        if (imageUrl == null || imageUrl.isEmpty()) {
            // 如果OSS上传失败或未完成，使用本地文件名
            if (imageFile != null && imageFile.exists()) {
                String fileName = imageFile.getName();
                imageUrl = "https://plant-monitor-2026.oss-cn-hangzhou.aliyuncs.com/plant_photos/" + fileName;
            }
        }
        
        String analysisText = result.getDetailedAnalysis();
        if (analysisText == null || analysisText.isEmpty()) {
            analysisText = result.getSuggestion();
        }
        if (analysisText == null || analysisText.isEmpty()) {
            analysisText = "健康状态: " + (result.getHealthStatus() != null ? result.getHealthStatus() : "未知");
        }
        
        PlantRecord record = new PlantRecord(
                imageUrl,
                result.getPlantName() != null ? result.getPlantName() : "未知植物",
                analysisText,
                currentCaptureTimestamp > 0 ? currentCaptureTimestamp : System.currentTimeMillis()
        );
        record.setWaterTemp(result.getWaterTemp());
        record.setAirTemp(result.getAirTemp());
        record.setAirHumidity(result.getAirHumidity());
        record.setHealthScore(result.getHealthScore());
        record.setHealthStatus(result.getHealthStatus());
        record.setPlantType(result.getPlantType());
        record.setConfidence(result.getConfidence());
        
        historyRecordsManager.saveRecord(record, new HistoryRecordsManager.SaveRecordCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "历史记录保存成功");
                sendPhotoSyncMessage(record);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "历史记录保存失败: " + error);
            }
        });
    }
    
    private void sendPhotoSyncMessage(PlantRecord record) {
        if (getActivity() instanceof MainActivity) {
            MqttManager mqttManager = ((MainActivity) getActivity()).getMqttManager();
            if (mqttManager != null && mqttManager.isConnected()) {
                mqttManager.reportPhotoUrl(record.getImageUrl());
                Log.d(TAG, "已发送照片同步消息: " + record.getImageUrl());
            }
        }
    }

    private void saveGrowthRecord(String imageUrl, float waterTemp, float airTemp, float airHumidity) {
        if (repository == null) return;
        currentCaptureTimestamp = System.currentTimeMillis();
        GrowthRecord record = new GrowthRecord();
        record.setImageUrl(imageUrl);
        record.setWaterTemp(waterTemp);
        record.setAirTemp(airTemp);
        record.setAirHumidity(airHumidity);
        record.setTimestamp(currentCaptureTimestamp);

        HealthEvaluator.EnvironmentData envData = new HealthEvaluator.EnvironmentData(
                waterTemp, airHumidity, airTemp, 0.8f);
        float healthScore = healthEvaluator.calculateHealthScore(envData, currentPlantType);
        String healthDesc = healthEvaluator.getStatusDescription(healthScore);
        record.setHealthScore(healthScore);
        record.setHealthStatus(healthDesc);
        record.setPlantType(currentPlantType);

        repository.insertGrowthRecord(record, id -> {
            lastGrowthRecordId = id;
            Log.d(TAG, "生长记录已保存，ID=" + id + " plantType=" + currentPlantType + " 水温=" + waterTemp + " 气温=" + airTemp + " 湿度=" + airHumidity + " 健康评分=" + healthScore);
        });
    }

    private String normalizePlantType(String plantName) {
        if (plantName == null) return "其他";
        if (plantName.contains("红掌") || plantName.contains("红烛") || plantName.contains("花烛")) return "花烛";
        if (plantName.contains("白掌")) return "白掌";
        if (plantName.contains("绿萝")) return "绿萝";
        return "其他";
    }

    private void scheduleAutoCapture() {
        if (getContext() == null || preferenceManager == null) return;
        int interval = preferenceManager.getCaptureInterval();
        AutoCaptureWorker.scheduleAutoCapture(getContext(), interval);
    }

    private void cancelAutoCapture() {
        if (getContext() == null) return;
        AutoCaptureWorker.cancelAutoCapture(getContext());
    }
    
    /**
     * 启动定时器倒计时显示
     * 根据用户设定的时间（比如15分钟）显示倒计时
     * 倒计时结束后自动拍照，然后重新开始倒计时
     */
    private void startTimerCountdown() {
        if (binding == null || getContext() == null || preferenceManager == null) return;
        
        // 获取用户设定的间隔时间（秒）
        int intervalSeconds = preferenceManager.getIntervalInSeconds();
        remainingTimeMillis = intervalSeconds * 1000L;
        
        isCountingDown = true;
        binding.countdownOverlay.setVisibility(View.VISIBLE);
        
        // 创建倒计时器
        countDownTimer = new CountDownTimer(remainingTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMillis = millisUntilFinished;
                if (binding != null) {
                    // 计算剩余时间
                    long seconds = millisUntilFinished / 1000;
                    long minutes = seconds / 60;
                    long secs = seconds % 60;
                    
                    // 显示倒计时
                    if (minutes > 0) {
                        binding.tvCountdown.setText(String.format("%d:%02d", minutes, secs));
                    } else {
                        binding.tvCountdown.setText(String.valueOf(secs));
                    }
                }
            }
            
            @Override
            public void onFinish() {
                if (binding != null) {
                    binding.countdownOverlay.setVisibility(View.GONE);
                }
                isCountingDown = false;
                
                // 倒计时结束，执行拍照
                if (hasCameraPermission()) {
                    capturePhoto();
                } else {
                    Toast.makeText(getContext(), "缺少相机权限，无法拍照", Toast.LENGTH_SHORT).show();
                }
                
                // 拍照后重新开始倒计时（循环定时拍摄）
                if (preferenceManager != null && preferenceManager.isAutoCaptureEnabled()) {
                    startTimerCountdown();
                }
            }
        };
        
        countDownTimer.start();
    }
    
    /**
     * 取消定时器倒计时
     */
    private void cancelTimerCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isCountingDown = false;
        remainingTimeMillis = 0;
        
        if (binding != null) {
            binding.countdownOverlay.setVisibility(View.GONE);
        }
    }

    private void updateIntervalDisplay() {
        if (binding == null || preferenceManager == null || getContext() == null) return;
        int seconds = preferenceManager.getIntervalInSeconds();
        String intervalText;
        
        if (seconds < 60) {
            intervalText = seconds + getString(R.string.seconds);
        } else {
            int minutes = seconds / 60;
            intervalText = minutes + getString(R.string.minutes);
        }
        
        binding.tvIntervalValue.setText(intervalText);
    }

    private void updateLastCaptureTime() {
        if (binding == null || preferenceManager == null) return;
        long lastCapture = preferenceManager.getLastCaptureTime();
        if (lastCapture > 0) {
            binding.tvLastCaptureTime.setText(DateUtils.getRelativeTime(lastCapture));
        } else {
            binding.tvLastCaptureTime.setText("--");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraManager != null) {
            cameraManager.stopCamera();
            isCameraReady = false;
        }
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext())
                    .unregisterReceiver(autoCaptureReceiver);
        }
        if (isCountingDown) {
            cancelTimerCountdown();
            if (binding != null) {
                binding.switchAutoCapture.setChecked(false);
            }
        }
        unregisterMqttConnectionListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelTimerCountdown();
        binding = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimerCountdown();
        if (cameraManager != null) {
            cameraManager.shutdown();
        }
    }
}
