package com.example.mygraduationproject.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.model.ChatSession;
import com.example.mygraduationproject.utils.DateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 鑱婂ぉ浼氳瘽鍒楄〃閫傞厤鍣紝灞曠ず鍘嗗彶浼氳瘽璁板綍鏉＄洰 */
public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.ViewHolder> {

    private List<ChatSession> sessions = new ArrayList<>();
    private OnSessionClickListener listener;
    private long currentSessionId = -1;

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
    }

    public void setSessions(List<ChatSession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCurrentSessionId(long sessionId) {
        this.currentSessionId = sessionId;
        notifyDataSetChanged();
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = sessions.get(position);
        holder.bind(session, session.getId() == currentSessionId);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvPlantName;
        private final TextView tvLastMessage;
        private final TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvPlantName = itemView.findViewById(R.id.tvPlantName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(ChatSession session, boolean isCurrent) {
            tvPlantName.setText(session.getPlantName() != null ? session.getPlantName() : "未知植物");
            tvLastMessage.setText(session.getLastMessage() != null ? session.getLastMessage() : "暂无消息");
            tvTime.setText(DateUtils.getRelativeTime(session.getUpdatedAt()));
            
            if (isCurrent) {
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.light_green_100));
            } else {
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.card_background));
            }
            
            if (session.getImagePath() != null) {
                File imageFile = new File(session.getImagePath());
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
            } else {
                ivThumbnail.setImageResource(R.drawable.ic_plant_placeholder);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });
        }
    }
}
