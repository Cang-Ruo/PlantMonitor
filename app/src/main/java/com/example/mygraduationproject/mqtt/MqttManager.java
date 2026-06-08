package com.example.mygraduationproject.mqtt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.example.mygraduationproject.config.ApiConfig;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * MQTT通信管理器，负责与OneNET物联网平台的MQTT连接、消息收发和设备控制
 * 支持监控端和控制端两种角色，提供传感器数据查询、远程拍照指令、属性上报等功能
 * 花烛，白掌、绿萝等植物的环境数据通过此管理器从M5硬件设备获取
 */
public class MqttManager {

    private static final String TAG = "MqttManager";

    // ==================== MQTT 连接配置 ====================
    // 请替换为你自己的 OneNET MQTT 服务器地址
    private static final String MQTT_SERVER = "tcp://YOUR_MQTT_SERVER:1883";
    
    // 替换为你的 OneNET 产品 ID
    private static final String PRODUCT_ID = "YOUR_PRODUCT_ID";
    
    // 替换为你的 M5 设备名称
    private static final String M5_DEVICE_NAME = "YOUR_M5_DEVICE_NAME";

    // 监控端配置
    private static final String MONITOR_CLIENT_ID = "Phone_Monitor"; // 监控端设备名
    
    // 替换为你的监控端 MQTT Token（从 OneNET 获取）
    private static final String MONITOR_TOKEN = "YOUR_MONITOR_TOKEN";

    // 控制端配置
    private static final String CONTROLLER_CLIENT_ID = "Phone_Controller"; // 控制端设备名
    
    // 替换为你的控制端 MQTT Token（从 OneNET 获取）
    private static final String CONTROLLER_TOKEN = "YOUR_CONTROLLER_TOKEN";

    private static MqttManager instance; // 单例实例
    private MqttAsyncClient mqttClient; // MQTT异步客户端
    private OkHttpClient httpClient; // HTTP客户端，用于OneNET API调用
    private Context context; // 应用上下文
    private DeviceRole currentRole = DeviceRole.MONITOR; // 当前设备角色（监控端/控制端）
    private volatile boolean isConnected = false; // MQTT连接状态标记
    private Handler mainHandler; // 主线程Handler，用于UI线程回调
    private ExecutorService executorService; // 单线程执行器
    private java.util.concurrent.ScheduledExecutorService scheduledExecutor; // 定时调度器，用于轮询远程指令
    private String apiToken; // OneNET API认证Token
    private java.util.concurrent.ScheduledFuture<?> commandPoller; // 轮询任务的定时调度句柄
    private long lastCommandTimestamp = 0; // 上次处理的远程指令时间戳，用于判断是否为新指令
    private String lastAction = ""; // 上次处理的远程指令动作，用于判断指令变化
    private String lastPolledRawResponse = ""; // 上次轮询的原始响应
    private volatile float cachedWaterTemp = 0f; // 缓存的水温数据（花烛/白掌/绿萝的水培水温）
    private volatile float cachedAirTemp = 0f; // 缓存的气温数据
    private volatile float cachedAirHumidity = 0f; // 缓存的空气湿度数据
    private volatile long lastSensorUpdateTime = 0; // 上次传感器数据更新时间
    private int reconnectAttempts = 0; // 当前重连尝试次数
    private static final int MAX_RECONNECT_ATTEMPTS = 10; // 最大重连尝试次数
    private Runnable pendingReconnect = null; // 待执行的重连任务

    private final List<MqttMessageListener> messageListeners = new ArrayList<>(); // 消息监听器列表
    private final List<ConnectionStateListener> connectionListeners = new ArrayList<>(); // 连接状态监听器列表

    /** 获取当前角色的设备名称 */
    private String getCurrentDeviceName() {
        return currentRole == DeviceRole.MONITOR ? MONITOR_CLIENT_ID : CONTROLLER_CLIENT_ID;
    }

    /** 设备角色枚举：监控端或控制端 */
    public enum DeviceRole {
        MONITOR("monitor"),   // 监控端：负责拍照和传感器数据采集
        CONTROLLER("controller"); // 控制端：负责远程控制和数据查看

        private final String value;

        DeviceRole(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /** MQTT消息监听接口 */
    public interface MqttMessageListener {
        void onMessageReceived(String topic, String payload);
    }

    /** 连接状态监听接口 */
    public interface ConnectionStateListener {
        void onConnected();     // 连接成功
        void onDisconnected();  // 连接断开
        void onConnectionFailed(String error); // 连接失败
    }

    private MqttManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        
        // 生成 API Token
        try {
            apiToken = generateApiToken();
            logError("API Token 生成成功: " + apiToken);
        } catch (Exception e) {
            logError("API Token 生成失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        logError("MqttManager 初始化完成");
    }

    /**
     * 生成 OneNET API Token
     * 使用产品 AccessKey，res 格式为 products/{productid}
     */
    private String generateApiToken() throws Exception {
        String version = "2022-05-01";
        String resourceName = "products/" + ApiConfig.ONENET_PRODUCT_ID;  // 产品格式
        long expirationTime = System.currentTimeMillis() / 1000 + 365 * 24 * 60 * 60; // 一年有效期
        String signatureMethod = "sha256";
        String accessKey = ApiConfig.ONENET_API_KEY;

        
        logError("生成Token参数:");
        logError("  version: " + version);
        logError("  resourceName: " + resourceName);
        logError("  expirationTime: " + expirationTime);
        logError("  signatureMethod: " + signatureMethod);

        
        String res = URLEncoder.encode(resourceName, "UTF-8");
        String encryptText = expirationTime + "\n" + signatureMethod + "\n" + resourceName + "\n" + version;
        
        byte[] keyBytes = Base64.decode(accessKey, Base64.DEFAULT);
        SecretKeySpec signinKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signinKey);
        byte[] signatureBytes = mac.doFinal(encryptText.getBytes());
        String signature = Base64.encodeToString(signatureBytes, Base64.DEFAULT).trim();
        String sig = URLEncoder.encode(signature, "UTF-8");

        StringBuilder sb = new StringBuilder();
        sb.append("version=").append(version)
          .append("&res=").append(res)
          .append("&et=").append(expirationTime)
          .append("&method=").append(signatureMethod)
          .append("&sign=").append(sig);

        
        String token = sb.toString();
        logError("生成的Token: " + token);
        return token;
    }

    public static synchronized MqttManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttManager(context);
        }
        return instance;
    }
    
    private void logError(String msg) {
        Log.e(TAG, msg);
    }
    
    private void logInfo(String msg) {
        Log.i(TAG, msg);
        Log.e(TAG, "[INFO] " + msg);
    }

    public void setRole(DeviceRole role) {
        this.currentRole = role;
        logInfo("设备角色设置为: " + role.getValue());
    }

    public DeviceRole getRole() {
        return currentRole;
    }

    private String getClientId() {
        return currentRole == DeviceRole.MONITOR ? MONITOR_CLIENT_ID : CONTROLLER_CLIENT_ID;
    }

    private String getToken() {
        return currentRole == DeviceRole.MONITOR ? MONITOR_TOKEN : CONTROLLER_TOKEN;
    }

    /** 连接MQTT服务器，异步执行 */
    public void connect() {
        logError("connect() 被调用, 角色: " + currentRole.getValue());
        executorService.execute(() -> {
            try {
                connectInternal();
            } catch (Exception e) {
                logError("连接线程异常: " + e.getMessage());
                e.printStackTrace();
                mainHandler.post(() -> notifyConnectionListenersFailed("连接异常: " + e.getMessage()));
            }
        });
    }

    private synchronized void connectInternal() {
        logError("connectInternal() 开始执行");
        
        if (mqttClient != null && mqttClient.isConnected()) {
            logInfo("MQTT 已连接，跳过连接");
            return;
        }
        
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
            } catch (Exception e) {
                logError("关闭旧客户端失败: " + e.getMessage());
            }
            mqttClient = null;
        }

        try {
            String clientId = getClientId();
            String token = getToken();
            
            logError("创建 MQTT 异步客户端");
            logError("服务器: " + MQTT_SERVER);
            logError("ClientId: " + clientId);
            
            mqttClient = new MqttAsyncClient(
                    MQTT_SERVER,
                    clientId,
                    new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(PRODUCT_ID);
            options.setPassword(token.toCharArray());
            options.setCleanSession(true);
            options.setKeepAliveInterval(120);
            options.setConnectionTimeout(60);
            options.setAutomaticReconnect(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logError("MQTT 连接丢失: " + (cause != null ? cause.getMessage() : "未知原因"));
                    if (cause != null) {
                        cause.printStackTrace();
                    }
                    isConnected = false;
                    mainHandler.post(() -> {
                        Toast.makeText(context, "MQTT连接断开，自动重连中...", Toast.LENGTH_SHORT).show();
                        notifyConnectionListenersDisconnected();
                    });
                    scheduleReconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    logInfo("收到消息 - 主题: " + topic + ", 内容: " + payload);
                    
                    if (topic.contains("/thing/property/set") && !topic.contains("/set_reply")) {
                        logError("[MQTT接收] 属性设置消息");
                        handlePropertySetMessage(topic, payload);
                    }
                    
                    if (topic.contains("/cmd/response")) {
                        logError("[MQTT接收] 远程指令响应");
                        handleRemoteResponse(payload);
                    }

                    if (topic.contains("/" + M5_DEVICE_NAME + "/thing/property/post")) {
                        updateSensorCacheFromMqtt(payload);
                    }
                    
                    mainHandler.post(() -> notifyMessageListeners(topic, payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    logInfo("消息发送完成");
                }
            });

            logError("开始执行 MQTT 异步连接...");
            
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logError("MQTT 连接成功!");
                    isConnected = true;
                    reconnectAttempts = 0;
                    if (pendingReconnect != null) {
                        mainHandler.removeCallbacks(pendingReconnect);
                        pendingReconnect = null;
                    }
                    subscribeToTopics();
                    mainHandler.post(() -> {
                        Toast.makeText(context, "MQTT连接成功!", Toast.LENGTH_LONG).show();
                        notifyConnectionListenersConnected();
                    });
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logError("MQTT 连接失败: " + (exception != null ? exception.getMessage() : "未知"));
                    isConnected = false;
                    String errorMsg = exception != null ? exception.getMessage() : "连接失败";
                    mainHandler.post(() -> {
                        Toast.makeText(context, "MQTT连接失败，5秒后重连:\n" + errorMsg, Toast.LENGTH_LONG).show();
                        notifyConnectionListenersFailed(errorMsg);
                    });
                    mainHandler.postDelayed(() -> {
                        if (!isConnected && mqttClient != null) {
                            logError("MQTT 自动重连中...");
                            try {
                                mqttClient.reconnect();
                            } catch (Exception e) {
                                logError("MQTT 重连异常: " + e.getMessage());
                            }
                        }
                    }, 5000);
                }
            });
            
        } catch (MqttException e) {
            logError("MQTT 连接异常 MqttException:");
            logError("  错误码: " + e.getReasonCode());
            logError("  消息: " + e.getMessage());
            e.printStackTrace();
            isConnected = false;
            final String errorMsg = "MqttException: " + e.getMessage();
            mainHandler.post(() -> {
                Toast.makeText(context, "MQTT连接失败:\n" + errorMsg, Toast.LENGTH_LONG).show();
                notifyConnectionListenersFailed(errorMsg);
            });
        } catch (Exception e) {
            logError("MQTT 初始化异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            isConnected = false;
            final String errorMsg = "初始化失败: " + e.getMessage();
            mainHandler.post(() -> {
                Toast.makeText(context, "MQTT连接失败:\n" + errorMsg, Toast.LENGTH_LONG).show();
                notifyConnectionListenersFailed(errorMsg);
            });
        }
    }

    private void subscribeToTopics() {
        if (!isConnected || mqttClient == null) {
            logError("MQTT 未连接，无法订阅主题");
            return;
        }

        try {
            String clientId = getClientId();
            
            java.util.List<String> topicList = new java.util.ArrayList<>();
            topicList.add("$sys/" + PRODUCT_ID + "/" + clientId + "/cmd/request/+");
            topicList.add("$sys/" + PRODUCT_ID + "/" + clientId + "/thing/property/post");
            topicList.add("$sys/" + PRODUCT_ID + "/" + clientId + "/thing/property/set");
            topicList.add("$sys/" + PRODUCT_ID + "/" + M5_DEVICE_NAME + "/thing/property/post");
            
            String[] topics = topicList.toArray(new String[0]);
            
            for (String topic : topics) {
                mqttClient.subscribe(topic, 1, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        logError("订阅成功: " + topic);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        logError("订阅失败: " + topic + " - " + exception.getMessage());
                    }
                });
            }
            
            logError("主题订阅完成");
        } catch (MqttException e) {
            logError("订阅主题失败: " + e.getMessage());
        }
    }

    private void updateSensorCacheFromMqtt(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            JSONObject params = json.optJSONObject("params");
            if (params == null) return;

            JSONObject waterTempObj = params.optJSONObject("water_temp");
            if (waterTempObj != null) cachedWaterTemp = (float) waterTempObj.optDouble("value", 0);

            JSONObject airTempObj = params.optJSONObject("air_temp");
            if (airTempObj != null) cachedAirTemp = (float) airTempObj.optDouble("value", 0);

            JSONObject airHumObj = params.optJSONObject("air_hum");
            if (airHumObj != null) cachedAirHumidity = (float) airHumObj.optDouble("value", 0);

            lastSensorUpdateTime = System.currentTimeMillis();
            logError("[MQTT接收] 更新传感器缓存: 水温=" + cachedWaterTemp + " 气温=" + cachedAirTemp + " 湿度=" + cachedAirHumidity);
        } catch (Exception e) {
            logError("[MQTT接收] 解析传感器数据异常: " + e.getMessage());
        }
    }

    private void handlePropertySetMessage(String topic, String payload) {
        logError("=== 开始处理属性设置消息 ===");
        logError("topic: " + topic);
        logError("payload: " + payload);
        
        try {
            JSONObject json = new JSONObject(payload);
            String id = json.optString("id", "");
            
            logError("解析成功, id=" + id);
            
            JSONObject params = json.optJSONObject("params");
            
            if (currentRole == DeviceRole.MONITOR && params != null && params.has("remote_action")) {
                String action = params.optString("remote_action", "");
                logError("[拍照执行] 监控端收到远程指令: " + action);
                
                if ("CAPTURE_NOW".equals(action)) {
                    logError("[拍照执行] 触发立即拍照");
                    if (remoteCommandReceivedListener != null) {
                        mainHandler.post(() -> {
                            logError("[拍照执行] 调用onCaptureNowCommand");
                            remoteCommandReceivedListener.onCaptureNowCommand();
                        });
                    }
                } else if ("SET_INTERVAL".equals(action)) {
                    int interval = params.optInt("remote_value", 30);
                    logError("[拍照执行] 设置间隔: " + interval + "分钟");
                    if (remoteCommandReceivedListener != null) {
                        final int finalInterval = interval;
                        mainHandler.post(() -> remoteCommandReceivedListener.onSetIntervalCommand(finalInterval));
                    }
                }
            } else if (currentRole == DeviceRole.CONTROLLER && params != null && params.has("remote_action")) {
                logError("[控制端] 忽略自己发送的远程指令属性设置");
            }
            
            String replyTopic = topic.replace("/set", "/set_reply");
            logError("响应主题: " + replyTopic);
            
            JSONObject reply = new JSONObject();
            reply.put("id", id);
            reply.put("code", 200);
            reply.put("msg", "success");
            
            String replyPayload = reply.toString();
            logError("响应内容: " + replyPayload);
            
            if (mqttClient == null) {
                logError("错误: mqttClient 为 null");
                return;
            }
            
            if (!mqttClient.isConnected()) {
                logError("错误: mqttClient 未连接");
                return;
            }
            
            MqttMessage replyMessage = new MqttMessage(replyPayload.getBytes());
            replyMessage.setQos(1);
            
            logError("开始发送响应...");
            
            mqttClient.publish(replyTopic, replyMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logError("=== 属性设置响应发送成功 ===");
                    logError("主题: " + replyTopic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logError("=== 属性设置响应发送失败 ===");
                    logError("错误: " + exception.getMessage());
                    exception.printStackTrace();
                }
            });
            
            logError("publish 调用完成，等待回调...");
            
        } catch (Exception e) {
            logError("=== 处理属性设置消息异常 ===");
            logError("异常类型: " + e.getClass().getName());
            logError("异常消息: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送控制指令到硬件设备
     * 使用 OneNET HTTP API
     */
    /** 发送控制指令到硬件设备（补光灯/水泵/风扇），通过OneNET HTTP API */
    public void sendControlCommand(String device, boolean state) {
        logError("sendControlCommand: " + device + " -> " + state);
        
        executorService.execute(() -> {
            try {
                sendControlCommandViaHttp(device, state);
            } catch (Exception e) {
                logError("发送控制指令异常: " + e.getMessage());
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(context, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void sendControlCommandViaHttp(String device, boolean state) throws Exception {
        if (apiToken == null || apiToken.isEmpty()) {
            logError("API Token 未生成");
            mainHandler.post(() -> {
                Toast.makeText(context, "API Token 未生成", Toast.LENGTH_LONG).show();
            });
            return;
        }

        JSONObject params = new JSONObject();
        params.put(device, state);

        JSONObject requestBody = new JSONObject();
        requestBody.put("product_id", ApiConfig.ONENET_PRODUCT_ID);
        requestBody.put("device_name", ApiConfig.ONENET_DEVICE_NAME);
        requestBody.put("params", params);

        logError("发送 HTTP 请求: " + requestBody.toString());
        logError("Authorization: " + apiToken);

        Request request = new Request.Builder()
                .url(ApiConfig.ONENET_API_URL)
                .addHeader("Authorization", apiToken)
                .post(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                ))
                .build();

        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";

        logError("HTTP 响应: " + response.code() + " - " + responseBody);

        if (response.isSuccessful()) {
            JSONObject jsonResponse = new JSONObject(responseBody);
            int code = jsonResponse.optInt("code", -1);
            
            if (code == 0) {
                logError("控制指令发送成功: " + device + " -> " + state);
                mainHandler.post(() -> {
                    String deviceName = getDeviceName(device);
                    Toast.makeText(context, deviceName + (state ? " 已开启" : " 已关闭"), Toast.LENGTH_SHORT).show();
                });
            } else {
                String msg = jsonResponse.optString("msg", "未知错误");
                logError("控制指令发送失败: " + msg);
                mainHandler.post(() -> {
                    Toast.makeText(context, "发送失败: " + msg, Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            logError("HTTP 请求失败: " + response.code());
            mainHandler.post(() -> {
                Toast.makeText(context, "网络错误: " + response.code(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    /** 将设备代码转换为中文设备名称 */
    private String getDeviceName(String device) {
        switch (device) {
            case "led":
                return "补光灯";
            case "pump":
                return "水泵";
            case "fan":
                return "风扇";
            default:
                return device;
        }
    }

    // ==================== 数据同步功能 ====================

    /**
     * 上报照片 URL 到 OneNET（监控端使用）
     */
    public void reportPhotoUrl(String photoUrl) {
        logError("reportPhotoUrl: " + photoUrl);
        
        executorService.execute(() -> {
            try {
                reportDeviceProperty("photo_url", photoUrl);
                reportDeviceProperty("photo_time", System.currentTimeMillis());
            } catch (Exception e) {
                logError("上报照片 URL 失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 查询照片 URL（查询两个设备，取最新的）
     */
    public void queryPhotoUrl(PhotoUrlCallback callback) {
        logError("queryPhotoUrl");
        
        executorService.execute(() -> {
            try {
                String latestUrl = null;
                long latestTime = 0;
                String sourceDevice = null;
                
                String[] devices = {MONITOR_CLIENT_ID, CONTROLLER_CLIENT_ID};
                
                for (String deviceName : devices) {
                    try {
                        String url = queryDeviceProperty("photo_url", deviceName);
                        long time = queryDevicePropertyTime("photo_time", deviceName);
                        
                        logError("查询设备 " + deviceName + ": url=" + url + ", time=" + time);
                        
                        if (url != null && !url.isEmpty() && time > latestTime) {
                            latestUrl = url;
                            latestTime = time;
                            sourceDevice = deviceName;
                        }
                    } catch (Exception e) {
                        logError("查询设备 " + deviceName + " 失败: " + e.getMessage());
                    }
                }
                
                final String resultUrl = latestUrl;
                final long resultTime = latestTime;
                final String resultDevice = sourceDevice;
                
                logError("最终结果: url=" + resultUrl + ", time=" + resultTime + ", 来源=" + resultDevice);
                
                if (resultUrl != null && !resultUrl.isEmpty()) {
                    mainHandler.post(() -> callback.onResult(resultUrl, resultTime));
                } else {
                    mainHandler.post(() -> callback.onResult(null, 0));
                }
            } catch (Exception e) {
                logError("查询照片 URL 失败: " + e.getMessage());
                e.printStackTrace();
                mainHandler.post(() -> callback.onResult(null, 0));
            }
        });
    }

    /**
     * 上报聊天消息到 OneNET
     */
    public void reportChatMessage(String message) {
        logError("reportChatMessage: " + message);
        
        executorService.execute(() -> {
            try {
                reportDeviceProperty("last_message", message);
                reportDeviceProperty("message_time", System.currentTimeMillis());
            } catch (Exception e) {
                logError("上报聊天消息失败: " + e.getMessage());
            }
        });
    }

    /**
     * 查询聊天消息（查询两个设备，取最新的）
     */
    public void queryChatMessage(ChatMessageCallback callback) {
        executorService.execute(() -> {
            try {
                String latestMessage = null;
                long latestTime = 0;
                
                String[] devices = {MONITOR_CLIENT_ID, CONTROLLER_CLIENT_ID};
                
                for (String deviceName : devices) {
                    try {
                        String message = queryDeviceProperty("last_message", deviceName);
                        long time = queryDevicePropertyTime("message_time", deviceName);
                        
                        logError("查询设备 " + deviceName + ": message=" + message + ", time=" + time);
                        
                        if (message != null && !message.isEmpty() && time > latestTime) {
                            latestMessage = message;
                            latestTime = time;
                        }
                    } catch (Exception e) {
                        logError("查询设备 " + deviceName + " 失败: " + e.getMessage());
                    }
                }
                
                final String resultMsg = latestMessage;
                final long resultTime = latestTime;
                
                logError("最终聊天消息: message=" + resultMsg + ", time=" + resultTime);
                
                mainHandler.post(() -> callback.onResult(resultMsg, resultTime));
            } catch (Exception e) {
                logError("查询聊天消息失败: " + e.getMessage());
                mainHandler.post(() -> callback.onResult(null, 0));
            }
        });
    }

    private void reportDeviceProperty(String key, Object value) throws Exception {
        if (mqttClient == null || !mqttClient.isConnected()) {
            logError("MQTT 未连接，无法上报属性");
            return;
        }

        String deviceName = currentRole == DeviceRole.MONITOR ? MONITOR_CLIENT_ID : CONTROLLER_CLIENT_ID;
        String topic = "$sys/" + PRODUCT_ID + "/" + deviceName + "/thing/property/post";

        JSONObject paramValue = new JSONObject();
        paramValue.put("value", value);

        JSONObject params = new JSONObject();
        params.put(key, paramValue);

        JSONObject message = new JSONObject();
        message.put("id", String.valueOf(System.currentTimeMillis()));
        message.put("version", "1.0");
        message.put("params", params);

        logError("MQTT上报属性: " + topic + " -> " + message.toString());

        MqttMessage mqttMessage = new MqttMessage(message.toString().getBytes());
        mqttMessage.setQos(1);

        mqttClient.publish(topic, mqttMessage, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                logError("MQTT上报属性成功: " + key);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                logError("MQTT上报属性失败: " + exception.getMessage());
            }
        });
    }

    private String queryDeviceProperty(String propertyName, String deviceName) throws Exception {
        if (apiToken == null || apiToken.isEmpty()) {
            logError("API Token 未生成");
            return null;
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 7 * 24 * 60 * 60 * 1000;

        String url = "https://iot-api.heclouds.com/thingmodel/query-device-property-history"
                + "?product_id=" + ApiConfig.ONENET_PRODUCT_ID
                + "&device_name=" + deviceName
                + "&identifier=" + propertyName
                + "&start_time=" + startTime
                + "&end_time=" + endTime
                + "&limit=1"
                + "&sort=desc";

        logError("查询属性历史 URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", apiToken)
                .get()
                .build();

        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";

        logError("查询属性历史响应: " + responseBody);

        if (response.isSuccessful()) {
            JSONObject jsonResponse = new JSONObject(responseBody);
            int code = jsonResponse.optInt("code", -1);
            
            if (code == 0) {
                JSONObject data = jsonResponse.optJSONObject("data");
                if (data != null) {
                    org.json.JSONArray list = data.optJSONArray("list");
                    if (list != null && list.length() > 0) {
                        JSONObject item = list.getJSONObject(0);
                        return item.optString("value");
                    }
                }
            }
        }
        
        return null;
    }

    private long queryDevicePropertyTime(String propertyName, String deviceName) throws Exception {
        if (apiToken == null || apiToken.isEmpty()) {
            return 0;
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 7 * 24 * 60 * 60 * 1000;

        String url = "https://iot-api.heclouds.com/thingmodel/query-device-property-history"
                + "?product_id=" + ApiConfig.ONENET_PRODUCT_ID
                + "&device_name=" + deviceName
                + "&identifier=" + propertyName
                + "&start_time=" + startTime
                + "&end_time=" + endTime
                + "&limit=1"
                + "&sort=desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", apiToken)
                .get()
                .build();

        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";

        if (response.isSuccessful()) {
            JSONObject jsonResponse = new JSONObject(responseBody);
            int code = jsonResponse.optInt("code", -1);
            
            if (code == 0) {
                JSONObject data = jsonResponse.optJSONObject("data");
                if (data != null) {
                    org.json.JSONArray list = data.optJSONArray("list");
                    if (list != null && list.length() > 0) {
                        JSONObject item = list.getJSONObject(0);
                        return item.optLong("time", 0);
                    }
                }
            }
        }
        
        return 0;
    }

    public interface PhotoUrlCallback {
        void onResult(String photoUrl, long photoTime);
    }

    public interface ChatMessageCallback {
        void onResult(String message, long time);
    }

    public interface RemoteCommandCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface RemoteResponseListener {
        void onResponseReceived(String status, String message);
    }

    private RemoteResponseListener remoteResponseListener;

    public void setRemoteResponseListener(RemoteResponseListener listener) {
        this.remoteResponseListener = listener;
    }

    public void sendRemoteCaptureCommand(RemoteCommandCallback callback) {
        logError("[MQTT发送] 发送拍照指令");
        sendRemoteCommandViaHttp("CAPTURE_NOW", -1, false, callback);
    }

    public void sendRemoteIntervalCommand(int intervalMinutes, RemoteCommandCallback callback) {
        logError("[MQTT发送] 发送间隔设置指令: " + intervalMinutes + "分钟");
        sendRemoteCommandViaHttp("SET_INTERVAL", intervalMinutes, true, callback);
    }

    public void sendRemoteTimerToggleCommand(boolean enabled, RemoteCommandCallback callback) {
        String action = enabled ? "ENABLE_TIMER" : "DISABLE_TIMER";
        logError("[MQTT发送] 发送定时开关指令: " + action);
        sendRemoteCommandViaHttp(action, -1, false, callback);
    }

    private void sendRemoteCommandViaHttp(String action, int value, boolean includeValue, RemoteCommandCallback callback) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                logError("[MQTT发送] MQTT未连接");
                mainHandler.post(() -> callback.onFailure("MQTT未连接"));
                return;
            }
            
            String topic = "$sys/" + PRODUCT_ID + "/" + CONTROLLER_CLIENT_ID + "/thing/property/post";
            
            long now = System.currentTimeMillis();
            JSONObject params = new JSONObject();
            JSONObject actionObj = new JSONObject();
            actionObj.put("value", action);
            actionObj.put("time", now);
            params.put("remote_action", actionObj);
            
            if (includeValue) {
                JSONObject valueObj = new JSONObject();
                valueObj.put("value", value);
                valueObj.put("time", now);
                params.put("remote_value", valueObj);
            }
            
            JSONObject message = new JSONObject();
            message.put("id", String.valueOf(now));
            message.put("version", "1.0");
            message.put("params", params);
            
            String payload = message.toString();
            logError("[MQTT发送] 上报属性: " + payload);
            
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
            mqttMessage.setQos(1);
            
            mqttClient.publish(topic, mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logError("[MQTT发送] 属性上报成功: " + action);
                    mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logError("[MQTT发送] 属性上报失败: " + exception.getMessage());
                    mainHandler.post(() -> callback.onFailure(exception.getMessage()));
                }
            });
        } catch (Exception e) {
            logError("[MQTT发送] 异常: " + e.getMessage());
            e.printStackTrace();
            mainHandler.post(() -> callback.onFailure(e.getMessage()));
        }
    }

    public void subscribeToRemoteResponse() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            logError("MQTT 未连接，无法订阅响应主题");
            return;
        }

        // 控制端订阅监控端的响应主题
        String monitorDeviceName = "Phone_Monitor";
        String topic = "$sys/" + PRODUCT_ID + "/" + monitorDeviceName + "/cmd/response/+";
        try {
            mqttClient.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logError("订阅响应主题成功: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logError("订阅响应主题失败: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            logError("订阅响应主题异常: " + e.getMessage());
        }
    }

    private void handleRemoteResponse(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            String status = json.optString("status", "");
            String message = json.optString("message", "");
            
            logError("[MQTT接收] 远程响应: status=" + status + ", message=" + message);
            
            if (remoteResponseListener != null) {
                mainHandler.post(() -> remoteResponseListener.onResponseReceived(status, message));
            }
        } catch (Exception e) {
            logError("[MQTT接收] 解析远程响应失败: " + e.getMessage());
        }
    }

    public void subscribeToRemoteCommands() {
        if (currentRole != DeviceRole.MONITOR) {
            logError("[轮询] 非监控端，跳过轮询启动");
            return;
        }
        logError("[轮询] 启动远程指令轮询");
        
        if (commandPoller != null) {
            commandPoller.cancel(false);
        }
        
        commandPoller = scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                pollRemoteCommand();
            } catch (Exception e) {
                logError("[轮询] 异常: " + e.getMessage());
            }
        }, 0, 2, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    public void stopRemoteCommandPolling() {
        if (commandPoller != null) {
            commandPoller.cancel(false);
            commandPoller = null;
        }
    }
    
    private void pollRemoteCommand() {
        try {
            if (apiToken == null || apiToken.isEmpty()) {
                logError("[轮询] API Token 为空，跳过轮询");
                return;
            }
            
            String controllerDeviceName = "Phone_Controller";
            
            String url = "https://iot-api.heclouds.com/thingmodel/query-device-property"
                    + "?product_id=" + PRODUCT_ID
                    + "&device_name=" + controllerDeviceName;
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", apiToken)
                    .get()
                    .build();
            
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (response.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                int code = jsonResponse.optInt("code", -1);
                if (code == 0) {
                    org.json.JSONArray dataArray = jsonResponse.optJSONArray("data");
                    if (dataArray != null) {
                        String action = "";
                        int value = 0;
                        long actionTime = 0;
                        
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);
                            String identifier = item.optString("identifier", "");
                            if ("remote_action".equals(identifier)) {
                                if (item.has("value")) {
                                    action = item.optString("value", "");
                                }
                                actionTime = item.optLong("time", 0);
                                logError("[轮询] remote_action项: " + item.toString());
                            } else if ("remote_value".equals(identifier)) {
                                if (item.has("value")) {
                                    String valueStr = item.optString("value", "0");
                                    try {
                                        value = Integer.parseInt(valueStr.replaceAll("[^0-9-]", ""));
                                    } catch (NumberFormatException e) {
                                        value = 0;
                                    }
                                }
                                logError("[轮询] remote_value项: " + item.toString());
                            }
                        }
                        
                        logError("[轮询] 解析结果: action=" + action + ", value=" + value + ", actionTime=" + actionTime + ", lastTimestamp=" + lastCommandTimestamp);
                        
                        if (!action.isEmpty()) {
                            boolean isNewCommand = false;
                            
                            if (lastCommandTimestamp == 0) {
                                logError("[轮询] 首次轮询，记录当前状态不执行");
                                lastCommandTimestamp = actionTime;
                                lastAction = action;
                            } else if (actionTime > lastCommandTimestamp) {
                                logError("[轮询] 检测到时间戳更新: " + actionTime + " > " + lastCommandTimestamp);
                                isNewCommand = true;
                            } else if (!action.equals(lastAction)) {
                                logError("[轮询] 检测到action变化: " + action + " != " + lastAction);
                                isNewCommand = true;
                            }
                            
                            if (isNewCommand) {
                                lastCommandTimestamp = actionTime;
                                lastAction = action;
                                logError("[轮询] 收到新的远程指令: " + action + ", time=" + actionTime + ", value=" + value);
                                
                                if ("SET_INTERVAL".equals(action) && value > 0) {
                                    handlePolledCommand(action, value);
                                } else {
                                    handlePolledCommand(action, 0);
                                }
                            }
                        }
                    }
                } else {
                    logError("[轮询] API返回错误: code=" + code + ", body=" + responseBody);
                }
            } else {
                logError("[轮询] HTTP请求失败: " + response.code());
            }
        } catch (Exception e) {
            logError("[轮询] 异常: " + e.getMessage());
        }
    }
    
    private void handlePolledCommand(String action, int value) {
        logError("[拍照执行] 收到轮询指令: action=" + action + ", value=" + value);
        
        if ("CAPTURE_NOW".equals(action)) {
            logError("[拍照执行] 触发立即拍照");
            if (remoteCommandReceivedListener != null) {
                mainHandler.post(() -> {
                    logError("[拍照执行] 调用onCaptureNowCommand");
                    remoteCommandReceivedListener.onCaptureNowCommand();
                });
            }
        } else if ("SET_INTERVAL".equals(action) && value > 0) {
            logError("[拍照执行] 设置间隔: " + value + "分钟");
            if (remoteCommandReceivedListener != null) {
                final int finalInterval = value;
                mainHandler.post(() -> remoteCommandReceivedListener.onSetIntervalCommand(finalInterval));
            }
        } else if ("ENABLE_TIMER".equals(action)) {
            logError("[拍照执行] 远程开启定时拍照");
            if (remoteCommandReceivedListener != null) {
                mainHandler.post(() -> remoteCommandReceivedListener.onToggleTimerCommand(true));
            }
        } else if ("DISABLE_TIMER".equals(action)) {
            logError("[拍照执行] 远程关闭定时拍照");
            if (remoteCommandReceivedListener != null) {
                mainHandler.post(() -> remoteCommandReceivedListener.onToggleTimerCommand(false));
            }
        }
    }

    public void sendRemoteResponse(String status, String message) {
        logError("[响应] 远程指令响应: " + status + " - " + message + " (不再通过MQTT发送，避免断开)");
    }

    public interface RemoteCommandReceivedListener {
        void onCaptureNowCommand();
        void onSetIntervalCommand(int intervalMinutes);
        void onToggleTimerCommand(boolean enabled);
    }

    private RemoteCommandReceivedListener remoteCommandReceivedListener;

    public void setRemoteCommandReceivedListener(RemoteCommandReceivedListener listener) {
        this.remoteCommandReceivedListener = listener;
    }

    public void syncPhotoResult(String payload) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            logError("MQTT 未连接，无法同步照片结果");
            return;
        }

        try {
            // 使用OneNET标准主题格式：自定义主题
            String topic = "$sys/" + PRODUCT_ID + "/" + getCurrentDeviceName() + "/custom/photo/sync";
            
            logError("同步照片结果: " + topic + " -> " + payload);

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);

            mqttClient.publish(topic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logError("照片结果同步成功");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logError("照片结果同步失败: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            logError("同步照片结果异常: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (pendingReconnect != null) {
            mainHandler.removeCallbacks(pendingReconnect);
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            logError("MQTT 重连次数已达上限(" + MAX_RECONNECT_ATTEMPTS + ")，停止重连");
            reconnectAttempts = 0;
            return;
        }

        reconnectAttempts++;
        long delay = Math.min(5000L * reconnectAttempts, 30000L);
        logError("MQTT 计划 " + delay + "ms 后重连 (第" + reconnectAttempts + "次)");

        pendingReconnect = () -> {
            if (!isConnected && mqttClient != null) {
                logError("MQTT 执行重连...");
                try {
                    if (mqttClient.isConnected()) {
                        isConnected = true;
                        reconnectAttempts = 0;
                        return;
                    }
                    mqttClient.reconnect();
                } catch (Exception e) {
                    logError("MQTT reconnect() 失败: " + e.getMessage() + "，尝试完全重建连接");
                    executorService.execute(() -> {
                        try {
                            connectInternal();
                        } catch (Exception ex) {
                            logError("MQTT 重建连接失败: " + ex.getMessage());
                        }
                    });
                }
            }
        };
        mainHandler.postDelayed(pendingReconnect, delay);
    }

    public void disconnect() {
        executorService.execute(() -> {
            if (mqttClient != null) {
                try {
                    if (mqttClient.isConnected()) {
                        mqttClient.disconnect();
                        logInfo("MQTT 断开连接成功");
                    }
                    mqttClient.close();
                    mqttClient = null;
                } catch (MqttException e) {
                    logError("断开连接异常: " + e.getMessage());
                }
            }
            isConnected = false;
        });
    }

    public boolean isConnected() {
        return isConnected && mqttClient != null && mqttClient.isConnected();
    }

    public void addMessageListener(MqttMessageListener listener) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    public void removeMessageListener(MqttMessageListener listener) {
        messageListeners.remove(listener);
    }

    public void addConnectionListener(ConnectionStateListener listener) {
        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
            if (isConnected()) {
                mainHandler.post(() -> listener.onConnected());
            }
        }
    }

    public void removeConnectionListener(ConnectionStateListener listener) {
        connectionListeners.remove(listener);
    }

    private void notifyMessageListeners(String topic, String payload) {
        for (MqttMessageListener listener : messageListeners) {
            listener.onMessageReceived(topic, payload);
        }
    }

    private void notifyConnectionListenersConnected() {
        logError("通知所有监听器: 已连接");
        for (ConnectionStateListener listener : connectionListeners) {
            listener.onConnected();
        }
    }

    private void notifyConnectionListenersDisconnected() {
        logError("通知所有监听器: 已断开");
        for (ConnectionStateListener listener : connectionListeners) {
            listener.onDisconnected();
        }
    }

    private void notifyConnectionListenersFailed(String error) {
        logError("通知所有监听器: 连接失败 - " + error);
        for (ConnectionStateListener listener : connectionListeners) {
            listener.onConnectionFailed(error);
        }
    }

    public void release() {
        disconnect();
        stopRemoteCommandPolling();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
        }
        mainHandler.removeCallbacksAndMessages(null);
        instance = null;
    }

    public interface SensorDataCallback {
        void onResult(float waterTemp, float airTemp, float airHumidity);
        void onError(String error);
    }

    /**
     * 查询传感器数据（水温、气温、湿度）
     * 优先使用30秒内的缓存数据，缓存过期则从OneNET API查询
     */
    public void querySensorData(SensorDataCallback callback) {
        // 30秒内的缓存数据直接使用
        if (lastSensorUpdateTime > 0 && System.currentTimeMillis() - lastSensorUpdateTime < 30000) {
            logError("[传感器查询] 使用MQTT缓存数据 水温=" + cachedWaterTemp + " 气温=" + cachedAirTemp + " 湿度=" + cachedAirHumidity);
            mainHandler.post(() -> callback.onResult(cachedWaterTemp, cachedAirTemp, cachedAirHumidity));
            return;
        }

        // 缓存过期，从OneNET API查询最新数据
        executorService.execute(() -> {
            try {
                String m5DeviceName = M5_DEVICE_NAME;
                float[] values = queryAllSensorProperties(m5DeviceName);
                float wt = values[0];
                float at = values[1];
                float ah = values[2];

                if (wt > 0 || at > 0 || ah > 0) {
                    cachedWaterTemp = wt;
                    cachedAirTemp = at;
                    cachedAirHumidity = ah;
                    lastSensorUpdateTime = System.currentTimeMillis();
                }

                logError("[传感器查询] waterTemp=" + wt + ", airTemp=" + at + ", airHum=" + ah);
                mainHandler.post(() -> callback.onResult(wt, at, ah));
            } catch (Exception e) {
                logError("[传感器查询] 失败: " + e.getMessage());
                // API查询失败时，降级使用旧缓存
                if (lastSensorUpdateTime > 0 && (cachedWaterTemp > 0 || cachedAirTemp > 0 || cachedAirHumidity > 0)) {
                    mainHandler.post(() -> callback.onResult(cachedWaterTemp, cachedAirTemp, cachedAirHumidity));
                } else {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    private float[] queryAllSensorProperties(String deviceName) throws Exception {
        float[] result = querySensorPropertiesViaHistory(deviceName);

        if (result[0] == 0 && result[1] == 0 && result[2] == 0) {
            logError("[传感器查询] 历史API无数据，尝试最新属性API");
            result = querySensorPropertiesViaLatest(deviceName);
        }

        logError("[传感器查询] 结果 waterTemp=" + result[0] + " airTemp=" + result[1] + " airHum=" + result[2]);
        return result;
    }

    private float[] querySensorPropertiesViaHistory(String deviceName) throws Exception {
        float[] result = {0f, 0f, 0f};

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 7 * 24 * 60 * 60 * 1000;

        String[] identifiers = {"water_temp", "air_temp", "air_hum"};

        for (int idx = 0; idx < identifiers.length; idx++) {
            try {
                result[idx] = queryPropertyHistory(deviceName, identifiers[idx], startTime, endTime);
            } catch (Exception e) {
                logError("[传感器查询-历史] " + identifiers[idx] + " 异常: " + e.getMessage());
            }
        }

        if (result[0] == 0 && result[1] == 0 && result[2] == 0) {
            logError("[传感器查询-历史] Unix时间范围无数据，尝试宽范围查询(兼容millis时间戳)");
            for (int idx = 0; idx < identifiers.length; idx++) {
                try {
                    float val = queryPropertyHistory(deviceName, identifiers[idx], 0, endTime);
                    if (val > 0) result[idx] = val;
                } catch (Exception e) {
                    logError("[传感器查询-历史宽范围] " + identifiers[idx] + " 异常: " + e.getMessage());
                }
            }
        }

        return result;
    }

    private float queryPropertyHistory(String deviceName, String identifier, long startTime, long endTime) throws Exception {
        String url = "https://iot-api.heclouds.com/thingmodel/query-device-property-history"
                + "?product_id=" + ApiConfig.ONENET_PRODUCT_ID
                + "&device_name=" + deviceName
                + "&identifier=" + identifier
                + "&start_time=" + startTime
                + "&end_time=" + endTime
                + "&limit=1"
                + "&sort=desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", apiToken)
                .get()
                .build();

        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";

        logError("[传感器查询-历史] " + identifier + " start=" + startTime + " 响应=" + responseBody);

        if (response.isSuccessful()) {
            JSONObject jsonResponse = new JSONObject(responseBody);
            int code = jsonResponse.optInt("code", -1);
            if (code == 0) {
                JSONObject data = jsonResponse.optJSONObject("data");
                if (data != null) {
                    org.json.JSONArray list = data.optJSONArray("list");
                    if (list != null && list.length() > 0) {
                        JSONObject item = list.getJSONObject(0);
                        String val = item.optString("value", "");
                        if (!val.isEmpty()) {
                            return Float.parseFloat(val);
                        }
                    }
                }
            }
        }
        return 0f;
    }

    private float[] querySensorPropertiesViaLatest(String deviceName) throws Exception {
        float[] result = {0f, 0f, 0f};

        try {
            String url = "https://iot-api.heclouds.com/thingmodel/query-device-property"
                    + "?product_id=" + ApiConfig.ONENET_PRODUCT_ID
                    + "&device_name=" + deviceName;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", apiToken)
                    .get()
                    .build();

            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";

            logError("[传感器查询-最新] 响应=" + responseBody);

            if (response.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                int code = jsonResponse.optInt("code", -1);
                if (code == 0) {
                    org.json.JSONArray dataArray = jsonResponse.optJSONArray("data");
                    if (dataArray != null) {
                        for (int i = 0; i < dataArray.length(); i++) {
                            try {
                                JSONObject item = dataArray.getJSONObject(i);
                                String identifier = item.optString("identifier", "");
                                if (!("water_temp".equals(identifier) || "air_temp".equals(identifier) || "air_hum".equals(identifier))) {
                                    continue;
                                }
                                if (item.has("value")) {
                                    String valueStr = item.optString("value", "");
                                    if (!valueStr.isEmpty()) {
                                        try {
                                            float val = Float.parseFloat(valueStr);
                                            if ("water_temp".equals(identifier)) result[0] = val;
                                            else if ("air_temp".equals(identifier)) result[1] = val;
                                            else if ("air_hum".equals(identifier)) result[2] = val;
                                        } catch (NumberFormatException nfe) {
                                            logError("[传感器查询-最新] " + identifier + " 值格式错误: " + valueStr);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logError("[传感器查询-最新] 解析项异常: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logError("[传感器查询-最新] 异常: " + e.getMessage());
        }

        return result;
    }

    private float querySensorPropertyValue(String identifier, String deviceName) throws Exception {
        String url = "https://iot-api.heclouds.com/thingmodel/query-device-property"
                + "?product_id=" + ApiConfig.ONENET_PRODUCT_ID
                + "&device_name=" + deviceName;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", apiToken)
                .get()
                .build();

        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";

        if (response.isSuccessful()) {
            JSONObject jsonResponse = new JSONObject(responseBody);
            int code = jsonResponse.optInt("code", -1);
            if (code == 0) {
                JSONObject data = jsonResponse.optJSONObject("data");
                if (data != null) {
                    org.json.JSONArray list = data.optJSONArray("list");
                    if (list != null && list.length() > 0) {
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject item = list.getJSONObject(i);
                            if (identifier.equals(item.optString("identifier"))) {
                                String value = item.optString("value", "0");
                                if (!value.isEmpty()) {
                                    return Float.parseFloat(value);
                                }
                            }
                        }
                    }
                }
            } else {
                logError("[传感器查询] API返回错误 code=" + code + " msg=" + jsonResponse.optString("msg"));
            }
        } else {
            logError("[传感器查询] HTTP错误 code=" + response.code());
        }
        return 0f;
    }
}
