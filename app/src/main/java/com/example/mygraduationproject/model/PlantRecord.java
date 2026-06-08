package com.example.mygraduationproject.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 植物记录模型，用于云端数据同步（OSS历史记录文件）
 * 包含一次拍照识别的完整信息，序列化为JSON存储在阿里云OSS
 * 监控端保存时序列化上传，控制端同步时反序列化下载
 */
public class PlantRecord {
    private String imageUrl;        // 图片在OSS上的路径（如"plant_20260419_170555.jpg"）
    private String plantName;       // 识别出的植物名称
    private String analysisText;    // AI分析结果文本
    private long timestamp;         // 拍照时间戳，与本地AIResult/GrowthRecord保持一致
    private float waterTemp;        // 水温（°C）
    private float airTemp;          // 气温（°C）
    private float airHumidity;      // 空气湿度（%）
    private float healthScore;      // 健康评分，0~100
    private String healthStatus;    // 健康状态描述
    private String plantType;       // 植物类型分类（"花烛"、"白掌"、"绿萝"）
    private double confidence;      // 识别置信度，0~1

    public PlantRecord() {
    }

    /** 创建PlantRecord，timestamp由外部传入以保证与本地记录一致 */
    public PlantRecord(String imageUrl, String plantName, String analysisText, long timestamp) {
        this.imageUrl = imageUrl;
        this.plantName = plantName;
        this.analysisText = analysisText;
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getAnalysisText() {
        return analysisText;
    }

    public void setAnalysisText(String analysisText) {
        this.analysisText = analysisText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public float getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(float healthScore) {
        this.healthScore = healthScore;
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

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    /** 序列化为JSON，用于上传到OSS云端存储 */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("imageUrl", imageUrl);
        json.put("plantName", plantName);
        json.put("analysisText", analysisText);
        json.put("timestamp", timestamp);
        json.put("waterTemp", waterTemp);
        json.put("airTemp", airTemp);
        json.put("airHumidity", airHumidity);
        json.put("healthScore", healthScore);
        json.put("healthStatus", healthStatus != null ? healthStatus : "");
        json.put("plantType", plantType != null ? plantType : "");
        json.put("confidence", confidence);
        return json;
    }

    /** 从JSON反序列化，用于从OSS下载后解析云端数据 */
    public static PlantRecord fromJson(JSONObject json) throws JSONException {
        PlantRecord record = new PlantRecord();
        record.setImageUrl(json.optString("imageUrl", ""));
        record.setPlantName(json.optString("plantName", ""));
        record.setAnalysisText(json.optString("analysisText", ""));
        record.setTimestamp(json.optLong("timestamp", 0));
        record.setWaterTemp((float) json.optDouble("waterTemp", 0));
        record.setAirTemp((float) json.optDouble("airTemp", 0));
        record.setAirHumidity((float) json.optDouble("airHumidity", 0));
        record.setHealthScore((float) json.optDouble("healthScore", 0));
        String status = json.optString("healthStatus", "");
        record.setHealthStatus(status.isEmpty() ? null : status);
        String plantType = json.optString("plantType", "");
        record.setPlantType(plantType.isEmpty() ? null : plantType);
        record.setConfidence(json.optDouble("confidence", 0));
        return record;
    }
}
