package com.example.mygraduationproject.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.model.OSSRequest;
import com.alibaba.sdk.android.oss.model.OSSResult;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.example.mygraduationproject.data.Repository;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.ChatMessageEntity;
import com.example.mygraduationproject.model.ChatSession;
import com.example.mygraduationproject.model.GrowthRecord;
import com.example.mygraduationproject.model.PlantRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 历史记录云端同步管理器
 * 负责将PlantRecord上传到阿里云OSS，以及从云端下载同步到本地数据库
 * 实现监控端与控制端之间的数据共享
 * 
 * 重要提示：请替换为你的阿里云 OSS 配置信息
 */
public class HistoryRecordsManager {
    private static final String TAG = "HistoryRecordsManager";
    
    // ==================== OSS 配置参数 ====================
    // Bucket名称（请替换为你的 Bucket 名称）
    private static final String BUCKET_NAME = "YOUR_BUCKET_NAME";
    
    // OSS Endpoint（请替换为你的 Endpoint）
    private static final String ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com";
    
    // OSS存储目录前缀
    private static final String HISTORY_FILE_KEY = "history/history_records.json";
    
    public static final String ACTION_HISTORY_SYNCED = "com.example.mygraduationproject.HISTORY_SYNCED";

    private static HistoryRecordsManager instance;
    private OSS ossClient;
    private ExecutorService executorService;
    private Context context;

    private HistoryRecordsManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        initOssClient();
    }

    public static synchronized HistoryRecordsManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryRecordsManager(context);
        }
        return instance;
    }

    /** 初始化阿里云OSS客户端，配置AccessKey和Endpoint */
    private void initOssClient() {
        // ==================== 请替换为你的 AccessKey ====================
        // AccessKey ID（请替换为你的 AccessKey ID）
        String ACCESS_KEY_ID = "YOUR_ACCESS_KEY_ID";
        
        // AccessKey Secret（请替换为你的 AccessKey Secret）
        String ACCESS_KEY_SECRET = "YOUR_ACCESS_KEY_SECRET";
        
        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(
                ACCESS_KEY_ID,
                ACCESS_KEY_SECRET
        );
        ossClient = new OSSClient(context, ENDPOINT, credentialProvider);
        Log.d(TAG, "OSS客户端初始化完成");
    }

    public interface SaveRecordCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void saveRecord(PlantRecord record, SaveRecordCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "开始保存记录: " + record.getImageUrl());

                JSONArray recordsArray = downloadHistoryFile();
                recordsArray.put(record.toJson());
                Log.d(TAG, "记录已追加，当前共 " + recordsArray.length() + " 条记录");

                uploadHistoryFile(recordsArray);

                Log.d(TAG, "历史记录文件已上传");
                
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "保存记录失败: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }
        });
    }

    private JSONArray downloadHistoryFile() throws Exception {
        try {
            GetObjectRequest request = new GetObjectRequest(BUCKET_NAME, HISTORY_FILE_KEY);
            GetObjectResult result = ossClient.getObject(request);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = result.getObjectContent().read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            String jsonContent = outputStream.toString("UTF-8");
            Log.d(TAG, "下载历史记录文件成功，内容长度: " + jsonContent.length());

            return new JSONArray(jsonContent);
        } catch (Exception e) {
            Log.d(TAG, "历史记录文件不存在，创建新文件");
            return new JSONArray();
        }
    }

    private void uploadHistoryFile(JSONArray recordsArray) throws Exception {
        String jsonContent = recordsArray.toString();
        byte[] data = jsonContent.getBytes(StandardCharsets.UTF_8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/json");
        metadata.setContentLength(data.length);

        PutObjectRequest request = new PutObjectRequest(BUCKET_NAME, HISTORY_FILE_KEY, data);
        request.setMetadata(metadata);

        PutObjectResult result = ossClient.putObject(request);
        Log.d(TAG, "上传历史记录文件成功，ETag: " + result.getETag());
    }

    public void clearAllRecords(ClearRecordsCallback callback) {
        executorService.execute(() -> {
            try {
                JSONArray emptyArray = new JSONArray();
                uploadHistoryFile(emptyArray);
                Log.d(TAG, "云端历史记录已清除");
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "清除云端记录失败: " + e.getMessage());
                if (callback != null) callback.onFailure(e.getMessage());
            }
        });
    }

    public interface ClearRecordsCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface LoadRecordsCallback {
        void onSuccess(List<PlantRecord> records);
        void onFailure(String error);
    }

    public void loadRecords(LoadRecordsCallback callback) {
        executorService.execute(() -> {
            try {
                JSONArray recordsArray = downloadHistoryFile();
                List<PlantRecord> records = new ArrayList<>();

                for (int i = 0; i < recordsArray.length(); i++) {
                    JSONObject json = recordsArray.getJSONObject(i);
                    records.add(PlantRecord.fromJson(json));
                }

                Log.d(TAG, "加载历史记录成功，共 " + records.size() + " 条");

                if (callback != null) {
                    callback.onSuccess(records);
                }
            } catch (Exception e) {
                Log.e(TAG, "加载历史记录失败: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }
        });
    }

    public interface SyncCallback {
        void onSyncComplete(int newRecordsCount);
        void onSyncFailed(String error);
    }

    public void syncToLocalDatabase(SyncCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "开始同步历史记录到本地数据库");
                
                JSONArray recordsArray = downloadHistoryFile();
                Repository repository = Repository.getInstance(context);
                int newRecordsCount = 0;
                
                Set<String> existingPaths = getExistingImagePaths(repository);
                Set<Long> existingTimestamps = getExistingTimestamps(repository);
                Set<Long> existingAITimestamps = getExistingAITimestamps(repository);
                Log.d(TAG, "本地已有 " + existingPaths.size() + " 条AI记录，" + existingTimestamps.size() + " 条生长记录");
                
                for (int i = 0; i < recordsArray.length(); i++) {
                    JSONObject json = recordsArray.getJSONObject(i);
                    PlantRecord record = PlantRecord.fromJson(json);
                    
                    String imageUrl = record.getImageUrl();
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        continue;
                    }
                    
                    String localPath = imageUrlToLocalPath(imageUrl);
                    long recordTimestamp = record.getTimestamp();
                    boolean aiResultExists = existingPaths.contains(localPath) || existingAITimestamps.contains(recordTimestamp);
                    if (!aiResultExists) {
                        AIResult aiResult = plantRecordToAIResult(record, localPath);
                        repository.insertAIResultFromSync(aiResult, id -> {
                            Log.d(TAG, "同步插入记录成功，ID=" + id);
                        });
                        
                        if (record.getPlantName() != null && !record.getPlantName().isEmpty()) {
                            String sessionImagePath = aiResult.getImagePath();
                            repository.getChatSessionByImagePath(sessionImagePath, session -> {
                                if (session == null) {
                                    ChatSession newSession = new ChatSession(sessionImagePath, record.getPlantName());
                                    repository.insertChatSession(newSession, sessionId -> {
                                        Log.d(TAG, "同步创建聊天会话，ID=" + sessionId);
                                        
                                        StringBuilder response = new StringBuilder();
                                        response.append("我识别了这张图片：\n\n");
                                        response.append("🌱 植物名称：").append(record.getPlantName()).append("\n");
                                        if (record.getConfidence() > 0) {
                                            response.append("📊 置信度：").append(String.format("%.1f%%", record.getConfidence() * 100)).append("\n");
                                        }
                                        if (record.getHealthStatus() != null && !record.getHealthStatus().isEmpty()) {
                                            response.append("💚 健康状态：").append(record.getHealthStatus()).append("\n");
                                        }
                                        if (record.getHealthScore() > 0) {
                                            response.append("📊 健康评分：").append(String.format("%.1f", record.getHealthScore())).append("\n");
                                        }
                                        if (record.getAnalysisText() != null && !record.getAnalysisText().isEmpty()) {
                                            response.append("\n📝 详细分析：\n").append(record.getAnalysisText()).append("\n\n");
                                        }
                                        response.append("如果识别有误，请告诉我正确的植物名称，我会重新分析~");
                                        
                                        ChatMessageEntity message = new ChatMessageEntity();
                                        message.setSessionId(sessionId);
                                        message.setContent(response.toString());
                                        message.setUser(false);
                                        message.setTimestamp(record.getTimestamp());
                                        repository.insertChatMessage(message, null);
                                    });
                                }
                            });
                        }
                        
                        newRecordsCount++;
                    } else {
                        Log.d(TAG, "跳过重复AI记录，timestamp=" + recordTimestamp);
                    }

                    if (record.getWaterTemp() > 0 || record.getAirTemp() > 0 || record.getAirHumidity() > 0) {
                        if (!existingTimestamps.contains(recordTimestamp)) {
                            GrowthRecord growthRecord = new GrowthRecord();
                            growthRecord.setImageUrl(record.getImageUrl());
                            growthRecord.setWaterTemp(record.getWaterTemp());
                            growthRecord.setAirTemp(record.getAirTemp());
                            growthRecord.setAirHumidity(record.getAirHumidity());
                            growthRecord.setHealthScore(record.getHealthScore());
                            growthRecord.setHealthStatus(record.getHealthStatus());
                            growthRecord.setPlantType(record.getPlantType());
                            growthRecord.setTimestamp(recordTimestamp);
                            repository.insertGrowthRecord(growthRecord, id -> {
                                Log.d(TAG, "同步生长记录成功，ID=" + id);
                            });
                        } else {
                            Log.d(TAG, "跳过重复生长记录，timestamp=" + recordTimestamp);
                        }
                    }
                }
                
                Log.d(TAG, "同步完成，新增 " + newRecordsCount + " 条记录");
                
                if (newRecordsCount > 0) {
                    notifyHistorySynced();
                }
                
                if (callback != null) {
                    callback.onSyncComplete(newRecordsCount);
                }
            } catch (Exception e) {
                Log.e(TAG, "同步历史记录失败: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.onSyncFailed(e.getMessage());
                }
            }
        });
    }
    
    private Set<String> getExistingImagePaths(Repository repository) {
        Set<String> paths = new HashSet<>();
        try {
            List<AIResult> existingResults = new ArrayList<>();
            repository.getRecentAIResults(1000, results -> {
                if (results != null) {
                    existingResults.addAll(results);
                }
            });
            
            Thread.sleep(500);
            
            for (AIResult result : existingResults) {
                if (result.getImagePath() != null) {
                    paths.add(extractFileName(result.getImagePath()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取已有记录失败: " + e.getMessage());
        }
        return paths;
    }
    
    private Set<Long> getExistingTimestamps(Repository repository) {
        Set<Long> timestamps = new HashSet<>();
        try {
            List<Long> existing = repository.getGrowthRecordTimestamps();
            timestamps.addAll(existing);
        } catch (Exception e) {
            Log.e(TAG, "获取已有时间戳失败: " + e.getMessage());
        }
        return timestamps;
    }
    
    private Set<Long> getExistingAITimestamps(Repository repository) {
        Set<Long> timestamps = new HashSet<>();
        try {
            List<Long> existing = repository.getAIResultTimestamps();
            timestamps.addAll(existing);
        } catch (Exception e) {
            Log.e(TAG, "获取已有AI时间戳失败: " + e.getMessage());
        }
        return timestamps;
    }
    
    private String imageUrlToLocalPath(String imageUrl) {
        return extractFileName(imageUrl);
    }
    
    private String extractFileName(String pathOrUrl) {
        if (pathOrUrl == null) return "";
        String path = pathOrUrl;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        int lastBackSlash = path.lastIndexOf('\\');
        if (lastBackSlash >= 0) {
            return path.substring(lastBackSlash + 1);
        }
        return path;
    }
    
    private AIResult plantRecordToAIResult(PlantRecord record, String localPath) {
        AIResult aiResult = new AIResult();
        aiResult.setPlantName(record.getPlantName());
        aiResult.setDetailedAnalysis(record.getAnalysisText());
        aiResult.setTimestamp(record.getTimestamp());
        
        String imageUrl = record.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String signedUrl = generateSignedUrl(imageUrl);
            aiResult.setImagePath(signedUrl);
        } else {
            aiResult.setImagePath(localPath);
        }
        
        aiResult.setConfidence(record.getConfidence());
        aiResult.setWaterTemp(record.getWaterTemp());
        aiResult.setAirTemp(record.getAirTemp());
        aiResult.setAirHumidity(record.getAirHumidity());
        aiResult.setHealthScore(record.getHealthScore());
        aiResult.setPlantType(record.getPlantType());
        if (record.getHealthStatus() != null && !record.getHealthStatus().isEmpty()) {
            aiResult.setHealthStatus(record.getHealthStatus());
        } else if (record.getHealthScore() > 0) {
            aiResult.setHealthStatus("同步数据");
        } else {
            aiResult.setHealthStatus("同步数据");
        }
        return aiResult;
    }
    
    private String generateSignedUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) {
            return originalUrl;
        }
        
        if (originalUrl.contains("OSSAccessKeyId") || originalUrl.contains("Signature")) {
            return originalUrl;
        }
        
        try {
            String objectKey = extractObjectKey(originalUrl);
            if (objectKey != null && !objectKey.isEmpty()) {
                String signedUrl = ossClient.presignConstrainedObjectURL(
                        BUCKET_NAME,
                        objectKey,
                        7 * 24 * 60 * 60
                );
                Log.d(TAG, "生成签名URL: " + signedUrl);
                return signedUrl;
            }
        } catch (Exception e) {
            Log.e(TAG, "生成签名URL失败: " + e.getMessage());
        }
        return originalUrl;
    }
    
    public String getSignedUrl(String originalUrl) {
        return generateSignedUrl(originalUrl);
    }
    
    private String extractObjectKey(String url) {
        if (url == null) return null;
        
        // 从完整URL中提取对象键
        String prefix = BUCKET_NAME + ".oss-cn-hangzhou.aliyuncs.com/";
        int index = url.indexOf(prefix);
        if (index >= 0) {
            return url.substring(index + prefix.length());
        }
        
        if (url.startsWith("plant_photos/")) {
            return url;
        }
        
        return null;
    }
    
    private void notifyHistorySynced() {
        Intent intent = new Intent(ACTION_HISTORY_SYNCED);
        context.sendBroadcast(intent);
        Log.d(TAG, "已发送历史记录同步完成广播");
    }

    public void release() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        instance = null;
    }
}
