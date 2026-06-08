package com.example.mygraduationproject.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.mygraduationproject.service.CameraService;
import com.example.mygraduationproject.utils.PreferenceManager;
import com.example.mygraduationproject.worker.AutoCaptureWorker;

/**
 * 开机广播接收器。
 * 设备重启后，若用户之前已开启自动拍摄，则自动恢复 WorkManager 任务和前台服务。
 */
/** 寮€鏈哄箍鎾帴鏀跺櫒锛氭仮澶嶅畾鏃舵媿鐓т换鍔″拰鏈嶅姟 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Log.d(TAG, "Boot completed received");

        PreferenceManager pref = new PreferenceManager(context);
        if (pref.isAutoCaptureEnabled()) {
            int interval = pref.getCaptureInterval();
            Log.d(TAG, "Auto capture enabled, rescheduling. interval=" + interval + " min");

            // 重新调度 WorkManager 周期任务（保底，应用不在前台时仍能触发通知）
            AutoCaptureWorker.scheduleAutoCapture(context, interval);

            // 重启前台服务（App 被用户打开后才真正拍照，但服务持续存活保活）
            CameraService.start(context);
        } else {
            Log.d(TAG, "Auto capture not enabled, skipping boot restore");
        }
    }
}
