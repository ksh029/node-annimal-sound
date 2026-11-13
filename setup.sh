#!/usr/bin/env bash
set -e
npm install
mkdir -p www
cp index.html www/index.html
npx cap add android
DEST=android/app/src/main/java/com/worktable/app
mkdir -p "$DEST"
cp AlarmPlugin.java MainActivity.java "$DEST/"
MF=android/app/src/main/AndroidManifest.xml
grep -q SET_ALARM "$MF" || sed -i 's|<application|<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />\n    <application|' "$MF"
npx cap sync android
