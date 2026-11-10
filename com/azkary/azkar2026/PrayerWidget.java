package com.azkary.azkar2026;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PrayerWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "azkary_prefs";

    private static final String[] AYAT_TITLES = {
            "سُورَةُ الْبَقَرَةِ ٢:١٥٢",
            "سُورَةُ الْأَحْزَابِ ٣٣:٤١",
            "سُورَةُ الْإِسْرَاءِ ١٧:٧٨",
            "سُورَةُ طه ٢٠:١٤",
            "سُورَةُ النُّورِ ٢٤:٣٦",
            "سُورَةُ فَاطِرٍ ٣٥:٢٩",
            "سُورَةُ الزُّمَرِ ٣٩:٥٣",
            "سُورَةُ ق ٥٠:٣٩",
            "سُورَةُ الذَّارِيَاتِ ٥١:٥٦",
            "سُورَةُ الضُّحَىٰ ٩٣:٣",
            "سُورَةُ النِّسَاءِ ٤:١٠٣",
            "سُورَةُ الْكَهْفِ ١٨:٢٤"
    };

    private static final String[] AYAT_TEXTS = {
            "فَاذْكُرُونِي أَذْكُرْكُمْ",
            "يَا أَيُّهَا الَّذِينَ آمَنُوا اذْكُرُوا اللَّهَ ذِكْرًا كَثِيرًا",
            "أَقِمِ الصَّلَاةَ لِدُلُوكِ الشَّمْسِ إِلَىٰ غَسَقِ اللَّيْلِ",
            "إِنَّنِي أَنَا اللَّهُ لَا إِلَٰهَ إِلَّا أَنَا فَاعْبُدْنِي وَأَقِمِ الصَّلَاةَ لِذِكْرِي",
            "فِي بُيُوتٍ أَذِنَ اللَّهُ أَن تُرْفَعَ وَيُذْكَرَ فِيهَا اسْمُهُ",
            "إِنَّ الَّذِينَ يَتْلُونَ كِتَابَ اللَّهِ وَأَقَامُوا الصَّلَاةَ يَرْجُونَ تِجَارَةً لَّن تَبُورَ",
            "۞ قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ",
            "فَاصْبِرْ عَلَىٰ مَا يَقُولُونَ وَسَبِّحْ بِحَمْدِ رَبِّكَ قَبْلَ طُلُوعِ الشَّمْسِ",
            "وَمَا خَلَقْتُ الْجِنَّ وَالْإِنسَ إِلَّا لِيَعْبُدُونِ",
            "مَا وَدَّعَكَ رَبُّكَ وَمَا قَلَىٰ",
            "فَإِذَا اطْمَأْنَنتُمْ فَأَقِيمُوا الصَّلَاةَ ۚ إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا",
            "وَاذْكُر رَّبَّكَ إِذَا نَسِيتَ وَقُلْ عَسَىٰ أَن يَهْدِيَنِ رَبِّي لِأَقْرَبَ مِنْ هَٰذَا رَشَدًا"
    };

    private static final String[] FLOWERS = {"🌵", "🌸", "🌹", "🌻", "🌺", "🌷", "🌼", "🌿"};

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        forceUpdate(context);
        try { new AlarmHelper(context).scheduleAllPrayers(); } catch (Exception ignored) {}
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Intent.ACTION_TIME_TICK.equals(intent.getAction()) || 
            Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) ||
            Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()) ||
            "com.azkary.azkar2026.FORCE_WIDGET_UPDATE".equals(intent.getAction())) {
            forceUpdate(context);
        }
    }

    public static void forceUpdate(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            if (manager != null) {
                int[] ids = manager.getAppWidgetIds(new ComponentName(context, PrayerWidget.class));
                for (int id : ids) updateAppWidget(context, manager, id);
            }
        } catch (Exception ignored) {}
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_floating_widget);
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Locale arLocale = new Locale("ar");
            Calendar now = Calendar.getInstance();

            // النقر على الويدجت يفتح التطبيق مباشرة
            Intent appIntent = new Intent(context, MainActivity.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getActivity(context, 0, appIntent, flags);
            views.setOnClickPendingIntent(R.id.widget_root, pi);

            // التاريخ الميلادي
            SimpleDateFormat gregSdf = new SimpleDateFormat("EEEE، d MMMM yyyy", arLocale);
            views.setTextViewText(R.id.widget_gregorian, gregSdf.format(now.getTime()));

            // التاريخ الهجري الموحد
            views.setTextViewText(R.id.widget_hijri, HijriHelper.getTodayHijri(context));

            // الساعة المباشرة يتم تحديثها تلقائياً بواسطة TextClock بالـ Layout دون الحاجة إلى نص ثابت

            // تحديث الآية والمواقيت
            updateDailyAyah(views, context, prefs, now);
            updatePrayerInfo(views, prefs, arLocale, now);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e("PrayerWidget", "Widget update error: " + e.getMessage());
        }
    }

    private static void updateDailyAyah(RemoteViews views, Context context, SharedPreferences prefs, Calendar now) {
        try {
            int fajrMins = 281; // الافتراضي 4:41 ص
            String fajrStr = prefs.getString("prayer_Fajr", "04:41");
            if (fajrStr != null && fajrStr.contains(":")) {
                try {
                    String[] parts = fajrStr.split(":");
                    fajrMins = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
            }

            int currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            long dayKey = now.get(Calendar.DAY_OF_YEAR) + (now.get(Calendar.YEAR) * 365L);

            // يتغير اختيار الآية والأيقونة المرفقة يومياً بعد صلاة الفجر
            if (currentMins < fajrMins) {
                dayKey = dayKey - 1;
            }

            int index = (int) (Math.abs(dayKey) % AYAT_TEXTS.length);
            int flowerIndex = (int) (Math.abs(dayKey) % FLOWERS.length);

            views.setTextViewText(R.id.widget_ayah_title, AYAT_TITLES[index]);
            views.setTextViewText(R.id.widget_ayah_text, AYAT_TEXTS[index] + " " + FLOWERS[flowerIndex]);
        } catch (Exception ignored) {}
    }

    private static void updatePrayerInfo(RemoteViews views, SharedPreferences prefs, Locale locale, Calendar now) {
        String[] keys = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] names = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
        String[] icons = {"🌅", "☀️", "🌤️", "🌇", "🌙"};

        String[] defaultTimes = {"04:41", "12:00", "15:30", "18:45", "20:00"};

        int currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int nextIdx = -1;

        for (int i = 0; i < keys.length; i++) {
            String t = prefs.getString("prayer_" + keys[i], defaultTimes[i]);
            if (t == null || t.isEmpty()) t = defaultTimes[i];
            
            try {
                String[] p = t.split(":");
                int prayerMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1].replaceAll("[^0-9]", ""));
                if (prayerMins > currentMins) {
                    nextIdx = i;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (nextIdx == -1) nextIdx = 0;
        int secondIdx = (nextIdx + 1) % keys.length;

        String t1 = prefs.getString("prayer_" + keys[nextIdx], defaultTimes[nextIdx]);
        if (t1 == null || t1.isEmpty()) t1 = defaultTimes[nextIdx];

        String t2 = prefs.getString("prayer_" + keys[secondIdx], defaultTimes[secondIdx]);
        if (t2 == null || t2.isEmpty()) t2 = defaultTimes[secondIdx];

        views.setTextViewText(R.id.widget_prayer_1, icons[nextIdx] + " " + names[nextIdx] + " " + formatTime12h(t1, locale));
        views.setTextViewText(R.id.widget_prayer_2, icons[secondIdx] + " " + names[secondIdx] + " " + formatTime12h(t2, locale));
    }

    private static String formatTime12h(String time, Locale locale) {
        try {
            if (time == null || time.isEmpty() || time.equals("--:--")) return time;
            String[] p = time.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1].replaceAll("[^0-9]", ""));
            String suffix = (h >= 12) ? " م" : " ص";
            h = (h > 12) ? h - 12 : (h == 0 ? 12 : h);
            return String.format(locale, "%d:%02d%s", h, m, suffix);
        } catch (Exception e) { return time; }
    }
}
