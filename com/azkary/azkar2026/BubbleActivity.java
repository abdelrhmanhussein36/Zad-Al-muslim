package com.azkary.azkar2026;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;

import com.getcapacitor.BridgeActivity;

public class BubbleActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ ضبط أبعاد النافذة المنبثقة الشفافة (85% من الشاشة)
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.BOTTOM;
        getWindow().setAttributes(params);

        // ✅ جعل خلفية الـ Activity شفافة
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}