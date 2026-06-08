package com.example.mygraduationproject.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mygraduationproject.model.AIResult;

import java.util.List;

/**
 * AI识别结果数据访问对象，提供ai_results表的增删改查操作
 * 用于管理花烛、白掌、绿萝等植物的AI识别结果数据
 */
@Dao
public interface AIResultDao {

    /** 插入一条AI识别结果，返回自增ID */
    @Insert
    long insert(AIResult result);

    /** 更新一条AI识别结果 */
    @Update
    void update(AIResult result);

    /** 删除一条AI识别结果 */
    @Delete
    void delete(AIResult result);

    /** 按时间倒序获取全部识别结果 */
    @Query("SELECT * FROM ai_results ORDER BY timestamp DESC")
    List<AIResult> getAllResults();

    /** 按时间倒序获取最近指定数量的识别结果 */
    @Query("SELECT * FROM ai_results ORDER BY timestamp DESC LIMIT :limit")
    List<AIResult> getRecentResults(int limit);

    /** 根据记录ID获取识别结果 */
    @Query("SELECT * FROM ai_results WHERE id = :id")
    AIResult getById(long id);

    /** 根据图片ID获取关联的识别结果 */
    @Query("SELECT * FROM ai_results WHERE imageId = :imageId")
    AIResult getByImageId(long imageId);

    /** 根据图片完整路径精确查找识别结果（限制1条） */
    @Query("SELECT * FROM ai_results WHERE imagePath = :imagePath LIMIT 1")
    AIResult getByImagePath(String imagePath);

    /** 获取最新一条识别结果（按时间倒序取第一条） */
    @Query("SELECT * FROM ai_results ORDER BY timestamp DESC LIMIT 1")
    AIResult getLatestResult();

    /** 获取识别结果总条数 */
    @Query("SELECT COUNT(*) FROM ai_results")
    int getCount();

    /**
     * 删除旧记录，仅保留最近keepCount条
     * 通过子查询找出最新的keepCount条记录，删除不在其中的旧记录
     */
    @Query("DELETE FROM ai_results WHERE id NOT IN (SELECT id FROM ai_results ORDER BY timestamp DESC LIMIT :keepCount)")
    void deleteOldResults(int keepCount);

    /** 删除全部识别结果 */
    @Query("DELETE FROM ai_results")
    void deleteAll();

    /**
     * 根据ID更新植物识别信息
     * @param id 记录ID
     * @param plantName 植物名称（如花烛、白掌、绿萝）
     * @param confidence 识别置信度
     * @param healthStatus 健康状态描述
     * @param detailedAnalysis 详细分析结果
     * @param healthScore 健康评分
     * @param waterTemp 水温
     * @param airTemp 气温
     * @param airHumidity 空气湿度
     * @param plantType 植物类型（如花烛、白掌、绿萝）
     */
    @Query("UPDATE ai_results SET plantName = :plantName, confidence = :confidence, healthStatus = :healthStatus, detailedAnalysis = :detailedAnalysis, healthScore = :healthScore, waterTemp = :waterTemp, airTemp = :airTemp, airHumidity = :airHumidity, plantType = :plantType WHERE id = :id")
    void updatePlantInfo(long id, String plantName, double confidence, String healthStatus, String detailedAnalysis, float healthScore, float waterTemp, float airTemp, float airHumidity, String plantType);

    /** 按植物类型（如花烛、白掌、绿萝）筛选，获取最近指定数量的识别结果 */
    @Query("SELECT * FROM ai_results WHERE plantType = :plantType ORDER BY timestamp DESC LIMIT :limit")
    List<AIResult> getRecentResultsByPlantType(String plantType, int limit);

    /** 观察全部识别结果（LiveData方式，数据变化时自动通知UI更新） */
    @Query("SELECT * FROM ai_results ORDER BY timestamp DESC")
    LiveData<List<AIResult>> observeAllResults();

    /** 根据文件名模糊匹配查找识别结果（用于签名URL与本地路径的兼容匹配） */
    @Query("SELECT * FROM ai_results WHERE imagePath LIKE '%' || :fileName || '%' LIMIT 1")
    AIResult getByFileName(String fileName);

    /** 根据时间戳精确查找识别结果（限制1条） */
    @Query("SELECT * FROM ai_results WHERE timestamp = :timestamp LIMIT 1")
    AIResult getByTimestamp(long timestamp);

    /** 获取所有识别结果的时间戳列表 */
    @Query("SELECT timestamp FROM ai_results")
    List<Long> getAllTimestamps();

    /** 观察最近指定数量的识别结果（LiveData方式，数据变化时自动通知UI更新） */
    @Query("SELECT * FROM ai_results ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<AIResult>> observeRecentResults(int limit);
}
