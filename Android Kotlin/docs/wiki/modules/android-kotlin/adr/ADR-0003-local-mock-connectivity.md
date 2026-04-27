# ADR-0003: Local Mock Connectivity

## Status

Accepted

## Context

Android emulator, physical phones and real Ktor use different host addresses:

- Emulator reaches the host machine through `10.0.2.2`.
- Physical phone over USB can use `adb reverse` and `127.0.0.1`.
- Physical phone over Wi-Fi needs the host LAN IP and firewall access.

## Decision

Read `API_BASE_URL` and `WS_BASE_URL` from Gradle properties or environment variables. Defaults are local debug values for `adb reverse`:

- `http://127.0.0.1:8081/`
- `ws://127.0.0.1:8081/api/v1/quotes/ws`

## Alternatives

- Hardcode `10.0.2.2`. Rejected because it fails on physical phones.
- Hardcode LAN IP. Rejected because it changes per network.

## Consequences

- Developers can override URLs without editing source code:

```powershell
.\gradlew.bat :app:assembleDebug -PAPI_BASE_URL=http://10.0.2.2:8081/ -PWS_BASE_URL=ws://10.0.2.2:8081/api/v1/quotes/ws
```

- `.env.example` documents the expected variable names.
