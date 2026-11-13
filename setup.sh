#!/usr/bin/env bash
set -e
npm install
npx cap add android
DEST=android/app/src/main/java/com/worktable/app
mkdir -p "$DEST"
cp native/AlarmPlugin.java native/MainActivity.java "$DEST/"
MF=android/app/src/main/AndroidManifest.xml
grep -q SET_ALARM "$MF" || sed -i 's|<application|<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />\n    <application|' "$MF"
npx cap sync android
echo "완료. 이제 'npx cap open android' 로 Android Studio를 열어 APK를 빌드하세요."
