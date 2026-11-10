package com.azkary.azkar2026;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;

public class AzkarOverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private WebView webView;
    private ImageView closeBtn;
    private FrameLayout adContainer;

    private static final String CHANNEL_ID = "azkar_overlay_service";
    private static final int NOTIFICATION_ID = 1004;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        // ✅ ربط ملف التصميم الخاص بالنافذة العائمة
        overlayView = inflater.inflate(R.layout.layout_bubble_overlay, null);

        // ✅ ربط عناصر الواجهة
        webView = overlayView.findViewById(R.id.bubble_webview);
        closeBtn = overlayView.findViewById(R.id.overlay_close);
        adContainer = overlayView.findViewById(R.id.banner_container);

        // ✅ إعداد النافذة لتظهر فوق جميع التطبيقات
        int layoutParamsType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParamsType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParamsType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutParamsType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.CENTER;

        // ✅ إضافة النافذة إلى مدير النوافذ
        windowManager.addView(overlayView, params);

        // ✅ إظهار النافذة فوراً (ستكون مخفية مبدئياً حتى يأتي الإعلان)
        overlayView.setVisibility(View.GONE);

        // ✅ تجهيز الـ WebView
        setupWebView();

        // ✅ زر الإغلاق
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> closeOverlay());
        }

        // ✅ تحميل الإعلان مسبقاً
        BubbleAdActivity.loadAd(getApplicationContext());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (webView == null) return;

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // ✅ تمكين التمرير داخل الـ WebView
        webView.setVerticalScrollBarEnabled(true);
        webView.setOverScrollMode(WebView.OVER_SCROLL_ALWAYS);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // ✅ إضافة CSS لتحسين التمرير والمظهر
                String css = "document.body.style.overflow = 'auto';" +
                             "document.body.style.webkitOverflowScrolling = 'touch';";
                view.evaluateJavascript(css, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/public/index.html");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "ACTION_SHOW_WINDOW".equals(intent.getAction())) {
            // ✅ إظهار النافذة بعد الإعلان
            showOverlayAfterAd();
        }
        return START_STICKY;
    }

    /**
     * ✅ عرض النافذة (يتم استدعاؤها بعد الإعلان)
     */
    public void showOverlayAfterAd() {
        if (overlayView != null) {
            overlayView.setVisibility(View.VISIBLE);
            // ✅ تحميل الإعلان التالي مسبقاً
            BubbleAdActivity.loadAd(getApplicationContext());
        }
    }

    /**
     * ✅ إغلاق النافذة
     */
    public void closeOverlay() {
        if (overlayView != null) {
            overlayView.setVisibility(View.GONE);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "النافذة العائمة",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("زاد المسلم")
                .setContentText("النافذة العائمة مفعلة")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {}
        }
        // ✅ تحرير الـ WebView لتجنب تسريب الذاكرة
        if (webView != null) {
            webView.destroy();
        }
    }
}