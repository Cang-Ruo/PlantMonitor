package com.example.mygraduationproject.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 生长记录实体类，对应数据库 growth_records 表
 * 存储每次拍照时的环境数据和健康评分，用于环境健康趋势图表展示
 */
@Entity(tableName = "growth_records", indices = {@Index("timestamp"), @Index("plantType")})
public class GrowthRecord {

    @PrimaryKey(autoGenerate = true)
    private long id;                // 主键，自增
    private String imageUrl;        // 图片URL（OSS路径或签名URL）
    private float airTemp;          // 气温（°C）
    private float airHumidity;      // 空气湿度（%）
    private float waterTemp;        // 水温（°C）
    private long timestamp;         // 记录时间戳，同时作为去重依据
    private float healthScore;      // 健康评分，0~100
    private String plantName;       // 植物名称
    private String healthStatus;    // 健康状态描述
    private String plantType;       // 植物类型分类（"花烛"、"白掌"、"绿萝"）

    public GrowthRecord() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public float getAirTemp() {
        return airTemp;
    }

    public void setAirTemp(float airTemp) {
        this.airTemp = airTemp;
    }

    public float getAirHumidity() {
        return airHumidity;
    }

    public void setAirHumidity(float airHumidity) {
        this.airHumidity = airHumidity;
    }

    public float getWaterTemp() {
        return waterTemp;
    }

    public void setWaterTemp(float waterTemp) {
        this.waterTemp = waterTemp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(float healthScore) {
        this.healthScore = healthScore;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }
}
