package com.example.mygraduationproject.ui.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mygraduationproject.data.Repository;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.GrowthRecord;
import com.example.mygraduationproject.model.HealthReport;

import java.util.ArrayList;
import java.util.List;

/** 鍒嗘瀽ViewModel锛氳姳鐑?鐧芥帉/缁胯悵鏁版嵁绛涢€夈€佸浘琛ㄦ暟鎹浆鎹€佸懆鎶ョ敓鎴?*/
public class AnalysisViewModel extends AndroidViewModel {

    private final Repository repository;

    private final LiveData<List<AIResult>> allAIResultsLive;
    private final LiveData<List<GrowthRecord>> allGrowthRecordsLive;

    private final MutableLiveData<String> chartPlantType = new MutableLiveData<>("花烛");
    private final MutableLiveData<String> historyPlantFilter = new MutableLiveData<>("全部");

    private final MediatorLiveData<List<AIResult>> filteredAIResults = new MediatorLiveData<>();
    private final MediatorLiveData<List<GrowthRecord>> chartGrowthRecords = new MediatorLiveData<>();

    private LiveData<List<GrowthRecord>> currentChartSource;

    public AnalysisViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);

        allAIResultsLive = repository.observeRecentAIResults(100);
        allGrowthRecordsLive = repository.observeAllGrowthRecords();

        filteredAIResults.addSource(allAIResultsLive, results -> {
            applyHistoryFilter(results, historyPlantFilter.getValue());
        });
        filteredAIResults.addSource(historyPlantFilter, filter -> {
            if (allAIResultsLive.getValue() != null) {
                applyHistoryFilter(allAIResultsLive.getValue(), filter);
            }
        });

        chartGrowthRecords.addSource(chartPlantType, plantType -> {
            switchChartSource(plantType);
        });

        switchChartSource(chartPlantType.getValue());
    }

    private void applyHistoryFilter(List<AIResult> results, String filter) {
        if (results == null) {
            filteredAIResults.setValue(new ArrayList<>());
            return;
        }

        List<AIResult> filtered;
        if ("全部".equals(filter)) {
            filtered = new ArrayList<>(results);
        } else if ("其他".equals(filter)) {
            filtered = new ArrayList<>();
            for (AIResult result : results) {
                String pt = result.getPlantType();
                if (pt == null || (!pt.equals("花烛") && !pt.equals("白掌") && !pt.equals("绿萝"))) {
                    filtered.add(result);
                }
            }
        } else {
            filtered = new ArrayList<>();
            for (AIResult result : results) {
                String pt = result.getPlantType();
                if (pt != null && pt.equals(filter)) {
                    filtered.add(result);
                } else if (pt == null && matchesPlantName(result.getPlantName(), filter)) {
                    filtered.add(result);
                }
            }
        }
        filteredAIResults.setValue(filtered);
    }

    private boolean matchesPlantName(String plantName, String plantType) {
        if (plantName == null) return false;
        return plantName.contains(plantType);
    }

    private void switchChartSource(String plantType) {
        if (currentChartSource != null) {
            chartGrowthRecords.removeSource(currentChartSource);
        }

        if (plantType == null || plantType.isEmpty()) {
            currentChartSource = repository.observeRecentGrowthRecords(50);
        } else {
            currentChartSource = repository.observeRecentGrowthRecordsByPlantType(plantType, 50);
        }

        chartGrowthRecords.addSource(currentChartSource, records -> {
            chartGrowthRecords.setValue(records);
        });
    }

    public LiveData<List<AIResult>> getFilteredAIResults() {
        return filteredAIResults;
    }

    public LiveData<List<GrowthRecord>> getChartGrowthRecords() {
        return chartGrowthRecords;
    }

    public LiveData<List<GrowthRecord>> getAllGrowthRecords() {
        return allGrowthRecordsLive;
    }

    public String getChartPlantType() {
        return chartPlantType.getValue();
    }

    public void setChartPlantType(String plantType) {
        chartPlantType.setValue(plantType);
    }

    public String getHistoryPlantFilter() {
        return historyPlantFilter.getValue();
    }

    public void setHistoryPlantFilter(String filter) {
        historyPlantFilter.setValue(filter);
    }

    public Repository getRepository() {
        return repository;
    }

    public void refreshData() {
        historyPlantFilter.setValue(historyPlantFilter.getValue());
        chartPlantType.setValue(chartPlantType.getValue());
    }

    // [Data Analytics Module] - 论文第四章：数据聚合与报告生成
    public interface ReportCallback {
        void onReportGenerated(HealthReport report);
        void onNoData();
    }

    // [Data Analytics Module] - 提取最近7天特定植物的GrowthRecord并计算统计指标
    public void generateWeeklyReport(String plantType, ReportCallback callback) {
        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;

        repository.getRecentGrowthRecordsByPlantType(plantType, 200, records -> {
            List<GrowthRecord> filtered = new ArrayList<>();
            for (GrowthRecord r : records) {
                if (r.getTimestamp() >= sevenDaysAgo) {
                    filtered.add(r);
                }
            }

            if (filtered.isEmpty()) {
                if (callback != null) callback.onNoData();
                return;
            }

            float sumScore = 0;
            float maxWT = Float.MIN_VALUE, minWT = Float.MAX_VALUE;
            float maxAT = Float.MIN_VALUE, minAT = Float.MAX_VALUE;
            float sumHum = 0;
            long earliestTs = Long.MAX_VALUE, latestTs = 0;

            for (GrowthRecord r : filtered) {
                sumScore += r.getHealthScore();
                if (r.getWaterTemp() > maxWT) maxWT = r.getWaterTemp();
                if (r.getWaterTemp() < minWT) minWT = r.getWaterTemp();
                if (r.getAirTemp() > maxAT) maxAT = r.getAirTemp();
                if (r.getAirTemp() < minAT) minAT = r.getAirTemp();
                sumHum += r.getAirHumidity();
                if (r.getTimestamp() < earliestTs) earliestTs = r.getTimestamp();
                if (r.getTimestamp() > latestTs) latestTs = r.getTimestamp();
            }

            int count = filtered.size();
            float avgScore = sumScore / count;
            float avgHum = sumHum / count;
            float waterTempRange = maxWT - minWT;

            long spanMs = latestTs - earliestTs;
            int daySpan = (int) (spanMs / (24 * 60 * 60 * 1000)) + 1;

            // [Data Analytics Module] - 基于规则的健康等级判定与文字总结生成
            String healthLevel;
            if (avgScore >= 85) healthLevel = "优秀 🌟";
            else if (avgScore >= 65) healthLevel = "良好 ✅";
            else if (avgScore >= 45) healthLevel = "一般 ⚠️";
            else healthLevel = "预警 🚨";

            String summary = buildSummary(plantType, avgScore, waterTempRange, maxAT, minAT, avgHum, count, daySpan);

            HealthReport report = new HealthReport(
                    plantType, count, avgScore,
                    maxWT, minWT, maxAT, minAT, avgHum, waterTempRange,
                    healthLevel, summary, earliestTs, latestTs, daySpan
            );

            if (callback != null) callback.onReportGenerated(report);
        });
    }

    // [Data Analytics Module] - 自然语言总结生成引擎
    private String buildSummary(String plantType, float avgScore, float waterTempRange,
                                float maxAirTemp, float minAirTemp, float avgHum, int photoCount, int daySpan) {
        StringBuilder sb = new StringBuilder();

        String periodDesc;
        if (daySpan <= 1) {
            periodDesc = "今日";
        } else if (daySpan <= 2) {
            periodDesc = "近2天";
        } else if (daySpan <= 3) {
            periodDesc = "近3天";
        } else {
            periodDesc = "本周";
        }

        sb.append(periodDesc).append(plantType).append("平均得分 ")
          .append(String.format("%.0f", avgScore)).append(" 分，");

        if (avgScore >= 85) {
            sb.append("生长环境极佳，各项指标均在适宜范围内。");
        } else if (avgScore >= 65) {
            sb.append("生长状态良好，部分指标略有波动。");
        } else if (avgScore >= 45) {
            sb.append("存在一定环境压力，建议关注异常指标。");
        } else {
            sb.append("生长环境较差，需及时调整养护条件。");
        }

        if (waterTempRange <= 2f) {
            sb.append("水温波动在").append(String.format("%.1f", waterTempRange)).append("℃以内，非常稳定；");
        } else if (waterTempRange <= 5f) {
            sb.append("水温波动").append(String.format("%.1f", waterTempRange)).append("℃，在可接受范围；");
        } else {
            sb.append("水温波动达").append(String.format("%.1f", waterTempRange)).append("℃，波动较大，建议检查温控设备；");
        }

        float airTempRange = maxAirTemp - minAirTemp;
        if (airTempRange > 10f) {
            sb.append("气温波动较大（").append(String.format("%.1f", airTempRange)).append("℃），注意通风调节；");
        }

        if (avgHum < 50f) {
            sb.append("平均湿度偏低，建议增加加湿措施；");
        } else if (avgHum > 90f) {
            sb.append("平均湿度偏高，注意通风防霉；");
        }

        sb.append("共记录").append(photoCount).append("次观测数据。");
        return sb.toString();
    }
}
