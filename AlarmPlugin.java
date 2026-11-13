package com.worktable.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.AlarmClock;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

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
