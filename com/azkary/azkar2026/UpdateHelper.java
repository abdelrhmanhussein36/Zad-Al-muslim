package com.azkary.azkar2026;

import android.app.Activity;
import android.content.IntentSender;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

public class UpdateHelper {

    private static final String TAG = "UpdateHelper";
    public static final int UPDATE_REQUEST_CODE = 501;

    public static void checkForAppUpdate(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        try {
            AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.FLEXIBLE,
                                activity,
                                UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "Error starting update flow: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Check update error: " + e.getMessage());
        }
    }
}
