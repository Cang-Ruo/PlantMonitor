package com.example.mygraduationproject.ui.analysis;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.utils.DateUtils;

import java.io.File;

/** 妞嶇墿璇︽儏椤碉紝灞曠ず鍗曟AI鍒嗘瀽鐨勫畬鏁寸粨鏋滀俊鎭?*/
public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLANT_NAME = "plant_name";
    public static final String EXTRA_HEALTH_STATUS = "health_status";
    public static final String EXTRA_HEALTH_SCORE = "health_score";
    public static final String EXTRA_WATER_TEMP = "water_temp";
    public static final String EXTRA_AIR_TEMP = "air_temp";
    public static final String EXTRA_AIR_HUMIDITY = "air_humidity";
    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_SUGGESTION = "suggestion";
    public static final String EXTRA_DETAILED_ANALYSIS = "detailed_analysis";
    public static final String EXTRA_TIMESTAMP = "timestamp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        String plantName = extras.getString(EXTRA_PLANT_NAME, "未知植物");
        String healthStatus = extras.getString(EXTRA_HEALTH_STATUS, "--");
        float healthScore = extras.getFloat(EXTRA_HEALTH_SCORE, 0f);
        float waterTemp = extras.getFloat(EXTRA_WATER_TEMP, 0f);
        float airTemp = extras.getFloat(EXTRA_AIR_TEMP, 0f);
        float airHumidity = extras.getFloat(EXTRA_AIR_HUMIDITY, 0f);
        String imagePath = extras.getString(EXTRA_IMAGE_PATH, "");
        String suggestion = extras.getString(EXTRA_SUGGESTION, "");
        String detailedAnalysis = extras.getString(EXTRA_DETAILED_ANALYSIS, "");
        long timestamp = extras.getLong(EXTRA_TIMESTAMP, 0);

        TextView tvPlantName = findViewById(R.id.tvDetailPlantName);
        TextView tvHealthStatus = findViewById(R.id.tvDetailHealthStatus);
        TextView tvHealthScore = findViewById(R.id.tvDetailHealthScore);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        ImageView ivImage = findViewById(R.id.ivDetailImage);

        tvPlantName.setText(plantName);
        tvHealthStatus.setText(healthStatus);
        if ("健康".equals(healthStatus)) {
            tvHealthStatus.setTextColor(getColor(R.color.health_good));
        } else if ("可能异常".equals(healthStatus)) {
            tvHealthStatus.setTextColor(getColor(R.color.health_warning));
        } else {
            tvHealthStatus.setTextColor(getColor(R.color.health_bad));
        }

        if (healthScore > 0) {
            tvHealthScore.setText(String.format("%.0f分", healthScore));
        } else {
            tvHealthScore.setText("--");
        }

        if (timestamp > 0) {
            tvTime.setText(DateUtils.formatDateTime(timestamp));
        } else {
            tvTime.setText("--");
        }

        if (imagePath != null && !imagePath.isEmpty()) {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Glide.with(this).load(imagePath).centerCrop()
                        .placeholder(R.drawable.ic_plant_placeholder)
                        .error(R.drawable.ic_plant_placeholder)
                        .into(ivImage);
            } else {
                File f = new File(imagePath);
                if (f.exists()) {
                    Glide.with(this).load(f).centerCrop()
                            .placeholder(R.drawable.ic_plant_placeholder)
                            .error(R.drawable.ic_plant_placeholder)
                            .into(ivImage);
                }
            }
        }

        View cardEnv = findViewById(R.id.cardEnvironment);
        if (waterTemp > 0 || airTemp > 0 || airHumidity > 0) {
            cardEnv.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvDetailWaterTemp)).setText(String.format("%.1f°C", waterTemp));
            ((TextView) findViewById(R.id.tvDetailAirTemp)).setText(String.format("%.1f°C", airTemp));
            ((TextView) findViewById(R.id.tvDetailAirHumidity)).setText(String.format("%.1f%%", airHumidity));
        }

        View cardSuggestion = findViewById(R.id.cardSuggestion);
        String analysisText = detailedAnalysis != null && !detailedAnalysis.isEmpty() ? detailedAnalysis : suggestion;
        if (analysisText != null && !analysisText.isEmpty()) {
            cardSuggestion.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvDetailSuggestion)).setText(analysisText);
        }
    }
}
