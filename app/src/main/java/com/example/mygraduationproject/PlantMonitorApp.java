package com.example.mygraduationproject;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/** Application鍏ㄥ眬绫伙細寮傚父澶勭悊銆佸疄渚嬬鐞?*/
public class PlantMonitorApp extends Application {
    
    private static final String TAG = "PlantMonitorApp";
    private static PlantMonitorApp instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("=== PlantMonitorApp onCreate ===");
        Log.e("PlantMonitorApp", "=== Application onCreate ===");
        instance = this;
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println("=== Uncaught exception: " + throwable.getMessage());
            Log.e("PlantMonitorApp", "Uncaught exception in thread: " + thread.getName(), throwable);
            throwable.printStackTrace();
        });
        System.out.println("=== PlantMonitorApp onCreate 完成 ===");
        Log.e("PlantMonitorApp", "=== Application onCreate 完成 ===");
    }
    
    public static PlantMonitorApp getInstance() {
        return instance;
    }
    
    public static Context getContext() {
        return instance.getApplicationContext();
    }
}
