package com.example.mygraduationproject.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mygraduationproject.config.ApiConfig;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.utils.Base64Utils;
import com.example.mygraduationproject.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI 服务和设备控制 API 服务类
 * 提供植物识别、AI 聊天、设备控制等功能
 */
/** API鏈嶅姟锛氱櫨搴︽鐗╄瘑鍒€侀€氫箟鍗冮棶瀵硅瘽銆丼TM32璁惧鎺у埗 */
public class ApiService {
    
    private static final String TAG = "ApiService";
    
    private final Context context;
    private final OkHttpClient okHttpClient;
    private final Handler mainHandler;
    private String cachedBaiduToken;
    private long tokenExpireTime;
    private PreferenceManager preferenceManager;
    
    public ApiService(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.preferenceManager = new PreferenceManager(context);
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 植物识别功能
     * 调用百度 AI 植物识别 API
     */
    public void identifyPlant(File imageFile, PlantIdentifyCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting plant identification for: " + imageFile.getAbsolutePath());
                
                String base64Image = Base64Utils.encodeFileToBase64(imageFile);
                Log.d(TAG, "Image encoded to base64, length: " + base64Image.length());
                
                String token = getBaiduAccessTokenSync();
                if (token == null) {
                    Log.e(TAG, "Failed to get Baidu token");
                    notifyError(callback, "获取 Token 失败，请检查网络");
                    return;
                }
                Log.d(TAG, "Got Baidu token successfully");
                
                List<AIResult> results = callBaiduPlantAPIForAllResults(token, base64Image);
                if (results != null && !results.isEmpty()) {
                    Log.d(TAG, "Plant identified: " + results.size() + " candidates");
                    
                    AIResult primary = results.get(0);
                    String detailedAnalysis = getDetailedAnalysisFromAI(primary);
                    primary.setDetailedAnalysis(detailedAnalysis);
                    
                    notifySuccessWithCandidates(callback, results);
                } else {
                    Log.e(TAG, "Plant identification returned null");
                    notifyError(callback, "植物识别失败，请重试");
                }
            } catch (Exception e) {
                Log.e(TAG, "Identify plant failed", e);
                notifyError(callback, "识别失败：" + e.getMessage());
            }
        }).start();
    }
    
    private String getDetailedAnalysisFromAI(AIResult result) {
        try {
            String prompt = buildDetailedAnalysisPrompt(result);
            return callQwenChatAPI(prompt);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get detailed analysis", e);
            return null;
        }
    }
    
    private String buildDetailedAnalysisPrompt(AIResult result) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请对以下植物进行详细分析，请以结构化的方式回答：\n\n");
        prompt.append("植物名称：").append(result.getPlantName() != null ? result.getPlantName() : "未知").append("\n");
        prompt.append("识别置信度：").append(String.format("%.0f%%", result.getConfidence() * 100)).append("\n\n");
        prompt.append("请按以下格式回答：\n");
        prompt.append("【植物简介】简要介绍这种植物\n");
        prompt.append("【养护要点】光照、水分、土壤、温度要求\n");
        prompt.append("【常见问题】可能遇到的病虫害及预防方法\n");
        prompt.append("【健康建议】根据当前状态给出改善建议\n");
        prompt.append("\n请用简洁清晰的语言回答，每部分不超过 100 字。");
        return prompt.toString();
    }
    
    private String getBaiduAccessTokenSync() throws IOException {
        if (cachedBaiduToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedBaiduToken;
        }
        
        String url = ApiConfig.BAIDU_TOKEN_API + "?grant_type=client_credentials" +
                "&client_id=" + ApiConfig.BAIDU_API_KEY +
                "&client_secret=" + ApiConfig.BAIDU_SECRET_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .build();
        
        Response response = okHttpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        
        if (response.isSuccessful() && !responseBody.isEmpty()) {
            try {
                JSONObject json = new JSONObject(responseBody);
                if (json.has("access_token")) {
                    cachedBaiduToken = json.getString("access_token");
                    int expiresIn = json.optInt("expires_in", 86400);
                    tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                    return cachedBaiduToken;
                }
            } catch (Exception e) {
                Log.e(TAG, "Parse token response failed", e);
            }
        }
        return null;
    }
    
    private List<AIResult> callBaiduPlantAPIForAllResults(String token, String base64Image) throws IOException {
        String url = ApiConfig.BAIDU_PLANT_API + "?access_token=" + token + "&baike_num=5";
        
        String encodedImage = URLEncoder.encode(base64Image, "UTF-8");
        String bodyStr = "image=" + encodedImage + "&image_type=BASE64&baike_num=5";
        
        RequestBody body = RequestBody.create(bodyStr, 
                MediaType.parse("application/x-www-form-urlencoded"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        Response response = okHttpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        
        Log.d(TAG, "Baidu Plant API response length: " + responseBody.length());
        
        List<AIResult> results = new ArrayList<>();
        
        try {
            JSONObject json = new JSONObject(responseBody);
            
            if (json.has("error_code")) {
                String errorMsg = json.optString("error_msg", "未知错误");
                Log.e(TAG, "Baidu API error: " + json.optInt("error_code") + " - " + errorMsg);
                return null;
            }
            
            if (json.has("result")) {
                JSONArray resultArray = json.getJSONArray("result");
                StringBuilder candidatesBuilder = new StringBuilder();
                
                for (int i = 0; i < resultArray.length() && i < 5; i++) {
                    JSONObject item = resultArray.getJSONObject(i);
                    AIResult aiResult = new AIResult();
                    aiResult.setPlantName(item.optString("name", "未知植物"));
                    aiResult.setConfidence(item.optDouble("score", 0));
                    
                    double score = aiResult.getConfidence();
                    if (score > 0.85) {
                        aiResult.setHealthStatus("健康");
                    } else if (score > 0.6) {
                        aiResult.setHealthStatus("可能异常");
                    } else {
                        aiResult.setHealthStatus("需要检查");
                    }
                    
                    results.add(aiResult);
                    
                    if (i > 0) {
                        if (candidatesBuilder.length() > 0) {
                            candidatesBuilder.append("|");
                        }
                        candidatesBuilder.append(aiResult.getPlantName())
                                .append(",")
                                .append(String.format("%.1f", aiResult.getConfidence() * 100));
                    }
                }
                
                if (!results.isEmpty() && candidatesBuilder.length() > 0) {
                    results.get(0).setCandidates(candidatesBuilder.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse plant response failed", e);
        }
        
        return results;
    }
    
    /**
     * AI 聊天功能
     * 与 AI 进行对话交流
     */
    public void chatWithAI(String userMessage, List<ChatMessage> history, ChatCallback callback) {
        new Thread(() -> {
            try {
                String response = callQwenChatAPIWithHistory(userMessage, history);
                if (response != null) {
                    notifyChatSuccess(callback, response);
                } else {
                    notifyChatError(callback, "AI 回复失败，请重试");
                }
            } catch (Exception e) {
                Log.e(TAG, "Chat with AI failed", e);
                notifyChatError(callback, "网络错误：" + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 纠正植物名称功能
     * 当 AI 识别错误时，用户可提供正确的植物名称
     */
    public void correctPlantName(AIResult result, String correctName, CorrectCallback callback) {
        new Thread(() -> {
            try {
                result.setPlantName(correctName);
                String detailedAnalysis = getDetailedAnalysisFromAI(result);
                result.setDetailedAnalysis(detailedAnalysis);
                notifyCorrectSuccess(callback, result);
            } catch (Exception e) {
                Log.e(TAG, "Correct plant name failed", e);
                notifyCorrectError(callback, "纠正失败：" + e.getMessage());
            }
        }).start();
    }
    
    private String callQwenChatAPI(String prompt) {
        try {
            JSONArray messages = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是" + ApiConfig.AI_NAME + "，一个专业、友好的植物养护助手。你擅长识别植物、诊断病虫害、提供养护建议。回答要专业、详细、有条理。");
            messages.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            
            return callQwenAPI(messages);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call Qwen API", e);
            return null;
        }
    }
    
    private String callQwenChatAPIWithHistory(String userMessage, List<ChatMessage> history) {
        try {
            JSONArray messages = new JSONArray();
            
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是" + ApiConfig.AI_NAME + "，一个专业、友好的植物养护助手。你擅长识别植物、诊断病虫害、提供养护建议。回答要简洁专业，不超过 200 字。用亲切的语气回答，可以适当使用表情符号。如果用户指出识别错误，请道歉并询问正确的植物名称，然后重新分析。");
            messages.put(systemMessage);
            
            if (history != null) {
                for (ChatMessage msg : history) {
                    if (msg != null && msg.getContent() != null) {
                        JSONObject msgJson = new JSONObject();
                        msgJson.put("role", msg.isUser() ? "user" : "assistant");
                        msgJson.put("content", msg.getContent());
                        messages.put(msgJson);
                    }
                }
            }
            
            JSONObject newUserMessage = new JSONObject();
            newUserMessage.put("role", "user");
            newUserMessage.put("content", userMessage);
            messages.put(newUserMessage);
            
            return callQwenAPI(messages);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call Qwen API", e);
            return null;
        }
    }
    
    private String callQwenAPI(JSONArray messages) throws IOException, org.json.JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", ApiConfig.QWEN_MODEL);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 800);
        requestBody.put("temperature", 0.7);
        
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        
        Request request = new Request.Builder()
                .url(ApiConfig.QWEN_API_URL)
                .addHeader("Authorization", "Bearer " + ApiConfig.QWEN_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        
        Log.d(TAG, "Calling Qwen API with model: " + ApiConfig.QWEN_MODEL);
        
        Response response = okHttpClient.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        
        Log.d(TAG, "Qwen API response length: " + responseBody.length());
        
        if (response.isSuccessful() && !responseBody.isEmpty()) {
            JSONObject jsonResponse = new JSONObject(responseBody);
            if (jsonResponse.has("choices")) {
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice.has("message")) {
                        JSONObject messageObj = firstChoice.getJSONObject("message");
                        if (messageObj.has("content")) {
                            return messageObj.getString("content");
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 发送控制命令到 STM32 设备
     * 支持 HTTP 协议
     */
    public void sendControlCommand(String deviceCode, String action, ControlCallback callback) {
        new Thread(() -> {
            try {
                String deviceIp = preferenceManager.getDeviceIp();
                int devicePort = preferenceManager.getDevicePort();
                String baseUrl = "http://" + deviceIp + ":" + devicePort;
                
                Log.d(TAG, "Sending control command to: " + baseUrl + "/api/control");
                Log.d(TAG, "Device: " + deviceCode + ", Action: " + action);
                
                sendHttpControlCommand(baseUrl, deviceCode, action, callback);
            } catch (Exception e) {
                Log.e(TAG, "Send control command failed", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("发送失败：" + e.getMessage()));
                }
            }
        }).start();
    }
    
    /**
     * 通过 HTTP 发送控制命令到 STM32
     * 预期 STM32 端提供 REST API: POST /api/control
     * 请求体：{"device": "light", "action": "on"}
     * 响应体：{"success": true, "message": "控制成功", "device": "light", "status": "on"}
     */
    private void sendHttpControlCommand(String baseUrl, String deviceCode, String action, ControlCallback callback) {
        try {
            String url = baseUrl + "/api/control";
            
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("device", deviceCode);
            jsonBody.put("action", action);
            
            RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
            
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "Control response: " + responseBody);
            
            if (response.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                boolean success = jsonResponse.optBoolean("success", true);
                String message = jsonResponse.optString("message", "控制成功");
                
                if (success) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(message));
                    }
                } else {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(message));
                    }
                }
            } else {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("HTTP 错误：" + response.code()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "HTTP control failed", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("网络错误：" + e.getMessage()));
            }
        }
    }
    
    public boolean isApiKeyConfigured() {
        return true;
    }
    
    private void notifySuccessWithCandidates(PlantIdentifyCallback callback, List<AIResult> results) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(results));
        }
    }
    
    private void notifyError(PlantIdentifyCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
    
    private void notifyChatSuccess(ChatCallback callback, String response) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(response));
        }
    }
    
    private void notifyChatError(ChatCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
    
    private void notifyCorrectSuccess(CorrectCallback callback, AIResult result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(result));
        }
    }
    
    private void notifyCorrectError(CorrectCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
    
    public interface PlantIdentifyCallback {
        void onSuccess(List<AIResult> results);
        void onError(String message);
    }
    
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String message);
    }
    
    public interface CorrectCallback {
        void onSuccess(AIResult result);
        void onError(String message);
    }
    
    public interface ControlCallback {
        void onSuccess(String message);
        void onError(String message);
    }
    
    public static class ChatMessage {
        private String content;
        private boolean isUser;
        private long timestamp;
        
        public ChatMessage(String content, boolean isUser) {
            this.content = content;
            this.isUser = isUser;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getContent() {
            return content;
        }
        
        public boolean isUser() {
            return isUser;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
