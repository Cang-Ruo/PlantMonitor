package com.example.mygraduationproject.network.response;

/** 璁惧鎺у埗鍝嶅簲浣擄紝鍚墽琛岀姸鎬佸拰璁惧淇℃伅 */
public class ControlResponse {
    private int code;
    private String message;
    private boolean success;
    private String device;
    private String status;
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getDevice() {
        return device;
    }
    
    public void setDevice(String device) {
        this.device = device;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
