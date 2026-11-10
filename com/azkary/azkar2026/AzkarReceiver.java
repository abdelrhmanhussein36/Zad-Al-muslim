package com.azkary.azkar2026;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class AzkarReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "azkary_reminders_v2";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        if (type == null) type = "morning";

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);

        // ✅ حارس النافذة الزمنية (Time Window Guard) لمنع ظهور التنبيهات المتأخرة أو المتراكمة
        boolean isValidTime = true;
        if ("tahajjud".equals(type)) {
            // قيام الليل: بين 1:00 صباحاً و 5:00 صباحاً
            if (hour < 1 || hour >= 5) isValidTime = false;
        } else if ("morning".equals(type)) {
            // أذكار الصباح: بين 4:30 صباحاً و 11:30 صباحاً
            if (hour < 4 || hour >= 12) isValidTime = false;
        } else if ("evening".equals(type)) {
            // أذكار المساء: بين 3:00 عصراً (15) و 10:30 مساءً (22)
            if (hour < 15 || hour >= 23) isValidTime = false;
        } else if ("midday".equals(type)) {
            // منتصف اليوم: بين 11:30 صباحاً و 3:00 عصراً (15)
            if (hour < 11 || hour >= 15) isValidTime = false;
        }

        if (!isValidTime) {
            Log.w("AzkarReceiver", "⚠️ تجاهل تنبيه متأخر/متراكم من النوع: " + type + " في الساعة: " + hour);
            rescheduleNext(context, type);
            return;
        }

        String title = "زاد المسلم";
        String message = "حان الآن وقت الذكر";

        if ("morning".equals(type)) {
            title = "☀️ أذكار الصباح";
            message = "أصبحنا وأصبح الملك لله، حان وقت أذكار الصباح";
        } else if ("evening".equals(type)) {
            title = "🌙 أذكار المساء";
            message = "أمسينا وأمسى الملك لله، حان وقت أذكار المساء";
        } else if ("tahajjud".equals(type)) {
            title = "🌌 تذكير قيام الليل";
            message = "شرف المؤمن قيامه بالليل، لا تنس صلاة الوتر";
        } else if ("midday".equals(type)) {
            title = "🕒 تذكير منتصف اليوم";
            message = "لا تنس ذكر الله في زحمة يومك";
        }

        showNotification(context, title, message, type);
        rescheduleNext(context, type);
    }

    private void rescheduleNext(Context context, String type) {
        try {
            AlarmHelper helper = new AlarmHelper(context);
            if ("tahajjud".equals(type)) helper.scheduleExactAzkar("tahajjud", 2, 30, 10001);
            else if ("morning".equals(type)) helper.scheduleExactAzkar("morning", 7, 0, 10002);
            else if ("midday".equals(type)) helper.scheduleExactAzkar("midday", 12, 30, 10003);
            else if ("evening".equals(type)) helper.scheduleExactAzkar("evening", 16, 30, 10004);
        } catch (Exception e) {
            Log.e("AzkarReceiver", "Error rescheduling next: " + e.getMessage());
        }
    }

    private void showNotification(Context context, String title, String message, String type) {
        createNotificationChannel(context);
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("startPath", "/" + type);
        PendingIntent pi = PendingIntent.getActivity(context, type.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setTimeoutAfter(30 * 60 * 1000) // ✅ إلغاء الإشعار تلقائياً بعد 30 دقيقة لمنع التراكم
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(type.hashCode(), notification);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تنبيهات الأذكار", NotificationManager.IMPORTANCE_HIGH);
                manager.createNotificationChannel(channel);
            }
        }
    }
}
