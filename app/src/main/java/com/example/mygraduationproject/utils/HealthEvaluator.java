package com.example.mygraduationproject.utils;

/**
 * 植物健康评估器
 * 根据水温、气温、湿度等环境数据和AI识别置信度，计算花烛、白掌、绿萝的健康评分
 * 评分权重：水温40%、湿度30%、气温20%、AI置信度10%
 */
public class HealthEvaluator {

    /** 环境数据封装类，包含评估所需的四项数据 */
    public static class EnvironmentData {
        public final float waterTemp;    // 水温（℃）
        public final float airHumidity;  // 空气湿度（%）
        public final float airTemp;      // 气温（℃）
        public final float aiConfidence; // AI识别置信度（0~1）

        public EnvironmentData(float waterTemp, float airHumidity, float airTemp, float aiConfidence) {
            this.waterTemp = waterTemp;
            this.airHumidity = airHumidity;
            this.airTemp = airTemp;
            this.aiConfidence = aiConfidence;
        }
    }

    /** 植物环境阈值配置，定义各植物适宜的水温、湿度、气温范围 */
    public static class PlantThresholds {
        public final float waterTempMin, waterTempMax; // 水温适宜范围
        public final float humidityMin, humidityMax;   // 湿度适宜范围
        public final float airTempMin, airTempMax;     // 气温适宜范围
        public final String displayName;               // 植物显示名称

        public PlantThresholds(String displayName,
                               float waterTempMin, float waterTempMax,
                               float humidityMin, float humidityMax,
                               float airTempMin, float airTempMax) {
            this.displayName = displayName;
            this.waterTempMin = waterTempMin;
            this.waterTempMax = waterTempMax;
            this.humidityMin = humidityMin;
            this.humidityMax = humidityMax;
            this.airTempMin = airTempMin;
            this.airTempMax = airTempMax;
        }
    }

    /** 花烛阈值：水温18~28℃，湿度60~85%，气温18~30℃ */
    public static final PlantThresholds HUAZHU = new PlantThresholds(
            "花烛", 18, 28, 60, 85, 18, 30
    );

    /** 白掌阈值：水温16~26℃，湿度50~80%，气温16~28℃ */
    public static final PlantThresholds BAIZHANG = new PlantThresholds(
            "白掌", 16, 26, 50, 80, 16, 28
    );

    /** 绿萝阈值：水温15~25℃，湿度60~90%，气温15~28℃ */
    public static final PlantThresholds LULUO = new PlantThresholds(
            "绿萝", 15, 25, 60, 90, 15, 28
    );

    public static final String STATUS_EXCELLENT = "生长环境卓越"; // 评分≥85
    public static final String STATUS_GOOD = "生长状态良好";     // 评分≥65
    public static final String STATUS_STRESS = "存在环境压力";   // 评分≥45
    public static final String STATUS_WARNING = "生长预警：请检查营养液温度"; // 评分<45

    /**
     * 根据植物类型获取对应的环境阈值
     * "红掌"和"红烛"统一映射到花烛的阈值
     */
    public PlantThresholds getThresholds(String plantType) {
        if ("花烛".equals(plantType) || "红掌".equals(plantType) || "红烛".equals(plantType)) return HUAZHU;
        if ("白掌".equals(plantType)) return BAIZHANG;
        if ("绿萝".equals(plantType)) return LULUO;
        return HUAZHU; // 默认使用花烛阈值
    }

    /**
     * 计算健康评分（0~100分）
     * 权重：水温40% + 湿度30% + 气温20% + AI置信度10%
     */
    public float calculateHealthScore(EnvironmentData data, String plantType) {
        PlantThresholds th = getThresholds(plantType);
        float waterTempScore = calcRangeScore(data.waterTemp, th.waterTempMin, th.waterTempMax, 5f);
        float humidityScore = calcRangeScore(data.airHumidity, th.humidityMin, th.humidityMax, 10f);
        float airTempScore = calcRangeScore(data.airTemp, th.airTempMin, th.airTempMax, 5f);
        float aiScore = calcAiScore(data.aiConfidence);

        return waterTempScore * 0.40f
                + humidityScore * 0.30f
                + airTempScore * 0.20f
                + aiScore * 0.10f;
    }

    /** 计算健康评分（默认使用花烛阈值） */
    public float calculateHealthScore(EnvironmentData data) {
        return calculateHealthScore(data, "花烛");
    }

    /** 根据评分返回健康状态描述 */
    public String getStatusDescription(float score) {
        if (score >= 85f) return STATUS_EXCELLENT;
        if (score >= 65f) return STATUS_GOOD;
        if (score >= 45f) return STATUS_STRESS;
        return STATUS_WARNING;
    }

    /**
     * 计算范围评分（0~100分）
     * 将数值与[min, max]范围比较，tolerance为容差区间
     * 5个评分区间：范围内100分 → 容差内60~100分 → 二级容差30~60分 → 超出范围30分
     */
    float calcRangeScore(float value, float min, float max, float tolerance) {
        if (value >= min && value <= max) return 100f; // 在适宜范围内，满分
        if (value >= min - tolerance && value < min) {
            // 略低于下限，在容差范围内线性插值60~100
            return lerp(60f, 100f, (value - (min - tolerance)) / tolerance);
        }
        if (value > max && value <= max + tolerance) {
            // 略高于上限，在容差范围内线性插值100~60
            return lerp(100f, 60f, (value - max) / tolerance);
        }
        if (value >= min - tolerance * 2 && value < min - tolerance) {
            // 二级容差：远低于下限，线性插值30~60
            return lerp(30f, 60f, (value - (min - tolerance * 2)) / tolerance);
        }
        if (value > max + tolerance && value <= max + tolerance * 2) {
            // 二级容差：远高于上限，线性插值60~30
            return lerp(60f, 30f, (value - (max + tolerance)) / tolerance);
        }
        return 30f; // 严重超出范围，最低分
    }

    /** 计算AI置信度评分（0~100分），置信度直接乘以100 */
    float calcAiScore(float confidence) {
        if (confidence < 0f) confidence = 0f;
        if (confidence > 1f) confidence = 1f;
        return confidence * 100f;
    }

    /** 线性插值：从from到to，按ratio比例计算 */
    float lerp(float from, float to, float ratio) {
        if (ratio < 0f) ratio = 0f;
        if (ratio > 1f) ratio = 1f;
        return from + (to - from) * ratio;
    }
}
