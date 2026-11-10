package com.azkary.azkar2026;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Arrays;

public class AdHelper {
    private static final String TAG = "AdHelper";
    
    private static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-4851467684137333/7453800751";
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4851467684137333/7781848672";
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-4851467684137333/3748516159";

    private static InterstitialAd mInterstitialAd;
    private static AppOpenAd mAppOpenAd;
    public static boolean isShowingAd = false;
    public static long lastAdDismissedTime = 0;

    public static boolean hasShownAppOpenThisUsage = false;
    public static boolean hasShownInterstitialAzkarThisUsage = false;
    public static boolean hasShownInterstitialMuezzinThisUsage = false;

    public static void initialize(Activity activity) {
        try {
            resetUsageFlags();
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(Arrays.asList("3F1923F44015DAE20B36F37C829CE02B"))
                    .build();
            MobileAds.setRequestConfiguration(configuration);

            MobileAds.initialize(activity, initializationStatus -> {
                Log.d(TAG, "MobileAds initialized successfully");
                loadAppOpenAd(activity);
                loadInterstitialAd(activity);
            });
        } catch (Exception e) {
            Log.e(TAG, "Initialize error: " + e.getMessage());
        }
    }

    public static void resetUsageFlags() {
        hasShownAppOpenThisUsage = false;
        hasShownInterstitialAzkarThisUsage = false;
        hasShownInterstitialMuezzinThisUsage = false;
    }

    public static void showBannerAd(Activity activity, android.webkit.WebView webView) {
        activity.runOnUiThread(() -> {
            try {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                
                for (int i = 0; i < rootView.getChildCount(); i++) {
                    if (rootView.getChildAt(i) instanceof AdView) {
                        rootView.removeViewAt(i);
                    }
                }

                AdView adView = new AdView(activity);
                adView.setAdSize(AdSize.BANNER);
                adView.setAdUnitId(BANNER_AD_UNIT_ID);
                adView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        Log.d(TAG, "✅ Banner Ad Loaded");
                        if (webView != null) {
                            float d = activity.getResources().getDisplayMetrics().density;
                            int h = (int) (50 * d);
                            ViewGroup.LayoutParams lp = webView.getLayoutParams();
                            if (lp instanceof ViewGroup.MarginLayoutParams) {
                                ((ViewGroup.MarginLayoutParams) lp).bottomMargin = h;
                                webView.setLayoutParams(lp);
                            }
                        }
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        Log.e(TAG, "❌ Banner Ad Failed: " + error.getMessage());
                    }
                });

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (50 * activity.getResources().getDisplayMetrics().density)
                );
                params.gravity = Gravity.BOTTOM;
                adView.setLayoutParams(params);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    adView.setElevation(100f);
                    adView.setTranslationZ(100f);
                }
                
                rootView.addView(adView);
                adView.bringToFront();
                rootView.requestLayout();

                adView.loadAd(new AdRequest.Builder().build());
            } catch (Exception e) { Log.e(TAG, e.getMessage()); }
        });
    }

    public static void bringBannerToFront(Activity activity) {
        activity.runOnUiThread(() -> {
            try {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                for (int i = 0; i < rootView.getChildCount(); i++) {
                    if (rootView.getChildAt(i) instanceof AdView) {
                        rootView.getChildAt(i).bringToFront();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            rootView.getChildAt(i).setTranslationZ(9999f);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public static void loadInterstitialAd(Context context) {
        try {
            InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(TAG, "✅ Interstitial Ad Loaded");
                        mInterstitialAd = interstitialAd;
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "❌ Interstitial Ad Failed: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "loadInterstitialAd error: " + e.getMessage());
        }
    }

    public static void showInterstitialAd(Activity activity, String endMessage, boolean isMuezzinTest) {
        if (isMuezzinTest && hasShownInterstitialMuezzinThisUsage) return;
        if (!isMuezzinTest && hasShownInterstitialAzkarThisUsage) return;

        activity.runOnUiThread(() -> {
            if (mInterstitialAd != null) {
                if (endMessage != null && !endMessage.isEmpty()) {
                    new android.app.AlertDialog.Builder(activity)
                        .setTitle("تقبل الله")
                        .setMessage(endMessage)
                        .setPositiveButton("حسنًا", (dialog, which) -> triggerInterstitial(activity, isMuezzinTest))
                        .setCancelable(false).show();
                } else {
                    triggerInterstitial(activity, isMuezzinTest);
                }
            }
        });
    }

    private static void triggerInterstitial(Activity activity, boolean isMuezzinTest) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    isShowingAd = false;
                    lastAdDismissedTime = System.currentTimeMillis();
                    if (isMuezzinTest) hasShownInterstitialMuezzinThisUsage = true;
                    else hasShownInterstitialAzkarThisUsage = true;
                    loadInterstitialAd(activity);
                }
                @Override
                public void onAdShowedFullScreenContent() { isShowingAd = true; }
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    mInterstitialAd = null;
                    isShowingAd = false;
                    lastAdDismissedTime = System.currentTimeMillis();
                    loadInterstitialAd(activity);
                }
            });
            mInterstitialAd.show(activity);
        }
    }

    public static void loadAppOpenAd(Context context) {
        try {
            AppOpenAd.load(context, APP_OPEN_AD_UNIT_ID, new AdRequest.Builder().build(), 
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    Log.d(TAG, "✅ AppOpenAd Loaded");
                    mAppOpenAd = ad;
                    if (context instanceof Activity) {
                        Activity activity = (Activity) context;
                        if (!activity.isFinishing() && !activity.isDestroyed() && !hasShownAppOpenThisUsage) {
                            showAppOpenAd(activity);
                        }
                    }
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e(TAG, "❌ AppOpenAd Failed: " + loadAdError.getMessage());
                    mAppOpenAd = null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadAppOpenAd error: " + e.getMessage());
        }
    }

    public static void showAppReturnAd(Activity activity) {
        if (isShowingAd || (System.currentTimeMillis() - lastAdDismissedTime < 3000)) return;

        SharedPreferences prefs = activity.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        int usageCount = prefs.getInt("usage_count", 0);
        if (usageCount < 2) return;

        activity.runOnUiThread(() -> {
            if (mAppOpenAd != null) {
                mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mAppOpenAd = null;
                        isShowingAd = false;
                        lastAdDismissedTime = System.currentTimeMillis();
                        loadAppOpenAd(activity);
                    }
                    @Override
                    public void onAdShowedFullScreenContent() { isShowingAd = true; }
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        mAppOpenAd = null;
                        isShowingAd = false;
                        lastAdDismissedTime = System.currentTimeMillis();
                        loadAppOpenAd(activity);
                    }
                });
                mAppOpenAd.show(activity);
            } else if (mInterstitialAd != null) {
                triggerInterstitial(activity, false);
            } else {
                loadAppOpenAd(activity);
            }
        });
    }

    public static void showAppOpenAd(Activity activity) {
        if (hasShownAppOpenThisUsage || isShowingAd) return;

        SharedPreferences prefs = activity.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        int usageCount = prefs.getInt("usage_count", 0);
        if (usageCount < 2) {
            Log.d(TAG, "Skipping AppOpenAd for first usage count: " + usageCount);
            return;
        }

        activity.runOnUiThread(() -> {
            if (activity == null || activity.isFinishing() || activity.isDestroyed() || hasShownAppOpenThisUsage) return;

            if (mAppOpenAd != null) {
                hasShownAppOpenThisUsage = true;
                mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mAppOpenAd = null;
                        isShowingAd = false;
                        lastAdDismissedTime = System.currentTimeMillis();
                        loadAppOpenAd(activity);
                    }
                    @Override
                    public void onAdShowedFullScreenContent() {
                        isShowingAd = true;
                        hasShownAppOpenThisUsage = true;
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        mAppOpenAd = null;
                        isShowingAd = false;
                        hasShownAppOpenThisUsage = false;
                        lastAdDismissedTime = System.currentTimeMillis();
                        loadAppOpenAd(activity);
                    }
                });
                mAppOpenAd.show(activity);
            } else if (mInterstitialAd != null) {
                hasShownAppOpenThisUsage = true;
                triggerInterstitial(activity, false);
            } else {
                loadAppOpenAd(activity);
                loadInterstitialAd(activity);

                final long startTime = System.currentTimeMillis();
                final Handler handler = new Handler(Looper.getMainLooper());
                final Runnable checkRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (activity == null || activity.isFinishing() || activity.isDestroyed() || hasShownAppOpenThisUsage) return;

                        if (mAppOpenAd != null || mInterstitialAd != null) {
                            showAppOpenAd(activity);
                        } else if (System.currentTimeMillis() - startTime < 5000) {
                            handler.postDelayed(this, 300);
                        }
                    }
                };
                handler.postDelayed(checkRunnable, 300);
            }
        });
    }
}
