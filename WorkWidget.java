package com.worktable.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkWidget extends AppWidgetProvider {
    static final int[] ROWS = {R.id.r0, R.id.r1, R.id.r2, R.id.r3, R.id.r4, R.id.r5, R.id.r6};

    @Override
    public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) render(c, mgr, id);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            refreshAll(c);
        }
    }

    static void refreshAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(c, WorkWidget.class));
        for (int id : ids) render(c, mgr, id);
    }

    static void span(SpannableStringBuilder sb, int from, int color, boolean bold) {
        sb.setSpan(new ForegroundColorSpan(color), from, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (bold) sb.setSpan(new StyleSpan(Typeface.BOLD), from, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static void render(Context c, AppWidgetManager mgr, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_work);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String json = c.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE).getString("widget", "[]");
        int start = -1;
        JSONArray arr = new JSONArray();
        try {
            arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                if (today.equals(arr.getJSONObject(i).optString("d"))) {
                    start = i;
                    break;
                }
            }
        } catch (Exception e) {
            start = -1;
        }
        for (int r : ROWS) v.setTextViewText(r, "");
        if (start < 0) {
            v.setTextViewText(R.id.title, "근무표 앱을 한 번 열어 주세요");
        } else {
            v.setTextViewText(R.id.title, "근무표 · " + Integer.parseInt(today.substring(5, 7)) + "월 "
                    + Integer.parseInt(today.substring(8, 10)) + "일");
            for (int i = 0; i < ROWS.length && start + i < arr.length(); i++) {
                try {
                    JSONObject o = arr.getJSONObject(start + i);
                    String d = o.optString("d");
                    String n = o.optString("n");
                    String t = o.optString("t");
                    SpannableStringBuilder sb = new SpannableStringBuilder();
                    int s0 = sb.length();
                    sb.append(i == 0 ? "오늘 " : "").append(String.valueOf(Integer.parseInt(d.substring(8, 10))))
                            .append("일(").append(o.optString("w")).append(") ");
                    span(sb, s0, o.optInt("h") == 1 ? Color.parseColor("#D64545") : Color.parseColor("#18212B"), i == 0);
                    if (!n.isEmpty()) {
                        int s1 = sb.length();
                        sb.append(n);
                        int col = Color.parseColor("#18212B");
                        try {
                            col = Color.parseColor(o.optString("c"));
                        } catch (Exception e) {
                            // keep default
                        }
                        span(sb, s1, col, true);
                    } else {
                        int s1 = sb.length();
                        sb.append("-");
                        span(sb, s1, Color.parseColor("#6B7684"), false);
                    }
                    if (!t.isEmpty()) {
                        int s2 = sb.length();
                        sb.append("  ⏰ ").append(t);
                        span(sb, s2, Color.parseColor("#6B7684"), false);
                    }
                    v.setTextViewText(ROWS[i], sb);
                } catch (Exception e) {
                    // skip row
                }
            }
        }
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open != null) {
            v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }
        mgr.updateAppWidget(id, v);
    }
}
