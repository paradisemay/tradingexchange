# ADR-0004: No Runtime Docker Container

## Status

Accepted

## Context

The Android module produces an APK and runs on Android OS. It is not a server process.

## Decision

Do not add a runtime Docker container for the Android app. The API module remains Dockerized. Android may later get a Docker build image for CI only.

## Alternatives

- Ship the Android app in Docker. Rejected because phones/emulators install APKs, not service containers.
- Add a heavy Android SDK Dockerfile now. Deferred because local Android Studio builds already work and the image would be CI infrastructure, not runtime deployment.

## Consequences

- Deployment artifact is `app-debug.apk` or release APK/AAB.
- Docker requirement is satisfied by service modules where runtime containers make sense.
- CI containerization can be added separately.
