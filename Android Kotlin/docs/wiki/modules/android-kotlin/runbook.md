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
3. Start an Android emulator.
4. Run `app`.

The emulator reaches the host API mock through:

```text
http://10.0.2.2:8081/
```

## Switching to Ktor

Change build config fields in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
buildConfigField("String", "WS_BASE_URL", "\"ws://10.0.2.2:8080/api/v1/quotes/ws\"")
```

Ktor must be running with PostgreSQL and Redis.

## Physical Device

For a real phone, replace `10.0.2.2` with the computer LAN IP:

```text
http://192.168.x.x:8081/
ws://192.168.x.x:8081/api/v1/quotes/ws
```

The phone and computer must be on the same network.

## Manual Smoke

1. Login with default mock credentials.
2. Open portfolio and verify positions/cash.
3. Open instruments and search `SBER`.
4. Create BUY order for `SBER`.
5. Return to portfolio and refresh.
6. Open transactions and verify BUY transaction.
7. Keep portfolio open and verify quote price updates.
8. Logout and verify auth screen appears.
