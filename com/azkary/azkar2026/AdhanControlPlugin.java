package com.azkary.azkar2026;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@CapacitorPlugin(name = "AdhanControl")
public class AdhanControlPlugin extends Plugin {

    private MediaPlayer testMediaPlayer = null;

    @PluginMethod
    public void playAdhan(PluginCall call) {
        String audioFile = call.getString("audioFile");
        String prayer = call.getString("prayer", "");

        if (audioFile == null || audioFile.isEmpty()) {
            call.reject("اسم الملف الصوتي مفقود");
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                if (testMediaPlayer != null) {
                    try { testMediaPlayer.stop(); } catch (Exception ignored) {}
                    try { testMediaPlayer.release(); } catch (Exception ignored) {}
                    testMediaPlayer = null;
                }

                if (audioFile.startsWith("custom_")) {
                    File file = new File(getContext().getFilesDir(), audioFile);
                    if (!file.exists()) {
                        Toast.makeText(getContext(), "❌ الملف الصوتي غير موجود", Toast.LENGTH_LONG).show();
                        call.reject("Custom file not found");
                        return;
                    }
                    testMediaPlayer = new MediaPlayer();
                    testMediaPlayer.setDataSource(file.getAbsolutePath());
                    testMediaPlayer.prepare();
                } else {
                    final String resName = audioFile.replace(".mp3", "").trim();
                    int resId = getContext().getResources().getIdentifier(resName, "raw", getContext().getPackageName());

                    if (resId == 0) {
                        Toast.makeText(getContext(), "❌ الملف الصوتي غير موجود: " + resName, Toast.LENGTH_LONG).show();
                        call.reject("Resource not found: " + resName);
                        return;
                    }
                    testMediaPlayer = MediaPlayer.create(getContext(), resId);
                }

                if (testMediaPlayer != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        AudioAttributes aa = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build();
                        testMediaPlayer.setAudioAttributes(aa);
                    } else {
                        testMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    }
                    
                    testMediaPlayer.start();
                    Toast.makeText(getContext(), "🔊 جاري تجربة الأذان...", Toast.LENGTH_SHORT).show();
                    testMediaPlayer.setOnCompletionListener(mp -> {
                        mp.release();
                        testMediaPlayer = null;
                    });
                    call.resolve();
                } else {
                    call.reject("MediaPlayer.create returned null");
                }
            } catch (Exception e) {
                Log.e("AdhanControl", "Error playing adhan: " + e.getMessage());
                call.reject(e.getMessage());
            }
        });
    }

    @PluginMethod
    public void setMuezzin(PluginCall call) {
        String audioFile = call.getString("audioFile");
        String prayer = call.getString("prayer", "");

        if (audioFile == null || audioFile.isEmpty()) {
            call.reject("اسم الملف الصوتي مفقود");
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (prayer != null && !prayer.isEmpty()) {
            editor.putString("selected_muezzin_" + prayer, audioFile);
        } else {
            editor.putString("selected_muezzin_الظهر", audioFile);
        }
        
        editor.apply();
        
        Log.d("AdhanControl", "✅ تم حفظ صوت مؤذن لصلاة " + prayer + ": " + audioFile);
        call.resolve();
    }

    @PluginMethod
    public void getMuezzin(PluginCall call) {
        String prayer = call.getString("prayer", "");
        SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        JSObject ret = new JSObject();
        JSObject muezzins = new JSObject();

        if ("الفجر".equals(prayer)) {
            String currentAudio = prefs.getString("selected_muezzin_" + prayer, "fajr_adhan.mp3");
            
            muezzins.put("fajr_adhan.mp3", "الشيخ احمد جلال");
            muezzins.put("fajr_adhan_abdulbasit.mp3", "الشيخ عبدالباسط عبدالصمد");
            muezzins.put("fajr_adhan_makkah.mp3", "اذان مكة المكرمة");
            muezzins.put("fajr_adhan_refat2.mp3", "الشيخ نصر الدين طوبار");
            
            ret.put("audioFile", currentAudio);
            ret.put("muezzinList", muezzins);
        } else {
            String currentAudio = prefs.getString("selected_muezzin_" + prayer, "adhan_refat1.mp3");
            
            if (currentAudio != null && currentAudio.startsWith("fajr_")) {
                currentAudio = "adhan_refat1.mp3";
            }
            
            muezzins.put("adhan.mp3", "الشيخ احمد جلال");
            muezzins.put("adhan_refat1.mp3", "الشيخ محمد رفعت");
            muezzins.put("adhan_abdulbasit.mp3", "الشيخ عبدالباسط عبدالصمد");
            muezzins.put("adhan_makkah.mp3", "اذان مكة المكرمة");
            muezzins.put("adhan_naqshbandi.mp3", "الشيخ سيد النقشبندي");
            
            ret.put("audioFile", currentAudio);
            ret.put("muezzinList", muezzins);
        }

        String customAudio = prefs.getString("selected_muezzin_" + prayer, null);
        if (customAudio != null && customAudio.startsWith("custom_")) {
            String displayName = prefs.getString("custom_name_" + customAudio, "صوت خارجي");
            muezzins.put(customAudio, "📂 " + displayName);
        }

        muezzins.put("PICK_FROM_DEVICE", "➕ اختيار صوت من الموبايل (.mp3)");

        call.resolve(ret);
    }

    @PluginMethod
    public void pickCustomAudio(PluginCall call) {
        saveCall(call);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/mpeg");
        startActivityForResult(call, intent, "pickAudioResult");
    }

    @ActivityCallback
    private void pickAudioResult(PluginCall call, ActivityResult result) {
        if (call == null) return;

        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                try {
                    String mimeType = getContext().getContentResolver().getType(uri);
                    if (mimeType == null || !mimeType.startsWith("audio/")) {
                        call.reject("الرجاء اختيار ملف صوتي (.mp3)");
                        return;
                    }

                    String fileName = getFileName(uri);
                    if (fileName == null || fileName.isEmpty()) {
                        fileName = "custom_" + System.currentTimeMillis() + ".mp3";
                    }
                    
                    if (!fileName.toLowerCase().endsWith(".mp3")) {
                        fileName = fileName + ".mp3";
                    }

                    String savedFileName = "custom_" + System.currentTimeMillis() + "_" + fileName;
                    File destFile = new File(getContext().getFilesDir(), savedFileName);
                    
                    InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                    FileOutputStream outputStream = new FileOutputStream(destFile);
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    outputStream.close();
                    inputStream.close();

                    SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
                    String displayName = fileName.replace(".mp3", "").replace("_", " ");
                    prefs.edit().putString("custom_name_" + savedFileName, "📂 " + displayName).apply();

                    JSObject ret = new JSObject();
                    ret.put("audioFile", savedFileName);
                    ret.put("displayName", "📂 " + displayName);
                    call.resolve(ret);
                    
                    Toast.makeText(getContext(), "✅ تم حفظ الملف: " + displayName, Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    Log.e("AdhanControl", "Error saving audio: " + e.getMessage());
                    call.reject("خطأ في حفظ الملف: " + e.getMessage());
                }
            } else {
                call.reject("لم يتم اختيار ملف");
            }
        } else {
            call.reject("تم إلغاء العملية");
        }
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("AdhanControl", "Error getting file name: " + e.getMessage());
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            if (fileName != null) {
                int cut = fileName.lastIndexOf('/');
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1);
                }
            }
        }
        return fileName;
    }

    @PluginMethod
    public void stopAdhan(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (testMediaPlayer != null) {
                try { testMediaPlayer.stop(); } catch (Exception ignored) {}
                try { testMediaPlayer.release(); } catch (Exception ignored) {}
                testMediaPlayer = null;
                Toast.makeText(getContext(), "تم إيقاف التجربة", Toast.LENGTH_SHORT).show();
            }
        });
        call.resolve();
    }

    @PluginMethod
    public void scheduleAll(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("silent_adhan_enabled", false).apply();
        new AlarmHelper(getContext()).scheduleAllPrayers();
        call.resolve();
    }

    @PluginMethod
    public void cancelAll(PluginCall call) {
        // بدلاً من إلغاء المواعيد، نجعلها صامتة (إشعار فقط) بناءً على طلب المستخدم
        SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("silent_adhan_enabled", true).apply();
        // نبقي المواعيد مجدولة لتظهر كإشعارات
        new AlarmHelper(getContext()).scheduleAllPrayers();
        call.resolve();
    }
}