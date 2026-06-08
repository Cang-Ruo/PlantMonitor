package com.example.mygraduationproject.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygraduationproject.R;
import com.example.mygraduationproject.network.ApiService;

import java.util.ArrayList;
import java.util.List;

/** 鑱婂ぉ娑堟伅鍒楄〃閫傞厤鍣紝鍖哄垎鐢ㄦ埛鍜孉I娑堟伅鐨勬樉绀烘牱寮?*/
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ApiService.ChatMessage> messages = new ArrayList<>();

    public void addMessage(ApiService.ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void addMessageAndScroll(ApiService.ChatMessage message, RecyclerView recyclerView) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
        // 强制滚动到最新消息
        if (recyclerView != null) {
            recyclerView.post(() -> {
                recyclerView.scrollToPosition(messages.size() - 1);
            });
        }
    }

    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }

    public List<ApiService.ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApiService.ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSender;
        private final TextView tvMessage;
        private final LinearLayout messageContainer;
        private final LinearLayout contentContainer;
        private final ImageView ivAvatar;
        private final CardView cardMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            contentContainer = itemView.findViewById(R.id.contentContainer);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            cardMessage = itemView.findViewById(R.id.cardMessage);
        }

        public void bind(ApiService.ChatMessage message) {
            tvMessage.setText(message.getContent());
            
            if (message.isUser()) {
                tvSender.setText("我");
                ivAvatar.setVisibility(View.GONE);
                messageContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                contentContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                cardMessage.setCardBackgroundColor(itemView.getContext().getColor(R.color.primary));
                tvMessage.setTextColor(itemView.getContext().getColor(R.color.white));
            } else {
                tvSender.setText("小植");
                ivAvatar.setVisibility(View.VISIBLE);
                messageContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                contentContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                cardMessage.setCardBackgroundColor(itemView.getContext().getColor(R.color.card_background));
                tvMessage.setTextColor(itemView.getContext().getColor(R.color.text_dark));
            }
        }
    }
}
