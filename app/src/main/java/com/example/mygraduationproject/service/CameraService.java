package com.example.mygraduationproject.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mygraduationproject.MainActivity;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.utils.PreferenceManager;
import com.example.mygraduationproject.worker.AutoCaptureWorker;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 植物监测前台服务。
 * 当 App 处于后台或被切换至后台时，由此服务持续保活并定时触发自动拍摄广播。
 * 使用方式：
 *   启动：startService / startForegroundService
 *   停止：stopService
 */
/** 鍓嶅彴鎷嶇収鏈嶅姟锛氫繚娲诲畾鏃舵媿鐓у箍鎾Е鍙戝櫒 */
public class CameraService extends Service {

    private static final String TAG = "CameraService";
    private static final String CHANNEL_ID = "camera_service_channel";
    private static final int NOTIFICATION_ID = 1;

    /** 用于从外部控制服务行为的 Action */
    public static final String ACTION_START_MONITORING = "com.example.mygraduationproject.START_MONITORING";
    public static final String ACTION_STOP_MONITORING  = "com.example.mygraduationproject.STOP_MONITORING";

    private Timer captureTimer;
    private PreferenceManager preferenceManager;

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
        createNotificationChannel();
        Log.d(TAG, "CameraService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 立即升级为前台服务，避免被系统杀死
        startForeground(NOTIFICATION_ID, buildNotification("植物监测中…"));

        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP_MONITORING.equals(action)) {
            Log.d(TAG, "Received STOP_MONITORING, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // 默认或 ACTION_START_MONITORING：开始定时触发
        startCaptureTimer();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopCaptureTimer();
        Log.d(TAG, "CameraService destroyed");
        super.onDestroy();
    }

    // -------------------------------------------------------------------------
    // 定时触发逻辑
    // -------------------------------------------------------------------------

    /**
     * 启动定时器，按用户设定的间隔向 MonitorFragment 发送本地广播，触发拍照。
     * 支持秒级和分钟级定时。
     */
    private void startCaptureTimer() {
        stopCaptureTimer();

        int intervalSeconds = preferenceManager.getIntervalInSeconds();
        long intervalMs = intervalSeconds * 1000L;

        captureTimer = new Timer("auto_capture_timer", true);
        captureTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!preferenceManager.isAutoCaptureEnabled()) {
                    Log.d(TAG, "Auto capture disabled, skipping timer tick");
                    return;
                }
                Log.d(TAG, "Timer tick: sending auto capture broadcast, interval=" + intervalSeconds + "s");
                Intent broadcastIntent = new Intent(AutoCaptureWorker.ACTION_AUTO_CAPTURE);
                LocalBroadcastManager.getInstance(CameraService.this).sendBroadcast(broadcastIntent);
            }
        }, intervalMs, intervalMs);

        String unitText = intervalSeconds < 60 ? "秒" : "分钟";
        int displayValue = intervalSeconds < 60 ? intervalSeconds : intervalSeconds / 60;
        Log.d(TAG, "Capture timer started, interval=" + intervalSeconds + "s (" + displayValue + " " + unitText + ")");
        updateNotification("植物监测中… 间隔 " + displayValue + " " + unitText);
    }

    private void stopCaptureTimer() {
        if (captureTimer != null) {
            captureTimer.cancel();
            captureTimer = null;
            Log.d(TAG, "Capture timer stopped");
        }
    }

    // -------------------------------------------------------------------------
    // 通知工具
    // -------------------------------------------------------------------------

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "植物监测服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持植物状态定时监测");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_plant_placeholder)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    // -------------------------------------------------------------------------
    // 静态启动/停止辅助方法
    // -------------------------------------------------------------------------

    public static void start(Context context) {
        Intent intent = new Intent(context, CameraService.class);
        intent.setAction(ACTION_START_MONITORING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, CameraService.class);
        intent.setAction(ACTION_STOP_MONITORING);
        context.startService(intent);
    }
}
