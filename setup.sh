#!/usr/bin/env bash
set -e
npm install
mkdir -p www
cp index.html www/index.html
npx cap add android
DEST=android/app/src/main/java/com/worktable/app
mkdir -p "$DEST"
cp AlarmPlugin.java AlarmScheduler.java AlarmReceiver.java BootReceiver.java MainActivity.java "$DEST/"
MF=android/app/src/main/AndroidManifest.xml
grep -q SET_ALARM "$MF" || sed -i 's|<application|<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />\n    <application|' "$MF"
grep -q POST_NOTIFICATIONS "$MF" || sed -i 's|<application|<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n    <application|' "$MF"
grep -q USE_FULL_SCREEN_INTENT "$MF" || sed -i 's|<application|<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />\n    <application|' "$MF"
grep -q AlarmReceiver "$MF" || sed -i 's|</application>|<receiver android:name=".AlarmReceiver" android:exported="false" />\n        <receiver android:name=".BootReceiver" android:exported="true"><intent-filter><action android:name="android.intent.action.BOOT_COMPLETED" /></intent-filter></receiver>\n    </application>|' "$MF"
npx cap sync android
