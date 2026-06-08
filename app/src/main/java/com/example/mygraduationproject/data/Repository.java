package com.example.mygraduationproject.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.ChatMessageEntity;
import com.example.mygraduationproject.model.ChatSession;
import com.example.mygraduationproject.model.ControlCommand;
import com.example.mygraduationproject.model.GrowthRecord;
import com.example.mygraduationproject.model.PlantImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据仓库，统一管理所有数据访问操作
 * 封装各DAO的增删改查方法，提供线程安全的异步执行机制
 * 所有数据库操作均通过线程池异步执行，避免阻塞UI线程
 */
public class Repository {
    
    private static final int MAX_RECORDS = 100; // 每种数据表保留的最大记录数
    private final PlantImageDao plantImageDao;           // 植物图片数据访问对象
    private final AIResultDao aiResultDao;               // AI识别结果数据访问对象
    private final ControlCommandDao controlCommandDao;   // 控制命令数据访问对象
    private final ChatSessionDao chatSessionDao;         // 聊天会话数据访问对象
    private final ChatMessageDao chatMessageDao;         // 聊天消息数据访问对象
    private final GrowthRecordDao growthRecordDao;       // 生长记录数据访问对象
    private final ExecutorService executorService;       // 线程池，用于异步执行数据库操作
    
    private static volatile Repository INSTANCE; // 单例实例，volatile保证多线程可见性
    
    private Repository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        plantImageDao = db.plantImageDao();
        aiResultDao = db.aiResultDao();
        controlCommandDao = db.controlCommandDao();
        chatSessionDao = db.chatSessionDao();
        chatMessageDao = db.chatMessageDao();
        growthRecordDao = db.growthRecordDao();
        executorService = Executors.newFixedThreadPool(4); // 4个核心线程的线程池
    }
    
    /** 获取Repository单例，双重检查锁定保证线程安全 */
    public static Repository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (Repository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Repository(context);
                }
            }
        }
        return INSTANCE;
    }
    
    /** 获取线程池实例，供外部需要异步执行的场景使用 */
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    /** 插入一条植物图片记录，自动将id重置为0避免主键冲突 */
    public void insertPlantImage(PlantImage image, OnResultCallback<Long> callback) {
        executorService.execute(() -> {
            image.setId(0);
            long id = plantImageDao.insert(image);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }
    
    /** 更新一条植物图片记录 */
    public void updatePlantImage(PlantImage image) {
        executorService.execute(() -> plantImageDao.update(image));
    }
    
    /** 获取最新一条植物图片 */
    public void getLatestImage(OnResultCallback<PlantImage> callback) {
        executorService.execute(() -> {
            PlantImage image = plantImageDao.getLatestImage();
            if (callback != null) {
                callback.onResult(image);
            }
        });
    }
    
    /** 获取最近指定数量的植物图片 */
    public void getRecentImages(int limit, OnResultCallback<List<PlantImage>> callback) {
        executorService.execute(() -> {
            List<PlantImage> images = plantImageDao.getRecentImages(limit);
            if (callback != null) {
                callback.onResult(images);
            }
        });
    }
    
    /**
     * 插入AI识别结果，带多层去重逻辑
     * 去重顺序：1.imageId匹配 → 2.图片路径精确匹配 → 3.文件名模糊匹配 → 4.时间戳匹配
     * 若已存在则更新，否则插入新记录
     */
    public void insertAIResult(AIResult result, OnResultCallback<Long> callback) {
        executorService.execute(() -> {
            AIResult existing = null;
            // 第一层：根据图片ID查找
            if (result.getImageId() > 0) {
                existing = aiResultDao.getByImageId(result.getImageId());
            }
            // 第二层：根据图片完整路径精确查找
            if (existing == null && result.getImagePath() != null) {
                existing = aiResultDao.getByImagePath(result.getImagePath());
            }
            // 第三层：根据文件名模糊查找（兼容签名URL与本地路径差异）
            if (existing == null && result.getImagePath() != null) {
                String fileName = extractFileName(result.getImagePath());
                if (!fileName.isEmpty()) {
                    existing = aiResultDao.getByFileName(fileName);
                }
            }
            // 第四层：根据时间戳查找（防止同一拍照产生不同文件名）
            if (existing == null) {
                existing = aiResultDao.getByTimestamp(result.getTimestamp());
            }
            
            if (existing != null) {
                // 已存在则更新植物信息
                result.setId(existing.getId());
                aiResultDao.updatePlantInfo(
                    existing.getId(),
                    result.getPlantName(),
                    result.getConfidence(),
                    result.getHealthStatus(),
                    result.getDetailedAnalysis(),
                    result.getHealthScore(),
                    result.getWaterTemp(),
                    result.getAirTemp(),
                    result.getAirHumidity(),
                    result.getPlantType()
                );
                android.util.Log.i("Repository", "已更新现有记录，ID=" + existing.getId() + ", plantName=" + result.getPlantName() + ", healthScore=" + result.getHealthScore());
                aiResultDao.deleteOldResults(MAX_RECORDS);
                if (callback != null) {
                    callback.onResult(existing.getId());
                }
            } else {
                // 不存在则插入新记录
                long id = aiResultDao.insert(result);
                aiResultDao.deleteOldResults(MAX_RECORDS);
                android.util.Log.i("Repository", "已插入新记录，ID=" + id + ", plantName=" + result.getPlantName() + ", healthScore=" + result.getHealthScore() + ", waterTemp=" + result.getWaterTemp() + ", airTemp=" + result.getAirTemp() + ", airHumidity=" + result.getAirHumidity());
                if (callback != null) {
                    callback.onResult(id);
                }
            }
        });
    }
    
    /** 更新一条AI识别结果 */
    public void updateAIResult(AIResult result) {
        executorService.execute(() -> aiResultDao.update(result));
    }
    
    /**
     * 安全更新植物信息（避免外键约束错误）
     * 先检查记录是否存在，存在则更新，不存在则返回false
     */
    public void updatePlantInfoSafely(long id, String plantName, double confidence, String healthStatus, String detailedAnalysis, float healthScore, float waterTemp, float airTemp, float airHumidity, String plantType, OnResultCallback<Boolean> callback) {
        executorService.execute(() -> {
            AIResult existing = aiResultDao.getById(id);
            if (existing != null) {
                aiResultDao.updatePlantInfo(id, plantName, confidence, healthStatus, detailedAnalysis, healthScore, waterTemp, airTemp, airHumidity, plantType);
                android.util.Log.i("Repository", "成功更新植物信息，ID=" + id + ", plantName=" + plantName + ", healthScore=" + healthScore);
                if (callback != null) {
                    callback.onResult(true);
                }
            } else {
                android.util.Log.e("Repository", "找不到记录，ID=" + id + "，无法更新");
                if (callback != null) {
                    callback.onResult(false);
                }
            }
        });
    }
    
    /** 获取最新一条AI识别结果 */
    public void getLatestAIResult(OnResultCallback<AIResult> callback) {
        executorService.execute(() -> {
            AIResult result = aiResultDao.getLatestResult();
            if (callback != null) {
                callback.onResult(result);
            }
        });
    }
    
    /** 获取最近指定数量的AI识别结果 */
    public void getRecentAIResults(int limit, OnResultCallback<List<AIResult>> callback) {
        executorService.execute(() -> {
            List<AIResult> results = aiResultDao.getRecentResults(limit);
            if (callback != null) {
                callback.onResult(results);
            }
        });
    }
    
    /**
     * 根据图片路径查找AI识别结果
     * 先精确匹配路径，再模糊匹配文件名（兼容签名URL）
     */
    public void getAIResultByImagePath(String imagePath, OnResultCallback<AIResult> callback) {
        executorService.execute(() -> {
            AIResult result = null;
            // 先精确匹配完整路径
            if (imagePath != null) {
                result = aiResultDao.getByImagePath(imagePath);
            }
            // 精确匹配失败则按文件名模糊匹配
            if (result == null && imagePath != null) {
                String fileName = extractFileName(imagePath);
                if (!fileName.isEmpty()) {
                    result = aiResultDao.getByFileName(fileName);
                }
            }
            if (callback != null) {
                callback.onResult(result);
            }
        });
    }
    
    /** 检查指定图片路径的AI识别结果是否已存在 */
    public void checkRecordExists(String imagePath, OnResultCallback<Boolean> callback) {
        executorService.execute(() -> {
            AIResult result = aiResultDao.getByImagePath(imagePath);
            boolean exists = result != null;
            if (callback != null) {
                callback.onResult(exists);
            }
        });
    }
    
    /**
     * 从云端同步插入AI识别结果
     * 与insertAIResult类似，但imageId强制为0（云端数据无本地图片关联）
     * 去重顺序：1.路径精确匹配 → 2.文件名模糊匹配 → 3.时间戳匹配
     */
    public void insertAIResultFromSync(AIResult result, OnResultCallback<Long> callback) {
        executorService.execute(() -> {
            result.setImageId(0); // 云端同步数据无本地图片ID关联
            
            AIResult existing = null;
            
            // 第一层：根据图片完整路径精确查找
            if (result.getImagePath() != null) {
                existing = aiResultDao.getByImagePath(result.getImagePath());
            }
            
            // 第二层：根据文件名模糊查找（兼容签名URL与本地路径差异）
            if (existing == null && result.getImagePath() != null) {
                String fileName = extractFileName(result.getImagePath());
                if (!fileName.isEmpty()) {
                    existing = aiResultDao.getByFileName(fileName);
                }
            }
            
            // 第三层：根据时间戳查找
            if (existing == null) {
                existing = aiResultDao.getByTimestamp(result.getTimestamp());
            }
            
            if (existing != null) {
                // 已存在则更新
                result.setId(existing.getId());
                aiResultDao.updatePlantInfo(
                    existing.getId(),
                    result.getPlantName(),
                    result.getConfidence(),
                    result.getHealthStatus(),
                    result.getDetailedAnalysis(),
                    result.getHealthScore(),
                    result.getWaterTemp(),
                    result.getAirTemp(),
                    result.getAirHumidity(),
                    result.getPlantType()
                );
                android.util.Log.i("Repository", "同步更新已有记录，ID=" + existing.getId());
                if (callback != null) {
                    callback.onResult(existing.getId());
                }
                return;
            }
            
            // 不存在则插入
            try {
                long id = aiResultDao.insert(result);
                aiResultDao.deleteOldResults(MAX_RECORDS);
                android.util.Log.i("Repository", "同步插入记录，ID=" + id + ", plantName=" + result.getPlantName() + ", healthScore=" + result.getHealthScore());
                if (callback != null) {
                    callback.onResult(id);
                }
            } catch (Exception e) {
                android.util.Log.e("Repository", "同步插入记录失败: " + e.getMessage() + ", plantName=" + result.getPlantName());
                if (callback != null) {
                    callback.onResult(-1L);
                }
            }
        });
    }
    
    /** 插入一条控制命令，并清理超出上限的旧记录 */
    public void insertControlCommand(ControlCommand command, OnResultCallback<Long> callback) {
        executorService.execute(() -> {
            long id = controlCommandDao.insert(command);
            controlCommandDao.deleteOldCommands(MAX_RECORDS);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }
    
    /** 更新一条控制命令 */
    public void updateControlCommand(ControlCommand command) {
        executorService.execute(() -> controlCommandDao.update(command));
    }
    
    /** 获取最近指定数量的控制命令 */
    public void getRecentControlCommands(int limit, OnResultCallback<List<ControlCommand>> callback) {
        executorService.execute(() -> {
            List<ControlCommand> commands = controlCommandDao.getRecentCommands(limit);
            if (callback != null) {
                callback.onResult(commands);
            }
        });
    }
    
    /** 根据设备编码获取该设备最新一条控制命令 */
    public void getLatestCommandByDevice(String deviceCode, OnResultCallback<ControlCommand> callback) {
        executorService.execute(() -> {
            ControlCommand command = controlCommandDao.getLatestByDevice(deviceCode);
            if (callback != null) {
                callback.onResult(command);
            }
        });
    }
    
    /** 插入一条聊天会话，并清理超出上限的旧会话 */
    public void insertChatSession(ChatSession session, OnResultCallback<Long> callback) {
        executorService.execute(() -> {
            long id = chatSessionDao.insert(session);
            chatSessionDao.deleteOldSessions(MAX_RECORDS);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }
    
    /** 更新一条聊天会话 */
    public void updateChatSession(ChatSession session) {
        executorService.execute(() -> chatSessionDao.update(session));
    }
    
    /**
     * 根据图片路径查找关联的聊天会话
     * 先精确匹配路径，再模糊匹配文件名（兼容签名URL）
     */
    public void getChatSessionByImagePath(String imagePath, OnResultCallback<ChatSession> callback) {
        executorService.execute(() -> {
            ChatSession session = null;
            // 先精确匹配完整路径
            if (imagePath != null) {
                session = chatSessionDao.getByImagePath(imagePath);
            }
            // 精确匹配失败则按文件名模糊匹配
            if (session == null && imagePath != null) {
                String fileName = extractFileName(imagePath);
                if (!fileName.isEmpty()) {
                    session = chatSessionDao.getByFileName(fileName);
                }
            }
            if (callback != null) {
                callback.onResult(session);
            }
        });
    }
    
    /** 获取全部聊天会话，按更新时间倒序 */
    public void getAllChatSessions(OnResultCallback<List<ChatSession>> callback) {
        executorService.execute(() -> {
            List<ChatSession> sessions = chatSessionDao.getAllSessions();
            if (callback != null) {
                callback.onResult(sessions);
            }
        });
    }
    
    /** 根据会话ID获取聊天会话 */
    public void getChatSessionById(long id, OnResultCallback<ChatSession> callback) {
        executorService.execute(() -> {
            ChatSession session = chatSessionDao.getById(id);
            if (callback != null) {
                callback.onResult(session);
            }
        });
    }
    
    /** 插入一条聊天消息 */
    public void insertChatMessage(ChatMessageEntity message, OnResultCallback<Long> callback) {
        executorService.execute(() -> {
            long id = chatMessageDao.insert(message);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }
    
    /** 根据会话ID获取该会话下的所有聊天消息 */
    public void getMessagesBySession(long sessionId, OnResultCallback<List<ChatMessageEntity>> callback) {
        executorService.execute(() -> {
            List<ChatMessageEntity> messages = chatMessageDao.getMessagesBySession(sessionId);
            if (callback != null) {
                callback.onResult(messages);
            }
        });
    }
    
    /** 清除所有本地缓存数据（包括所有表） */
    public void clearAllCache(OnResultCallback<Boolean> callback) {
        executorService.execute(() -> {
            plantImageDao.deleteAll();
            aiResultDao.deleteAll();
            controlCommandDao.deleteAll();
            chatSessionDao.deleteAll();
            chatMessageDao.deleteAll();
            growthRecordDao.deleteAll();
            if (callback != null) {
                callback.onResult(true);
            }
        });
    }

    /**
     * 插入一条生长记录，带时间戳去重
     * 若该时间戳已存在记录则跳过插入，防止数据同步时产生重复记录
     */
    public void insertGrowthRecord(GrowthRecord record, OnResultCallback<Long> callback) {
        executorService.execute(() -> {
            int count = growthRecordDao.countByTimestamp(record.getTimestamp());
            if (count > 0) {
                // 时间戳已存在，跳过重复插入
                android.util.Log.i("Repository", "跳过重复生长记录，timestamp=" + record.getTimestamp());
                if (callback != null) {
                    callback.onResult(-1L);
                }
                return;
            }
            long id = growthRecordDao.insert(record);
            growthRecordDao.deleteOldRecords(MAX_RECORDS);
            android.util.Log.i("Repository", "插入生长记录，ID=" + id);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }

    /** 获取最近指定数量的生长记录 */
    public void getRecentGrowthRecords(int limit, OnResultCallback<List<GrowthRecord>> callback) {
        executorService.execute(() -> {
            List<GrowthRecord> records = growthRecordDao.getRecentRecords(limit);
            if (callback != null) {
                callback.onResult(records);
            }
        });
    }

    /** 获取最新一条生长记录 */
    public void getLatestGrowthRecord(OnResultCallback<GrowthRecord> callback) {
        executorService.execute(() -> {
            GrowthRecord record = growthRecordDao.getLatestRecord();
            if (callback != null) {
                callback.onResult(record);
            }
        });
    }

    /** 按植物类型（如花烛、白掌、绿萝）筛选，获取最近指定数量的AI识别结果 */
    public void getRecentAIResultsByPlantType(String plantType, int limit, OnResultCallback<List<AIResult>> callback) {
        executorService.execute(() -> {
            List<AIResult> results = aiResultDao.getRecentResultsByPlantType(plantType, limit);
            if (callback != null) {
                callback.onResult(results);
            }
        });
    }

    /** 按植物类型（如花烛、白掌、绿萝）筛选，获取最近指定数量的生长记录 */
    public void getRecentGrowthRecordsByPlantType(String plantType, int limit, OnResultCallback<List<GrowthRecord>> callback) {
        executorService.execute(() -> {
            List<GrowthRecord> records = growthRecordDao.getRecentRecordsByPlantType(plantType, limit);
            if (callback != null) {
                callback.onResult(records);
            }
        });
    }

    /** 更新生长记录的植物类型和健康评分 */
    public void updateGrowthRecordTypeAndScore(long recordId, String plantType, float healthScore, String healthStatus) {
        executorService.execute(() -> {
            growthRecordDao.updateTypeAndScore(recordId, plantType, healthScore, healthStatus);
            android.util.Log.i("Repository", "更新生长记录植物类型和评分，ID=" + recordId + ", plantType=" + plantType + ", healthScore=" + healthScore);
        });
    }
    
    /** 清除所有AI识别结果和生长记录数据 */
    public void clearAllData() {
        executorService.execute(() -> {
            aiResultDao.deleteAll();
            growthRecordDao.deleteAll();
            android.util.Log.i("Repository", "所有本地数据已清除");
        });
    }

    /** 观察最近指定数量的生长记录（LiveData方式，数据变化时自动通知UI更新） */
    public LiveData<List<GrowthRecord>> observeRecentGrowthRecords(int limit) {
        return growthRecordDao.observeRecentRecords(limit);
    }

    /** 按植物类型观察最近指定数量的生长记录 */
    public LiveData<List<GrowthRecord>> observeRecentGrowthRecordsByPlantType(String plantType, int limit) {
        return growthRecordDao.observeRecentRecordsByPlantType(plantType, limit);
    }

    /** 观察最近指定数量的AI识别结果 */
    public LiveData<List<AIResult>> observeRecentAIResults(int limit) {
        return aiResultDao.observeRecentResults(limit);
    }

    /** 观察全部生长记录 */
    public LiveData<List<GrowthRecord>> observeAllGrowthRecords() {
        return growthRecordDao.observeAllRecords();
    }

    /** 观察全部AI识别结果 */
    public LiveData<List<AIResult>> observeAllAIResults() {
        return aiResultDao.observeAllResults();
    }

    /** 获取所有生长记录的时间戳列表（用于同步去重检查） */
    public List<Long> getGrowthRecordTimestamps() {
        try {
            return growthRecordDao.getAllTimestamps();
        } catch (Exception e) {
            android.util.Log.e("Repository", "获取时间戳失败: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /** 获取所有AI识别结果的时间戳列表（用于同步去重检查） */
    public List<Long> getAIResultTimestamps() {
        try {
            return aiResultDao.getAllTimestamps();
        } catch (Exception e) {
            android.util.Log.e("Repository", "获取AI时间戳失败: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    /** 异步操作结果回调接口 */
    public interface OnResultCallback<T> {
        void onResult(T result);
    }

    /**
     * 从路径或URL中提取文件名
     * 处理逻辑：先去除URL查询参数，再提取最后一个斜杠后的部分
     * 例如：https://oss.com/plant_20260419.jpg?sign=xxx → plant_20260419.jpg
     */
    private String extractFileName(String pathOrUrl) {
        if (pathOrUrl == null) return "";
        String path = pathOrUrl;
        // 去除URL查询参数（?之后的部分）
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        // 提取正斜杠后的文件名
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        // 提取反斜杠后的文件名（Windows路径兼容）
        int lastBackSlash = path.lastIndexOf('\\');
        if (lastBackSlash >= 0) {
            return path.substring(lastBackSlash + 1);
        }
        return path;
    }
}
