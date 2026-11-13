package com.worktable.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmScheduler {
    static final String PREF = "wt_alarms";

    static PendingIntent fire(Context c, int i, String msg) {
        Intent in = new Intent(c, AlarmReceiver.class);
        in.putExtra("msg", msg);
        in.putExtra("id", i);
        return PendingIntent.getBroadcast(c, i, in,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void schedule(Context c, String json) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        int old = sp.getInt("n", 0);
        for (int i = 0; i < old; i++) {
            am.cancel(fire(c, i, ""));
        }
        int n = 0;
        try {
            JSONArray arr = new JSONArray(json);
            long now = System.currentTimeMillis();
            Intent launch = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
            PendingIntent show = PendingIntent.getActivity(c, 0, launch,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            for (int k = 0; k < arr.length() && n < 100; k++) {
                JSONObject o = arr.getJSONObject(k);
                long t = o.getLong("t");
                if (t <= now) continue;
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show),
                        fire(c, n, o.optString("m", "근무 알람")));
                n++;
            }
        } catch (Exception e) {
            // ignore: keep whatever was scheduled so far
        }
        sp.edit().putString("json", json).putInt("n", n).apply();
    }
}
