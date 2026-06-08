package com.example.mygraduationproject.network;

import com.example.mygraduationproject.network.request.ControlRequest;
import com.example.mygraduationproject.network.response.ControlResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** 璁惧鎺у埗鎺ュ彛瀹氫箟锛歋TM32纭欢鎸囦护涓嬪彂 */
public interface DeviceControlApi {
    
    @POST("api/control")
    Call<ControlResponse> sendCommand(@Body ControlRequest request);
}
