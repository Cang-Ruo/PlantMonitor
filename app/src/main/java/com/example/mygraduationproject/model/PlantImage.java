package com.example.mygraduationproject.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 植物图片实体类，对应数据库 plant_images 表
 * 存储本地拍摄/选择的图片信息，监控端拍照时创建
 */
@Entity(tableName = "plant_images")
public class PlantImage {
    
    @PrimaryKey(autoGenerate = true)
    private long id;                // 主键，自增
    private String imagePath;       // 图片本地存储路径
    private long timestamp;         // 拍照时间戳
    private boolean uploaded;       // 是否已上传到OSS
    private String thumbnailPath;   // 缩略图路径
    private long fileSize;          // 文件大小（字节）
    
    public PlantImage() {
        this.timestamp = System.currentTimeMillis();
        this.uploaded = false;
    }
    
    @Ignore
    public PlantImage(String imagePath, long fileSize) {
        this.imagePath = imagePath;
        this.timestamp = System.currentTimeMillis();
        this.uploaded = false;
        this.fileSize = fileSize;
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
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isUploaded() {
        return uploaded;
    }
    
    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }
    
    public String getThumbnailPath() {
        return thumbnailPath;
    }
    
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
