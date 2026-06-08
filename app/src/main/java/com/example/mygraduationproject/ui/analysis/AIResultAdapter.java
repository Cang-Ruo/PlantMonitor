package com.example.mygraduationproject.ui.analysis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.utils.DateUtils;

import java.io.File;
import java.util.List;

/** AI璇嗗埆缁撴灉鍒楄〃閫傞厤鍣紝灞曠ず鑺辩儧/鐧芥帉/缁胯悵鐨勫仴搴峰垎鏋愮粨鏋滃崱鐗?*/
public class AIResultAdapter extends RecyclerView.Adapter<AIResultAdapter.ViewHolder> {

    private List<AIResult> results;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AIResult result);
    }

    public AIResultAdapter(List<AIResult> results) {
        this.results = results;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AIResult result = results.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return results != null ? results.size() : 0;
    }

    public void updateData(List<AIResult> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvPlantName;
        private final TextView tvHealthStatus;
        private final TextView tvTime;
        private final TextView tvHealthScore;
        private final TextView tvEnvWaterTemp;
        private final TextView tvEnvAirTemp;
        private final TextView tvEnvAirHumidity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvPlantName = itemView.findViewById(R.id.tvPlantName);
            tvHealthStatus = itemView.findViewById(R.id.tvHealthStatus);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvHealthScore = itemView.findViewById(R.id.tvHealthScore);
            tvEnvWaterTemp = itemView.findViewById(R.id.tvEnvWaterTemp);
            tvEnvAirTemp = itemView.findViewById(R.id.tvEnvAirTemp);
            tvEnvAirHumidity = itemView.findViewById(R.id.tvEnvAirHumidity);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(results.get(getAdapterPosition()));
                }
            });
        }

        public void bind(AIResult result) {
            tvPlantName.setText(result.getPlantName() != null ? result.getPlantName() : "未知植物");

            String healthStatus = result.getHealthStatus();
            if (healthStatus != null) {
                tvHealthStatus.setText(healthStatus);
                float score = result.getHealthScore();
                if (score >= 85) {
                    tvHealthStatus.setTextColor(itemView.getContext().getColor(R.color.health_good));
                } else if (score >= 65) {
                    tvHealthStatus.setTextColor(itemView.getContext().getColor(R.color.primary));
                } else if (score >= 45) {
                    tvHealthStatus.setTextColor(itemView.getContext().getColor(R.color.health_warning));
                } else if (score > 0) {
                    tvHealthStatus.setTextColor(itemView.getContext().getColor(R.color.health_bad));
                } else {
                    tvHealthStatus.setTextColor(itemView.getContext().getColor(R.color.text_gray));
                }
            } else {
                tvHealthStatus.setText("--");
                tvHealthStatus.setTextColor(itemView.getContext().getColor(R.color.text_gray));
            }

            tvTime.setText(DateUtils.getRelativeTime(result.getTimestamp()));

            float score = result.getHealthScore();
            if (score > 0) {
                tvHealthScore.setText(String.format("%.0f分", score));
            } else {
                tvHealthScore.setText(String.format("%.0f%%", result.getConfidence() * 100));
            }

            float waterTemp = result.getWaterTemp();
            float airTemp = result.getAirTemp();
            float airHumidity = result.getAirHumidity();
            if (waterTemp > 0 || airTemp > 0 || airHumidity > 0) {
                tvEnvWaterTemp.setText(String.format("%.1f°C", waterTemp));
                tvEnvAirTemp.setText(String.format("%.1f°C", airTemp));
                tvEnvAirHumidity.setText(String.format("%.0f%%", airHumidity));
            } else {
                tvEnvWaterTemp.setText("--°C");
                tvEnvAirTemp.setText("--°C");
                tvEnvAirHumidity.setText("--%");
            }

            String imagePath = result.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    Glide.with(itemView.getContext())
                            .load(imagePath)
                            .centerCrop()
                            .placeholder(R.drawable.ic_plant_placeholder)
                            .error(R.drawable.ic_plant_placeholder)
                            .into(ivThumbnail);
                } else {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        Glide.with(itemView.getContext())
                                .load(imageFile)
                                .centerCrop()
                                .placeholder(R.drawable.ic_plant_placeholder)
                                .error(R.drawable.ic_plant_placeholder)
                                .into(ivThumbnail);
                    } else {
                        ivThumbnail.setImageResource(R.drawable.ic_plant_placeholder);
                    }
                }
            } else {
                ivThumbnail.setImageResource(R.drawable.ic_plant_placeholder);
            }
        }
    }
}
