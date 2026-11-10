package com.azkary.azkar2026;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID = "azkary_bg_service_v2";

    @Override
    public void onCreate() {
        super.onCreate();
        startSafeForeground();
        new AlarmHelper(this).scheduleAllPrayers();
    }

    private void startSafeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "تنبيهات الأذان",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("زاد المسلم")
            .setContentText("يتم تحديث مواقيت الصلاة تلقائيًا")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(1001, notification);
            }
        } catch (Exception e) {
            try { startForeground(1001, notification); } catch (Exception ignored) {}
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSafeForeground();
        ensureFloatingBubbleService();
        return START_STICKY;
    }

    private void ensureFloatingBubbleService() {
        try {
            SharedPreferences prefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            if (prefs.getBoolean("user_disabled_bubble", false)) return;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && android.provider.Settings.canDrawOverlays(this)) {
                if (FloatingBubbleService.instance == null) {
                    Intent bblIntent = new Intent(this, FloatingBubbleService.class);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(bblIntent);
                    } else {
                        startService(bblIntent);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
