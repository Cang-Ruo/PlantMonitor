package com.example.mygraduationproject.network;

import android.content.Context;
import android.util.Log;

import com.example.mygraduationproject.utils.PreferenceManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Retrofit鍗曚緥绠＄悊锛屾彁渚涚櫨搴I鍜岃澶囨帶鍒朵袱濂楀鎴风 */
public class RetrofitClient {
    
    private static final String TAG = "RetrofitClient";
    private static final String BAIDU_AI_BASE_URL = "https://aip.baidubce.com/";
    private static final int CONNECT_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;
    
    private static RetrofitClient instance;
    private final PreferenceManager preferenceManager;
    private Retrofit baiduAiRetrofit;
    private Retrofit deviceControlRetrofit;
    private BaiduAIApi baiduAIApi;
    private DeviceControlApi deviceControlApi;
    
    private RetrofitClient(Context context) {
        preferenceManager = new PreferenceManager(context);
        initBaiduAiRetrofit();
    }
    
    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
        return instance;
    }
    
    private void initBaiduAiRetrofit() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build();
        
        baiduAiRetrofit = new Retrofit.Builder()
                .baseUrl(BAIDU_AI_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        baiduAIApi = baiduAiRetrofit.create(BaiduAIApi.class);
    }
    
    public void initDeviceControlRetrofit(String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build();
        
        String url = baseUrl;
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        
        deviceControlRetrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        deviceControlApi = deviceControlRetrofit.create(DeviceControlApi.class);
    }
    
    public BaiduAIApi getBaiduAIApi() {
        return baiduAIApi;
    }
    
    public DeviceControlApi getDeviceControlApi() {
        if (deviceControlApi == null) {
            String ip = preferenceManager.getDeviceIp();
            int port = preferenceManager.getDevicePort();
            String baseUrl = "http://" + ip + ":" + port + "/";
            initDeviceControlRetrofit(baseUrl);
        }
        return deviceControlApi;
    }
    
    public void refreshDeviceControlApi() {
        deviceControlApi = null;
        deviceControlRetrofit = null;
    }
}
