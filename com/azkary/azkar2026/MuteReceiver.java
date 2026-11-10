package com.azkary.azkar2026;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MuteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MuteReceiver", "🔇 تم استلام طلب كتم الصوت من الإشعار");
        AdhanReceiver.stopAdhan(context);
    }
}
