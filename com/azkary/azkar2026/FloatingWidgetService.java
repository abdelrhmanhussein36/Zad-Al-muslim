package com.azkary.azkar2026;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class FloatingWidgetService extends Service {
    private static final String CHANNEL_ID = "widget_service_v11";

    @Override
    public void onCreate() {
        // ✅ البدء الفوري لخدمة الويدجت لمنع كراش أندرويد 14
        startSafeForeground();
        super.onCreate();
    }

    private void startSafeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "خدمة الويدجت", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("زاد المسلم")
                .setContentText("الويدجت يعمل في الخلفية")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // ✅ إضافة نوع الخدمة لمنع الكراش في أندرويد 14
                startForeground(1003, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(1003, notification);
            }
        } catch (Exception e) {
            try { startForeground(1003, notification); } catch (Exception ignored) {}
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSafeForeground();
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
