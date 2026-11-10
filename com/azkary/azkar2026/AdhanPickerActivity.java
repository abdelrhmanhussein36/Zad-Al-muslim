package com.azkary.azkar2026;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AdhanPickerActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 1234;
    private String targetPrayer = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        targetPrayer = getIntent().getStringExtra("prayer");
        
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/mpeg");
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                saveAudio(uri);
            }
        }
        finish();
    }

    private void saveAudio(Uri uri) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "custom_" + System.currentTimeMillis() + ".mp3";
            }
            if (!fileName.toLowerCase().endsWith(".mp3")) {
                fileName = fileName + ".mp3";
            }

            String savedFileName = "custom_" + System.currentTimeMillis() + "_" + fileName;
            File destFile = new File(getFilesDir(), savedFileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            SharedPreferences prefs = getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            String displayName = fileName.replace(".mp3", "").replace("_", " ");
            prefs.edit().putString("custom_name_" + savedFileName, "📂 " + displayName).apply();
            
            // ربط الصوت بالصلاة المختارة مباشرة
            if (targetPrayer != null && !targetPrayer.isEmpty()) {
                prefs.edit().putString("selected_muezzin_" + targetPrayer, savedFileName).apply();
            }

            Toast.makeText(this, "✅ تم إضافة الصوت بنجاح", Toast.LENGTH_LONG).show();
            
            // إرسال رسالة للخدمة لتحديث الـ WebView إذا كانت مفتوحة
            if (FloatingBubbleService.instance != null) {
                FloatingBubbleService.instance.refreshWebView();
            }

        } catch (Exception e) {
            Log.e("AdhanPicker", "Error: " + e.getMessage());
            Toast.makeText(this, "❌ خطأ في حفظ الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }
}
