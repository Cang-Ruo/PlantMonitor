package com.example.mygraduationproject.network.request;

/** 璁惧鎺у埗璇锋眰浣擄紝灏佽璁惧浠ｇ爜鍜屽姩浣滄寚浠?*/
public class ControlRequest {
    private String device;
    private String action;
    
    public ControlRequest(String device, String action) {
        this.device = device;
        this.action = action;
    }
    
    public String getDevice() {
        return device;
    }
    
    public void setDevice(String device) {
        this.device = device;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
}
