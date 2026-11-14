package com.worktable.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import androidx.activity.result.ActivityResult;
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
    public void pickSound(PluginCall call) {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알람음 선택");
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, AlarmScheduler.soundUri(getContext()));
        startActivityForResult(call, i, "soundResult");
    }

    @ActivityCallback
    private void soundResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri u = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            getContext().getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE)
                    .edit().putString("sound", u == null ? "" : u.toString()).apply();
        }
        call.resolve();
    }

    @PluginMethod
    public void getSound(PluginCall call) {
        JSObject r = new JSObject();
        r.put("title", AlarmScheduler.soundTitle(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void previewSound(PluginCall call) {
        try {
            final Ringtone r = RingtoneManager.getRingtone(getContext(), AlarmScheduler.soundUri(getContext()));
            if (Build.VERSION.SDK_INT >= 28) {
                r.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }
            r.play();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    r.stop();
                }
            }, 4000);
        } catch (Exception e) {
            // ignore
        }
        call.resolve();
    }

    @PluginMethod
    public void setWidgetData(PluginCall call) {
        JSArray arr = call.getArray("rows");
        getContext().getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE)
                .edit().putString("widget", arr == null ? "[]" : arr.toString()).apply();
        WorkWidget.refreshAll(getContext());
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
