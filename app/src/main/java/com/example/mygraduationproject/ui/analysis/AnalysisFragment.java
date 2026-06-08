package com.example.mygraduationproject.ui.analysis;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mygraduationproject.MainActivity;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.data.Repository;
import com.example.mygraduationproject.databinding.FragmentAnalysisBinding;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.GrowthRecord;
import com.example.mygraduationproject.mqtt.MqttManager;
import com.example.mygraduationproject.utils.HealthEvaluator;
import com.example.mygraduationproject.utils.HistoryRecordsManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 分析界面：环境趋势图表、传感器仪表盘、花烛白掌绿萝筛选、健康报告 */
public class AnalysisFragment extends Fragment {

    private static final String TAG = "AnalysisFragment";
    private static final long SENSOR_QUERY_INTERVAL = 15000;

    private FragmentAnalysisBinding binding;
    private AnalysisViewModel viewModel;
    private AIResultAdapter adapter;
    private MqttManager mqttManager;
    private HistoryRecordsManager historyRecordsManager;

    private HealthEvaluator healthEvaluator = new HealthEvaluator();
    private Handler sensorQueryHandler = new Handler(Looper.getMainLooper());
    private List<Long> chartTimestamps = new ArrayList<>();

    private final Runnable sensorQueryRunnable = new Runnable() {
        @Override
        public void run() {
            querySensorData();
            sensorQueryHandler.postDelayed(this, SENSOR_QUERY_INTERVAL);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new AIResultAdapter(new ArrayList<>());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() == null) return;

        viewModel = new ViewModelProvider(this).get(AnalysisViewModel.class);

        historyRecordsManager = HistoryRecordsManager.getInstance(getContext());

        if (getActivity() instanceof MainActivity) {
            mqttManager = ((MainActivity) getActivity()).getMqttManager();
        }

        setupViews();
        setupChart();
        setupChartPlantSpinner();
        setupPlantFilterTabs();
        setupSwipeRefresh();
        setupReportButton();
        observeViewModel();
        syncCloudData();
        sensorQueryHandler.post(sensorQueryRunnable);
    }

    private void observeViewModel() {
        viewModel.getFilteredAIResults().observe(getViewLifecycleOwner(), filtered -> {
            if (binding == null || filtered == null) return;

            adapter.updateData(filtered);
            if (filtered.isEmpty()) {
                binding.tvEmptyHint.setVisibility(View.VISIBLE);
                binding.tvEmptyHint.setText(getString(R.string.no_analysis_result));
            } else {
                binding.tvEmptyHint.setVisibility(View.GONE);
            }
        });

        viewModel.getChartGrowthRecords().observe(getViewLifecycleOwner(), records -> {
            if (binding == null || records == null) return;

            updateChart(records);
        });
    }

    private void setupSwipeRefresh() {
        if (binding == null) return;

        binding.swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                android.R.color.holo_green_light,
                android.R.color.holo_green_dark
        );
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            querySensorData();
            viewModel.refreshData();
            syncCloudData();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (binding != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }, 3000);
        });
    }

    private void syncCloudData() {
        if (historyRecordsManager == null) return;
        historyRecordsManager.syncToLocalDatabase(new HistoryRecordsManager.SyncCallback() {
            @Override
            public void onSyncComplete(int newRecordsCount) {
                Log.d(TAG, "云端数据同步完成，新增 " + newRecordsCount + " 条记录");
            }

            @Override
            public void onSyncFailed(String error) {
                Log.e(TAG, "云端数据同步失败: " + error);
            }
        });
    }

    // [Data Analytics Module] - 生成报告按钮交互，支持选择植物类型
    private void setupReportButton() {
        if (binding == null) return;

        binding.btnGenerateReport.setOnClickListener(v -> {
            String[] plantTypes = {"花烛", "白掌", "绿萝"};
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("选择报告植物类型")
                    .setItems(plantTypes, (dialog, which) -> {
                        final String plantType = plantTypes[which];
                        viewModel.generateWeeklyReport(plantType, new AnalysisViewModel.ReportCallback() {
                            @Override
                            public void onReportGenerated(com.example.mygraduationproject.model.HealthReport report) {
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    ReportDialogFragment dialogFrag = ReportDialogFragment.newInstance(report);
                                    dialogFrag.show(getChildFragmentManager(), "health_report");
                                });
                            }

                            @Override
                            public void onNoData() {
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "暂无" + plantType + "的观测数据", Toast.LENGTH_SHORT).show()
                                );
                            }
                        });
                    })
                    .show();
        });
    }

    private void setupChart() {
        if (binding == null) return;

        LineChart chart = binding.chartEnvironment;
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setTextColor(Color.GRAY);
        chart.getLegend().setTextSize(11f);
        chart.setExtraBottomOffset(8f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-30f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat fullFmt = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < chartTimestamps.size()) {
                    return fullFmt.format(new Date(chartTimestamps.get(idx)));
                }
                return "";
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#20000000"));

        chart.getAxisRight().setEnabled(false);

        LineData data = new LineData();
        chart.setData(data);
        chart.invalidate();
    }

    private void updateChart(List<GrowthRecord> records) {
        if (binding == null) return;

        LineChart chart = binding.chartEnvironment;

        if (records == null || records.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        List<GrowthRecord> sorted = new ArrayList<>(records);
        Collections.sort(sorted, Comparator.comparingLong(GrowthRecord::getTimestamp));

        chartTimestamps.clear();

        List<Entry> waterEntries = new ArrayList<>();
        List<Entry> airEntries = new ArrayList<>();
        List<Entry> humEntries = new ArrayList<>();
        List<Entry> healthEntries = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {
            GrowthRecord r = sorted.get(i);
            waterEntries.add(new Entry(i, r.getWaterTemp()));
            airEntries.add(new Entry(i, r.getAirTemp()));
            humEntries.add(new Entry(i, r.getAirHumidity()));
            healthEntries.add(new Entry(i, r.getHealthScore()));
            chartTimestamps.add(r.getTimestamp());
        }

        String plantType = viewModel.getChartPlantType();

        LineDataSet waterSet = createDataSet("水温(°C)", Color.parseColor("#2196F3"));
        waterSet.setValues(waterEntries);

        LineDataSet airSet = createDataSet("气温(°C)", Color.parseColor("#F44336"));
        airSet.setValues(airEntries);

        LineDataSet humSet = createDataSet("湿度(%)", Color.parseColor("#FFC107"));
        humSet.setValues(humEntries);

        LineDataSet healthSet = createDataSet("健康评分", Color.parseColor("#4CAF50"));
        healthSet.setValues(healthEntries);
        healthSet.setLineWidth(3f);

        LineData lineData = new LineData(waterSet, airSet, humSet, healthSet);
        chart.setData(lineData);

        if (sorted.size() > 3) {
            chart.setVisibleXRangeMaximum(Math.min(sorted.size(), 10));
            chart.moveViewToX(Math.max(0, sorted.size() - 10));
        } else {
            chart.fitScreen();
        }

        chart.notifyDataSetChanged();
        chart.invalidate();

        if (!sorted.isEmpty()) {
            GrowthRecord latest = sorted.get(sorted.size() - 1);
            updateDashboardFromRecord(latest);
        }
    }

    private void updateDashboardFromRecord(GrowthRecord record) {
        if (binding == null) return;

        binding.tvDashboardWaterTemp.setText(String.format("%.1f", record.getWaterTemp()));
        binding.tvDashboardAirTemp.setText(String.format("%.1f", record.getAirTemp()));
        binding.tvDashboardAirHumidity.setText(String.format("%.1f", record.getAirHumidity()));

        float score = record.getHealthScore();
        binding.tvHealthScore.setText(String.format("%.0f", score));
        binding.tvHealthScore.setTextColor(getScoreColor(score));

        String status = record.getHealthStatus();
        if (status != null && !status.isEmpty()) {
            binding.tvHealthStatusDesc.setText(status);
        } else {
            binding.tvHealthStatusDesc.setText(healthEvaluator.getStatusDescription(score));
        }
    }

    private int getPlantChartColor(String plantType) {
        if ("花烛".equals(plantType) || "红掌".equals(plantType) || "红烛".equals(plantType)) return Color.parseColor("#E53935");
        if ("白掌".equals(plantType)) return Color.parseColor("#1E88E5");
        if ("绿萝".equals(plantType)) return Color.parseColor("#43A047");
        return Color.parseColor("#FF9800");
    }

    private LineDataSet createDataSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setColor(color);
        set.setCircleColor(color);
        set.setCircleRadius(4f);
        set.setLineWidth(2f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawCircles(true);
        set.setDrawCircleHole(false);
        return set;
    }

    private void querySensorData() {
        if (mqttManager == null || !mqttManager.isConnected()) return;

        mqttManager.querySensorData(new MqttManager.SensorDataCallback() {
            @Override
            public void onResult(float waterTemp, float airTemp, float airHumidity) {
                if (getActivity() == null || binding == null) return;

                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;

                    binding.tvDashboardWaterTemp.setText(String.format("%.1f", waterTemp));
                    binding.tvDashboardAirTemp.setText(String.format("%.1f", airTemp));
                    binding.tvDashboardAirHumidity.setText(String.format("%.1f", airHumidity));

                    HealthEvaluator.EnvironmentData envData = new HealthEvaluator.EnvironmentData(
                            waterTemp, airHumidity, airTemp, 0.8f);
                    String scorePlantType = viewModel.getChartPlantType();
                    float score = healthEvaluator.calculateHealthScore(envData, scorePlantType);
                    String desc = healthEvaluator.getStatusDescription(score);

                    binding.tvHealthScore.setText(String.format("%.0f", score));
                    binding.tvHealthScore.setTextColor(getScoreColor(score));
                    binding.tvHealthStatusDesc.setText(desc);
                });
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "传感器查询失败: " + error);
            }
        });
    }

    private int getScoreColor(float score) {
        if (score >= 85) return getResources().getColor(R.color.health_good, null);
        if (score >= 65) return getResources().getColor(R.color.primary, null);
        if (score >= 45) return getResources().getColor(R.color.health_warning, null);
        return getResources().getColor(R.color.health_bad, null);
    }

    private void setupChartPlantSpinner() {
        if (binding == null || getContext() == null) return;

        String[] plantTypes = {"花烛", "白掌", "绿萝"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(), R.layout.spinner_item, plantTypes);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.spinnerChartPlant.setAdapter(spinnerAdapter);

        binding.spinnerChartPlant.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = plantTypes[position];
                if (!selected.equals(viewModel.getChartPlantType())) {
                    viewModel.setChartPlantType(selected);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupPlantFilterTabs() {
        if (binding == null) return;

        TabLayout tabLayout = binding.tabLayoutPlantFilter;
        tabLayout.addTab(tabLayout.newTab().setText("全部"));
        tabLayout.addTab(tabLayout.newTab().setText("花烛"));
        tabLayout.addTab(tabLayout.newTab().setText("白掌"));
        tabLayout.addTab(tabLayout.newTab().setText("绿萝"));
        tabLayout.addTab(tabLayout.newTab().setText("其他"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String filter = tab.getText().toString();
                viewModel.setHistoryPlantFilter(filter);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorQueryHandler.post(sensorQueryRunnable);
    }

    private void setupViews() {
        if (binding == null) return;

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvHistory.setAdapter(adapter);

        adapter.setOnItemClickListener(result -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToDetail(result);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorQueryHandler.removeCallbacks(sensorQueryRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sensorQueryHandler.removeCallbacks(sensorQueryRunnable);
        binding = null;
    }
}
