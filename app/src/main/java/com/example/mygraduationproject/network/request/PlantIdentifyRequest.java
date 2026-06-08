package com.example.mygraduationproject.network.request;

/** 妞嶇墿璇嗗埆璇锋眰浣擄紝灏佽Base64鍥剧墖鏁版嵁 */
public class PlantIdentifyRequest {
    private String image;
    private String image_type;
    
    public PlantIdentifyRequest(String imageBase64) {
        this.image = imageBase64;
        this.image_type = "BASE64";
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public String getImage_type() {
        return image_type;
    }
    
    public void setImage_type(String image_type) {
        this.image_type = image_type;
    }
}
