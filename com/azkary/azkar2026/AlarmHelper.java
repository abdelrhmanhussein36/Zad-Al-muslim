package com.azkary.azkar2026;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class AlarmHelper {

    private Context context;
    private AlarmManager alarmManager;

    private static final String[] PRAYER_NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
    private static final String[] PRAYER_KEYS = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private static final int DAYS_TO_SCHEDULE = 14;

    public AlarmHelper(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleAllPrayers() {
        Log.d("AlarmHelper", "🚀 تحديث الجدولة بنظام المنبه الرسمي...");
        new Thread(() -> {
            try {
                fetchAndScheduleMultiDays();
                scheduleAzkarReminders();
            } catch (Exception e) {
                Log.e("AlarmHelper", "Error: " + e.getMessage());
            }
        }).start();
    }

    private void fetchAndScheduleMultiDays() {
        double lat = 30.0444; double lng = 31.2357;
        SharedPreferences prefs = context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        
        try {
            String locationJson = prefs.getString("user_location", "");
            if (!locationJson.isEmpty()) {
                JSONObject obj = new JSONObject(locationJson);
                lat = obj.getDouble("lat"); lng = obj.getDouble("lng");
            }
        } catch (Exception ignored) {}

        for (int i = 0; i < DAYS_TO_SCHEDULE; i++) {
            Calendar targetDate = Calendar.getInstance();
            targetDate.add(Calendar.DAY_OF_YEAR, i);
            
            try {
                String urlString = "https://api.aladhan.com/v1/timings/" + 
                    targetDate.get(Calendar.DAY_OF_MONTH) + "-" + 
                    (targetDate.get(Calendar.MONTH) + 1) + "-" + 
                    targetDate.get(Calendar.YEAR) +
                    "?latitude=" + lat + "&longitude=" + lng + "&method=5";

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject timings = new JSONObject(sb.toString()).getJSONObject("data").getJSONObject("timings");
                
                if (i == 0) {
                    SharedPreferences.Editor editor = prefs.edit();
                    for (String key : PRAYER_KEYS) editor.putString("prayer_" + key, timings.getString(key));
                    editor.apply();
                    try { PrayerWidget.forceUpdate(context); } catch (Exception ignored) {}
                }

                for (int p = 0; p < PRAYER_KEYS.length; p++) {
                    scheduleExactAlarm(PRAYER_NAMES[p], timings.getString(PRAYER_KEYS[p]), targetDate, p);
                }

            } catch (Exception e) {
                scheduleOffline(targetDate);
            }
        }
    }

    private void scheduleExactAlarm(String name, String timeStr, Calendar baseDate, int prayerIdx) {
        try {
            String[] parts = timeStr.split(":");
            Calendar prayerTime = (Calendar) baseDate.clone();
            prayerTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            prayerTime.set(Calendar.MINUTE, Integer.parseInt(parts[1].replaceAll("[^0-9]", "")));
            prayerTime.set(Calendar.SECOND, 0);
            prayerTime.set(Calendar.MILLISECOND, 0);

            if (prayerTime.getTimeInMillis() <= System.currentTimeMillis()) return;

            int requestCode = (baseDate.get(Calendar.YEAR) * 1000) + (baseDate.get(Calendar.DAY_OF_YEAR) * 10) + prayerIdx;
            
            Intent intent = new Intent(context, AdhanReceiver.class);
            intent.putExtra("prayer", name);
            
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

            if (alarmManager != null) {
                // ✅ استخدام نظام AlarmClock لضمان الدقة ومنع القتل من توفير البطارية
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(prayerTime.getTimeInMillis(), pi);
                    alarmManager.setAlarmClock(info, pi);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, prayerTime.getTimeInMillis(), pi);
                }
            }
        } catch (Exception ignored) {}
    }

    private void scheduleOffline(Calendar date) {
        SharedPreferences prefs = context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        for (int p = 0; p < PRAYER_KEYS.length; p++) {
            String savedTime = prefs.getString("prayer_" + PRAYER_KEYS[p], null);
            if (savedTime != null) scheduleExactAlarm(PRAYER_NAMES[p], savedTime, date, p);
        }
    }

    private void scheduleAzkarReminders() {
        scheduleExactAzkar("tahajjud", 2, 30, 10001);
        scheduleExactAzkar("morning", 7, 0, 10002);
        scheduleExactAzkar("midday", 12, 30, 10003);
        scheduleExactAzkar("evening", 16, 30, 10004);
    }

    public void scheduleExactAzkar(String type, int h, int m, int requestCode) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(context, AzkarReceiver.class);
            intent.putExtra("type", type);
            
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
            
            if (alarmManager != null) {
                // ✅ استخدام نظام AlarmClock لتفعيل التنبيه بدقة رسمية صارمة كأيقونة المنبه
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(cal.getTimeInMillis(), pi);
                    alarmManager.setAlarmClock(info, pi);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }
            }
            Log.d("AlarmHelper", "⏰ Scheduled exact Azkar AlarmClock for " + type + " at " + cal.getTime());
        } catch (Exception e) {
            Log.e("AlarmHelper", "Error scheduling exact Azkar: " + e.getMessage());
        }
    }
}
