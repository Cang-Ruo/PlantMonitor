package com.example.mygraduationproject.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 控制指令实体类，对应数据库 control_commands 表
 * 记录发送给M5StickC设备的继电器控制指令（补光灯、水泵、风扇）
 */
@Entity(tableName = "control_commands")
public class ControlCommand {
    
    @PrimaryKey(autoGenerate = true)
    private long id;                // 主键，自增
    private String deviceName;      // 设备显示名称（如"补光灯"、"水泵"、"风扇"）
    private String deviceCode;      // 设备代码（light/pump/fan）
    private String action;          // 动作（on/off）
    private long sendTime;          // 发送时间戳
    private boolean success;        // 是否发送成功
    private String errorMessage;    // 失败时的错误信息
    private int retryCount;         // 重试次数
    
    public static final String DEVICE_LIGHT = "light";  // 补光灯设备代码
    public static final String DEVICE_PUMP = "pump";    // 水泵设备代码
    public static final String DEVICE_FAN = "fan";      // 风扇设备代码
    
    public static final String ACTION_ON = "on";        // 开启动作
    public static final String ACTION_OFF = "off";      // 关闭动作
    
    public ControlCommand() {
        this.sendTime = System.currentTimeMillis();
        this.success = false;
        this.retryCount = 0;
    }
    
    @Ignore
    public ControlCommand(String deviceCode, String action) {
        this.deviceCode = deviceCode;
        this.action = action;
        this.sendTime = System.currentTimeMillis();
        this.success = false;
        this.retryCount = 0;
        this.deviceName = getDeviceDisplayName(deviceCode);
    }
    
    /** 将设备代码转换为中文显示名称 */
    public static String getDeviceDisplayName(String code) {
        if (code == null) return "";
        switch (code) {
            case DEVICE_LIGHT:
                return "补光灯";
            case DEVICE_PUMP:
                return "水泵";
            case DEVICE_FAN:
                return "风扇";
            default:
                return code;
        }
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getDeviceCode() {
        return deviceCode;
    }
    
    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public long getSendTime() {
        return sendTime;
    }
    
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
