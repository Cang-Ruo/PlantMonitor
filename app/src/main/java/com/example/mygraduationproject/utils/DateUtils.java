package com.example.mygraduationproject.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 鏃ユ湡鏃堕棿宸ュ叿锛氭椂闂存埑鏍煎紡鍖栥€佺浉瀵规椂闂淬€佹椂闀挎牸寮忓寲 */
public class DateUtils {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }
    
    public static String formatDateTime(long timestamp) {
        return DATETIME_FORMAT.format(new Date(timestamp));
    }
    
    public static String formatForFileName(long timestamp) {
        return FILE_DATE_FORMAT.format(new Date(timestamp));
    }
    
    public static String getRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return "刚刚";
        }
    }
    
    public static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d小时%d分", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d分%d秒", minutes, seconds % 60);
        } else {
            return seconds + "秒";
        }
    }
}
