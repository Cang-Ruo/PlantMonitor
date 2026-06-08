package com.example.mygraduationproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.mygraduationproject.databinding.ActivityMainBinding;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.mqtt.MqttManager;
import com.example.mygraduationproject.ui.analysis.DetailActivity;
import com.example.mygraduationproject.ui.role.RoleSelectionActivity;
import com.example.mygraduationproject.utils.HistoryRecordsManager;
import com.example.mygraduationproject.utils.PermissionUtils;
import com.example.mygraduationproject.utils.PreferenceManager;

import org.json.JSONObject;

/** 涓籄ctivity锛氬簳閮ㄥ鑸€丮QTT杩炴帴銆佹秷鎭垎鍙戙€佹潈闄愮敵璇枫€佸巻鍙茶褰曞悓姝?*/
public class MainActivity extends AppCompatActivity implements MqttManager.MqttMessageListener, MqttManager.ConnectionStateListener {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private NavController navController;
    private PreferenceManager preferenceManager;
    private MqttManager mqttManager;
    private MqttManager.DeviceRole currentRole;
    private Handler mainHandler;
    private BroadcastReceiver historySyncReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("=== MainActivity onCreate 开始 ===");
        Log.e(TAG, "=== onCreate 开始 ===");
        
        mainHandler = new Handler(Looper.getMainLooper());
        preferenceManager = new PreferenceManager(this);
        
        System.out.println("=== hasSelectedRole: " + preferenceManager.hasSelectedRole() + " ===");
        Log.e(TAG, "hasSelectedRole: " + preferenceManager.hasSelectedRole());
        
        if (!preferenceManager.hasSelectedRole()) {
            Log.e(TAG, "跳转到角色选择页面");
            navigateToRoleSelection();
            return;
        }
        
        Log.e(TAG, "角色已选择: " + preferenceManager.getDeviceRole());
        
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            requestPermissions();
            setupNavigation();
            
            clearOldDataIfNeeded();
            
            Log.e(TAG, "延迟 500ms 后初始化 MQTT");
            mainHandler.postDelayed(this::setupMqtt, 500);
            Log.e(TAG, "onCreate 完成");
        } catch (Exception e) {
            Log.e(TAG, "MainActivity 初始化失败: " + e.getMessage(), e);
            finish();
        }
    }
    
    private void navigateToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void clearOldDataIfNeeded() {
        if (!preferenceManager.getBoolean("data_cleared_v8", false)) {
            Log.i(TAG, "首次启动v8版本，清除本地旧数据...");
            
            // 仅清除本地数据库，不清除云端OSS历史记录
            // 避免控制端启动时把监控端已上传的云端数据清空
            com.example.mygraduationproject.data.Repository repository = 
                    com.example.mygraduationproject.data.Repository.getInstance(this);
            if (repository != null) {
                repository.clearAllData();
            }
            
            preferenceManager.putBoolean("data_cleared_v8", true);
            Log.i(TAG, "本地旧数据清除完成");
        }
    }
    
    private void setupMqtt() {
        System.out.println("=== setupMqtt 开始 ===");
        Log.e(TAG, "=== setupMqtt 开始 ===");
        try {
            String roleValue = preferenceManager.getDeviceRole();
            Log.e(TAG, "角色值: " + roleValue);
            
            currentRole = "controller".equals(roleValue) ? 
                    MqttManager.DeviceRole.CONTROLLER : MqttManager.DeviceRole.MONITOR;
            
            Log.e(TAG, "获取 MqttManager 实例");
            mqttManager = MqttManager.getInstance(this);
            
            Log.e(TAG, "设置角色: " + currentRole.getValue());
            mqttManager.setRole(currentRole);
            
            Log.e(TAG, "添加监听器");
            mqttManager.addMessageListener(this);
            mqttManager.addConnectionListener(this);
            
            Log.e(TAG, "调用 connect()");
            mqttManager.connect();
            
            Log.e(TAG, "setupMqtt 完成");
        } catch (Exception e) {
            Log.e(TAG, "MQTT 初始化失败: " + e.getMessage(), e);
        }
    }
    
    private void requestPermissions() {
        if (!PermissionUtils.hasAllPermissions(this)) {
            PermissionUtils.requestAllPermissions(this);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        PermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults, 
                new PermissionUtils.PermissionCallback() {
            @Override
            public void onGranted(int requestCode) {
                Toast.makeText(MainActivity.this, "权限已授予", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(int requestCode) {
                Toast.makeText(MainActivity.this, "部分权限被拒绝，部分功能可能无法正常使用", Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
    
    @Override
    public void onMessageReceived(String topic, String payload) {
        Log.e(TAG, "收到MQTT消息 - 主题: " + topic + ", 内容: " + payload);
        
        runOnUiThread(() -> {
            handleMqttMessage(topic, payload);
        });
    }
    
    private void handleMqttMessage(String topic, String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            
            switch (topic) {
                case "cmd/photo":
                    handlePhotoCommand(json);
                    break;
                case "cmd/hardware":
                    handleHardwareCommand(json);
                    break;
                case "status/feedback":
                    handleStatusFeedback(json);
                    break;
                case "photo/sync":
                    handlePhotoSync(json);
                    break;
                case "chat/sync":
                    handleChatSync(json);
                    break;
                default:
                    Log.w(TAG, "未知主题: " + topic);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理MQTT消息失败: " + e.getMessage());
        }
    }
    
    private void handlePhotoCommand(JSONObject json) {
        Log.e(TAG, "处理拍照指令: " + json.toString());
        Toast.makeText(this, "收到远程拍照指令", Toast.LENGTH_SHORT).show();
    }
    
    private void handleHardwareCommand(JSONObject json) {
        Log.e(TAG, "处理硬件控制指令: " + json.toString());
        try {
            String device = json.optString("device", "");
            boolean state = json.optBoolean("state", false);
            Toast.makeText(this, "收到硬件控制: " + device + " -> " + (state ? "开" : "关"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "解析硬件指令失败: " + e.getMessage());
        }
    }
    
    private void handleStatusFeedback(JSONObject json) {
        Log.e(TAG, "处理状态反馈: " + json.toString());
        Toast.makeText(this, "收到设备状态反馈", Toast.LENGTH_SHORT).show();
    }
    
    private void handlePhotoSync(JSONObject json) {
        Log.e(TAG, "处理照片同步: " + json.toString());
        Toast.makeText(this, "收到照片同步消息", Toast.LENGTH_SHORT).show();
    }
    
    private void handleChatSync(JSONObject json) {
        Log.e(TAG, "处理聊天同步: " + json.toString());
        Toast.makeText(this, "收到聊天记录同步", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onConnected() {
        Log.e(TAG, "=== MQTT已连接 ===");
        runOnUiThread(() -> {
            Toast.makeText(this, "MQTT已连接", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onDisconnected() {
        Log.e(TAG, "=== MQTT已断开 ===");
        runOnUiThread(() -> {
            Toast.makeText(this, "MQTT连接断开", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onConnectionFailed(String error) {
        Log.e(TAG, "=== MQTT连接失败: " + error + " ===");
        final String errorMsg = error;
        runOnUiThread(() -> {
            Toast.makeText(this, "MQTT连接失败: " + errorMsg, Toast.LENGTH_LONG).show();
            System.out.println("=== MQTT连接失败: " + errorMsg + " ===");
        });
    }
    
    public MqttManager getMqttManager() {
        return mqttManager;
    }
    
    public MqttManager.DeviceRole getCurrentRole() {
        return currentRole;
    }
    
    public void navigateToDetail(AIResult result) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PLANT_NAME, result.getPlantName());
        intent.putExtra(DetailActivity.EXTRA_HEALTH_STATUS, result.getHealthStatus());
        intent.putExtra(DetailActivity.EXTRA_HEALTH_SCORE, result.getHealthScore());
        intent.putExtra(DetailActivity.EXTRA_WATER_TEMP, result.getWaterTemp());
        intent.putExtra(DetailActivity.EXTRA_AIR_TEMP, result.getAirTemp());
        intent.putExtra(DetailActivity.EXTRA_AIR_HUMIDITY, result.getAirHumidity());
        intent.putExtra(DetailActivity.EXTRA_IMAGE_PATH, result.getImagePath());
        intent.putExtra(DetailActivity.EXTRA_SUGGESTION, result.getSuggestion());
        intent.putExtra(DetailActivity.EXTRA_DETAILED_ANALYSIS, result.getDetailedAnalysis());
        intent.putExtra(DetailActivity.EXTRA_TIMESTAMP, result.getTimestamp());
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        
        if (currentRole == MqttManager.DeviceRole.CONTROLLER) {
            syncHistoryRecords();
        }
        
        registerHistorySyncReceiver();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        unregisterHistorySyncReceiver();
    }
    
    private void syncHistoryRecords() {
        Log.d(TAG, "开始同步历史记录");
        HistoryRecordsManager historyManager = HistoryRecordsManager.getInstance(this);
        historyManager.syncToLocalDatabase(new HistoryRecordsManager.SyncCallback() {
            @Override
            public void onSyncComplete(int newRecordsCount) {
                Log.d(TAG, "历史记录同步完成，新增 " + newRecordsCount + " 条");
                if (newRecordsCount > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                                "已同步 " + newRecordsCount + " 条历史记录", 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onSyncFailed(String error) {
                Log.e(TAG, "历史记录同步失败: " + error);
            }
        });
    }
    
    private void registerHistorySyncReceiver() {
        if (historySyncReceiver == null) {
            historySyncReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (HistoryRecordsManager.ACTION_HISTORY_SYNCED.equals(intent.getAction())) {
                        Log.d(TAG, "收到历史记录同步广播，刷新UI");
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                    "历史记录已更新", 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            };
            
            IntentFilter filter = new IntentFilter(HistoryRecordsManager.ACTION_HISTORY_SYNCED);
            registerReceiver(historySyncReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            Log.d(TAG, "历史记录同步广播接收器已注册");
        }
    }
    
    private void unregisterHistorySyncReceiver() {
        if (historySyncReceiver != null) {
            try {
                unregisterReceiver(historySyncReceiver);
                historySyncReceiver = null;
                Log.d(TAG, "历史记录同步广播接收器已注销");
            } catch (Exception e) {
                Log.e(TAG, "注销广播接收器失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        if (mqttManager != null) {
            mqttManager.removeMessageListener(this);
            mqttManager.removeConnectionListener(this);
        }
    }
}
