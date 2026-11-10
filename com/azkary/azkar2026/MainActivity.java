package com.azkary.azkar2026;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private boolean isBootFinished = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(LocationStoragePlugin.class);
        registerPlugin(AdhanControlPlugin.class);
        registerPlugin(FloatingWidgetPlugin.class);
        super.onCreate(savedInstanceState);

        incrementUsageCount();
        fixWebViewScrolling();
        AdHelper.resetUsageFlags();
        AdHelper.initialize(this); // ✅ تهيئة وتحميل إعلانات الفتح فوراً عند تشغيل التطبيق
        
        // تسلسل تشغيل الخدمات بأمان لأندرويد 14
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            bootSafeServices();
        }, 3000);

        requestInitialPermissions();
    }

    private void incrementUsageCount() {
        SharedPreferences prefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        int count = prefs.getInt("usage_count", 0);
        prefs.edit().putInt("usage_count", count + 1).commit();
    }

    private void bootSafeServices() {
        try {
            UpdateHelper.checkForAppUpdate(this);
            
            // ✅ تشغيل الجدولة العميقة فوراً عند فتح التطبيق
            new AlarmHelper(this).scheduleAllPrayers();
            
            // 1. تشغيل خدمة الخلفية أولاً
            if (!isServiceRunning(BackgroundService.class)) {
                Intent bgIntent = new Intent(this, BackgroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(bgIntent);
                else startService(bgIntent);
            }

            // 2. تشغيل البابل بعد استقرار النظام
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    if (!isServiceRunning(FloatingBubbleService.class)) {
                        Intent bblIntent = new Intent(this, FloatingBubbleService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(bblIntent);
                        else startService(bblIntent);
                    }
                }
                isBootFinished = true;
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "⚠️ Boot Error: " + e.getMessage());
        }
    }

    private static int returnTurnCount = 0;
    private boolean isBackgrounded = false;

    private void fixWebViewScrolling() {
        if (getBridge() != null && getBridge().getWebView() != null) {
            WebView wv = getBridge().getWebView();
            wv.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            WebSettings s = wv.getSettings();
            s.setDomStorageEnabled(true);
            s.setJavaScriptEnabled(true);
            s.setSupportZoom(true);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
            getBridge().getWebView().addJavascriptInterface(this, "Android");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        isBackgrounded = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(2001); // إلغاء إشعار الأذان السابق فور فتح التطبيق
            }
        } catch (Exception ignored) {}

        SharedPreferences tempPrefs = getSharedPreferences("temp_prefs", Context.MODE_PRIVATE);
        SharedPreferences azkaryPrefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        if (tempPrefs.getBoolean("is_adhan_playing", false) &&
            tempPrefs.getBoolean("is_fajr_adhan", false) &&
            azkaryPrefs.getBoolean("wake_up_quiz_enabled", false)) {
            Intent quizIntent = new Intent(this, WakeUpActivity.class);
            quizIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(quizIntent);
            return;
        }

        if (isBackgrounded) {
            isBackgrounded = false;
            returnTurnCount++;
            Log.d(TAG, "App returned from background, returnTurnCount = " + returnTurnCount);

            if (System.currentTimeMillis() - AdHelper.lastAdDismissedTime > 3000) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        AdHelper.showAppReturnAd(this);
                    } catch (Exception ignored) {}
                }, 500);
            }
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                try {
                    AdHelper.showAppOpenAd(this);
                    AdHelper.showBannerAd(this, getBridge().getWebView());
                } catch (Exception ignored) {}
            }, 800);
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        try {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void requestInitialPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }



    private void requestAddWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager manager = getSystemService(AppWidgetManager.class);
            ComponentName cn = new ComponentName(this, PrayerWidget.class);
            if (manager.isRequestPinAppWidgetSupported()) manager.requestPinAppWidget(cn, null, null);
        }
    }

    private void toggleWakeUpQuiz() {
        SharedPreferences prefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean("wake_up_quiz_enabled", false);
        prefs.edit().putBoolean("wake_up_quiz_enabled", !current).apply();
        Toast.makeText(this, !current ? "تم تفعيل أسئلة الإفاقة ✅" : "تم تعطيلها ❌", Toast.LENGTH_SHORT).show();
    }

    private void toggleFloatingBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            return;
        }
        bootSafeServices();
    }

    @JavascriptInterface
    public void showCompletionMessage(String msg) {
        runOnUiThread(() -> AdHelper.showInterstitialAd(this, msg, false));
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || 
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == android.view.KeyEvent.KEYCODE_POWER) {
            
            SharedPreferences tempPrefs = getSharedPreferences("temp_prefs", Context.MODE_PRIVATE);
            SharedPreferences azkaryPrefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            if (tempPrefs.getBoolean("is_adhan_playing", false)) {
                boolean isQuizEnabled = azkaryPrefs.getBoolean("wake_up_quiz_enabled", false);
                boolean isFajr = tempPrefs.getBoolean("is_fajr_adhan", false);
                
                if (isQuizEnabled && isFajr) {
                    Toast.makeText(this, "أسئلة الإفاقة مفعّلة: أجب عن الأسئلة لإغلاق أذان الفجر ⚠️", Toast.LENGTH_SHORT).show();
                    return true;
                }
                
                AdhanReceiver.stopAdhan(this);
                Toast.makeText(this, "تم كتم الأذان 🔇", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
