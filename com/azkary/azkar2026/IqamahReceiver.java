package com.azkary.azkar2026;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class IqamahReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "azkary_iqamah_v2"; // تغيير الـ ID لضمان تحديث الإعدادات في النظام
    private static final int NOTIFICATION_ID = 2002;

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayer = intent.getStringExtra("prayer");
        if (prayer == null) {
            prayer = "الصلاة";
        }

        showNotification(context, prayer);
        
        // ✅ تحديث الويدجت فوراً لضمان دقة المواقيت المعروضة
        try {
            PrayerWidget.forceUpdate(context);
        } catch (Exception ignored) {}
    }

    private void showNotification(Context context, String prayer) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // استخدام صوت التنبيه الافتراضي للجهاز
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🕌 حان وقت " + prayer)
            .setContentText("قامت الصلاة، قامت الصلاة 🕌")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(new long[]{1000, 1000, 1000})
            .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات الصلاة والإقامة",
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("تنبيهات مواعيد الصلاة");
                channel.enableVibration(true);
                // استخدام صوت النظام الافتراضي للقناة
                manager.createNotificationChannel(channel);
            }
        }
    }
}
