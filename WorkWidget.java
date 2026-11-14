package com.worktable.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class WorkWidget extends AppWidgetProvider {
    static final String A_PREV = "com.worktable.app.WIDGET_PREV";
    static final String A_NEXT = "com.worktable.app.WIDGET_NEXT";
    static final String A_RESET = "com.worktable.app.WIDGET_RESET";
    static final int RED = 0xFFD64545;
    static final int BLUE = 0xFF2F6FD6;
    static final int DARK = 0xFF18212B;
    static final int GRAY = 0xFF6B7684;

    @Override
    public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) render(c, mgr, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager mgr, int id, Bundle newOptions) {
        render(c, mgr, id);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        SharedPreferences sp = c.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE);
        if (A_PREV.equals(a)) {
            sp.edit().putInt("woffset", sp.getInt("woffset", 0) - 1).apply();
            refreshAll(c);
            return;
        }
        if (A_NEXT.equals(a)) {
            sp.edit().putInt("woffset", sp.getInt("woffset", 0) + 1).apply();
            refreshAll(c);
            return;
        }
        if (A_RESET.equals(a)) {
            sp.edit().putInt("woffset", 0).apply();
            refreshAll(c);
            return;
        }
        super.onReceive(c, i);
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

    static PendingIntent bc(Context c, String action, int req) {
        Intent i = new Intent(c, WorkWidget.class);
        i.setAction(action);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static String key(Calendar d) {
        return String.format(Locale.US, "%04d-%02d-%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH));
    }

    static int withAlpha(int color, int a) {
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    static void render(Context c, AppWidgetManager mgr, int id) {
        SharedPreferences sp = c.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE);
        int off = sp.getInt("woffset", 0);
        Calendar now = Calendar.getInstance();
        Calendar cur = Calendar.getInstance();
        cur.set(Calendar.DAY_OF_MONTH, 1);
        cur.add(Calendar.MONTH, off);
        int year = cur.get(Calendar.YEAR);
        int month = cur.get(Calendar.MONTH);

        Bundle o = mgr.getAppWidgetOptions(id);
        float dens = c.getResources().getDisplayMetrics().density;
        int wdp = o == null ? 0 : o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int hdp = o == null ? 0 : o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (wdp <= 0) wdp = 300;
        if (hdp <= 0) hdp = 300;
        int bw = (int) ((wdp - 16) * dens);
        int bh = (int) ((hdp - 16 - 44) * dens);
        if (bw < 200) bw = 200;
        if (bh < 200) bh = 200;
        double px = (double) bw * bh;
        if (px > 1500000d) {
            double sc = Math.sqrt(1500000d / px);
            bw = (int) (bw * sc);
            bh = (int) (bh * sc);
        }

        HashMap<String, JSONObject> map = new HashMap<String, JSONObject>();
        try {
            JSONArray arr = new JSONArray(sp.getString("widget", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject ob = arr.getJSONObject(i);
                map.put(ob.optString("d"), ob);
            }
        } catch (Exception e) {
            // ignore
        }

        Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        drawCal(new Canvas(bmp), bw, bh, year, month, map, now);

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_work);
        v.setImageViewBitmap(R.id.cal, bmp);
        v.setTextViewText(R.id.title, year + ". " + String.format(Locale.US, "%02d", month + 1));
        v.setOnClickPendingIntent(R.id.prev, bc(c, A_PREV, 1));
        v.setOnClickPendingIntent(R.id.next, bc(c, A_NEXT, 2));
        v.setOnClickPendingIntent(R.id.refresh, bc(c, A_RESET, 3));
        v.setOnClickPendingIntent(R.id.title, bc(c, A_RESET, 4));
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open != null) {
            PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            v.setOnClickPendingIntent(R.id.cal, pi);
            v.setOnClickPendingIntent(R.id.gear, pi);
        }
        mgr.updateAppWidget(id, v);
    }

    static void drawCal(Canvas cv, int w, int h, int year, int month, HashMap<String, JSONObject> map, Calendar now) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float headH = h * 0.075f;
        float cw = w / 7f;
        String[] wd = {"일", "월", "화", "수", "목", "금", "토"};
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(headH * 0.62f);
        for (int i = 0; i < 7; i++) {
            p.setColor(i == 0 ? RED : (i == 6 ? BLUE : GRAY));
            cv.drawText(wd[i], cw * i + cw / 2f, headH * 0.74f, p);
        }
        Calendar first = Calendar.getInstance();
        first.set(year, month, 1, 12, 0, 0);
        int lead = first.get(Calendar.DAY_OF_WEEK) - 1;
        int dim = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (lead + dim + 6) / 7;
        float ch = (h - headH) / rows;
        Calendar d = (Calendar) first.clone();
        d.add(Calendar.DAY_OF_MONTH, -lead);
        String todayKey = key(now);
        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < 7; col++) {
                String k = key(d);
                boolean inMonth = d.get(Calendar.MONTH) == month;
                int dayNum = d.get(Calendar.DAY_OF_MONTH);
                String ds = dayNum == 1
                        ? String.format(Locale.US, "%02d.01", d.get(Calendar.MONTH) + 1)
                        : String.format(Locale.US, "%02d", dayNum);
                drawCell(cv, p, col * cw, headH + r * ch, cw, ch, ds, inMonth, k.equals(todayKey), map.get(k), col);
                d.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
    }

    static void drawCell(Canvas cv, Paint p, float x, float y, float cw, float ch, String dateStr,
                         boolean inMonth, boolean isToday, JSONObject o, int col) {
        float pad = cw * 0.04f;
        int alpha = inMonth ? 255 : 90;
        if (isToday) {
            RectF rf = new RectF(x + pad / 2f, y + pad / 2f, x + cw - pad / 2f, y + ch - pad / 2f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFE3EEFF);
            cv.drawRoundRect(rf, 8f, 8f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, cw * 0.03f));
            p.setColor(0xFF1E6FE8);
            cv.drawRoundRect(rf, 8f, 8f, p);
            p.setStyle(Paint.Style.FILL);
        }
        boolean hol = o != null && o.optInt("h") == 1;
        int dc = (hol || col == 0) ? RED : (col == 6 ? BLUE : DARK);

        float dsz = Math.min(cw * 0.21f, ch * 0.17f);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(dsz);
        float dw = p.measureText(dateStr);
        if (dw > cw * 0.96f) {
            dsz = dsz * cw * 0.96f / dw;
            p.setTextSize(dsz);
            dw = p.measureText(dateStr);
        }
        String hn = o == null ? "" : o.optString("hn", "");
        float lsz = dsz * 0.62f;
        float lw = 0f;
        float gap = dsz * 0.2f;
        if (!hn.isEmpty()) {
            p.setTextSize(lsz);
            lw = p.measureText(hn);
            float avail = cw * 0.96f - dw - gap;
            if (avail > 0 && lw > avail) {
                lsz = lsz * avail / lw;
                lw = avail;
            }
            if (avail <= 0) {
                hn = "";
                lw = 0f;
            }
        }
        float total = dw + (lw > 0 ? gap + lw : 0f);
        float sx = x + (cw - total) / 2f;
        float base = y + ch * 0.05f + dsz;
        p.setTextSize(dsz);
        p.setColor(withAlpha(dc, alpha));
        cv.drawText(dateStr, sx, base, p);
        if (lw > 0) {
            p.setTextSize(lsz);
            p.setColor(withAlpha(RED, alpha));
            cv.drawText(hn, sx + dw + gap, base, p);
        }

        if (o == null) return;
        String s = o.optString("s", "");
        if (!s.isEmpty()) {
            int sc = DARK;
            try {
                sc = Color.parseColor(o.optString("c"));
            } catch (Exception e) {
                // keep default
            }
            float dia = Math.min(cw * 0.66f, ch * 0.40f);
            float cx = x + cw / 2f;
            float cy = y + ch * 0.50f;
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(dia * 0.52f);
            float tw = p.measureText(s);
            float bwid = Math.min(Math.max(dia, tw + dia * 0.45f), cw * 0.96f);
            RectF br = new RectF(cx - bwid / 2f, cy - dia / 2f, cx + bwid / 2f, cy + dia / 2f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(withAlpha(sc, alpha));
            cv.drawRoundRect(br, dia / 2f, dia / 2f, p);
            double lum = (0.299 * Color.red(sc) + 0.587 * Color.green(sc) + 0.114 * Color.blue(sc)) / 255d;
            p.setColor(withAlpha(lum > 0.72 ? DARK : Color.WHITE, alpha));
            p.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = p.getFontMetrics();
            cv.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, p);
        }
        String m = o.optString("m", "");
        if (!m.isEmpty()) {
            float bh2 = ch * 0.17f;
            RectF mr = new RectF(x + pad, y + ch - bh2 - pad, x + cw - pad, y + ch - pad);
            p.setStyle(Paint.Style.FILL);
            p.setColor(withAlpha(0xFF3C3C3C, alpha));
            cv.drawRect(mr, p);
            p.setTypeface(Typeface.DEFAULT);
            p.setTextSize(bh2 * 0.68f);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(withAlpha(Color.WHITE, alpha));
            int n = p.breakText(m, true, mr.width() - 4f, null);
            String mm = n < m.length() ? m.substring(0, n) : m;
            Paint.FontMetrics fm2 = p.getFontMetrics();
            cv.drawText(mm, mr.centerX(), mr.centerY() - (fm2.ascent + fm2.descent) / 2f, p);
        }
    }
}
