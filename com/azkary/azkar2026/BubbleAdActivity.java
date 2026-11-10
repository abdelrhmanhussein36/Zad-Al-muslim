package com.azkary.azkar2026;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class BubbleAdActivity extends AppCompatActivity {

    private static final String TAG = "BubbleAdActivity";
    public static InterstitialAd interstitialAd = null;
    private static boolean isAdLoading = false;

    public static void loadAd(Context context) {
        if (isAdLoading) return;
        isAdLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        isAdLoading = false;
                        Log.d(TAG, "✅ Ad loaded successfully");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        isAdLoading = false;
                        Log.e(TAG, "❌ Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ تطبيق Theme قبل super.onCreate
        setTheme(R.style.Theme_AppCompat_Translucent);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "🔄 BubbleAdActivity onCreate");

        // ✅ إذا لم يكن هناك إعلان، افتح النافذة فوراً
        if (interstitialAd == null) {
            Log.d(TAG, "⚠️ No ad available, showing window directly");
            showFloatingWindowAndFinish();
            return;
        }

        // ✅ عرض الإعلان
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "✅ Ad dismissed");
                interstitialAd = null;
                // تحميل الإعلان التالي مسبقاً
                loadAd(getApplicationContext());
                showFloatingWindowAndFinish();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "❌ Ad failed to show: " + adError.getMessage());
                interstitialAd = null;
                showFloatingWindowAndFinish();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "✅ Ad showed");
            }
        });

        interstitialAd.show(this);
    }

    private void showFloatingWindowAndFinish() {
        Log.d(TAG, "🔄 Showing floating window and finishing");

        // ✅ محاولة إظهار النافذة عبر الخدمة
        if (FloatingBubbleService.instance != null) {
            FloatingBubbleService.instance.showOverlayAfterAd();
        } else {
            // ✅ بدء الخدمة إذا لم تكن موجودة
            Intent intent = new Intent(this, FloatingBubbleService.class);
            intent.setAction("ACTION_SHOW_WINDOW");
            startService(intent);
        }

        // ✅ إنهاء النشاط بعد تأخير بسيط
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔄 BubbleAdActivity destroyed");
    }
}