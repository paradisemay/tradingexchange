# Security Notes

## Rules

- Do not commit real backend URLs, tokens, passwords or keystores.
- Build URLs are provided through Gradle properties or environment variables: `API_BASE_URL`, `WS_BASE_URL`.
- `.env.example` must be updated when a new variable is introduced.
- Access and refresh tokens are stored in DataStore, not hardcoded.
- Money and quantity values are never parsed as `Double` or `Float`.
- UI must map backend `errorCode` to typed client errors and user-facing messages.

## Local Cleartext Policy

Debug builds permit cleartext traffic for local mock and Ktor integration. This is for development only. Before release, production URLs must use HTTPS/WSS and cleartext should be disabled or scoped to debug builds.

## Early Checks

Run before handoff:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Recommended security tools for CI:

- Android Studio App Inspection for runtime checks.
- Android Lint.
- OWASP Dependency-Check, Snyk, GitHub Dependabot or an equivalent dependency scanner.
- Static code scanning for token/secret leaks.
