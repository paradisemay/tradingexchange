# Android Kotlin Module Plan

## Scope

The Android Kotlin module is a native Android broker terminal. It talks only to the unified API contract through REST and WebSocket.

## Current Iteration

- [x] Single Gradle `app` module.
- [x] Clean Architecture package split: `data`, `domain`, `ui`, `core`.
- [x] MVVM/UDF screens with Compose and Material 3.
- [x] Retrofit/OkHttp REST integration.
- [x] OkHttp WebSocket quotes integration with reconnect/resubscribe.
- [x] Hilt dependency injection.
- [x] DataStore token storage and Room screen cache.
- [x] Error mapping by `errorCode`.
- [x] Obsidian wiki under `docs/wiki/modules/android-kotlin/`.
- [x] ADR section for architecture and networking decisions.
- [x] Build-time configuration through Gradle properties or environment variables.
- [x] Basic unit tests for mapping and error mapping.

## Next Small Tasks

- [ ] Add ViewModel tests for auth, portfolio, instruments and order creation.
- [ ] Add Compose UI tests for login and main navigation.
- [ ] Add Jacoco/Kover coverage report and raise unit coverage toward the 80% target.
- [ ] Add CI build job for `assembleDebug` and `testDebugUnitTest`.
- [ ] Add dependency and APK security scan in CI.
- [ ] Add optional Docker build image for CI if the team standardizes Android builds in containers.

## Commit Policy

Use small logically complete commits in Conventional Commits format:

- `feat(android): add portfolio cache`
- `fix(android): allow debug cleartext for local mock`
- `docs(android): add networking ADR`
- `test(android): cover dto mapper`
