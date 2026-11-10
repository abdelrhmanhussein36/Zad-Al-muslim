package com.azkary.azkar2026;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;

public class AdhanReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "azkary_adhan_v8";
    private static final int NOTIFICATION_ID = 2001;
    public static MediaPlayer mediaPlayer = null;
    private static MediaSession mediaSession = null;
    private static PowerManager.WakeLock wakeLock = null;
    private static PowerManager.WakeLock cpuLock = null;
    private static BroadcastReceiver screenOffReceiver = null;

    private static synchronized void registerScreenOffReceiver(Context context) {
        if (screenOffReceiver == null) {
            screenOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        Log.d("AdhanReceiver", "📱 Screen turned off by power button or timeout - stopping Adhan");
                        stopAdhan(ctx);
                    }
                }
            };
            try {
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                context.getApplicationContext().registerReceiver(screenOffReceiver, filter);
            } catch (Exception e) {
                Log.e("AdhanReceiver", "Error registering screenOffReceiver: " + e.getMessage());
            }
        }
    }

    private static synchronized void unregisterScreenOffReceiver(Context context) {
        if (screenOffReceiver != null) {
            try {
                context.getApplicationContext().unregisterReceiver(screenOffReceiver);
            } catch (Exception ignored) {}
            screenOffReceiver = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayer = intent.getStringExtra("prayer");
        if (prayer == null) prayer = "الصلاة";

        SharedPreferences prefs = context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        boolean isSilent = prefs.getBoolean("silent_adhan_enabled", false);
        boolean isFajr = prayer != null && (prayer.contains("فجر") || prayer.toLowerCase().contains("fajr"));

        // تسجيل وقت البدء وتفعيل حالة التشغيل
        context.getSharedPreferences("temp_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_adhan_playing", true)
                .putBoolean("is_fajr_adhan", isFajr)
                .putLong("adhan_start_time", System.currentTimeMillis())
                .apply();

        if (!isSilent) {
            playAdhanSound(context, prayer);
            setupMediaSession(context); // تفعيل اعتراض أزرار الميديا وسماعات الأذن
        }

        vibrate(context);
        showNotification(context, prayer);
        wakeScreen(context);

        // تحديث الويدجت فوراً لضمان الانتقال للصلاة التالية
        try { PrayerWidget.forceUpdate(context); } catch (Exception ignored) {}

        if (isFajr && prefs.getBoolean("wake_up_quiz_enabled", false)) {
            try {
                Intent quizIntent = new Intent(context, WakeUpActivity.class);
                quizIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(quizIntent);
            } catch (Exception e) {
                Log.e("AdhanReceiver", "Error starting WakeUpActivity: " + e.getMessage());
            }
        }
    }

    private void playAdhanSound(Context context, String prayer) {
        try {
            stopAdhan(context); 

            SharedPreferences prefs = context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            
            // 1. جلب اسم ملف المؤذن المحدد من المستخدم لهذه الصلاة تحديداً
            boolean isFajr = prayer != null && (prayer.contains("فجر") || prayer.toLowerCase().contains("fajr"));
            String defaultSound = isFajr ? "fajr_adhan.mp3" : "adhan_refat1.mp3";
            String selectedMuezzin = prefs.getString("selected_muezzin_" + prayer, defaultSound);

            Log.d("AdhanReceiver", "🔊 Playing user selected muezzin for " + prayer + ": " + selectedMuezzin);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            // 2. تشغيل الملف المحدد من المستخدم (سواء كان ملفاً مخصصاً من الموبايل أو أذاناً مدمجاً)
            if (selectedMuezzin.startsWith("custom_")) {
                File file = new File(context.getFilesDir(), selectedMuezzin);
                if (file.exists() && file.length() > 0) {
                    mediaPlayer = new MediaPlayer();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaPlayer.setAudioAttributes(audioAttributes);
                    } else {
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    }
                    FileInputStream fis = new FileInputStream(file);
                    mediaPlayer.setDataSource(fis.getFD(), 0, file.length());
                    mediaPlayer.prepare(); // التحضير أولاً قبل إغلاق fis
                    fis.close();
                }
            } else {
                String resName = selectedMuezzin.replace(".mp3", "").trim();
                int resId = context.getResources().getIdentifier(resName, "raw", context.getPackageName());
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resId);
                    if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaPlayer.setAudioAttributes(audioAttributes);
                    }
                }
            }

            // 3. تشغيل الصوت بمستوى الصوت المحدد من المستخدم للجهاز دون رفعه أو التعديل عليه برمجياً
            if (mediaPlayer != null) {
                mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                                    .setAudioAttributes(audioAttributes)
                                    .build();
                            am.requestAudioFocus(focusRequest);
                        } catch (Exception ignored) {}
                    } else {
                        try { am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK); } catch (Exception ignored) {}
                    }
                }

                mediaPlayer.start();
                registerScreenOffReceiver(context);
                mediaPlayer.setOnCompletionListener(mp -> stopAdhan(context));
                Log.d("AdhanReceiver", "✅ Selected muezzin sound started playing successfully!");
            } else {
                Log.e("AdhanReceiver", "❌ Could not create MediaPlayer for: " + selectedMuezzin);
            }
        } catch (Exception e) {
            Log.e("AdhanReceiver", "❌ Play error for " + prayer + ": " + e.getMessage(), e);
        }
    }

    private void setupMediaSession(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mediaSession != null) mediaSession.release();
            mediaSession = new MediaSession(context, "AzkaryAdhan");
            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setActive(true);
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                    stopAdhan(context);
                    return true;
                }
            });
        }
    }

    private void vibrate(Context context) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(2000);
            }
        } catch (Exception ignored) {}
    }

    private void showNotification(Context context, String prayer) {
        createNotificationChannel(context);
        SharedPreferences prefs = context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        boolean isFajr = prayer != null && (prayer.contains("فجر") || prayer.toLowerCase().contains("fajr"));

        Intent intent = new Intent(context, MainActivity.class);
        if (isFajr && prefs.getBoolean("wake_up_quiz_enabled", false)) {
            intent = new Intent(context, WakeUpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🕌 أذان " + prayer)
            .setContentText("حان وقت صلاة " + prayer)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setTimeoutAfter(30 * 60 * 1000)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(false);

        if (isFajr && prefs.getBoolean("wake_up_quiz_enabled", false)) {
            Intent quizIntent = new Intent(context, WakeUpActivity.class);
            quizIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent fullScreenPi = PendingIntent.getActivity(context, 8888, quizIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
            builder.setFullScreenIntent(fullScreenPi, true);
        }

        Intent muteIntent = new Intent(context, MuteReceiver.class);
        PendingIntent mutePi = PendingIntent.getBroadcast(context, 0, muteIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        builder.addAction(android.R.drawable.ic_lock_silent_mode_off, "إيقاف الصوت 🔇", mutePi);

        NotificationManager m = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (m != null) m.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(CHANNEL_ID, "الأذان", NotificationManager.IMPORTANCE_HIGH);
            c.setSound(null, null); // 🔇 كتم صوت النقر/النوتيفيكيشن الافتراضي لتجنب التداخل مع صوت الأذان
            c.enableVibration(false);
            NotificationManager m = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (m != null) m.createNotificationChannel(c);
        }
    }

    private void wakeScreen(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "azkary:WakeLock");
            wakeLock.acquire(5 * 60 * 1000); // إبقاء الشاشة مضاءة لمدة 5 دقائق لضمان اكتمال الأذان
            
            if (cpuLock != null && cpuLock.isHeld()) cpuLock.release();
            cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "azkary:CPULock");
            cpuLock.acquire(5 * 60 * 1000); // 5 دقائق للمعالج
        } catch (Exception ignored) {}
    }

    public static void stopAdhan() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            try { mediaSession.setActive(false); mediaSession.release(); } catch (Exception ignored) {}
            mediaSession = null;
        }
        if (wakeLock != null) {
            try { if (wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
            wakeLock = null;
        }
        if (cpuLock != null) {
            try { if (cpuLock.isHeld()) cpuLock.release(); } catch (Exception ignored) {}
            cpuLock = null;
        }
    }

    public static void stopAdhan(Context context) {
        stopAdhan();
        unregisterScreenOffReceiver(context);
        try {
            context.getSharedPreferences("temp_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_adhan_playing", false)
                    .apply();

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIFICATION_ID);
            try { PrayerWidget.forceUpdate(context); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }
}
