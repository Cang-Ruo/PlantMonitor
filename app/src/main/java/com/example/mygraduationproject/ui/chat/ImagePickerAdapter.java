package com.example.mygraduationproject.ui.chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.model.PlantImage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 鍥剧墖閫夋嫨閫傞厤鍣紝鏀寔鏈湴鏂囦欢鍜岀鍚峌RL鍥剧墖鍔犺浇 */
public class ImagePickerAdapter extends RecyclerView.Adapter<ImagePickerAdapter.ViewHolder> {

    private static final String TAG = "ImagePickerAdapter";
    
    private List<PlantImage> images = new ArrayList<>();
    private OnImageSelectedListener listener;

    public interface OnImageSelectedListener {
        void onImageSelected(PlantImage image);
    }

    public void setImages(List<PlantImage> images) {
        this.images = images != null ? images : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnImageSelectedListener(OnImageSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlantImage image = images.get(position);
        holder.bind(image);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
        }

        public void bind(PlantImage image) {
            if (image != null && image.getImagePath() != null) {
                try {
                    String path = image.getImagePath();
                    if (path.startsWith("http://") || path.startsWith("https://")) {
                        Glide.with(itemView.getContext())
                                .load(path)
                                .centerCrop()
                                .placeholder(R.drawable.ic_plant_placeholder)
                                .error(R.drawable.ic_plant_placeholder)
                                .into(ivImage);
                    } else {
                        File imageFile = new File(path);
                        if (imageFile.exists()) {
                            Glide.with(itemView.getContext())
                                    .load(imageFile)
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_plant_placeholder)
                                    .error(R.drawable.ic_plant_placeholder)
                                    .into(ivImage);
                        } else {
                            ivImage.setImageResource(R.drawable.ic_plant_placeholder);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image", e);
                    ivImage.setImageResource(R.drawable.ic_plant_placeholder);
                }
            } else {
                ivImage.setImageResource(R.drawable.ic_plant_placeholder);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null && image != null) {
                    listener.onImageSelected(image);
                }
            });
        }
    }
}
