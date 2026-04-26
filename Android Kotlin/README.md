# Android Kotlin Broker Terminal

Native Android client for the TradingExchange project.

## Quick Start

1. Start the API mock server:

```powershell
cd ..\API
npm run start
```

2. Open `Android Kotlin/` in Android Studio.
3. Let Android Studio sync Gradle and install missing SDK packages if prompted.
4. Run the `app` configuration on an Android emulator.

Debug build points to:

```text
REST: http://10.0.2.2:8081/
WS:   ws://10.0.2.2:8081/api/v1/quotes/ws
```

Use the default quick login fields:

```text
email: trader@example.com
password: secret123
```

## Checks

```powershell
.\gradlew :app:assembleDebug
.\gradlew :app:testDebugUnitTest
```

If there is no local Gradle wrapper, run the same tasks from Android Studio Gradle tool window.

## Wiki

Detailed documentation is in:

```text
docs/wiki/modules/android-kotlin/
```
