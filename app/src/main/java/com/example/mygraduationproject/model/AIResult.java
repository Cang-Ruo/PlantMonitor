package com.example.mygraduationproject.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * AI识别结果实体类，对应数据库 ai_results 表
 * 存储每次拍照后百度AI识别的植物信息、健康状态及环境数据
 */
@Entity(tableName = "ai_results",
        indices = {@Index("imageId")})
public class AIResult {
    
    @PrimaryKey(autoGenerate = true)
    private long id;                // 主键，自增
    private long imageId;           // 关联的PlantImage的ID
    private String plantName;       // 识别出的植物名称（如"花烛"、"白掌"、"绿萝"）
    private double confidence;      // 识别置信度，0~1之间，越接近1越可信
    private String healthStatus;    // 健康状态描述（如"健康"、"亚健康"）
    private String diseaseName;     // 病害名称（如有）
    private String diseaseDescription; // 病害详细描述
    private String rawResult;       // 百度AI返回的原始JSON结果
    private String userInput;       // 用户手动输入的纠正名称
    private String suggestion;      // 养护建议
    private String imagePath;       // 图片路径（本地路径或OSS签名URL）
    private long timestamp;         // 记录时间戳
    private String candidates;      // 其他候选识别结果
    private String detailedAnalysis;// 详细分析文本
    private float healthScore;      // 健康评分，0~100
    private float waterTemp;        // 水温（°C）
    private float airTemp;          // 气温（°C）
    private float airHumidity;      // 空气湿度（%）
    private String plantType;       // 植物类型分类（"花烛"、"白掌"、"绿萝"）
    
    public AIResult() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getImageId() {
        return imageId;
    }
    
    public void setImageId(long imageId) {
        this.imageId = imageId;
    }
    
    public String getPlantName() {
        return plantName;
    }
    
    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public String getHealthStatus() {
        return healthStatus;
    }
    
    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }
    
    public String getDiseaseName() {
        return diseaseName;
    }
    
    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }
    
    public String getDiseaseDescription() {
        return diseaseDescription;
    }
    
    public void setDiseaseDescription(String diseaseDescription) {
        this.diseaseDescription = diseaseDescription;
    }
    
    public String getRawResult() {
        return rawResult;
    }
    
    public void setRawResult(String rawResult) {
        this.rawResult = rawResult;
    }
    
    public String getUserInput() {
        return userInput;
    }
    
    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getCandidates() {
        return candidates;
    }
    
    public void setCandidates(String candidates) {
        this.candidates = candidates;
    }
    
    public String getDetailedAnalysis() {
        return detailedAnalysis;
    }
    
    public void setDetailedAnalysis(String detailedAnalysis) {
        this.detailedAnalysis = detailedAnalysis;
    }

    public float getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(float healthScore) {
        this.healthScore = healthScore;
    }

    public float getWaterTemp() {
        return waterTemp;
    }

    public void setWaterTemp(float waterTemp) {
        this.waterTemp = waterTemp;
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

    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }
}
