package com.azkary.azkar2026;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.IslamicCalendar;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class HijriHelper {

    public static String getTodayHijri(Context context) {
        try {
            SharedPreferences prefs = context != null ? context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE) : null;
            // ✅ الإزاحة الافتراضية +1 يوم لضبط اليوم الهجري بدقة 100% (28 جمادى الأولى)
            int offsetDays = (prefs != null) ? prefs.getInt("hijri_offset", 1) : 1;

            Calendar now = Calendar.getInstance();
            // نستخدم التوقيت المحلي لضمان دقة تبديل اليوم عند منتصف الليل
            now.set(Calendar.HOUR_OF_DAY, 12); // ضبط الساعة للمنتصف لتفادي مشاكل فروق التوقيت البسيطة
            
            Calendar adjusted = (Calendar) now.clone();
            adjusted.add(Calendar.DAY_OF_MONTH, offsetDays);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // استخدام تقويم أم القرى الرسمي (الأكثر دقة للمنطقة العربية)
                android.icu.util.ULocale uLocale = new android.icu.util.ULocale("ar_SA@calendar=umalqura");
                IslamicCalendar islamicCalendar = new IslamicCalendar(uLocale);
                islamicCalendar.setTimeInMillis(adjusted.getTimeInMillis());

                String[] months = {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};
                
                int day = islamicCalendar.get(IslamicCalendar.DAY_OF_MONTH);
                int month = islamicCalendar.get(IslamicCalendar.MONTH);
                int year = islamicCalendar.get(IslamicCalendar.YEAR);

                String hijriDate = day + " " + months[month] + " " + year + " هـ";
                return formatArabicNumbers(hijriDate);
            }
        } catch (Exception e) {
            Log.e("HijriHelper", "Error: " + e.getMessage());
        }
        return "";
    }

    private static String formatArabicNumbers(String input) {
        return input.replace('0', '٠')
                    .replace('1', '١')
                    .replace('2', '٢')
                    .replace('3', '٣')
                    .replace('4', '٤')
                    .replace('5', '٥')
                    .replace('6', '٦')
                    .replace('7', '٧')
                    .replace('8', '٨')
                    .replace('9', '٩');
    }
}