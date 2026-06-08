package com.example.mygraduationproject.ui.chat;

import androidx.lifecycle.ViewModel;

import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.ChatSession;

/** 鑱婂ぉViewModel锛氱鐞嗗綋鍓嶄細璇濆拰AI璇嗗埆缁撴灉鐘舵€?*/
public class ChatViewModel extends ViewModel {
    
    private ChatSession currentSession;
    private AIResult currentAIResult;
    
    public ChatSession getCurrentSession() {
        return currentSession;
    }
    
    public void setCurrentSession(ChatSession session) {
        this.currentSession = session;
    }
    
    public AIResult getCurrentAIResult() {
        return currentAIResult;
    }
    
    public void setCurrentAIResult(AIResult result) {
        this.currentAIResult = result;
    }
    
    public void clear() {
        currentSession = null;
        currentAIResult = null;
    }
}
