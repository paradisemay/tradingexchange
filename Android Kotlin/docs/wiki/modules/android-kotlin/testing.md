# Testing

## Unit Tests

Current unit tests cover:

- DTO to domain decimal conversion.
- Nullable current price mapping.
- Current Ktor `lastPrice` instrument field.
- Safe enum mapping.
- Basic network error mapping.

Target coverage for new domain/data code is at least 80%. The current MVP has focused mapper/error tests; the next iteration should add ViewModel and repository tests plus a coverage report task before enforcing the threshold in CI.

Run:

```powershell
.\gradlew :app:testDebugUnitTest
```

## ViewModel Tests To Extend

Recommended next tests:

- Login success and validation failure.
- Portfolio cache-first load.
- Instrument search empty and content states.
- Order create success and insufficient funds mapping.
- WebSocket quote update propagated into portfolio state.

## Compose UI Tests To Extend

Recommended scenarios:

- Auth screen quick login.
- Portfolio shows cash and positions.
- Instruments search renders result cards.
- Order form creates market BUY.
- Profile logout clears protected navigation.

## Network Smoke

Run API mock first:

```powershell
cd API
npm run start
```

Then run Android app on emulator and execute the manual smoke from `runbook.md`.
