package com.azkary.azkar2026;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {

            Log.d("BootReceiver", "🚀 إقلاع الجهاز/تحديث التطبيق، بنشغل خدمات الويدجت والأذان...");

            try {
                // 1. تشغيل خدمة الخلفية
                Intent bgIntent = new Intent(context, BackgroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(bgIntent);
                } else {
                    context.startService(bgIntent);
                }

                // 2. إعادة تشغيل الأيقونة العائمة إذا كانت الصلاحية مفعّلة
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                    Intent bblIntent = new Intent(context, FloatingBubbleService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(bblIntent);
                    } else {
                        context.startService(bblIntent);
                    }
                }

                // 3. تحديث ويدجت الشاشة الرئيسية
                PrayerWidget.forceUpdate(context);
            } catch (Exception e) {
                Log.e("BootReceiver", "Error starting services: " + e.getMessage());
            }
        }
    }
}
