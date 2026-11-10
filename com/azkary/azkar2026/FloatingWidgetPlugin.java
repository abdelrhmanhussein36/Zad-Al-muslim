package com.azkary.azkar2026;

import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "FloatingWidget")
public class FloatingWidgetPlugin extends Plugin {

    @PluginMethod
    public void requestPinWidget(PluginCall call) {
        Context context = getContext();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppWidgetManager manager = context.getSystemService(AppWidgetManager.class);
                ComponentName cn = new ComponentName(context, PrayerWidget.class);
                if (manager != null && manager.isRequestPinAppWidgetSupported()) {
                    manager.requestPinAppWidget(cn, null, null);
                }
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void checkOverlayPermission(PluginCall call) {
        Context context = getContext();
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            granted = Settings.canDrawOverlays(context);
        }
        JSObject ret = new JSObject();
        ret.put("granted", granted);
        call.resolve(ret);
    }

    @PluginMethod
    public void requestOverlayPermission(PluginCall call) {
        Context context = getContext();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                    Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void startFloatingBubble(PluginCall call) {
        Context context = getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            call.reject("OVERLAY_PERMISSION_REQUIRED");
            return;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("user_disabled_bubble", false).apply();

            Intent bblIntent = new Intent(context, FloatingBubbleService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(bblIntent);
            } else {
                context.startService(bblIntent);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void stopFloatingBubble(PluginCall call) {
        Context context = getContext();
        try {
            SharedPreferences prefs = context.getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("user_disabled_bubble", true).apply();

            Intent bblIntent = new Intent(context, FloatingBubbleService.class);
            context.stopService(bblIntent);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void toggleWidget(PluginCall call) {
        Context context = getContext();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            call.reject("يرجى منح إذن الظهور فوق التطبيقات أولاً");
            return;
        }

        try {
            Intent intent = new Intent(context, FloatingWidgetService.class);
            if (isServiceRunning(FloatingWidgetService.class)) {
                context.stopService(intent);
                call.resolve();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
                call.resolve();
            }
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (serviceClass.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @PluginMethod
    public void setTextColor(PluginCall call) {
        String color = call.getString("color");
        if (color != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            prefs.edit().putString("floating_text_color", color).apply();
        }
        call.resolve();
    }

    @PluginMethod
    public void setWakeUpQuizEnabled(PluginCall call) {
        Boolean enabled = call.getBoolean("enabled", false);
        SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("wake_up_quiz_enabled", enabled != null && enabled).apply();
        call.resolve();
    }

    @PluginMethod
    public void getWakeUpQuizEnabled(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("wake_up_quiz_enabled", false);
        JSObject ret = new JSObject();
        ret.put("enabled", enabled);
        call.resolve(ret);
    }

    @PluginMethod
    public void openWakeUpQuizTest(PluginCall call) {
        Context context = getContext();
        try {
            Intent intent = new Intent(context, WakeUpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }
}
