package com.azkary.azkar2026;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AppState")
public class AppStatePlugin extends Plugin {

    @PluginMethod
    public void getStatus(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("isAdhanPlaying", AdhanReceiver.mediaPlayer != null && AdhanReceiver.mediaPlayer.isPlaying());
        call.resolve(ret);
    }
}