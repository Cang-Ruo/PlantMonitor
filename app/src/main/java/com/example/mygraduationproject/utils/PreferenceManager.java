package com.example.mygraduationproject.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 偏好设置管理器，封装SharedPreferences的读写操作
 * 管理拍照间隔、服务器地址、API密钥、设备角色等配置项
 */
public class PreferenceManager {
    
    private static final String PREF_NAME = "plant_monitor_prefs"; // 偏好设置文件名
    
    private static final String KEY_CAPTURE_INTERVAL = "capture_interval"; // 拍照间隔数值
    private static final String KEY_CAPTURE_INTERVAL_UNIT = "capture_interval_unit"; // 拍照间隔单位（秒/分钟）
    private static final String KEY_CUSTOM_INTERVAL_SECONDS = "custom_interval_seconds"; // 自定义秒数间隔
    private static final String KEY_SERVER_URL = "server_url"; // 服务器地址
    private static final String KEY_AI_SERVICE_URL = "ai_service_url"; // AI服务地址
    private static final String KEY_BAIDU_API_KEY = "baidu_api_key"; // 百度AI API Key
    private static final String KEY_BAIDU_SECRET_KEY = "baidu_secret_key"; // 百度AI Secret Key
    private static final String KEY_QWEN_API_KEY = "qwen_api_key"; // 通义千问API Key
    private static final String KEY_AUTO_CAPTURE = "auto_capture"; // 自动拍照开关
    private static final String KEY_LAST_CAPTURE_TIME = "last_capture_time"; // 上次拍照时间
    private static final String KEY_DEVICE_IP = "device_ip"; // 设备IP地址
    private static final String KEY_DEVICE_PORT = "device_port"; // 设备端口
    private static final String KEY_DEVICE_ROLE = "device_role"; // 设备角色（monitor/controller）
    private static final String KEY_DATA_CLEARED_V8 = "data_cleared_v8"; // 数据清理标记
    
    private static final int DEFAULT_CAPTURE_INTERVAL = 15; // 默认拍照间隔15分钟
    private static final String DEFAULT_INTERVAL_UNIT = "minutes"; // 默认间隔单位为分钟
    private static final int DEFAULT_CUSTOM_SECONDS = 30; // 默认自定义秒数30秒
    private static final String DEFAULT_SERVER_URL = "http://192.168.1.100:8080"; // 默认服务器地址
    private static final String DEFAULT_AI_URL = "https://aip.baidubce.com"; // 默认百度AI服务地址
    
    private final SharedPreferences preferences; // SharedPreferences实例
    
    public PreferenceManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void setCaptureInterval(int value) {
        preferences.edit().putInt(KEY_CAPTURE_INTERVAL, value).apply();
    }
    
    public int getCaptureInterval() {
        return preferences.getInt(KEY_CAPTURE_INTERVAL, DEFAULT_CAPTURE_INTERVAL);
    }
    
    public void setIntervalUnit(String unit) {
        preferences.edit().putString(KEY_CAPTURE_INTERVAL_UNIT, unit).apply();
    }
    
    public String getIntervalUnit() {
        return preferences.getString(KEY_CAPTURE_INTERVAL_UNIT, DEFAULT_INTERVAL_UNIT);
    }
    
    public void setCustomIntervalSeconds(int seconds) {
        preferences.edit().putInt(KEY_CUSTOM_INTERVAL_SECONDS, seconds).apply();
    }
    
    public int getCustomIntervalSeconds() {
        return preferences.getInt(KEY_CUSTOM_INTERVAL_SECONDS, DEFAULT_CUSTOM_SECONDS);
    }
    
    /** 获取拍照间隔（统一转换为秒），分钟单位乘以60 */
    public int getIntervalInSeconds() {
        int value = getCaptureInterval();
        String unit = getIntervalUnit();
        
        if ("seconds".equals(unit)) {
            return value; // 秒单位直接返回
        } else {
            return value * 60; // 分钟单位转换为秒
        }
    }
    
    public void setServerUrl(String url) {
        preferences.edit().putString(KEY_SERVER_URL, url).apply();
    }
    
    public String getServerUrl() {
        return preferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }
    
    public void setAiServiceUrl(String url) {
        preferences.edit().putString(KEY_AI_SERVICE_URL, url).apply();
    }
    
    public String getAiServiceUrl() {
        return preferences.getString(KEY_AI_SERVICE_URL, DEFAULT_AI_URL);
    }
    
    public void setBaiduApiKey(String apiKey) {
        preferences.edit().putString(KEY_BAIDU_API_KEY, apiKey).apply();
    }
    
    public String getBaiduApiKey() {
        return preferences.getString(KEY_BAIDU_API_KEY, "");
    }
    
    public void setBaiduSecretKey(String secretKey) {
        preferences.edit().putString(KEY_BAIDU_SECRET_KEY, secretKey).apply();
    }
    
    public String getBaiduSecretKey() {
        return preferences.getString(KEY_BAIDU_SECRET_KEY, "");
    }
    
    public void setQwenApiKey(String apiKey) {
        preferences.edit().putString(KEY_QWEN_API_KEY, apiKey).apply();
    }
    
    public String getQwenApiKey() {
        return preferences.getString(KEY_QWEN_API_KEY, "");
    }
    
    public void setAutoCapture(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_CAPTURE, enabled).apply();
    }
    
    public boolean isAutoCaptureEnabled() {
        return preferences.getBoolean(KEY_AUTO_CAPTURE, false);
    }
    
    public void setLastCaptureTime(long time) {
        preferences.edit().putLong(KEY_LAST_CAPTURE_TIME, time).apply();
    }
    
    public long getLastCaptureTime() {
        return preferences.getLong(KEY_LAST_CAPTURE_TIME, 0);
    }
    
    public void setDeviceIp(String ip) {
        preferences.edit().putString(KEY_DEVICE_IP, ip).apply();
    }
    
    public String getDeviceIp() {
        return preferences.getString(KEY_DEVICE_IP, "192.168.1.100");
    }
    
    public void setDevicePort(int port) {
        preferences.edit().putInt(KEY_DEVICE_PORT, port).apply();
    }
    
    public int getDevicePort() {
        return preferences.getInt(KEY_DEVICE_PORT, 8080);
    }
    
    public void setDeviceRole(String role) {
        preferences.edit().putString(KEY_DEVICE_ROLE, role).apply();
    }
    
    public String getDeviceRole() {
        return preferences.getString(KEY_DEVICE_ROLE, "");
    }
    
    /** 判断用户是否已选择设备角色 */
    public boolean hasSelectedRole() {
        return preferences.contains(KEY_DEVICE_ROLE);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }
    
    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }
    
    public void clearAll() {
        preferences.edit().clear().apply();
    }
}
