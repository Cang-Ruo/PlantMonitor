package com.example.mygraduationproject.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 聊天会话实体类，对应数据库 chat_sessions 表
 * 每次识别一张图片对应一个会话，通过imagePath关联AIResult
 */
@Entity(tableName = "chat_sessions")
public class ChatSession {
    
    @PrimaryKey(autoGenerate = true)
    private long id;                // 主键，自增
    private String imagePath;       // 关联的图片路径（本地路径或OSS签名URL），用于查找对应AIResult
    private String plantName;       // 会话关联的植物名称
    private String lastMessage;     // 最后一条消息内容
    private long createdAt;         // 会话创建时间
    private long updatedAt;         // 会话最后更新时间
    
    public ChatSession() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    @Ignore
    public ChatSession(String imagePath, String plantName) {
        this.imagePath = imagePath;
        this.plantName = plantName;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getPlantName() {
        return plantName;
    }
    
    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
