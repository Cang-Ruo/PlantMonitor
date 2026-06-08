package com.example.mygraduationproject.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mygraduationproject.model.PlantImage;

import java.util.List;

/**
 * 植物图片数据访问对象，提供plant_images表的增删改查操作
 * 用于管理花烛、白掌、绿萝等植物的拍摄图片记录
 */
@Dao
public interface PlantImageDao {

    /** 插入一条植物图片记录，返回自增ID */
    @Insert
    long insert(PlantImage image);

    /** 更新一条植物图片记录 */
    @Update
    void update(PlantImage image);

    /** 删除一条植物图片记录 */
    @Delete
    void delete(PlantImage image);

    /** 按时间倒序获取全部植物图片 */
    @Query("SELECT * FROM plant_images ORDER BY timestamp DESC")
    List<PlantImage> getAllImages();

    /** 按时间倒序获取最近指定数量的植物图片 */
    @Query("SELECT * FROM plant_images ORDER BY timestamp DESC LIMIT :limit")
    List<PlantImage> getRecentImages(int limit);

    /** 根据记录ID获取植物图片 */
    @Query("SELECT * FROM plant_images WHERE id = :id")
    PlantImage getById(long id);

    /** 获取最新一条植物图片（按时间倒序取第一条） */
    @Query("SELECT * FROM plant_images ORDER BY timestamp DESC LIMIT 1")
    PlantImage getLatestImage();

    /** 获取植物图片总条数 */
    @Query("SELECT COUNT(*) FROM plant_images")
    int getCount();

    /** 获取尚未上传到服务器的图片数量（uploaded=0表示未上传） */
    @Query("SELECT COUNT(*) FROM plant_images WHERE uploaded = 0")
    int getUnuploadedCount();

    /**
     * 删除旧图片记录，仅保留最近keepCount条
     * 通过子查询找出最新的keepCount条记录，删除不在其中的旧记录
     */
    @Query("DELETE FROM plant_images WHERE id NOT IN (SELECT id FROM plant_images ORDER BY timestamp DESC LIMIT :keepCount)")
    void deleteOldImages(int keepCount);

    /** 删除全部植物图片记录 */
    @Query("DELETE FROM plant_images")
    void deleteAll();
}
