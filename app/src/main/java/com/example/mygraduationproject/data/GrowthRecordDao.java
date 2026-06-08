package com.example.mygraduationproject.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mygraduationproject.model.GrowthRecord;

import java.util.List;

/**
 * 生长记录数据访问对象，提供growth_records表的增删改查操作
 * 用于管理花烛、白掌、绿萝等植物的生长监测记录
 */
@Dao
public interface GrowthRecordDao {

    /** 插入一条生长记录，返回自增ID */
    @Insert
    long insert(GrowthRecord record);

    /** 更新一条生长记录 */
    @Update
    void update(GrowthRecord record);

    /**
     * 根据ID更新植物类型、健康评分和健康状态
     * @param id 记录ID
     * @param plantType 植物类型（如花烛、白掌、绿萝）
     * @param healthScore 健康评分
     * @param healthStatus 健康状态描述
     */
    @Query("UPDATE growth_records SET plantType = :plantType, healthScore = :healthScore, healthStatus = :healthStatus WHERE id = :id")
    void updateTypeAndScore(long id, String plantType, float healthScore, String healthStatus);

    /** 删除一条生长记录 */
    @Delete
    void delete(GrowthRecord record);

    /** 按时间倒序获取全部生长记录 */
    @Query("SELECT * FROM growth_records ORDER BY timestamp DESC")
    List<GrowthRecord> getAllRecords();

    /** 按时间倒序获取最近指定数量的生长记录 */
    @Query("SELECT * FROM growth_records ORDER BY timestamp DESC LIMIT :limit")
    List<GrowthRecord> getRecentRecords(int limit);

    /** 根据记录ID获取生长记录 */
    @Query("SELECT * FROM growth_records WHERE id = :id")
    GrowthRecord getById(long id);

    /** 获取最新一条生长记录（按时间倒序取第一条） */
    @Query("SELECT * FROM growth_records ORDER BY timestamp DESC LIMIT 1")
    GrowthRecord getLatestRecord();

    /** 获取生长记录总条数 */
    @Query("SELECT COUNT(*) FROM growth_records")
    int getCount();

    /**
     * 删除旧记录，仅保留最近keepCount条
     * 通过子查询找出最新的keepCount条记录，删除不在其中的旧记录
     */
    @Query("DELETE FROM growth_records WHERE id NOT IN (SELECT id FROM growth_records ORDER BY timestamp DESC LIMIT :keepCount)")
    void deleteOldRecords(int keepCount);

    /** 删除全部生长记录 */
    @Query("DELETE FROM growth_records")
    void deleteAll();

    /** 按植物类型（如花烛、白掌、绿萝）筛选，获取最近指定数量的生长记录 */
    @Query("SELECT * FROM growth_records WHERE plantType = :plantType ORDER BY timestamp DESC LIMIT :limit")
    List<GrowthRecord> getRecentRecordsByPlantType(String plantType, int limit);

    /** 观察全部生长记录（LiveData方式，数据变化时自动通知UI更新） */
    @Query("SELECT * FROM growth_records ORDER BY timestamp DESC")
    LiveData<List<GrowthRecord>> observeAllRecords();

    /** 按植物类型（如花烛、白掌、绿萝）筛选，观察该类型的全部生长记录 */
    @Query("SELECT * FROM growth_records WHERE plantType = :plantType ORDER BY timestamp DESC")
    LiveData<List<GrowthRecord>> observeRecordsByPlantType(String plantType);

    /** 观察最近指定数量的生长记录（LiveData方式，数据变化时自动通知UI更新） */
    @Query("SELECT * FROM growth_records ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<GrowthRecord>> observeRecentRecords(int limit);

    /** 按植物类型（如花烛、白掌、绿萝）筛选，观察最近指定数量的生长记录 */
    @Query("SELECT * FROM growth_records WHERE plantType = :plantType ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<GrowthRecord>> observeRecentRecordsByPlantType(String plantType, int limit);

    /** 获取所有生长记录的时间戳列表 */
    @Query("SELECT timestamp FROM growth_records")
    List<Long> getAllTimestamps();

    /** 根据时间戳精确查找生长记录（限制1条） */
    @Query("SELECT * FROM growth_records WHERE timestamp = :timestamp LIMIT 1")
    GrowthRecord getByTimestamp(long timestamp);

    /** 统计指定时间戳对应的记录条数（用于判断该时间戳是否已存在记录，防止重复插入） */
    @Query("SELECT COUNT(*) FROM growth_records WHERE timestamp = :timestamp")
    int countByTimestamp(long timestamp);
}
