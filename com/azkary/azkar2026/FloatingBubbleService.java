package com.azkary.azkar2026;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.Locale;

public class FloatingBubbleService extends Service {
    private WindowManager windowManager;
    private View bubbleView;
    private WindowManager.LayoutParams bubbleParams;
    private View overlayView;
    private View removeView;
    private AdView adView;
    private WebView webView;
    private static final String NOTIF_CHANNEL_ID = "bubble_v70_final";
    public static FloatingBubbleService instance = null;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSafeForeground();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        startSafeForeground();
        super.onCreate();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createBubble();
        createRemoveArea();
        BubbleAdActivity.loadAd(getApplicationContext());
    }

    private void startSafeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, "زاد المسلم", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle("زاد المسلم")
                .setContentText("الأيقونة تعمل")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1002, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else { startForeground(1002, notification); }
        } catch (Exception ignored) {}
    }

    private void createBubble() {
        if (bubbleView != null) return;
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_floating_bubble, null);

        ImageView bubbleIcon = bubbleView.findViewById(R.id.bubble_icon);
        if (bubbleIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float cornerRadius = 16f * getResources().getDisplayMetrics().density;
            bubbleIcon.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                }
            });
            bubbleIcon.setClipToOutline(true);
        }

        int type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        bubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 100; bubbleParams.y = 300;
        windowManager.addView(bubbleView, bubbleParams);

        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x; initialY = bubbleParams.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        if (removeView != null) {
                            removeView.setVisibility(View.VISIBLE);
                            removeView.setScaleX(1.0f);
                            removeView.setScaleY(1.0f);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        bubbleParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        bubbleParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(bubbleView, bubbleParams);

                        if (removeView != null && removeView.getVisibility() == View.VISIBLE) {
                            if (isInRemoveZone(event.getRawX(), event.getRawY())) {
                                removeView.setScaleX(1.3f);
                                removeView.setScaleY(1.3f);
                            } else {
                                removeView.setScaleX(1.0f);
                                removeView.setScaleY(1.0f);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (removeView != null) removeView.setVisibility(View.GONE);
                        float dx = Math.abs(event.getRawX() - initialTouchX);
                        float dy = Math.abs(event.getRawY() - initialTouchY);

                        if (isInRemoveZone(event.getRawX(), event.getRawY())) {
                            removeBubble();
                        } else if (dx < 15 && dy < 15) {
                            toggleOverlay();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private boolean isInRemoveZone(float rawX, float rawY) {
        if (removeView == null) return false;
        int[] location = new int[2];
        removeView.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        int viewWidth = removeView.getWidth() > 0 ? removeView.getWidth() : 220;
        int viewHeight = removeView.getHeight() > 0 ? removeView.getHeight() : 220;

        int expand = 80;
        boolean insideRect = (rawX >= (viewX - expand) && rawX <= (viewX + viewWidth + expand) &&
                              rawY >= (viewY - expand) && rawY <= (viewY + viewHeight + expand));

        if (insideRect) return true;

        try {
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            if (rawY > (displayHeight * 0.78f) && Math.abs(rawX - (displayWidth / 2f)) < (displayWidth * 0.35f)) {
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private void removeBubble() {
        try {
            SharedPreferences prefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("user_disabled_bubble", true).apply();
        } catch (Exception ignored) {}
        stopSelf();
    }

    private void createRemoveArea() {
        try {
            removeView = LayoutInflater.from(this).inflate(R.layout.layout_remove_bubble, null);
        } catch (Exception e) {
            removeView = new ImageView(this);
            ((ImageView) removeView).setImageResource(android.R.drawable.ic_menu_delete);
            removeView.setBackgroundResource(android.R.drawable.presence_offline);
            removeView.setPadding(25, 25, 25, 25);
        }
        removeView.setVisibility(View.GONE);

        int type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams rp = new WindowManager.LayoutParams(220, 220, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        rp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; rp.y = 120;
        windowManager.addView(removeView, rp);
    }

    private void toggleOverlay() {
        Intent intent = new Intent(this, BubbleAdActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void showOverlayAfterAd() {
        if (overlayView != null) { try { windowManager.removeView(overlayView); } catch (Exception ignored) {} overlayView = null; webView = null; }
        createOverlay();
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void createOverlay() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.layout_bubble_overlay, null);
        int type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams op = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                type, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(overlayView, op);
        overlayView.findViewById(R.id.overlay_close).setOnClickListener(v -> closeOverlay());

        webView = overlayView.findViewById(R.id.bubble_webview);
        setupWebView();
        
        FrameLayout adContainer = overlayView.findViewById(R.id.banner_container);
        if (adContainer != null) {
            adView = new AdView(this); adView.setAdSize(AdSize.BANNER); adView.setAdUnitId("ca-app-pub-4851467684137333/3748516159");
            adContainer.addView(adView); adView.loadAd(new AdRequest.Builder().build());
        }
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); 
        s.setDomStorageEnabled(true); 
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true); 
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setGeolocationEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);

        // ✅ إزالة أي خلفية يدوية للسماح للتطبيق باستخدام خلفيته الأصلية (Dark/Light)
        webView.addJavascriptInterface(new BubbleBridge(), "BubbleBridge");
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // ✅ حقن المتغير فوراً ليعرف الفرونت ايند أنه في النافذة العائمة (لحذف قسم الأذان)
                view.evaluateJavascript("window.isAzkaryModal = true;", null);
                if (newProgress > 40) injectAtomicSystem();
            }
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("BubbleWebView", consoleMessage.message());
                return true;
            }
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        final WebViewAssetLoader.AssetsPathHandler assetsHandler = new WebViewAssetLoader.AssetsPathHandler(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) { 
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host == null || !host.equalsIgnoreCase("appassets.androidplatform.net")) return null;
                
                String path = uri.getPath();
                if (path == null) path = "/";
                String cleanPath = path.startsWith("/") ? path.substring(1) : path;
                
                if (cleanPath.startsWith("api/")) return null;

                String targetPath = (cleanPath.isEmpty() || cleanPath.equalsIgnoreCase("index.html")) 
                                    ? "public/index.html" 
                                    : "public/" + cleanPath;
                
                WebResourceResponse response = assetsHandler.handle(targetPath);
                if (response == null && request.getMethod().equalsIgnoreCase("GET")) {
                    if (!cleanPath.contains(".") || cleanPath.contains("prayer") || cleanPath.contains("times") || cleanPath.contains("home") || cleanPath.contains("quran") || cleanPath.contains("hadith") || cleanPath.contains("azkar")) {
                        return assetsHandler.handle("public/index.html");
                    }
                }
                return response;
            }

            @Override public void onPageFinished(WebView view, String url) { 
                injectAtomicSystem();
            }
        });
        // ✅ استخدام البارامتر لضمان الحذف من الفرونت ايند
        webView.loadUrl("https://appassets.androidplatform.net/?isModal=true");
    }

    private double[] getBestLocation() {
        SharedPreferences prefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        String userLoc = prefs.getString("user_location", null);
        if (userLoc != null) {
            try {
                JSONObject obj = new JSONObject(userLoc);
                return new double[]{ obj.getDouble("lat"), obj.getDouble("lng") };
            } catch (Exception ignored) {}
        }

        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location loc = null;
                try { loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); } catch (SecurityException ignored) {} catch (Exception ignored) {}
                if (loc == null) {
                    try { loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); } catch (SecurityException ignored) {} catch (Exception ignored) {}
                }
                if (loc != null) {
                    double lat = loc.getLatitude();
                    double lng = loc.getLongitude();
                    try {
                        JSONObject newObj = new JSONObject();
                        newObj.put("lat", lat);
                        newObj.put("lng", lng);
                        prefs.edit().putString("user_location", newObj.toString()).apply();
                    } catch (Exception ignored) {}
                    return new double[]{ lat, lng };
                }
            }
        } catch (SecurityException ignored) {} catch (Exception ignored) {}

        return new double[]{ 30.0444, 31.2357 };
    }

    private void injectAtomicSystem() {
        if (webView == null) return;
        String hijri = HijriHelper.getTodayHijri(this);
        
        double[] coords = getBestLocation();
        double lat = coords[0];
        double lng = coords[1];
        String locJson = String.format(Locale.US, "{\"latitude\":%f, \"longitude\":%f, \"accuracy\":10}", lat, lng);

        String js = "(function() {" +
                    "  const loc = " + locJson + ";" +
                    "  const hijriText = '" + hijri + "';" +
                    "  " +
                    "  function updateSystem() {" +
                    "    // تحديث التاريخ الهجري\n" +
                    "    document.querySelectorAll('div, span, p, ion-label').forEach(el => {" +
                    "      if ((el.innerText||'').includes(' هـ') && el.innerText.length < 30) el.innerText = hijriText;" +
                    "    });" +
                    "    " +
                    "    // تحديث الموقع\n" +
                    "    localStorage.setItem('azkary_location', JSON.stringify(loc));" +
                    "    localStorage.setItem('last_known_location', JSON.stringify(loc));" +
                    "    if (navigator.geolocation) {" +
                    "      const p = { coords: loc, timestamp: Date.now() };" +
                    "      navigator.geolocation.getCurrentPosition = (s) => s(p);" +
                    "      navigator.geolocation.watchPosition = (s) => { s(p); return 1; };" +
                    "    }" +
                    "  }" +
                    "  " +
                    "  if (!window.STRAWBERRY_RUNNING) {" +
                    "    window.STRAWBERRY_RUNNING = true;" +
                    "    setInterval(updateSystem, 1000);" +
                    "    updateSystem();" +
                    "  }" +
                    "})();";
        
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    public void refreshWebView() { if (webView != null) webView.post(() -> webView.reload()); }

    public void closeOverlay() {
        if (adView != null) adView.destroy();
        if (overlayView != null) { try { windowManager.removeView(overlayView); } catch (Exception ignored) {} overlayView = null; webView = null; }
    }

    @Override public void onDestroy() { 
        closeOverlay(); instance = null;
        if (bubbleView != null) { try { windowManager.removeView(bubbleView); } catch (Exception ignored) {} }
        if (removeView != null) { try { windowManager.removeView(removeView); } catch (Exception ignored) {} }
        super.onDestroy(); 
    }

    private class BubbleBridge {
        @JavascriptInterface public String getLocation() {
            double[] coords = getBestLocation();
            return String.format(Locale.US, "{\"latitude\":%f, \"longitude\":%f, \"accuracy\":10}", coords[0], coords[1]);
        }

        @JavascriptInterface public String getHijriDate() {
            return HijriHelper.getTodayHijri(FloatingBubbleService.this);
        }
    }
}
