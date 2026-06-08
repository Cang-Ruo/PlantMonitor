package com.example.mygraduationproject.network.response;

import java.util.List;

/** 妞嶇墿璇嗗埆鍝嶅簲浣擄紝鍚€欓€夌粨鏋滃垪琛?*/
public class PlantIdentifyResponse {
    private int log_id;
    private List<PlantResult> result;
    
    public int getLog_id() {
        return log_id;
    }
    
    public void setLog_id(int log_id) {
        this.log_id = log_id;
    }
    
    public List<PlantResult> getResult() {
        return result;
    }
    
    public void setResult(List<PlantResult> result) {
        this.result = result;
    }
    
    public static class PlantResult {
        private String name;
        private double score;
        private String baike_info;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
        
        public String getBaike_info() {
            return baike_info;
        }
        
        public void setBaike_info(String baike_info) {
            this.baike_info = baike_info;
        }
    }
}
