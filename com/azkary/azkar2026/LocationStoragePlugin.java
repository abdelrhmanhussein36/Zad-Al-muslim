package com.azkary.azkar2026;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@CapacitorPlugin(name = "LocationStorage")
public class LocationStoragePlugin extends Plugin {

    @PluginMethod
    public void save(PluginCall call) {
        saveLocation(call);
    }

    @PluginMethod
    public void saveLocation(PluginCall call) {
        Double lat = call.getDouble("lat");
        if (lat == null) {
            String latStr = call.getString("lat");
            if (latStr != null) try { lat = Double.parseDouble(latStr); } catch (Exception ignored) {}
        }
        if (lat == null) lat = call.getDouble("latitude");

        Double lng = call.getDouble("lng");
        if (lng == null) {
            String lngStr = call.getString("lng");
            if (lngStr != null) try { lng = Double.parseDouble(lngStr); } catch (Exception ignored) {}
        }
        if (lng == null) lng = call.getDouble("longitude");

        if (lat == null || lng == null) {
            call.reject("Must provide lat and lng");
            return;
        }

        final double finalLat = lat;
        final double finalLng = lng;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
                JSONObject locationObj = new JSONObject();
                locationObj.put("lat", finalLat);
                locationObj.put("lng", finalLng);

                String city = resolveCityName(getContext(), finalLat, finalLng);
                if (city != null) locationObj.put("city", city);

                prefs.edit().putString("user_location", locationObj.toString()).apply();

                new AlarmHelper(getContext()).scheduleAllPrayers();

                JSObject ret = new JSObject();
                ret.put("status", "saved");
                ret.put("city", city);
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("Error saving location: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void getLocation(PluginCall call) {
        try {
            SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
            String userLoc = prefs.getString("user_location", null);
            double lat = 30.0444;
            double lng = 31.2357;
            String city = "القاهرة (افتراضي)";

            if (userLoc != null) {
                JSONObject obj = new JSONObject(userLoc);
                lat = obj.getDouble("lat");
                lng = obj.getDouble("lng");
                if (obj.has("city")) city = obj.getString("city");

                JSObject ret = new JSObject();
                ret.put("lat", lat);
                ret.put("lng", lng);
                ret.put("latitude", lat);
                ret.put("longitude", lng);
                ret.put("city", city);
                call.resolve(ret);
            } else {
                requestLiveLocationInternal(call);
            }
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void requestLiveLocation(PluginCall call) {
        requestLiveLocationInternal(call);
    }

    private void requestLiveLocationInternal(PluginCall call) {
        try {
            LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                call.reject("LocationManager is null");
                return;
            }

            // 1. تجربة جلب آخر موقع مسجل لتوفير استجابة فورية
            Location bestLastLocation = null;
            try {
                Location netLoc = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) : null;
                Location gpsLoc = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) : null;
                Location passiveLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                bestLastLocation = getBetterLocation(gpsLoc, netLoc);
                bestLastLocation = getBetterLocation(bestLastLocation, passiveLoc);
            } catch (SecurityException ignored) {}

            final AtomicBoolean isResolved = new AtomicBoolean(false);
            final Handler handler = new Handler(Looper.getMainLooper());
            final Location finalBestLast = bestLastLocation;

            final LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location loc) {
                    if (loc != null && !isResolved.getAndSet(true)) {
                        try {
                            lm.removeUpdates(this);
                        } catch (SecurityException ignored) {}
                        processAndResolveLocation(call, loc);
                    }
                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };

            // مهلة أقصاها 2.5 ثانية فقط لتفادي أي تأخير في أي موقع
            final Runnable timeoutRunnable = () -> {
                if (!isResolved.getAndSet(true)) {
                    try {
                        lm.removeUpdates(listener);
                    } catch (SecurityException ignored) {}

                    if (finalBestLast != null) {
                        processAndResolveLocation(call, finalBestLast);
                    } else {
                        SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
                        String userLoc = prefs.getString("user_location", null);
                        if (userLoc != null) {
                            try {
                                JSONObject obj = new JSONObject(userLoc);
                                double lat = obj.getDouble("lat");
                                double lng = obj.getDouble("lng");
                                String city = obj.optString("city", "موقعك الحالي");

                                JSObject ret = new JSObject();
                                ret.put("lat", lat);
                                ret.put("lng", lng);
                                ret.put("latitude", lat);
                                ret.put("longitude", lng);
                                ret.put("city", city);
                                call.resolve(ret);
                                return;
                            } catch (Exception ignored) {}
                        }
                        call.reject("Location timeout");
                    }
                }
            };

            handler.postDelayed(timeoutRunnable, 2500);

            boolean listening = false;
            try {
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener, Looper.getMainLooper());
                    listening = true;
                }
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener, Looper.getMainLooper());
                    listening = true;
                }
            } catch (SecurityException e) {
                if (!isResolved.getAndSet(true)) {
                    handler.removeCallbacks(timeoutRunnable);
                    call.reject("Location permission not granted");
                }
                return;
            }

            if (!listening && bestLastLocation != null && !isResolved.getAndSet(true)) {
                handler.removeCallbacks(timeoutRunnable);
                processAndResolveLocation(call, bestLastLocation);
            }
        } catch (Exception e) {
            call.reject("Error: " + e.getMessage());
        }
    }

    private Location getBetterLocation(Location loc1, Location loc2) {
        if (loc1 == null) return loc2;
        if (loc2 == null) return loc1;
        return (loc1.getTime() > loc2.getTime()) ? loc1 : loc2;
    }

    private void processAndResolveLocation(PluginCall call, Location loc) {
        final double lat = loc.getLatitude();
        final double lng = loc.getLongitude();

        Executors.newSingleThreadExecutor().execute(() -> {
            String city = resolveCityName(getContext(), lat, lng);

            try {
                SharedPreferences prefs = getContext().getSharedPreferences("azkary_prefs", Context.MODE_PRIVATE);
                JSONObject locationObj = new JSONObject();
                locationObj.put("lat", lat);
                locationObj.put("lng", lng);
                if (city != null) locationObj.put("city", city);

                prefs.edit().putString("user_location", locationObj.toString()).apply();
                new AlarmHelper(getContext()).scheduleAllPrayers();
            } catch (Exception ignored) {}

            JSObject ret = new JSObject();
            ret.put("lat", lat);
            ret.put("lng", lng);
            ret.put("latitude", lat);
            ret.put("longitude", lng);
            ret.put("city", city != null ? city : "موقعك الحالي");
            call.resolve(ret);
        });
    }

    private String resolveCityName(Context context, double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, new Locale("ar"));
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                if (city == null || city.isEmpty()) city = address.getSubAdminArea();
                if (city == null || city.isEmpty()) city = address.getAdminArea();
                return city;
            }
        } catch (Exception ignored) {}
        return null;
    }

    @PluginMethod
    public void getHijriDate(PluginCall call) {
        try {
            String hijri = HijriHelper.getTodayHijri(getContext());
            JSObject ret = new JSObject();
            ret.put("hijri", hijri);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }
}
