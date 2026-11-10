package com.azkary.azkar2026;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {

    private static final String CHANNEL_ID = "azkar_notification_channel";

    public static void createNotificationChannel(Context context, String rawChannelName) {
        if (context == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String cleanChannelName = sanitizeText(rawChannelName);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, cleanChannelName, importance);
            channel.setDescription("قناة إشعارات تطبيق زاد المسلم");

            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private static String sanitizeText(String input) {
        if (input == null || input.isEmpty()) {
            return "زاد المسلم";
        }
        return input
                .replace("\n", "")
                .replace("\r", "")
                .replace("\"", "'")
                .trim();
    }
}