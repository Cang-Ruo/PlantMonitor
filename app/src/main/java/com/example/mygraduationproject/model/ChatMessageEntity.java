package com.example.mygraduationproject.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 聊天消息实体类，对应数据库 chat_messages 表
 * 存储小植助手会话中的每条消息（用户消息和AI回复）
 * 外键关联ChatSession，会话删除时级联删除消息
 */
@Entity(tableName = "chat_messages",
        foreignKeys = @ForeignKey(
                entity = ChatSession.class,
                parentColumns = "id",
                childColumns = "sessionId",
                onDelete = ForeignKey.CASCADE  // 会话删除时级联删除消息
        ),
        indices = {@Index("sessionId")})
public class ChatMessageEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;                // 主键，自增
    private long sessionId;         // 所属会话ID，外键关联chat_sessions.id
    private String content;         // 消息内容
    private boolean isUser;         // 是否为用户发送的消息（true=用户，false=AI回复）
    private long timestamp;         // 消息时间戳
    
    public ChatMessageEntity() {
        this.timestamp = System.currentTimeMillis();
    }
    
    @Ignore
    public ChatMessageEntity(long sessionId, String content, boolean isUser) {
        this.sessionId = sessionId;
        this.content = content;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public boolean isUser() {
        return isUser;
    }
    
    public void setUser(boolean user) {
        isUser = user;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
  public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
