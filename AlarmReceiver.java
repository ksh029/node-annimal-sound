package com.worktable.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String msg = intent.getStringExtra("msg");
        if (msg == null || msg.isEmpty()) msg = "근무 알람";
        int id = intent.getIntExtra("id", 0);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        String sv = c.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE).getString("sound", "");
        Uri snd = AlarmScheduler.soundUri(c);
        String chId = "wt_s_" + (sv == null || sv.isEmpty() ? "def" : Integer.toHexString(sv.hashCode()));
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(chId, "근무 알람", NotificationManager.IMPORTANCE_HIGH);
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            ch.setSound(snd, aa);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 600, 400, 600});
            nm.createNotificationChannel(ch);
        }
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        PendingIntent content = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, chId)
                : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("근무 알람")
                .setContentText(msg)
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(content)
                .setFullScreenIntent(content, true);
        if (Build.VERSION.SDK_INT >= 26) {
            b.setTimeoutAfter(120000);
        } else {
            b.setSound(snd);
            b.setPriority(Notification.PRIORITY_MAX);
        }
        Notification n = b.build();
        n.flags |= Notification.FLAG_INSISTENT;
        nm.notify(1000 + id, n);
    }
}
