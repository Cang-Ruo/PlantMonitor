package com.example.mygraduationproject.model;

/**
 * 健康报告模型类，用于生成植物生长健康报告
 * 由AnalysisViewModel根据GrowthRecord数据统计生成
 */
public class HealthReport {

    private final String plantType;     // 植物类型（"花烛"、"白掌"、"绿萝"）
    private final int photoCount;       // 拍照记录数
    private final float avgHealthScore; // 平均健康评分
    private final float maxWaterTemp;   // 最高水温（°C）
    private final float minWaterTemp;   // 最低水温（°C）
    private final float maxAirTemp;     // 最高气温（°C）
    private final float minAirTemp;     // 最低气温（°C）
    private final float avgAirHumidity; // 平均空气湿度（%）
    private final float waterTempRange; // 水温波动范围（最高-最低）
    private final String healthLevel;   // 健康等级（如"优秀"、"良好"、"一般"）
    private final String summary;       // 综合评估摘要
    private final long startDate;       // 统计起始时间
    private final long endDate;         // 统计结束时间
    private final int daySpan;          // 统计天数跨度

    public HealthReport(String plantType, int photoCount, float avgHealthScore,
                        float maxWaterTemp, float minWaterTemp,
                        float maxAirTemp, float minAirTemp,
                        float avgAirHumidity, float waterTempRange,
                        String healthLevel, String summary,
                        long startDate, long endDate, int daySpan) {
        this.plantType = plantType;
        this.photoCount = photoCount;
        this.avgHealthScore = avgHealthScore;
        this.maxWaterTemp = maxWaterTemp;
        this.minWaterTemp = minWaterTemp;
        this.maxAirTemp = maxAirTemp;
        this.minAirTemp = minAirTemp;
        this.avgAirHumidity = avgAirHumidity;
        this.waterTempRange = waterTempRange;
        this.healthLevel = healthLevel;
        this.summary = summary;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daySpan = daySpan;
    }

    public String getPlantType() { return plantType; }
    public int getPhotoCount() { return photoCount; }
    public float getAvgHealthScore() { return avgHealthScore; }
    public float getMaxWaterTemp() { return maxWaterTemp; }
    public float getMinWaterTemp() { return minWaterTemp; }
    public float getMaxAirTemp() { return maxAirTemp; }
    public float getMinAirTemp() { return minAirTemp; }
    public float getAvgAirHumidity() { return avgAirHumidity; }
    public float getWaterTempRange() { return waterTempRange; }
    public String getHealthLevel() { return healthLevel; }
    public String getSummary() { return summary; }
    public long getStartDate() { return startDate; }
    public long getEndDate() { return endDate; }
    public int getDaySpan() { return daySpan; }

    /** 根据天数跨度返回报告标题 */
    public String getPeriodTitle() {
        if (daySpan <= 1) return "植物生长健康日报";
        if (daySpan <= 2) return "植物生长健康2日报告";
        if (daySpan <= 3) return "植物生长健康3日报告";
        return "植物生长健康周报";
    }

    /** 根据天数跨度返回统计周期描述 */
    public String getPeriodDesc() {
        if (daySpan <= 1) return "今日";
        if (daySpan <= 2) return "近2天";
        if (daySpan <= 3) return "近3天";
        return "最近7天";
    }

    /** 生成完整的文本格式健康报告 */
    public String generateFullReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================\n");
        sb.append("  ").append(getPeriodTitle()).append("\n");
        sb.append("============================\n\n");
        sb.append("植物类型：").append(plantType).append("\n");
        sb.append("统计周期：").append(getPeriodDesc()).append("\n\n");
        sb.append("【数据概览】\n");
        sb.append("  拍照记录数：").append(photoCount).append(" 次\n");
        sb.append("  平均健康评分：").append(String.format("%.1f", avgHealthScore)).append(" 分\n");
        sb.append("  健康等级：").append(healthLevel).append("\n\n");
        sb.append("【水温分析】\n");
        sb.append("  最高水温：").append(String.format("%.1f", maxWaterTemp)).append(" C\n");
        sb.append("  最低水温：").append(String.format("%.1f", minWaterTemp)).append(" C\n");
        sb.append("  水温波动：").append(String.format("%.1f", waterTempRange)).append(" C\n\n");
        sb.append("【气温分析】\n");
        sb.append("  最高气温：").append(String.format("%.1f", maxAirTemp)).append(" C\n");
        sb.append("  最低气温：").append(String.format("%.1f", minAirTemp)).append(" C\n\n");
        sb.append("【湿度分析】\n");
        sb.append("  平均湿度：").append(String.format("%.1f", avgAirHumidity)).append("%\n\n");
        sb.append("============================\n");
        sb.append("【综合评估】\n");
        sb.append(summary).append("\n");
        sb.append("============================\n");
        return sb.toString();
    }
}
