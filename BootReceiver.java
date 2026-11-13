package com.worktable.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String json = c.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE).getString("json", "[]");
        AlarmScheduler.schedule(c, json);
    }
}
