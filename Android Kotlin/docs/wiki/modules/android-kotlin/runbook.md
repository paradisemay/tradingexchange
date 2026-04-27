# Runbook

## Start API Mock

From repository root:

```powershell
cd API
npm run start
```

Check:

```powershell
curl.exe http://localhost:8081/health
```

## Run Android App

1. Open `Android Kotlin/` in Android Studio.
2. Sync Gradle.
3. Choose a networking mode.
4. Run `app`.

Default debug builds use `127.0.0.1:8081` and expect ADB reverse for physical phones:

```powershell
adb reverse tcp:8081 tcp:8081
adb reverse --list
```

For emulator builds, override the URLs:

```powershell
.\gradlew.bat :app:assembleDebug `
  -PAPI_BASE_URL=http://10.0.2.2:8081/ `
  -PWS_BASE_URL=ws://10.0.2.2:8081/api/v1/quotes/ws
```

The emulator reaches the host API mock through:

```text
http://10.0.2.2:8081/
```

## Switching to Ktor

Override build config values:

```powershell
.\gradlew.bat :app:assembleDebug `
  -PAPI_BASE_URL=http://10.0.2.2:8080/ `
  -PWS_BASE_URL=ws://10.0.2.2:8080/api/v1/quotes/ws
```

Ktor must be running with PostgreSQL and Redis.

## Physical Device

For a real phone, replace `10.0.2.2` with the computer LAN IP:

```text
http://192.168.x.x:8081/
ws://192.168.x.x:8081/api/v1/quotes/ws
```

The phone and computer must be on the same network.

If LAN access fails but USB debugging works, prefer:

```powershell
adb reverse tcp:8081 tcp:8081
```

## Manual Smoke

1. Login with default mock credentials.
2. Open portfolio and verify positions/cash.
3. Open instruments and search `SBER`.
4. Open `SBER` chart and switch between `Line` and `Candles`.
5. Change ranges `1m`, `1h`, `1D`, `1W`, `1M`, `6M`, `1Y`.
6. Change intervals `1s`, `1m`, `1h`.
7. Wait for a WebSocket quote and confirm `Last`, point count and chart shape update without reopening the screen.
7. Create BUY order for `SBER`.
8. Return to portfolio and refresh.
9. Open transactions and verify BUY transaction.
10. Keep portfolio open and verify quote price updates.
11. Logout and verify auth screen appears.
