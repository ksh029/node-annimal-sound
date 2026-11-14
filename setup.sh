#!/usr/bin/env bash
set -e
npm install
mkdir -p www
cp index.html www/index.html
npx cap add android
DEST=android/app/src/main/java/com/worktable/app
mkdir -p "$DEST"
cp AlarmPlugin.java AlarmScheduler.java AlarmReceiver.java BootReceiver.java WorkWidget.java MainActivity.java "$DEST/"
RES=android/app/src/main/res
mkdir -p "$RES/layout" "$RES/xml" "$RES/drawable"
cp widget_work.xml "$RES/layout/"
cp widget_info.xml "$RES/xml/"
cp widget_bg.xml "$RES/drawable/"
MF=android/app/src/main/AndroidManifest.xml
grep -q SET_ALARM "$MF" || sed -i 's|<application|<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />\n    <application|' "$MF"
grep -q POST_NOTIFICATIONS "$MF" || sed -i 's|<application|<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n    <application|' "$MF"
grep -q USE_FULL_SCREEN_INTENT "$MF" || sed -i 's|<application|<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />\n    <application|' "$MF"
grep -q AlarmReceiver "$MF" || sed -i 's|</application>|<receiver android:name=".AlarmReceiver" android:exported="false" />\n        <receiver android:name=".BootReceiver" android:exported="true"><intent-filter><action android:name="android.intent.action.BOOT_COMPLETED" /></intent-filter></receiver>\n    </application>|' "$MF"
grep -q WorkWidget "$MF" || sed -i 's|</application>|<receiver android:name=".WorkWidget" android:exported="true"><intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /><action android:name="android.intent.action.DATE_CHANGED" /><action android:name="android.intent.action.TIME_SET" /><action android:name="android.intent.action.TIMEZONE_CHANGED" /></intent-filter><meta-data android:name="android.appwidget.provider" android:resource="@xml/widget_info" /></receiver>\n    </application>|' "$MF"
npx cap sync android
