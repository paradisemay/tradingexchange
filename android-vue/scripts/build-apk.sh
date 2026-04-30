#!/usr/bin/env bash
set -euo pipefail

echo "[apk-job] npm install"
npm install

echo "[apk-job] build web bundle"
npm run build

echo "[apk-job] capacitor sync"
npm run cap:sync

echo "[apk-job] gradle assembleDebug"
cd android
./gradlew assembleDebug

echo "[apk-job] APK generated at:"
echo "/workspace/android/app/build/outputs/apk/debug/app-debug.apk"
