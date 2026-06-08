package com.example.mygraduationproject.network;

import com.example.mygraduationproject.network.response.BaiduTokenResponse;
import com.example.mygraduationproject.network.response.PlantIdentifyResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** 鐧惧害AI鎺ュ彛瀹氫箟锛歍oken鑾峰彇鍜屾鐗╄瘑鍒?*/
public interface BaiduAIApi {
    
    @FormUrlEncoded
    @POST("oauth/2.0/token")
    Call<BaiduTokenResponse> getAccessToken(
            @Field("grant_type") String grantType,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret
    );
    
    @FormUrlEncoded
    @POST("rest/2.0/image-classify/v1/plant")
    Call<PlantIdentifyResponse> identifyPlant(
            @Query("access_token") String accessToken,
            @Field("image") String image,
            @Field("image_type") String imageType
    );
}
