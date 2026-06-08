package com.example.mygraduationproject.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.mygraduationproject.MainActivity;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.utils.PreferenceManager;

import java.util.concurrent.TimeUnit;

/** WorkManager瀹氭椂鎷嶇収浠诲姟锛氬悗鍙伴€氱煡瑙﹀彂鎷嶇収 */
public class AutoCaptureWorker extends Worker {

    private static final String TAG = "AutoCaptureWorker";
    private static final String WORK_NAME = "auto_capture_work";

    /** 广播 Action，MonitorFragment 监听此 Action 后执行拍照 */
    public static final String ACTION_AUTO_CAPTURE = "com.example.mygraduationproject.ACTION_AUTO_CAPTURE";

    private static final String NOTIFY_CHANNEL_ID = "auto_capture_channel";
    private static final int NOTIFY_ID = 1001;

    public AutoCaptureWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Auto capture worker triggered");

        Context context = getApplicationContext();
        PreferenceManager pref = new PreferenceManager(context);

        // 若用户已关闭自动拍摄，直接跳过
        if (!pref.isAutoCaptureEnabled()) {
            Log.d(TAG, "Auto capture is disabled, skipping.");
            return Result.success();
        }

        // 1. 发送本地广播，通知 MonitorFragment（App 在前台时）执行拍照
        Intent broadcastIntent = new Intent(ACTION_AUTO_CAPTURE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
        Log.d(TAG, "Local broadcast sent: " + ACTION_AUTO_CAPTURE);

        // 2. 同时发送系统通知，提醒用户定时拍摄已触发（App 在后台时也能看到）
        sendCaptureNotification(context);

        return Result.success();
    }

    /** 发送"定时拍摄提醒"通知，点击后跳转到 MainActivity */
    private void sendCaptureNotification(Context context) {
        createNotificationChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_plant_placeholder)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("定时拍摄已触发，请打开 App 完成植物状态采集")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFY_ID, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFY_CHANNEL_ID,
                    "定时拍摄提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("植物状态定时采集提醒");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 调度周期性自动拍摄任务。
     * WorkManager 最小间隔为 15 分钟，若设置值小于 15 分钟将自动取整。
     */
    public static void scheduleAutoCapture(Context context, int intervalMinutes) {
        int safeInterval = Math.max(intervalMinutes, 15);
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                AutoCaptureWorker.class,
                safeInterval,
                TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );

        Log.d(TAG, "Auto capture scheduled, interval=" + safeInterval + " min");
    }

    public static void cancelAutoCapture(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Auto capture cancelled");
    }
}
