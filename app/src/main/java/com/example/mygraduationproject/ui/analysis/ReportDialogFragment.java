package com.example.mygraduationproject.ui.analysis;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.mygraduationproject.R;
import com.example.mygraduationproject.model.HealthReport;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// [Data Analytics Module] - 论文第四章：报告展示与交互组件
/** 鍋ュ悍鎶ュ憡寮圭獥锛屽睍绀鸿姳鐑?鐧芥帉/缁胯悵鐨勫懆鎶ョ粺璁′俊鎭?*/
public class ReportDialogFragment extends DialogFragment {

    private static final String ARG_PLANT_TYPE = "plant_type";
    private static final String ARG_PHOTO_COUNT = "photo_count";
    private static final String ARG_AVG_SCORE = "avg_score";
    private static final String ARG_MAX_WATER = "max_water";
    private static final String ARG_MIN_WATER = "min_water";
    private static final String ARG_MAX_AIR = "max_air";
    private static final String ARG_MIN_AIR = "min_air";
    private static final String ARG_AVG_HUMIDITY = "avg_humidity";
    private static final String ARG_HEALTH_LEVEL = "health_level";
    private static final String ARG_SUMMARY = "summary";
    private static final String ARG_START_DATE = "start_date";
    private static final String ARG_END_DATE = "end_date";
    private static final String ARG_DAY_SPAN = "day_span";

    private HealthReport report;

    public static ReportDialogFragment newInstance(HealthReport report) {
        ReportDialogFragment fragment = new ReportDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLANT_TYPE, report.getPlantType());
        args.putInt(ARG_PHOTO_COUNT, report.getPhotoCount());
        args.putFloat(ARG_AVG_SCORE, report.getAvgHealthScore());
        args.putFloat(ARG_MAX_WATER, report.getMaxWaterTemp());
        args.putFloat(ARG_MIN_WATER, report.getMinWaterTemp());
        args.putFloat(ARG_MAX_AIR, report.getMaxAirTemp());
        args.putFloat(ARG_MIN_AIR, report.getMinAirTemp());
        args.putFloat(ARG_AVG_HUMIDITY, report.getAvgAirHumidity());
        args.putString(ARG_HEALTH_LEVEL, report.getHealthLevel());
        args.putString(ARG_SUMMARY, report.getSummary());
        args.putLong(ARG_START_DATE, report.getStartDate());
        args.putLong(ARG_END_DATE, report.getEndDate());
        args.putInt(ARG_DAY_SPAN, report.getDaySpan());
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            report = new HealthReport(
                    args.getString(ARG_PLANT_TYPE, ""),
                    args.getInt(ARG_PHOTO_COUNT, 0),
                    args.getFloat(ARG_AVG_SCORE, 0),
                    args.getFloat(ARG_MAX_WATER, 0),
                    args.getFloat(ARG_MIN_WATER, 0),
                    args.getFloat(ARG_MAX_AIR, 0),
                    args.getFloat(ARG_MIN_AIR, 0),
                    args.getFloat(ARG_AVG_HUMIDITY, 0),
                    0,
                    args.getString(ARG_HEALTH_LEVEL, ""),
                    args.getString(ARG_SUMMARY, ""),
                    args.getLong(ARG_START_DATE, 0),
                    args.getLong(ARG_END_DATE, 0),
                    args.getInt(ARG_DAY_SPAN, 1)
            );
        }

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_report, null);

        bindViews(view);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view);
        return builder.create();
    }

    private void bindViews(View view) {
        if (report == null) return;

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        TextView tvPlantType = view.findViewById(R.id.tvReportPlantType);
        TextView tvDateRange = view.findViewById(R.id.tvReportDateRange);
        TextView tvScore = view.findViewById(R.id.tvReportScore);
        TextView tvLevel = view.findViewById(R.id.tvReportLevel);
        TextView tvPhotoCount = view.findViewById(R.id.tvReportPhotoCount);
        TextView tvWaterRange = view.findViewById(R.id.tvReportWaterRange);
        TextView tvAirRange = view.findViewById(R.id.tvReportAirRange);
        TextView tvHumidity = view.findViewById(R.id.tvReportHumidity);
        TextView tvSummary = view.findViewById(R.id.tvReportSummary);
        MaterialButton btnShare = view.findViewById(R.id.btnShareReport);

        tvPlantType.setText("植物类型：" + report.getPlantType());
        tvDateRange.setText("统计周期：" + report.getPeriodDesc() + "（"
                + fmt.format(new Date(report.getStartDate()))
                + " ~ " + fmt.format(new Date(report.getEndDate())) + "）");
        tvScore.setText(String.format("%.0f", report.getAvgHealthScore()));
        tvLevel.setText(report.getHealthLevel());
        tvPhotoCount.setText(String.valueOf(report.getPhotoCount()));
        tvWaterRange.setText(String.format("%.1f~%.1f", report.getMinWaterTemp(), report.getMaxWaterTemp()));
        tvAirRange.setText(String.format("%.1f~%.1f", report.getMinAirTemp(), report.getMaxAirTemp()));
        tvHumidity.setText(String.format("%.1f", report.getAvgAirHumidity()));
        tvSummary.setText(report.getSummary());

        btnShare.setOnClickListener(v -> {
            String text = report.generateFullReport();
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("生长健康报告", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "报告已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });
    }
}
