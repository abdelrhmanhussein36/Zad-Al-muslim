package com.azkary.azkar2026;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.gms.tasks.Task;

public class ReviewHelper {

    private static final String PREFS_NAME = "azkary_review_prefs";
    private static final String INSTALL_DATE_KEY = "install_date";
    private static final String REVIEW_SHOWN_KEY = "review_shown";

    public static void checkAndShowReview(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // حفظ تاريخ التثبيت إذا كانت أول مرة
        if (!prefs.contains(INSTALL_DATE_KEY)) {
            prefs.edit().putLong(INSTALL_DATE_KEY, System.currentTimeMillis()).apply();
            return;
        }

        // إذا تم إظهار التقييم مسبقاً لا تظهره مرة أخرى
        if (prefs.getBoolean(REVIEW_SHOWN_KEY, false)) {
            return;
        }

        long installDate = prefs.getLong(INSTALL_DATE_KEY, 0);
        long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;

        if (System.currentTimeMillis() >= (installDate + threeDaysInMillis)) {
            showReviewDialog(activity, prefs);
        }
    }

    private static void showReviewDialog(Activity activity, SharedPreferences prefs) {
        ReviewManager manager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(t -> {
                    // تم الانتهاء من العملية (سواء قيم المستخدم أو أغلق النافذة)
                    prefs.edit().putBoolean(REVIEW_SHOWN_KEY, true).apply();
                });
            }
        });
    }
}
