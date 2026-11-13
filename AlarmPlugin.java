package com.worktable.app;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;

@CapacitorPlugin(name = "AlarmPlugin")
public class AlarmPlugin extends Plugin {

    @PluginMethod
    public void scheduleAll(PluginCall call) {
        JSArray arr = call.getArray("alarms");
        AlarmScheduler.schedule(getContext(), arr == null ? "[]" : arr.toString());
        call.resolve();
    }

    @PluginMethod
    public void requestNotif(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33
                && getContext().checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            getActivity().requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1001);
        }
        call.resolve();
    }

    @PluginMethod
    public void status(PluginCall call) {
        Context c = getContext();
        JSObject r = new JSObject();
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        r.put("notif", nm.areNotificationsEnabled());
        SharedPreferences sp = c.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE);
        r.put("n", sp.getInt("n", 0));
        long next = 0;
        try {
            JSONArray arr = new JSONArray(sp.getString("json", "[]"));
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.length(); i++) {
                long t = arr.getJSONObject(i).getLong("t");
                if (t > now && (next == 0 || t < next)) next = t;
            }
        } catch (Exception e) {
            // ignore
        }
        r.put("next", next);
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        r.put("battery", pm.isIgnoringBatteryOptimizations(c.getPackageName()));
        call.resolve(r);
    }

    @PluginMethod
    public void openNotifSettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } catch (Exception e) {
            // ignore
        }
        call.resolve();
    }

    @PluginMethod
    public void openBatterySettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } catch (Exception e) {
            // ignore
        }
        call.resolve();
    }

    @PluginMethod
    public void setAlarm(PluginCall call) {
        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
        i.putExtra(AlarmClock.EXTRA_HOUR, call.getInt("hour", 0));
        i.putExtra(AlarmClock.EXTRA_MINUTES, call.getInt("minute", 0));
        i.putExtra(AlarmClock.EXTRA_MESSAGE, call.getString("message", ""));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("clock app not found");
        }
    }
}
